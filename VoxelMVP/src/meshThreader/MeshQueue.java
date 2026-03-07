package meshThreader;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import main.Chunk;

public class MeshQueue {

    public static final Queue<Chunk> meshInputQueue = new ConcurrentLinkedQueue();

    private MeshQueue() {
        // private constructor: no one instantiates this!
    }

}