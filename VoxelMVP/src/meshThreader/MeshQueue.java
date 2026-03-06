package meshThreader;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.minecraft.src.WorldRenderer;

public class MeshQueue {

    public static final Queue<> meshInputQueue = new ConcurrentLinkedQueue();

    private MeshQueue() {
        // private constructor: no one instantiates this!
    }

}