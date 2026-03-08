package main;

import resourceLoader.ResourceLoader;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.Display;

import bufferManager.SceneBufferManager;
import meshThreader.MeshThread;


public class Main {
	
	public static ShaderProgram shaderProgram;
	public static final GameEngine gameEngine = new GameEngine();
	public static final Renderer renderer = new Renderer();
	public static final Thread gameEngineThread = new Thread(gameEngine);
	public static final TextureCache textureAtlas = new TextureCache();
	public static SceneBufferManager bufferManager;
	public static MeshThread meshThreader;
	public static final WorldMap gameMap = new WorldMap();
	
	private static long lastTime;
	private static float delta;
	private static int fps;
	private static int frames;
	private static long fpsTimer;
	
	
	
    public static void main(String[] args) throws InterruptedException {
    	
    	try {
    		
            init();
            bufferManager = new SceneBufferManager();
            meshThreader = new MeshThread(bufferManager);
            renderer.bindBufferMananger(bufferManager);
            
            Runtime rt = Runtime.getRuntime();
            long usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            fpsTimer = System.currentTimeMillis();
            frames = 0;
            
            Texture atlas = testGameSetup();
            renderer.bindTextureAtlas(atlas);
             
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
    	
    	renderer.initDisplay(960,540);
    	
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(ResourceLoader.loadResourceAsString("resources/vertex.vs"));
        shaderProgram.createFragmentShader(ResourceLoader.loadResourceAsString("resources/fragment.fs"));
        shaderProgram.link();
        
        renderer.createUniforms();
    }
    
    public static Texture testGameSetup() {
    	TextureCache textureCache = renderer.getTextureCache();
    	MaterialCache materialCache = renderer.getMaterialCache();
    	
    	int worldSize = 32;
    	int worldHeight = 16;
    	
        for(int x = 0; x <worldSize; x++) {
        	for(int y = 0; y < worldHeight; y++) {
        		for (int z = 0; z < worldSize; z++) {
            		Chunk chunk = new Chunk(ChunkCoord.pack(x,y,-z));
            		gameMap.addToWorldMap(ChunkCoord.pack(x,y,-z), chunk);
            		//meshThreader.meshChunk(chunk);
            	}
        	}
        	
        }
        
        for(int x = 0; x < worldSize; x++) {
    		for(int y = 0; y < worldHeight; y++) {
    			for (int z = 0; z < worldSize; z++) {
    				meshThreader.meshChunk(gameMap.getChunk(ChunkCoord.pack(x,y,-z)));
    			}
    		}
        }
        Texture atlas = textureCache.createTexture(0, "bin/resources/homemadeTerrain.png");
        return atlas;
    }
   
}