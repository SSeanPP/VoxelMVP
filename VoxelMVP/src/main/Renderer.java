package main;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Renderer {
	
	private Map<String, Model> models = new HashMap<String, Model>();
	private TextureCache textureCache = new TextureCache();
	private MaterialCache materialCache = new MaterialCache();
	
	private Projection projection;
	
	private Vector3f renderPos = new Vector3f();
	private Matrix4f renderMatrix = new Matrix4f();
	
	public Renderer () {
		
	}
	
	public void render(GameState state, double alpha) {
		
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		Main.shaderProgram.bind();
		Main.shaderProgram.setUniform("projectionMatrix", projection.getProjMatrix());
		Main.shaderProgram.setUniform("txtSampler", 0);
		
				
		ArrayList<Entity> entities = state.getEntityList();
		
		for (Entity entity : entities) {
			
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
			}
		}
		Main.shaderProgram.unbind();
		
		gameInput();
		Display.update();
	}
	
	public void initDisplay(int width, int height) throws LWJGLException {
        Display.setDisplayMode(new DisplayMode(width, height));
        Display.setTitle("LWJGL 2 Simple 3D Loop");
        Display.create();
        
        glViewport(0, 0, width, height);
        
        this.projection = new Projection(width, height);
        //System.out.println(projection.getProjMatrix());
        
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        glEnable(GL11.GL_TEXTURE_2D);
        
        glClearColor(0.2f, 0.3f, 0.4f, 1f);
    }
	
	public void cleanup(){
		Display.destroy();
	}
	
	private void gameInput() {
		while(Keyboard.next()) {
			Input.inputQueue.add(new Input(Keyboard.getEventKeyState(), Keyboard.getEventKey()));
		}
		
	}
	
	public void createUniforms() {
		Main.shaderProgram.createUniform("projectionMatrix");
		Main.shaderProgram.createUniform("modelMatrix");
		Main.shaderProgram.createUniform("txtSampler");
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
}
