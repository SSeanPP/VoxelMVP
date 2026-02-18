package main;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Input {
	public final static ConcurrentLinkedQueue<Input> inputQueue = new ConcurrentLinkedQueue<Input>();

	public boolean isPressed;
	public int keyCode;
	
	Input(boolean pressed, int code) {
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
