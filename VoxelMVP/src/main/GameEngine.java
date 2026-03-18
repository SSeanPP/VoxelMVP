package main;

import java.util.HashSet;
import java.util.Queue;

import org.joml.Vector3f;
import org.lwjgl.input.Keyboard;

import meshThreader.MeshQueue;

public class GameEngine implements Runnable {
	public volatile boolean running;
	
	private volatile GameState state = new GameState();
	private volatile double alpha = 0.0;
	
	private InputState inputState = new InputState();
	
	private MeshQueue meshQueue;
	
	private final int CHUNK_SHIFT = 4; // 2^4 = 16
	
	private int lastPx = Integer.MIN_VALUE;
	private int lastPy = Integer.MIN_VALUE;
	private int lastPz = Integer.MIN_VALUE;
	
	private HashSet<Chunk> pendingEvictions = new HashSet<Chunk>();
	private Queue<Chunk> evictionQueue;
	private RenderCache renderCache;
	
	private double newTime;
    private double frameTime;
    
    private Vector3f cameraPos;
	
	public GameEngine(MeshQueue queue, Queue<Chunk> evictionQueueFromRenderer, RenderCache renderCacheFromRenderer) {
		meshQueue = queue;
		evictionQueue = evictionQueueFromRenderer;
		renderCache = renderCacheFromRenderer;
		
		System.out.println("Input class on game engine: " + GameInput.inputQueue.getClass().getClassLoader());
		System.out.println("Input FQN  on game engine: " + GameInput.class.getName());
		System.out.println("Queue identity  on gameEngine: " + System.identityHashCode(GameInput.inputQueue));
	}
	
	public void gameLoop() {
		double t = 0.0;
	    double dt = 1.0 / 20.0;

	    double currentTime = System.nanoTime() /1000000000.0;
	    double accumulator = 0.0;
	    
	    while ( running )
	    {
	    	//System.out.println("running");
	    	
	    	newTime = System.nanoTime() / 1000000000.0;
	        frameTime = (newTime - currentTime);
	        if ( frameTime > 0.25 )
	            frameTime = 0.25;
	        currentTime = newTime;

	        accumulator += frameTime;
	        
	        while ( accumulator >= dt )
	        {
	        	// Responsible for random chunk updates
	        	/*chunksProcessed = 0;
	        	loopTotal = 0;
	        	while(chunksProcessed < 20 && loopTotal != renderSize) {
	        		if(meshQueue.submit(worldMap.getRandomChunk())) {
	        			this.chunksProcessed++;
	        		}
	        		loopTotal++;
	        	}*/

		        drainInputQueue();
	        	updateChunksAroundPlayer(state);
	        	
	        	state.integrate(t, dt, inputState);
	        	
	            t += dt;
	            accumulator -= dt;
	        }

	        this.alpha = accumulator / dt;
	        //System.out.println("Alpha: " + alpha); 
	    }
	}
	
	public GameState getPublishedState() {
	    return state;
	}
	
	public double getPublishedAlpha() {
		return alpha;
	}
	
	private void drainInputQueue() {
		//System.out.println("Queue class: " + Input.inputQueue.getClass().getName());
	    //System.out.println("Queue size: " + Input.inputQueue.size());
	    //System.out.println("Queue identity: " + System.identityHashCode(Input.inputQueue));
		System.out.println("Drain called, size: " + GameInput.inputQueue.size());
	    GameInput input;
        while ((input = GameInput.inputQueue.poll()) != null) {
        	System.out.println("Draining: " + input.getEventKey());
             switch (input.getEventKey()) {
             
                case Keyboard.KEY_W:
                    inputState.forward = input.getEventState();
                    break;
                case Keyboard.KEY_S:
                    inputState.backward = input.getEventState();
                    break;
                case Keyboard.KEY_A:
                    inputState.left = input.getEventState();
                    break;
                case Keyboard.KEY_D:
                    inputState.right = input.getEventState();
                    break;
                case Keyboard.KEY_SPACE:
                    inputState.down = input.getEventState();
                    break;
                case Keyboard.KEY_LSHIFT:
                    inputState.up = input.getEventState();
                    break;
                case Keyboard.KEY_F3:
                	if(input.getEventState()) {
                    	state.setF3state(!state.getF3state());
                	}
                	break;
                case Keyboard.KEY_F4:
                	if(input.getEventState()) {
                    	state.setF4state(!state.getF4state());
                	}
                	break;
            }
        }
    }
	
	public void updateChunksAroundPlayer(GameState state) {
		updateChunksAroundVector3f(state.getCamera().getPosition());
	}
	
