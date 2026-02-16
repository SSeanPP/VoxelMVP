package main;

public class GameEngine implements Runnable {
	public volatile boolean running;
	private volatile GameState publishedState = new GameState();

	
	public GameEngine() {
		
	}
	
	public void gameLoop() {
		double t = 0.0;
	    double dt = 0.01;

	    double currentTime = System.nanoTime();
	    double accumulator = 0.0;

	    GameState previousState = publishedState;
	    GameState currentState = publishedState.copy();
	    
	    while ( running )
	    {
	    	//System.out.println("running");
	        double newTime = System.nanoTime() / 1000000000.0;
	        double frameTime = (newTime - currentTime) / 1000000000.0;
	        if ( frameTime > 0.25 )
	            frameTime = 0.25;
	        currentTime = newTime;

	        accumulator += frameTime;

	        while ( accumulator >= dt )
	        {
	        	//System.out.println("running");
	            previousState = currentState;
	            currentState.integrate(t, dt);;
	            t += dt;
	            accumulator -= dt;
	        }

	        double alpha = accumulator / dt;

	        GameState state = GameState.lerp(previousState, currentState, alpha);

	        this.publishedState = state;
	    }
	}
	
	public GameState getPublishedState() {
	    return publishedState;
	}

	@Override
	public void run() {
		this.running = true;
		gameLoop();
	}
}
