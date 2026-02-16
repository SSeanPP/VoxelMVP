package main;

import resourceLoader.ResourceLoader;
import static org.lwjgl.opengl.GL11.*;

public class Main {
	
	public static ShaderProgram shaderProgram;
	public static final Renderer renderer = new Renderer();
	public static final GameEngine gameEngine = new GameEngine();
	public static final Thread gameEngineThread = new Thread(gameEngine);
	
    public static void main(String[] args) throws InterruptedException {
    	try {
    		
            init();
            gameEngineThread.start();
            
            while ()
            
        } catch (Exception excp) {
            excp.printStackTrace();
        } finally {
            gameEngine.running = false;
            gameEngineThread.join();
            renderer.cleanup();
        }
    }
    
    public static void init() throws Exception {
    	
    	renderer.initDisplay();
    	
    	glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    	
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(ResourceLoader.loadResourceAsString("resources/vertex.vs"));
        shaderProgram.createFragmentShader(ResourceLoader.loadResourceAsString("resources/fragment.fs"));
        shaderProgram.link();
        
        glClearColor(0.2f, 0.3f, 0.4f, 1f);

        //setPerspective(70f, 1280f / 720f, 0.1f, 1000f);
        
    }
    
    
    
}