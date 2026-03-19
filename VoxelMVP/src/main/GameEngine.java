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
	
	private int lastPx = (int)(Settings.spawnChunk.x);
	private int lastPy = (int)(Settings.spawnChunk.y);
	private int lastPz = (int)(Settings.spawnChunk.z);
	
	private HashSet<Chunk> pendingEvictions = new HashSet<Chunk>();
	private Queue<Chunk> evictionQueue;
	private RenderCache renderCache;
	private GameInputQueue gameInputQueue;
	
	private double newTime;
    private double frameTime;
    
    
    private Vector3f cameraPos;
	
	public GameEngine(MeshQueue queue, Queue<Chunk> evictionQueueFromRenderer, RenderCache renderCacheFromRenderer, GameInputQueue inputQueue) {
		meshQueue = queue;
		evictionQueue = evictionQueueFromRenderer;
		renderCache = renderCacheFromRenderer;
		this.gameInputQueue = inputQueue;
		
		//System.out.println("Input class on game engine: " + GameInput.inputQueue.getClass().getClassLoader());
		//System.out.println("Input FQN  on game engine: " + GameInput.class.getName());
		//System.out.println("Queue identity  on gameEngine: " + System.identityHashCode(GameInput.inputQueue));
	}
	
	public void gameLoop() {
		double t = 0.0;
	    double dt = 1.0 / 20.0;

	    double currentTime = System.nanoTime() /1000000000.0;
	    double accumulator = 0.0;
	    
	    while ( running )
	    {
	    	drainInputQueue();
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

		        
	        	//updateChunksAroundPlayer(state);
	        	
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
		//System.out.println("Drain called, size: " + gameInputQueue.inputQueue.size());
	    GameInput input;
	   
	    
        while ((input = gameInputQueue.inputQueue.poll()) != null) {
        	//System.out.println("Draining: " + input.getEventKey());
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
	
	
	
	@Deprecated
	/*private void submitChunksInRadius(int px, int py, int pz) {
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
	}*/
	
	
	
	@Override
	public void run() {
		this.running = true;
		gameLoop();
	}
}
