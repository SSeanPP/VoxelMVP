package meshThreader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.joml.Vector3f;

import bufferManager.SceneBufferManager;

import main.Chunk;
import main.Settings;
public class MeshQueue {

    private static int THREAD_COUNT;
    private final SceneBufferManager bufferManager;
    
    private Vector3f playerPos = Settings.spawnPoint;
    public static final BlockingQueue<Chunk> meshQueue = new ArrayBlockingQueue<Chunk>(Settings.WORLD_SIZE_WIDTH*Settings.WORLD_SIZE_HEIGHT*Settings.WORLD_SIZE_WIDTH);
    private final List<Thread> workers = new ArrayList<Thread>();

    public MeshQueue(SceneBufferManager manager) {
    	THREAD_COUNT = (Runtime.getRuntime().availableProcessors() /2) -2;
    	if (THREAD_COUNT < 1) {
    		THREAD_COUNT = 1;
    	}
    	
    			
    	System.out.println("Mesh threads spawned: "+ THREAD_COUNT);
    	
    	bufferManager = manager;
    	for (int i = 0; i < THREAD_COUNT; i++) {
    	    Thread t = new Thread(new MeshThreadBinaryGreedy(bufferManager, meshQueue));
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