	public void updateChunksAroundVector3f(Vector3f position) {
	    cameraPos = position;

	    int px = (int)cameraPos.x >> CHUNK_SHIFT;
	    int py = (int)cameraPos.y >> CHUNK_SHIFT;
	    int pz = (int)cameraPos.z >> CHUNK_SHIFT;

	    
	    if (px == lastPx && pz == lastPz && py == lastPy) {
	        return;
	    }
	    
	    int dx = px - lastPx;
	    int dy = py - lastPy;
	    int dz = pz - lastPz;
	    
	    pendingEvictions.clear();
	    
	    if (dx > 0) {
	        for (int step = 0; step < dx; step++) {
	            int evictX = lastPx + step - Settings.RENDER_DISTANCE;
	            int loadX  = lastPx + step + Settings.RENDER_DISTANCE + 1;
	            for (int y = py - Settings.RENDER_HEIGHT; y <= py + Settings.RENDER_HEIGHT; y++) {
	                for (int z = pz - Settings.RENDER_DISTANCE; z <= pz + Settings.RENDER_DISTANCE; z++) {
	                	addEviction(evictX, y, z, loadX, y, z);
	                }
	            }
	        }
	    } else if (dx < 0) {
	        for (int step = 0; step > dx; step--) {
	            int evictX = lastPx + step + Settings.RENDER_DISTANCE;
	            int loadX  = lastPx + step - Settings.RENDER_DISTANCE - 1;
	            for (int y = py - Settings.RENDER_HEIGHT; y <= py + Settings.RENDER_HEIGHT; y++) {
	                for (int z = pz - Settings.RENDER_DISTANCE; z <= pz + Settings.RENDER_DISTANCE; z++) {
	                	addEviction(evictX, y, z, loadX, y, z);
	                }
	            }
	        }
	    }
	    
	 // Handle Y slabs
	    if (dy > 0) {
	        for (int step = 0; step < dy; step++) {
	            int evictY = lastPy + step - Settings.RENDER_HEIGHT;
	            int loadY  = lastPy + step + Settings.RENDER_HEIGHT + 1;
	            for (int x = px - Settings.RENDER_DISTANCE; x <= px + Settings.RENDER_DISTANCE; x++) {
	                for (int z = pz - Settings.RENDER_DISTANCE; z <= pz + Settings.RENDER_DISTANCE; z++) {
	                	addEviction(x, evictY, z, x, loadY, z);
	                }
	            }
	        }
	    } else if (dy < 0) {
	        for (int step = 0; step > dy; step--) {
	            int evictY = lastPy + step + Settings.RENDER_HEIGHT;
	            int loadY  = lastPy + step - Settings.RENDER_HEIGHT - 1;
	            for (int x = px - Settings.RENDER_DISTANCE; x <= px + Settings.RENDER_DISTANCE; x++) {
	                for (int z = pz - Settings.RENDER_DISTANCE; z <= pz + Settings.RENDER_DISTANCE; z++) {
	                	addEviction(x, evictY, z, x, loadY, z);
	                }
	            }
	        }
	    }

	    // Handle Z slabs — exclude X overlap to avoid double-queuing corners
	    if (dz > 0) {
	        for (int step = 0; step < dz; step++) {
	            int evictZ = lastPz + step - Settings.RENDER_DISTANCE;
	            int loadZ  = lastPz + step + Settings.RENDER_DISTANCE + 1;
	            for (int x = px - Settings.RENDER_DISTANCE + Math.abs(dx); x <= px + Settings.RENDER_DISTANCE - Math.abs(dx); x++) {
	                for (int y = py - Settings.RENDER_HEIGHT; y <= py + Settings.RENDER_HEIGHT; y++) {
	                	addEviction(x, y, evictZ, x, y, loadZ);
	                }
	            }
	        }
	    } else if (dz < 0) {
	        for (int step = 0; step > dz; step--) {
	            int evictZ = lastPz + step + Settings.RENDER_DISTANCE;
	            int loadZ  = lastPz + step - Settings.RENDER_DISTANCE - 1;
	            for (int x = px - Settings.RENDER_DISTANCE + Math.abs(dx); x <= px + Settings.RENDER_DISTANCE - Math.abs(dx); x++) {
	                for (int y = py - Settings.RENDER_HEIGHT; y <= py + Settings.RENDER_HEIGHT; y++) {
	                	addEviction(x, y, evictZ, x, y, loadZ);
	                }
	            }
	        }
	    }
	    
	    renderCache.updateChunkPos(cameraPos);
	    
	    for (Chunk chunk : pendingEvictions) {
	    	chunk.needsUpdate = true;
	    	evictionQueue.add(chunk);
	    }
	    
	    lastPx = px;
	    lastPy = py;
	    lastPz = pz;
	}
	
	@Deprecated
	private void submitChunksInRadius(int px, int py, int pz) {
	    for (int r = 0; r <= Settings.RENDER_DISTANCE; r++) {
	        for (int x = -r; x <= r; x++) {
	            for (int z = -r; z <= r; z++) {
	                if (Math.max(Math.abs(x), Math.abs(z)) != r) continue;

	                int cx = px + x;
	                int cz = pz + z;

	                for (int cy = 0; cy < 16; cy++) {
	                    Chunk chunk = WorldMap.getChunkDirect(cx,cy,cz);
	                    if (chunk != null && chunk.needsUpdate && !chunk.queuedForMeshing) {
	                        meshQueue.submit(chunk);
	                    }
	                }
	            }
	        }
	    }
	}
	
	// Only add to pendingEvictions if newChunk not already present
	private void addEviction(int evictX, int evictY, int evictZ, int loadX, int loadY, int loadZ) {
	    Chunk newChunk = WorldMap.getChunkDirect(loadX, loadY, loadZ);
	    if (newChunk == null) return; // out of world bounds
	    
	    // HashSet.add() returns false if already present - skip if so
	    if (pendingEvictions.add(newChunk)) {
	        Chunk oldChunk = WorldMap.getChunkDirect(evictX, evictY, evictZ);
	        newChunk.previous = oldChunk; // safe - only written once per frame
	    }
	}
	
	
	@Override
	public void run() {
		this.running = true;
		gameLoop();
	}
}
