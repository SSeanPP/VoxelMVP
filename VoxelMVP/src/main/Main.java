package main;

import resourceLoader.ResourceLoader;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.Display;


public class Main {
	
	public static ShaderProgram shaderProgram;
	public static final Renderer renderer = new Renderer();
	public static final GameEngine gameEngine = new GameEngine();
	public static final Thread gameEngineThread = new Thread(gameEngine);
	public static final TextureCache textureAtlas = new TextureCache();
	
	private static long lastTime;
	private static float delta;
	private static int fps;
	private static int frames;
	private static long fpsTimer;
	
	
	
    public static void main(String[] args) throws InterruptedException {
    	
    	try {
    		
            init();
            
            Runtime rt = Runtime.getRuntime();
            long usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            fpsTimer = System.currentTimeMillis();
            frames = 0;
            
            testGameSetup();
             
            gameEngineThread.start();
            
            while(!Display.isCloseRequested()) {
            	long now = System.nanoTime();
            	delta = (now - lastTime) / 1000000000f;
            	lastTime = now;
            	
            	Main.shaderProgram.bind();
            	renderer.render(gameEngine.getPublishedState(), gameEngine.getPublishedAlpha());
            	
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
    
    public static void testGameSetup() {
    	TextureCache textureCache = renderer.getTextureCache();
    	MaterialCache materialCache = renderer.getMaterialCache();
    	
    	Texture textureTest = textureCache.createTexture(0, "bin/resources/textureTest(1).png");
    	Material materialTest = materialCache.createMaterial("test", textureTest);
    	
    	float[] vertexData = new float[]{
    		    // Top face
    		    -0.5f, 0.5f, -0.5f,  0.0f, 0.5f,   // V0
    		    0.5f, 0.5f, -0.5f,   0.5f, 0.5f,   // V1
    		    -0.5f, 0.5f, 0.5f,   0.0f, 1.0f,   // V2
    		    0.5f, 0.5f, 0.5f,    0.5f, 1.0f,   // V3

    		    // Front face (z = 0.5)
    		    -0.5f, 0.5f, 0.5f,   0.6f, 1f,     // V4
    		    0.5f, 0.5f, 0.5f,    1f, 1f,       // V5
    		    0.5f, -0.5f, 0.5f,   1f, 0.5f,     // V6
    		    -0.5f, -0.5f, 0.5f,  0.6f, 0.5f,   // V7

    		    // Bottom face
    		    -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,   // V8
    		    0.5f, -0.5f, -0.5f,  0.5f, 0.0f,   // V9
    		    -0.5f, -0.5f, 0.5f,  0.0f, 0.5f,   // V10
    		    0.5f, -0.5f, 0.5f,   0.5f, 0.5f,   // V11

    		    // Back face (z = -0.5)
    		    -0.5f, 0.5f, -0.5f,  0.6f, 1f,     // V12
    		    0.5f, 0.5f, -0.5f,   1f, 1f,       // V13
    		    0.5f, -0.5f, -0.5f,  1f, 0.5f,     // V14
    		    -0.5f, -0.5f, -0.5f, 0.6f, 0.5f,   // V15

    		    // Left face (x = -0.5)
    		    -0.5f, 0.5f, -0.5f,  0.6f, 1f,     // V16
    		    -0.5f, 0.5f, 0.5f,   1f, 1f,       // V17
    		    -0.5f, -0.5f, 0.5f,  1f, 0.5f,     // V18
    		    -0.5f, -0.5f, -0.5f, 0.6f, 0.5f,   // V19

    		    // Right face (x = 0.5)
    		    0.5f, 0.5f, -0.5f,   0.6f, 1f,     // V20
    		    0.5f, 0.5f, 0.5f,    1f, 1f,       // V21
    		    0.5f, -0.5f, 0.5f,   1f, 0.5f,     // V22
    		    0.5f, -0.5f, -0.5f,  0.6f, 0.5f,   // V23
    		};

    		int[] indices = new int[]{
    		    3, 1, 2, 2, 1, 0,       // Top
    		    4, 7, 6, 6, 5, 4,       // Front
    		    8, 9, 11, 11, 10, 8,    // Bottom
    		    13, 14, 15, 15, 12, 13, // Back
    		    16, 19, 18, 18, 17, 16, // Left
    		    21, 22, 23, 23, 20, 21  // Right
    		};
    		
        
        List<Mesh> meshList = new ArrayList<Mesh>();
        Mesh mesh = new Mesh(vertexData, indices, materialTest);
        meshList.add(mesh);
        String cubeModelId = "cube-model";
        Model model = new Model(cubeModelId, meshList);
        renderer.addModel(model.getId(), model);
        
        renderer.addModel(model.getId(), model);
        for(int x = 0; x < 32; x++) {
        	for (int y = 0; y < 32; y++) {
        		for (int z = 1; z < 33; z++) {
        			Entity cubeEntity = new Entity("cube-entity"+x+y+z, cubeModelId);
    				cubeEntity.setPosition(x, y, -z);
    				gameEngine.getPublishedState().addEntity(cubeEntity);
        		}
        	}
        }
        
        
        
        
    }
   
}