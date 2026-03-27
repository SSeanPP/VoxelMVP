package main;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
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
	
	public void bindTextureAtlas(Texture texture) {
		textAtlas = texture;
		glActiveTexture(GL_TEXTURE0);
		textAtlas.bind();
	}
	
	
	public void render(GameState state, double alpha) {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		totalVertices = 0;
		totalIndices = 0;
		
		camera = state.getCamera();
		
		Main.shaderProgram.setUniform("viewMatrix", camera.handleCameraLerpAndMatrix(alpha, renderPos));
		Main.shaderProgram.setUniform("projectionMatrix", projection.getProjMatrix());
		Main.shaderProgram.setUniform("cameraPos", renderPos);
	

		for (Slot slot : chunkSSBO.getRenderToroid()) {
		    if (slot == null || slot.allocation == null || slot.allocation.getCounts() == 0) continue;

		    renderMatrix.translation(slot.x * Settings.CHUNK_SIZE, slot.y * Settings.CHUNK_SIZE, slot.z * Settings.CHUNK_SIZE);
		    Main.shaderProgram.setUniform("modelMatrix", renderMatrix);

		    GL32.glDrawElementsBaseVertex(
		        GL_TRIANGLES,
		        slot.allocation.indexCount,
		        GL_UNSIGNED_INT,
		        slot.allocation.indexOffset,
		        slot.allocation.vertexOffset / Settings.stride
		    );
		}
		
		
		if(!guiHelper.runGUI(state, totalIndices, totalVertices)) {
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
		Main.shaderProgram.createUniform("modelMatrix");
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
}
