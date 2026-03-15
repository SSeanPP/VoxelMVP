package meshThreader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import bufferManager.SceneBufferManager;

import main.Chunk;
public class MeshQueue {

    private static int THREAD_COUNT;
    private final SceneBufferManager bufferManager;

    private final BlockingQueue<Chunk> meshQueue = new LinkedBlockingQueue<Chunk>(32*32*16);
    private final List<Thread> workers = new ArrayList<Thread>();

    public MeshQueue(SceneBufferManager manager) {
    	THREAD_COUNT = Runtime.getRuntime().availableProcessors() - 2;
    	if (THREAD_COUNT < 1) {
    		THREAD_COUNT = 1;
    	}
    	
    	bufferManager = manager;
    	for (int i = 0; i < THREAD_COUNT; i++) {
    	    Thread t = new Thread(new MeshThread(bufferManager, meshQueue));
    	    t.start();
    	    workers.add(t);
    	}
    }

    //Returns true if updated, false if not
    public boolean submit(Chunk chunk) {

    	if (!chunk.needsUpdate || chunk.queuedForMeshing) {
    		return false;
    	}
    	
    	if(meshQueue.offer(chunk)){
    		chunk.queuedForMeshing = true;
    	}
    	
    	return true;
    }

}