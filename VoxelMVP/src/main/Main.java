package main;

import resourceLoader.ResourceLoader;

import org.joml.Vector3f;
import org.lwjgl.opengl.Display;

import bufferManager.ChunkSSBO;
import bufferManager.SceneBufferManager;
import imgui.ImGui;
import meshThreader.MeshQueue;


public class Main {
	
	public static ShaderProgram shaderProgram;
	public static ComputeProgram computeProgram;
	public static final GameInputQueue gameInput= new GameInputQueue();
	public static final WorldMap gameMap = new WorldMap();
	public static MeshQueue meshThreader;
	public static GameEngine gameEngine;
	public static final Renderer renderer = new Renderer(gameInput);
	public static Thread gameEngineThread;
	public static final TextureCache textureAtlas = new TextureCache();
	public static SceneBufferManager bufferManager;
	public static ChunkSSBO renderTorroid;
	
    public static void main(String[] args) throws InterruptedException {
    	
    	try {
            init();
            bufferManager = new SceneBufferManager();
            renderTorroid = new ChunkSSBO();
            meshThreader = new MeshQueue(bufferManager,renderTorroid);
            renderer.bindBufferMananger(bufferManager);
            renderer.setChunkSSBO(renderTorroid);
            renderer.bindBuffers();
            
            gameEngine = new GameEngine(meshThreader, renderer.getEvictionQueue(), renderTorroid, gameInput);
            gameEngineThread = new Thread(gameEngine);
            
            Main.shaderProgram.bind(); 
            
            Texture atlas = testGameSetup();
            renderer.bindTextureAtlas(atlas);
             
            gameEngineThread.start();
            
            
            while(!Display.isCloseRequested()) {
            	renderer.render(gameEngine.getPublishedState(), gameEngine.getPublishedAlpha());
            }
          
        } catch (Exception excp) {
            excp.printStackTrace();
        } finally {
            gameEngine.running = false;
            gameEngineThread.join();
            ImGui.shutdownOpenGL3();
            shaderProgram.cleanup();
            renderer.cleanup();
        }
    }
    
    public static void init() throws Exception {
    	
    	renderer.initDisplay(1920,1080);
    	
    	computeProgram = new ComputeProgram();
    	computeProgram.createComputeShader(ResourceLoader.loadResourceAsString("resources/cull.glsl"));
    	computeProgram.link();
    	
    	computeProgram.bind();
    	computeProgram.createUniform("slotCount");
    	for (int i = 0; i < 6; i++)
    	    computeProgram.createUniform("frustumPlanes[" + i + "]");
    	computeProgram.unbind();
    	
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(ResourceLoader.loadResourceAsString("resources/vertex.vs"));
        shaderProgram.createFragmentShader(ResourceLoader.loadResourceAsString("resources/fragment.fs"));
        shaderProgram.link();
        
        renderer.createUniforms();
    }
    
    public static Texture testGameSetup() {
    	TextureCache textureCache = renderer.getTextureCache();
    	//MaterialCache materialCache = renderer.getMaterialCache();
    	
        Texture atlas = textureCache.createTexture(0, "resources/homemadeTerrain.png");
        return atlas;
    }
   
}