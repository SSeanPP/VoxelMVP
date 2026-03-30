package main;

import java.util.Queue;

import org.joml.Vector3f;
import org.lwjgl.input.Keyboard;

import bufferManager.ChunkSSBO;
import bufferManager.ChunkSSBO.Slot;
import meshThreader.MeshQueue;

public class GameEngine implements Runnable {
	public volatile boolean running;
	
	private volatile GameState state = new GameState();
	private volatile double alpha = 0.0;
	
	private InputState inputState = new InputState();
	
	private MeshQueue meshQueue;
	
	private final int CHUNK_SHIFT = 4; // 2^4 = 16
	
	private Queue<Slot> evictionQueue;
	private ChunkSSBO renderTorroid;
	private GameInputQueue gameInputQueue;
	
	private double newTime;
    private double frameTime;
    
    private int lastPx = (int)(Settings.spawnChunk.x);
	private int lastPy = (int)(Settings.spawnChunk.y);
	private int lastPz = (int)(Settings.spawnChunk.z);
	
	private int px;
	private int py;
	private int pz;
	
	private Vector3f pPosition;
    private Vector3f cameraPos;
	
	public GameEngine(MeshQueue queue, Queue<Slot> evictionQueueFromRenderer, ChunkSSBO renderCacheFromRenderer, GameInputQueue inputQueue) {
		meshQueue = queue;
		evictionQueue = evictionQueueFromRenderer;
		renderTorroid = renderCacheFromRenderer;
		this.gameInputQueue = inputQueue;
		
		
		this.cameraPos = state.getCamera().getPosition();
		this.pPosition = new Vector3f().set(Settings.spawnChunk);
		
		initialView();
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

	        	px = (int)cameraPos.x >> CHUNK_SHIFT;
	    	    py = (int)cameraPos.y >> CHUNK_SHIFT;
	    	    pz = (int)cameraPos.z >> CHUNK_SHIFT;
	    		
	    		if (px != lastPx || py != lastPy || pz != lastPz) {
	    			
	    			pPosition.set(px,py,pz);
	    		    meshQueue.updatePos(pPosition);
	    		    
	    		    checkView();
	    		    
	    		    lastPx = px;
	    		    lastPy = py;
	    		    lastPz = pz;
	    		}
	        	
	        	state.integrate(t, dt, inputState);
	        	
	            t += dt;
	            accumulator -= dt;
	        }

	        this.alpha = accumulator / dt;
	    }
	}
	
	
    public void checkView() {
        for(Slot slot : renderTorroid.getRenderToroid()) {

            if (!slot.queued && slot.escaped(this.pPosition)) {
            	slot.queued = true; 
                WorldMap.removeChunk(WorldMap.key(slot.x, slot.y, slot.z));
                
                slot.newPos(this.pPosition);

                meshQueue.submit(slot);
            }
        }
    }
    
    public void initialView() {
        for(Slot slot : renderTorroid.getRenderToroid()) {
            if (!slot.queued) {
            	slot.queued = true; 
                WorldMap.removeChunk(WorldMap.key(slot.x, slot.y, slot.z));
                
                slot.newPos(this.pPosition);

                meshQueue.submit(slot);
            }
        }
    }
 
	
	public GameState getPublishedState() {
	    return state;
	}
	
	public double getPublishedAlpha() {
		return alpha;
	}
	
	private void drainInputQueue() {
	    GameInput input;
	   
	    
        while ((input = gameInputQueue.inputQueue.poll()) != null) {
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
	
	@Override
	public void run() {
		this.running = true;
		gameLoop();
	}
}
