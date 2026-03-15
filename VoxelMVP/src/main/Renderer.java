package main;

import org.joml.Vector3f;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

import bufferManager.SceneBufferManager;
import guiHandler.GUIHelper;
import imgui.ImGui;
import imgui.ImInput;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.util.HashMap;
import java.util.Map;

public class Renderer {
	
	private Map<String, Model> models = new HashMap<String, Model>();
	private TextureCache textureCache = new TextureCache();
	private MaterialCache materialCache = new MaterialCache();
	private SceneBufferManager bufferManager;
	private Projection projection;
	
	private Vector3f renderPos = new Vector3f();
	//private Matrix4f renderMatrix = new Matrix4f();
	private Camera camera;
	private GUIHelper guiHelper;
	
	private Texture textAtlas;
	
	public Renderer () {
		guiHelper = new GUIHelper();
	}
	
	public void bindBufferMananger(SceneBufferManager main) {
		bufferManager = main;
		bufferManager.bind();
	}
	
	public void bindTextureAtlas(Texture texture) {
		textAtlas = texture;
		glActiveTexture(GL_TEXTURE0);
		textAtlas.bind();
	}
	
	public void render(GameState state, double alpha) {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		camera = state.getCamera();
		
		Main.shaderProgram.setUniform("viewMatrix", camera.handleCameraLerpAndMatrix(alpha, renderPos));
		Main.shaderProgram.setUniform("projectionMatrix", projection.getProjMatrix());
		
		for (Chunk chunk : WorldMap.getChunks().values()) {
			if (chunk.allocation == null) continue;
			
			Main.shaderProgram.setUniform("modelMatrix", chunk.modelMatrix);
			
			GL32.glDrawElementsBaseVertex(
		        GL_TRIANGLES,
		        chunk.allocation.indexCount,
		        GL_UNSIGNED_INT,
		        chunk.allocation.indexOffset,
		        chunk.allocation.vertexOffset / bufferManager.getStride()
		    );
			//int error = GL11.glGetError();
			//if (error != 0) System.out.println("GL error after newFrame: " + error);
			//System.out.println("IndexCount: " +chunk.allocation.indexCount+" IndexOffset: "+ chunk.allocation.indexOffset + " VertexOffset: " + chunk.allocation.vertexOffset / 5);
		}
		
		if(!guiHelper.runGUI(state)) {
			camera.updateCameraMatrix(Mouse.getDX(), Mouse.getDY());
		}
		
		gameInput();
		
		Display.update();
	}
	
	public void initDisplay(int width, int height) throws LWJGLException {
        Display.setDisplayMode(new DisplayMode(width, height));
        Display.setTitle("LWJGL 2 Simple 3D Loop");
        
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
        
        glClearColor(0.2f, 0.3f, 0.4f, 1f);
        //GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        Mouse.setGrabbed(true);
        
    }
	
	public void cleanup(){
		Display.destroy();
	}
	
	private void gameInput() {
		while(Keyboard.next()) {
			boolean keyPress = Keyboard.getEventKeyState();
			int keyReference = Keyboard.getEventKey();
			if (keyReference == Keyboard.KEY_ESCAPE) {
				System.exit(0);
			} else if (!ImGui.wantCaptureKeyboard()) {
				Input.inputQueue.add(new Input(keyPress, keyReference));
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
	
	/*
	 * for (Entity entity : entities) {
			renderMatrix = entity.getModelMatrix();
			entity.lerpPos(alpha, renderPos);
	        renderMatrix.translationRotateScale(renderPos, entity.getRotation(), entity.getScale());
	        
	        Main.shaderProgram.setUniform("modelMatrix", renderMatrix);
	        
	        Model model = models.get(entity.getModelId());
			for (Mesh mesh : model.getMeshList()) {
				glActiveTexture(GL_TEXTURE0);
				mesh.getMaterial().getTexture().bind();
				glBindVertexArray(mesh.getVaoId());
				glDrawElements(GL_TRIANGLES, mesh.getNumVertices(), GL_UNSIGNED_INT, 0);
				//System.out.println("drawn something");
				/*
				int error = glGetError();
				if (error != GL_NO_ERROR) {
			        System.out.println("Draw error: " + error);
				}*/
			//}
		//}
	// */
}
