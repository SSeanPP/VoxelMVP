package main;

public class GameState {
	
	//Camera camera
	public GameState() {
		
	}
	
	public void integrate(double t, double dt) {
		//Should move camera in this state
	}
	
	public static GameState lerp(GameState previous, GameState current, double alpha) {
		GameState result = new GameState();
		
		/*
		  	renderState.x = currentState.x * alpha + previousState.x * (1 - alpha);
			renderState.y = currentState.y * alpha + previousState.y * (1 - alpha);
			renderState.velocityX = currentState.velocityX * alpha + previousState.velocityX * (1 - alpha);
			renderState.velocityY = currentState.velocityY * alpha + previousState.velocityY * (1 - alpha);

		 */
		
		return result;
	}
	
	public GameState copy() {
		GameState copy = new GameState();
		
		//Copy its shit
		
		return copy;
	}
}
