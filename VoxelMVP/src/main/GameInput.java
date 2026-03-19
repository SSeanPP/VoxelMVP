package main;

import java.util.concurrent.ConcurrentLinkedQueue;

public class GameInput {
	
	public boolean isPressed;
	public int keyCode;
	
	GameInput(boolean pressed, int code) {
		this.isPressed = pressed;
		this.keyCode = code;
	}
	
	public boolean getEventState() {
		return isPressed;
	}
	
	public int getEventKey() {
		return keyCode;
	}
	
}
