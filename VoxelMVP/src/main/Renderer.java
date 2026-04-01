package main;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ARBIndirectParameters;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GLSync;

import bufferManager.ChunkSSBO;
import bufferManager.ChunkSSBO.Slot;
import bufferManager.SceneBufferManager;
import guiHandler.GUIHelper;
import imgui.ImGui;
import imgui.ImInput;
import meshThreader.MeshQueue;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Renderer {
	
	private Map<String, Model> models = new HashMap<String, Model>();
	private TextureCache textureCache = new TextureCache();
	private MaterialCache materialCache = new MaterialCache();
	private SceneBufferManager bufferManager;
	private Projection projection;
	
	private Vector3f renderPos = new Vector3f();
	private Matrix4f renderMatrix = new Matrix4f();
	private Camera camera;
	private GUIHelper guiHelper;
	
	private Texture textAtlas;
	private int totalVertices;
	private int totalIndices;

	private GLSync lastFence;
	private final Queue<Slot> evictionQueue = new ConcurrentLinkedQueue<Slot>();
	private MeshQueue meshQueue;
	private ChunkSSBO chunkSSBO;
	private final GameInputQueue gameInputQueue;
	
	public Renderer (GameInputQueue gameInputQueue) {
		guiHelper = new GUIHelper();
		totalVertices = 0;
		totalIndices = 0;
		renderMatrix = new Matrix4f();
		this.gameInputQueue = gameInputQueue;
		
		
	}
	
	public void setChunkSSBO(ChunkSSBO input) {
		this.chunkSSBO = input;
	}
	
	public void bindBufferMananger(SceneBufferManager main) {
		bufferManager = main;
		bufferManager.bind();
	}
	
	public void bindMeshQueue(MeshQueue input) {
		this.meshQueue = input;
	}
	
	public void bindBuffers() {
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, chunkSSBO.getChunkSSBOid());
	    GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, chunkSSBO.getDrawSSBOid());
	    GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, chunkSSBO.getCountBufId());

        GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, chunkSSBO.getDrawSSBOid());
        GL15.glBindBuffer(ARBIndirectParameters.GL_PARAMETER_BUFFER_ARB, chunkSSBO.getCountBufId());
	}
	
	public void bindTextureAtlas(Texture texture) {
		textAtlas = texture;
		glActiveTexture(GL_TEXTURE0);
		textAtlas.bind();
	}
	
	
	public void render(GameState state, double alpha) {
	    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

	    camera = state.getCamera();
	    Matrix4f viewMatrix = camera.handleCameraLerpAndMatrix(alpha, renderPos);

	    Main.shaderProgram.setUniform("viewMatrix", viewMatrix);
	    Main.shaderProgram.setUniform("projectionMatrix", projection.getProjMatrix());
	    Main.shaderProgram.setUniform("cameraPos", renderPos);

	    /*
	    if (lastFence != null) {
	        GL32.glClientWaitSync(lastFence, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, 0);
	        GL32.glDeleteSync(lastFence);
	        lastFence = null;
	    }*/
	    
	    chunkSSBO.resetCount();
	    GL42.glMemoryBarrier(GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
	    Main.computeProgram.bind();
	    Main.computeProgram.setUniform("slotCount", chunkSSBO.getSlotCount());
	    uploadFrustumPlanes(viewMatrix);
	    int groups = (chunkSSBO.getSlotCount() + 63) / 64;
	    GL43.glDispatchCompute(groups, 1, 1);
	    GL42.glMemoryBarrier(GL42.GL_COMMAND_BARRIER_BIT | GL43.GL_SHADER_STORAGE_BARRIER_BIT);
	    Main.shaderProgram.bind();
	    bufferManager.bind();
	    ARBIndirectParameters.glMultiDrawElementsIndirectCountARB(
	        GL_TRIANGLES, GL_UNSIGNED_INT, 0, 0, chunkSSBO.getSlotCount(), 32);
	    
	    
	    
	    //lastFence = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);

	    if (!guiHelper.runGUI(state, 0, 0)) {
	        camera.updateCameraMatrix(Mouse.getDX(), Mouse.getDY());
	    }

	    Display.update();
	    gameInput();
	}
	
   
    
	public void initDisplay(int width, int height) throws LWJGLException {
        Display.setDisplayMode(new DisplayMode(width, height));
        Display.setTitle("VoxelMVP - A maximum performance Voxel Render Engine built on Java 1.6 and LWJGL 2.9.3");
        
        Display.create();
        
        //IMGUI init
        ImGui.createContext();
    	ImGui.setDisplaySize(width, height);
    	ImGui.initOpenGL3();
        
        //Display.setLocation(0, 0);
        System.out.println("OpenGL version: " + GL11.glGetString(GL11.GL_VERSION));
        
        glViewport(0, 0, width, height);        
        
        this.projection = new Projection(width, height);
        //System.out.println(projection.getProjMatrix());
        
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        glEnable(GL11.GL_TEXTURE_2D);
        
        //glClearColor(0.3f, 0.55f, 0.75f, 1f);
        glClearColor(0.2f, 0.3f, 0.4f, 1f);
        //GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        Mouse.setGrabbed(true);
        
    }
	
	public void cleanup(){
		Display.destroy();
	}
	
	private void gameInput() {
		//System.out.println("Producer queue: " + System.identityHashCode(Input.inputQueue));
		while(Keyboard.next()) {
			boolean keyPress = Keyboard.getEventKeyState();
			int keyReference = Keyboard.getEventKey();
			if (keyReference == Keyboard.KEY_ESCAPE) {
				System.exit(0);
			} else if (!ImGui.wantCaptureKeyboard()) {
				gameInputQueue.inputQueue.add(new GameInput(keyPress, keyReference));

				//System.out.println("Adding to queue: " + keyReference);
			} else {
				ImInput.handleKeyboardEvent(keyPress, keyReference);
			}
		}
	}
	
	public void createUniforms() {
		Main.shaderProgram.createUniform("projectionMatrix");
		Main.shaderProgram.createUniform("txtSampler");
		Main.shaderProgram.createUniform("viewMatrix");
		Main.shaderProgram.createUniform("cameraPos");
		Main.shaderProgram.setUniform("txtSampler", 0);
	}
	
	public TextureCache getTextureCache() {
		return this.textureCache;
	}
	
	public MaterialCache getMaterialCache() {
		return this.materialCache;
	}
	
	public void addModel(String name, Model model) {
		models.put(name, model);
	}
	
	public Queue<Slot> getEvictionQueue() {
		return this.evictionQueue;
	}
	
	private void uploadFrustumPlanes(Matrix4f view) {
	    Matrix4f vp = new Matrix4f(projection.getProjMatrix()).mul(view);

	    float[] m = new float[16];
	    vp.get(m);

	    Vector4f[] planes = new Vector4f[6];
	    planes[0] = new Vector4f(m[3]+m[0], m[7]+m[4], m[11]+m[8],  m[15]+m[12]); // left
	    planes[1] = new Vector4f(m[3]-m[0], m[7]-m[4], m[11]-m[8],  m[15]-m[12]); // right
	    planes[2] = new Vector4f(m[3]+m[1], m[7]+m[5], m[11]+m[9],  m[15]+m[13]); // bottom
	    planes[3] = new Vector4f(m[3]-m[1], m[7]-m[5], m[11]-m[9],  m[15]-m[13]); // top
	    planes[4] = new Vector4f(m[3]+m[2], m[7]+m[6], m[11]+m[10], m[15]+m[14]); // near
	    planes[5] = new Vector4f(m[3]-m[2], m[7]-m[6], m[11]-m[10], m[15]-m[14]); // far

	    for (int i = 0; i < 6; i++) {
	        float len = (float)Math.sqrt(
	            planes[i].x*planes[i].x + 
	            planes[i].y*planes[i].y + 
	            planes[i].z*planes[i].z);
	        planes[i].div(len);
	        // adjust for camera-relative rendering
	        planes[i].w -= planes[i].x*renderPos.x + 
	                       planes[i].y*renderPos.y + 
	                       planes[i].z*renderPos.z;
	        Main.computeProgram.setUniform("frustumPlanes[" + i + "]", planes[i]);
	    }
	}
}
