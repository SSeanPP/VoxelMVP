package main;

import resourceLoader.ResourceLoader;
import static org.lwjgl.opengl.GL11.*;

public class Main {
	
	public static ShaderProgram shaderProgram;
	
    public static void main(String[] args) {
    	try {
    		
            init();
            //gameLoop();
            
        } catch (Exception excp) {
            excp.printStackTrace();
        } finally {
            Renderer.cleanup();
        }
    }
    
    public static void init() throws Exception {
    	
    	Renderer.initDisplay();
    	
    	glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    	
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(ResourceLoader.loadResourceAsString("/resources/vertex.vs"));
        shaderProgram.createFragmentShader(ResourceLoader.loadResourceAsString("/resources/fragment.fs"));
        shaderProgram.link();
        
        glClearColor(0.2f, 0.3f, 0.4f, 1f);

        //setPerspective(70f, 1280f / 720f, 0.1f, 1000f);
        
    }
    
    
    
}