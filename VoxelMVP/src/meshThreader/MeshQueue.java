package meshThreader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.joml.Vector3f;

import bufferManager.ChunkSSBO.Slot;
import bufferManager.SceneBufferManager;

import main.Chunk;
import main.Settings;
public class MeshQueue {

    private static int THREAD_COUNT;
    private final SceneBufferManager bufferManager;
    
    private static Vector3f playerChunkPos = Settings.spawnChunk;
    public static final BlockingQueue<Slot> meshQueue = new PriorityBlockingQueue<Slot>((Settings.RENDER_DISTANCE*Settings.RENDER_HEIGHT*Settings.RENDER_DISTANCE),
    		new Comparator<Slot>() {
		    	public int compare(Slot a, Slot b) {
		            int dax = (int) (a.x - playerChunkPos.x);
		            int day = (int) (a.y - playerChunkPos.y);
		            int daz = (int) (a.z - playerChunkPos.z);
		            int dbx = (int) (b.x - playerChunkPos.x);
		            int dby = (int) (b.y - playerChunkPos.y);
		            int dbz = (int) (b.z - playerChunkPos.z);
		            
		            int distA = Math.max(Math.abs(dax), Math.max(Math.abs(day), Math.abs(daz)));
		            int distB = Math.max(Math.abs(dbx), Math.max(Math.abs(dby), Math.abs(dbz)));
		            
		            return distA < distB ? -1 : (distA > distB ? 1 : 0);
		        }
    		}
    );
    
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

    public void submit(Slot correct) {
    	meshQueue.offer(correct);
    }
    
    public void updatePos(Vector3f newPos)  {
    	MeshQueue.playerChunkPos.set(newPos);
    }

}