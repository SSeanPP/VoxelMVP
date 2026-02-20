package main;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Renderer {
	
	private List<Mesh> meshes = new ArrayList<Mesh>();
	private Projection projection;
	
	public Renderer () {
		
	}
	
	public void render(GameState state) {
		
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		Main.shaderProgram.bind();
		Main.shaderProgram.setUniform("projectionMatrix", projection.getProjMatrix());
		
		Collection<Model> models = state.getModelMap().values();
		
		for (Model model : models) {
			for (Mesh mesh : model.getMeshList()) {
				glBindVertexArray(mesh.getVaoId());
				for (Entity entity : model.getEntitiesList()) {
					Main.shaderProgram.setUniform("modelMatrix", entity.getModelMatrix());
					glDrawElements(GL_TRIANGLES, mesh.getNumVertices(), GL_UNSIGNED_INT, 0);
					//System.out.println("drawn something");
					int error = glGetError();
					if (error != GL_NO_ERROR) {
				        System.out.println("Draw error: " + error);
					}
					
			    }
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
        
        glClearColor(0.2f, 0.3f, 0.4f, 1f);
    }
	
	public void cleanup(){
		for(Mesh mesh : meshes) {
			mesh.cleanup();
		}
		Display.destroy();
	}
	
	private void gameInput() {
		while(Keyboard.next()) {
			Input.inputQueue.add(new Input(Keyboard.getEventKeyState(), Keyboard.getEventKey()));
		}
		
	}
	
	public void addMesh(Mesh mesh) {
		meshes.add(mesh);
	}
	
	public void createUniforms() {
		Main.shaderProgram.createUniform("projectionMatrix");
		Main.shaderProgram.createUniform("modelMatrix");
	}
}
