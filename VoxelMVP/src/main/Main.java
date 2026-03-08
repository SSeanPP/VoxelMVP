package main;

import resourceLoader.ResourceLoader;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.Display;

import bufferManager.SceneBufferManager;
import meshThreader.MeshQueue;
import meshThreader.MeshThread;


public class Main {
	
	public static ShaderProgram shaderProgram;
	public static final WorldMap gameMap = new WorldMap();
	public static MeshQueue meshThreader;
	public static GameEngine gameEngine;
	public static final Renderer renderer = new Renderer();
	public static Thread gameEngineThread;
	public static final TextureCache textureAtlas = new TextureCache();
	public static SceneBufferManager bufferManager;
	
	
	
	private static long lastTime;
	private static float delta;
	private static int fps;
	private static int frames;
	private static long fpsTimer;
	
	
	
    public static void main(String[] args) throws InterruptedException {
    	
    	try {
    		
            init();
            bufferManager = new SceneBufferManager();
            meshThreader = new MeshQueue(bufferManager);
            renderer.bindBufferMananger(bufferManager);
            
            gameEngine = new GameEngine(meshThreader,gameMap);
            gameEngineThread = new Thread(gameEngine);
            
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
            	
            	for(int i = 0; i < 20; i++) {
            		meshThreader.submit(gameMap.getRandomChunk());
            	}
            	
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
    	
        Texture atlas = textureCache.createTexture(0, "bin/resources/homemadeTerrain.png");
        return atlas;
    }
   
}