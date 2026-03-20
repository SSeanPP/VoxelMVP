package main;

import org.joml.Vector3f;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GLSync;

import bufferManager.SceneBufferManager;
import guiHandler.GUIHelper;
import imgui.ImGui;
import imgui.ImInput;
import meshThreader.MeshQueue;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Renderer {
	
	private Map<String, Model> models = new HashMap<String, Model>();
	private TextureCache textureCache = new TextureCache();
	private MaterialCache materialCache = new MaterialCache();
	private SceneBufferManager bufferManager;
	private Projection projection;
	
	private Vector3f renderPos = new Vector3f();
	//private Matrix4f renderMatrix = new Matrix4f();
	private Camera camera;
	private GUIHelper guiHelper;
	
	private Texture textAtlas;
	private int totalVertices;
	private int totalIndices;

	private GLSync lastFence;
	private final Queue<Chunk> evictionQueue = new ConcurrentLinkedQueue<Chunk>();
	private final Queue<Chunk> chunkQueue = new ConcurrentLinkedQueue<Chunk>();
	private MeshQueue meshQueue;
	private final RenderCache renderCache;
	private final GameInputQueue gameInputQueue;
	
	public Renderer (RenderCache renderCacheInput, GameInputQueue gameInputQueue) {
		guiHelper = new GUIHelper();
		totalVertices = 0;
		totalIndices = 0;
		renderCache = renderCacheInput;
		
		this.gameInputQueue = gameInputQueue;
		
		System.out.println("Input class on renderer: " + gameInputQueue.inputQueue.getClass().getClassLoader());
		//System.out.println("Input FQN on renderer: " + gameInputQueue.class.getName());
		// Both sides
		System.out.println("Queue identity  on renderer: " + System.identityHashCode(gameInputQueue.inputQueue));
	}
	
	public void bindBufferMananger(SceneBufferManager main) {
		bufferManager = main;
		bufferManager.bind();
	}
	
	public void bindMeshQueue(MeshQueue input) {
		this.meshQueue = input;
	}
	
	public void bindTextureAtlas(Texture texture) {
		textAtlas = texture;
		glActiveTexture(GL_TEXTURE0);
		textAtlas.bind();
	}
	
	private final int CHUNK_SHIFT = 4; // 2^4 = 16
	
	private int lastPx = (int)(Settings.spawnChunk.x);
	private int lastPy = (int)(Settings.spawnChunk.y);
	private int lastPz = (int)(Settings.spawnChunk.z);
	
	private int px;
	private int py;
	private int pz;
	
	public void render(GameState state, double alpha) {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		totalVertices = 0;
		totalIndices = 0;
		
		camera = state.getCamera();
		
		Main.shaderProgram.setUniform("viewMatrix", camera.handleCameraLerpAndMatrix(alpha, renderPos));
		Main.shaderProgram.setUniform("projectionMatrix", projection.getProjMatrix());
		
		px = (int)camera.getPosition().x >> CHUNK_SHIFT;
	    py = (int)camera.getPosition().y >> CHUNK_SHIFT;
	    pz = (int)camera.getPosition().z >> CHUNK_SHIFT;
		
		if (px != lastPx || py != lastPy || pz != lastPz) {
		    meshQueue.updatePos(new Vector3f(px, py, pz));
		    
		    updateChunksAroundVector3f(camera.getPosition());
		    
		    lastPx = px;
		    lastPy = py;
		    lastPz = pz;
		}
		
		if (lastFence != null) {
			GL32.glClientWaitSync(lastFence, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, 0);
	        GL32.glDeleteSync(lastFence);
	        lastFence = null;
	        
	        Chunk chunk;
	        
	        while ((chunk = evictionQueue.poll()) != null) {
	        	
	            if (chunk.allocation != null) {
	            	chunk.allocation.setCounts(0);
	                bufferManager.free(chunk.allocation);
	                chunk.allocation = null;
	            }
	            chunk.hasBlocks = false;

            	renderCache.clearSlot(chunk.cacheIndex);
	        }
	        
	        while ((chunk = chunkQueue.poll()) != null) {
	        
	        	if (chunk.previousAllocation != null) {
	        		chunk.previousAllocation.setCounts(0);
	        	}
	        	renderCache.updateTorroid(chunk);
	            meshQueue.submit(chunk);
	        }
		}
		
		for (Chunk chunk : renderCache.getRenderToroid()) {
			if (chunk == null || chunk.allocation == null || !chunk.hasBlocks) continue;
			
			Main.shaderProgram.setUniform("modelMatrix", chunk.modelMatrix);
			
			GL32.glDrawElementsBaseVertex(
		        GL_TRIANGLES,
		        chunk.allocation.indexCount,
		        GL_UNSIGNED_INT,
		        chunk.allocation.indexOffset,
		        chunk.allocation.vertexOffset / Settings.stride
		    );
			totalIndices += chunk.allocation.indexCount;
		    totalVertices += chunk.allocation.indexCount / 6 * 4;
		    
		}
		
		lastFence = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
		
		if(!guiHelper.runGUI(state, totalIndices, totalVertices)) {
			camera.updateCameraMatrix(Mouse.getDX(), Mouse.getDY());
		}
		
		Display.update();
		gameInput();
	}
    
	private long lastDebugTime;
	
    public void updateChunksAroundVector3f(Vector3f position) {
        px = (int)position.x >> CHUNK_SHIFT;
        py = (int)position.y >> CHUNK_SHIFT;
        pz = (int)position.z >> CHUNK_SHIFT;

        if (px == lastPx && py == lastPy && pz == lastPz) return;
        /*
        System.out.println("HashMap size: " + WorldMap.getChunks().size());
        
        if (System.currentTimeMillis() - lastDebugTime > 1000) {
            int withAlloc = 0;
            int withBlocks = 0;
            for (Chunk c : WorldMap.getChunks().values()) {
                if (c.allocation != null) withAlloc++;
                if (c.blocks != null) withBlocks++;
            }
            System.out.println("Chunks: " + WorldMap.getChunks().size() + 
                " withAlloc: " + withAlloc + " withBlocks: " + withBlocks);
            lastDebugTime = System.currentTimeMillis();
        }*/
        
        for (int rx = -Settings.RENDER_DISTANCE; rx <= Settings.RENDER_DISTANCE; rx++) {
            for (int ry = -Settings.RENDER_HEIGHT; ry <= Settings.RENDER_HEIGHT; ry++) {
                for (int rz = -Settings.RENDER_DISTANCE; rz <= Settings.RENDER_DISTANCE; rz++) {
                    int wx = px + rx;
                    int wy = py + ry;
                    int wz = pz + rz;

                    // Get the stable index for this world coordinate
                    int index = renderCache.getIndexAt(wx, wy, wz);
                    
                    Chunk current = renderCache.getRenderToroid()[index];

	                 // Only bother looking up/generating if slot might need updating
	                 if (wy < 0 || wy >= Settings.WORLD_SIZE_HEIGHT) {
	                     if (current != null) {
	                         renderCache.clearSlot(index);
	                         evictionQueue.add(current);
	                     }
	                     continue;
	                 }
	
	                 if (current != null && (current.x != wx || current.y != wy || current.z != wz)) {
                	    renderCache.clearSlot(index);
                	    // Don't add to evictionQueue here - let correct handle the allocation
                	}

                	Chunk correct = WorldMap.getChunkDirect(wx, wy, wz);

                	if (current != correct) {
                	    if (correct != null) {
                	        // Pass current's allocation to correct for reuse/free
                	        correct.previousAllocation = (current != null) ? current.allocation : null;
                	        if (current != null) current.allocation = null; // prevent double free
                	        chunkQueue.add(correct);
                	        WorldMap.removeChunk(WorldMap.key(current != null ? current.x : 0, 
    	                            current != null ? current.y : 0,
    	                            current != null ? current.z : 0));
                	    } else if (current != null) {
                	        // No replacement - eviction queue handles free
                	        current.previousAllocation = null; // ensure no dangling ref
                	        evictionQueue.add(current);
                	        WorldMap.removeChunk(WorldMap.key(current.x, current.y, current.z));
                	    }
                	}
                }
            }
        }
        
       
    }
	
	public void initDisplay(int width, int height) throws LWJGLException {
        Display.setDisplayMode(new DisplayMode(width, height));
        Display.setTitle("VoxelMVP - A maximum performance Voxel Render Engine built on Java 1.6 and LWJGL 2.9.3");
        
        Display.create();
        
        //IMGUI init
        ImGui.createContext();
    	ImGui.setDisplaySize(width, height);
    	ImGui.initOpenGL3();
        
        //Display.setLocation(0, 0);
        System.out.println("OpenGL version: " + GL11.glGetString(GL11.GL_VERSION));
        
        glViewport(0, 0, width, height);        
        
        this.projection = new Projection(width, height);
        //System.out.println(projection.getProjMatrix());
        
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        glEnable(GL11.GL_TEXTURE_2D);
        
        //glClearColor(0.3f, 0.55f, 0.75f, 1f);
        glClearColor(0.2f, 0.3f, 0.4f, 1f);
        //GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        Mouse.setGrabbed(true);
        
    }
	
	public void cleanup(){
		Display.destroy();
	}
	
	private void gameInput() {
		//System.out.println("Producer queue: " + System.identityHashCode(Input.inputQueue));
		while(Keyboard.next()) {
			boolean keyPress = Keyboard.getEventKeyState();
			int keyReference = Keyboard.getEventKey();
			if (keyReference == Keyboard.KEY_ESCAPE) {
				System.exit(0);
			} else if (!ImGui.wantCaptureKeyboard()) {
				gameInputQueue.inputQueue.add(new GameInput(keyPress, keyReference));

				//System.out.println("Adding to queue: " + keyReference);
			} else {
				ImInput.handleKeyboardEvent(keyPress, keyReference);
			}
		}
	}
	
	public void createUniforms() {
		Main.shaderProgram.createUniform("projectionMatrix");
		Main.shaderProgram.createUniform("modelMatrix");
		Main.shaderProgram.createUniform("txtSampler");
		Main.shaderProgram.createUniform("viewMatrix");
		Main.shaderProgram.setUniform("txtSampler", 0);
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
	
	public Queue<Chunk> getEvictionQueue() {
		return this.evictionQueue;
	}
	
	public RenderCache getRenderCache() {
		return this.renderCache;
	}
	
	
	
	
	/*
	 * for (Entity entity : entities) {
			renderMatrix = entity.getModelMatrix();
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
			//}
		//}
	// */
}
