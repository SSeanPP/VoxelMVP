package main;

import org.joml.Vector3f;
import org.lwjgl.input.Keyboard;

import meshThreader.MeshQueue;

public class GameEngine implements Runnable {
	public volatile boolean running;
	
	private volatile GameState state = new GameState();
	private volatile double alpha = 0.0;
	
	private InputState inputState = new InputState();
	
	private MeshQueue meshQueue;
	
	private WorldMap worldMap;
	private final int CHUNK_SHIFT = 4; // 2^4 = 16
	
	private int lastPx = Integer.MIN_VALUE;
	private int lastPz = Integer.MIN_VALUE;
	private static int VIEW_RADIUS = 64; // tune this
	
	private double newTime;
    private double frameTime;
    
    private Vector3f cameraPos;
	
	public GameEngine(MeshQueue queue, WorldMap map) {
		meshQueue = queue;
		worldMap = map;
		
	}
	
	public void gameLoop() {
		double t = 0.0;
	    double dt = 1.0 / 20.0;

	    double currentTime = System.nanoTime() /1000000000.0;
	    double accumulator = 0.0;
	    
	    while ( running )
	    {
	    	//System.out.println("running");
	    	drainInputQueue();
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
        Input input;
        while ((input = Input.inputQueue.poll()) != null) {
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
	    cameraPos = state.getCamera().getPosition();

	    int px = (int)cameraPos.x >> CHUNK_SHIFT;
	    int py = (int)cameraPos.y >> CHUNK_SHIFT;
	    int pz = (int)cameraPos.z >> CHUNK_SHIFT;

	    if (px == lastPx && pz == lastPz) {
	        return;
	    }
	    lastPx = px;
	    lastPz = pz;

	    submitChunksInRadius(px, py, pz);
	}
	
	private void submitChunksInRadius(int px, int py, int pz) {
	    for (int r = 0; r <= VIEW_RADIUS; r++) {
	        for (int x = -r; x <= r; x++) {
	            for (int z = -r; z <= r; z++) {
	                if (Math.max(Math.abs(x), Math.abs(z)) != r) continue;

	                int cx = px + x;
	                int cz = pz + z;

	                for (int cy = 0; cy < 16; cy++) {
	                    long key = ChunkCoord.pack(cx, cy, cz);
	                    Chunk chunk = worldMap.getChunk(key);
	                    if (chunk != null && chunk.needsUpdate && !chunk.queuedForMeshing) {
	                        meshQueue.submit(chunk);
	                    }
	                }
	            }
	        }
	    }
	}
	
	
	@Override
	public void run() {
		this.running = true;
		gameLoop();
	}
}
