package main;

import org.lwjgl.input.Keyboard;

import meshThreader.MeshQueue;

public class GameEngine implements Runnable {
	public volatile boolean running;
	
	private volatile GameState state = new GameState();
	private volatile double alpha = 0.0;
	
	private InputState inputState = new InputState();
	
	private MeshQueue meshQueue;
	private int chunksProcessed = 0;
	private int loopTotal = 0;
	
	private WorldMap worldMap;
	private final int renderSize = 32 * 32 * 16;
	
	double newTime;
    double frameTime;
	
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
	        	chunksProcessed = 0;
	        	loopTotal = 0;
	        	while(chunksProcessed < 20 && loopTotal != renderSize) {
	        		if(meshQueue.submit(worldMap.getRandomChunk())) {
	        			this.chunksProcessed++;
	        		}
	        		loopTotal++;
	        	}
	        	
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
	
	
	@Override
	public void run() {
		this.running = true;
		gameLoop();
	}
	
	
}
