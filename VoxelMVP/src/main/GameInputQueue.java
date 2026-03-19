package main;

import java.util.concurrent.ConcurrentLinkedQueue;

public class GameInputQueue {
	public final ConcurrentLinkedQueue<GameInput> inputQueue = new ConcurrentLinkedQueue<GameInput>();
}
