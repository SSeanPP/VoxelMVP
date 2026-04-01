package meshThreader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.joml.Vector3f;

import bufferManager.ChunkSSBO;
import bufferManager.ChunkSSBO.Slot;
import bufferManager.SceneBufferManager;

import main.Settings;
public class MeshQueue {

    private static int THREAD_COUNT;
    private final SceneBufferManager bufferManager;
    private final ChunkSSBO renderTorroid;
    
    private static Vector3f initialChunkPos = new Vector3f().set(Settings.spawnChunk);
    private static volatile int playerCX = (int) initialChunkPos.x, playerCY = (int) initialChunkPos.y, playerCZ = (int) initialChunkPos.z;
    
    public static final BlockingQueue<Slot> meshQueue = new PriorityBlockingQueue<Slot>((Settings.RENDER_DISTANCE*Settings.RENDER_HEIGHT*Settings.RENDER_DISTANCE),
    		new Comparator<Slot>() {
		    	public int compare(Slot a, Slot b) {
		    		int dax = a.x - playerCX;
		    		int day = a.y - playerCY;
		    		int daz = a.z - playerCZ;
		    		int dbx = b.x - playerCX;
		    		int dby = b.y - playerCY;
		    		int dbz = b.z - playerCZ;
		            
		            int distA = Math.max(Math.abs(dax), Math.max(Math.abs(day), Math.abs(daz)));
		            int distB = Math.max(Math.abs(dbx), Math.max(Math.abs(dby), Math.abs(dbz)));
		            
		            return distA < distB ? -1 : (distA > distB ? 1 : 0);
		        }
    		}
    );
    
    private final List<Thread> workers = new ArrayList<Thread>();

    public MeshQueue(SceneBufferManager manager, ChunkSSBO torroid) {
    	THREAD_COUNT = (Runtime.getRuntime().availableProcessors() /2) -2;
    	if (THREAD_COUNT < 1) {
    		THREAD_COUNT = 1;
    	}
    	
    	System.out.println("Mesh threads spawned: "+ THREAD_COUNT);
    	
    	bufferManager = manager;
    	this.renderTorroid = torroid;
    	for (int i = 0; i < THREAD_COUNT; i++) {
    	    Thread t = new Thread(new MeshThreadBinaryGreedy(bufferManager, meshQueue, renderTorroid));
    	    t.start();
    	    workers.add(t);
    	}
    }

    public void submit(Slot correct) {
    	meshQueue.offer(correct);
    }
    
    public void updatePos(Vector3f newPos) {
        playerCX = (int) newPos.x;
        playerCY = (int) newPos.y;
        playerCZ = (int) newPos.z;
    }

}