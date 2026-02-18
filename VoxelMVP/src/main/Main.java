package main;

import resourceLoader.ResourceLoader;
import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.opengl.Display;

public class Main {
	
	public static ShaderProgram shaderProgram;
	public static final Renderer renderer = new Renderer();
	public static final GameEngine gameEngine = new GameEngine();
	public static final Thread gameEngineThread = new Thread(gameEngine);
	
	private static long lastTime;
	private static float delta;
	private static int fps;
	private static int frames;
	private static long fpsTimer;
	
	
	
    public static void main(String[] args) throws InterruptedException {
    	
    	try {
    		
            init();
            gameEngineThread.start();
            Runtime rt = Runtime.getRuntime();
            long usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            fpsTimer = System.currentTimeMillis();
            frames = 0;
            
            float[] positions = new float[]{
                    -0.5f, 0.5f, -2f,
                    -0.5f, -0.5f, -2f,
                    0.5f, -0.5f, -2f,
                    0.5f, 0.5f, -2f,
            };
            float[] colors = new float[]{
                    0.5f, 0.0f, 0.0f,
                    0.0f, 0.5f, 0.0f,
                    0.0f, 0.0f, 0.5f,
                    0.0f, 0.5f, 0.5f,
            };
            int[] indices = new int[]{
                    0, 1, 3, 3, 1, 2,
            };
            Mesh mesh = new Mesh(positions, colors, indices);
            renderer.addMesh(mesh);
            
            while(!Display.isCloseRequested()) {
            	long now = System.nanoTime();
            	delta = (now - lastTime) / 1000000000f;
            	lastTime = now;
            	
            	renderer.render(gameEngine.getPublishedState());
            	
            	frames++;
            	if (System.currentTimeMillis() - fpsTimer >= 1000) {
            	    fps = frames;
            	    frames = 0;
            	    fpsTimer += 1000;
            	    usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            	    Display.setTitle("FPS: " + fps + " | Used MB: " + usedMB);
            	}
            }
          
        } catch (Exception excp) {
            excp.printStackTrace();
        } finally {
            gameEngine.running = false;
            gameEngineThread.join();
            shaderProgram.cleanup();
            renderer.cleanup();
            
        }
    }
    
    public static void init() throws Exception {
    	
    	renderer.initDisplay(1920,1080);
    	
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(ResourceLoader.loadResourceAsString("resources/vertex.vs"));
        shaderProgram.createFragmentShader(ResourceLoader.loadResourceAsString("resources/fragment.fs"));
        shaderProgram.link();
        
        renderer.createUniforms();
    }
   
}