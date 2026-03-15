package meshThreader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import bufferManager.SceneBufferManager;

import main.Chunk;
public class MeshQueue {

    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors() - 2;
    private final SceneBufferManager bufferManager;

    private final BlockingQueue<Chunk> meshQueue = new ArrayBlockingQueue<Chunk>(256);
    private final List<Thread> workers = new ArrayList<Thread>();

    public MeshQueue(SceneBufferManager manager) {
    	bufferManager = manager;
    	for (int i = 0; i < THREAD_COUNT; i++) {
    	    Thread t = new Thread(new MeshThread(bufferManager, meshQueue));
    	    t.start();
    	    workers.add(t);
    	}
    }

    //Returns true if updated, false if not
    public boolean submit(Chunk chunk) {

    	if (!chunk.needsUpdate || chunk.queuedForMeshing)
    	    return false;

    	chunk.queuedForMeshing = true;
    	meshQueue.offer(chunk);
    	
    	return true;
    }

}