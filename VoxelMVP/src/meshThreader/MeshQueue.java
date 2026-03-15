package meshThreader;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bufferManager.SceneBufferManager;

import main.Chunk;
public class MeshQueue {

    private final SceneBufferManager bufferManager;
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors() - 1;;
    private static final int MAX_FREE_THREADS = 20;

    private final ExecutorService executor;
    private final Queue<MeshThread> freeThreads = new ConcurrentLinkedQueue<MeshThread>();

    public MeshQueue(SceneBufferManager input) {
        bufferManager = input;
        executor = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    //Returns true if updated, false if not
    public boolean submit(Chunk chunk) {

        MeshThread worker = freeThreads.poll();

        if (worker == null) {
            worker = new MeshThread(bufferManager, this);
        }
        
        if (chunk.needsUpdate) {
        	chunk.needsUpdate = false;
        	worker.setChunk(chunk);
        	executor.submit(worker);
        	return true;
        } else {
        	return false;
        }
    }

    public void returnWorker(MeshThread worker) {
        if (freeThreads.size() < MAX_FREE_THREADS) {
            freeThreads.offer(worker);
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}