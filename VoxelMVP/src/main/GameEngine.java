package main;

import org.lwjgl.input.Keyboard;

public class GameEngine implements Runnable {
	public volatile boolean running;
	private volatile GameState publishedState = new GameState();
	private InputState inputState = new InputState();
	
	public GameEngine() {
		
	}
	
	public void gameLoop() {
		double t = 0.0;
	    double dt = 0.04;

	    double currentTime = System.nanoTime() /1000000000.0;
	    double accumulator = 0.0;
 
	    GameState previousState = publishedState;
	    GameState currentState = publishedState.copy();
	    
	    while ( running )
	    {
	    	//System.out.println("running");
	        double newTime = System.nanoTime() / 1000000000.0;
	        double frameTime = (newTime - currentTime);
	        if ( frameTime > 0.25 )
	            frameTime = 0.25;
	        currentTime = newTime;

	        accumulator += frameTime;

	        while ( accumulator >= dt )
	        {
	        	drainInputQueue();
	            previousState = currentState.copy();
	            currentState.integrate(t, dt, inputState);;
	            t += dt;
	            accumulator -= dt;
	        }

	        double alpha = accumulator / dt;
	        //System.out.println("Alpha: " + alpha); 

	        GameState state = GameState.lerp(previousState, currentState, alpha);

	        this.publishedState = state;
	    }
	}
	
	public GameState getPublishedState() {
	    return publishedState;
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
            }
        }
    }

	@Override
	public void run() {
		this.running = true;
		gameLoop();
	}
}
