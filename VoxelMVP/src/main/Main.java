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
    	
    	renderer.initDisplay(1280,720);
    	
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(ResourceLoader.loadResourceAsString("resources/vertex.vs"));
        shaderProgram.createFragmentShader(ResourceLoader.loadResourceAsString("resources/fragment.fs"));
        shaderProgram.link();
        
        renderer.createUniforms();
    }
    
    public static void addEntityToGame(Model model, Entity entity) {
    	renderer.addModel(entity.getModelId(), model);
    	gameEngine.getPublishedState().addEntity(entity);
    }
    
    public static void testGameSetup() {
    	TextureCache textureCache = renderer.getTextureCache();
    	MaterialCache materialCache = renderer.getMaterialCache();
    	
    	Texture textureTest = textureCache.createTexture(0, "bin/resources/textureTest(1).png");
    	Material materialTest = materialCache.createMaterial("test", textureTest);
    	
    	float[] positions = new float[]{
                // V0
                -0.5f, 0.5f, 0.5f,
                // V1
                -0.5f, -0.5f, 0.5f,
                // V2
                0.5f, -0.5f, 0.5f,
                // V3
                0.5f, 0.5f, 0.5f,
                // V4
                -0.5f, 0.5f, -0.5f,
                // V5
                0.5f, 0.5f, -0.5f,
                // V6
                -0.5f, -0.5f, -0.5f,
                // V7
                0.5f, -0.5f, -0.5f,

                // For text coords in top face
                // V8: V4 repeated
                -0.5f, 0.5f, -0.5f,
                // V9: V5 repeated
                0.5f, 0.5f, -0.5f,
                // V10: V0 repeated
                -0.5f, 0.5f, 0.5f,
                // V11: V3 repeated
                0.5f, 0.5f, 0.5f,

                // For text coords in right face
                // V12: V3 repeated
                0.5f, 0.5f, 0.5f,
                // V13: V2 repeated
                0.5f, -0.5f, 0.5f,

                // For text coords in left face
                // V14: V0 repeated
                -0.5f, 0.5f, 0.5f,
                // V15: V1 repeated
                -0.5f, -0.5f, 0.5f,

                // For text coords in bottom face
                // V16: V6 repeated
                -0.5f, -0.5f, -0.5f,
                // V17: V7 repeated
                0.5f, -0.5f, -0.5f,
                // V18: V1 repeated
                -0.5f, -0.5f, 0.5f,
                // V19: V2 repeated
                0.5f, -0.5f, 0.5f,
        };
        float[] textCoords = new float[]{
        		//front
                0.0f, 0.0f,
                0.0f, 0.5f,
                0.5f, 0.5f,
                0.5f, 0.0f,
                //back
                0.0f, 0.0f,
                0.5f, 0.0f,
                0.0f, 0.5f,
                0.5f, 0.5f,

                // For text coords in top face
                0.0f, 0.5f,
                0.5f, 0.5f,
                0.0f, 1.0f,
                0.5f, 1.0f,

                // For text coords in right face
                0.0f, 0.0f,
                0.0f, 0.5f,

                // For text coords in left face
                0.5f, 0.0f,
                0.5f, 0.5f,

                // For text coords in bottom face
                0.5f, 0.0f,
                1.0f, 0.0f,
                0.5f, 0.5f,
                1.0f, 0.5f,
        };
        int[] indices = new int[]{
        		// Front face (z = 0.5)
        	    0, 1, 3, 3, 1, 2,
        	    
        	    // Top Face (y = 0.5)
        	    8, 10, 9, 9, 10, 11,
        	    
        	    // Right face (x = 0.5)
        	    12, 13, 5, 5, 13, 7,
        	    
        	    // Left face (x = -0.5)
        	    4, 6, 14, 14, 6, 15,
        	    
        	    // Bottom face (y = -0.5)
        	    18, 16, 19, 19, 16, 17,
        	    
        	    // Back face (z = -0.5)
        	    5, 7, 4, 4, 7, 6,};
        
        List<Mesh> meshList = new ArrayList<Mesh>();
        Mesh mesh = new Mesh(positions, textCoords, indices, materialTest);
        meshList.add(mesh);
        String cubeModelId = "cube-model";
        Model model = new Model(cubeModelId, meshList);
        
        Entity cubeEntity = new Entity("cube-entity", cubeModelId);
        cubeEntity.setPosition(0, 0, -5);
        
        addEntityToGame(model, cubeEntity);
       
    }
   
}