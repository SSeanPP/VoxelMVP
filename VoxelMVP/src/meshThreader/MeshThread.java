package meshThreader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BlockingQueue;

import bufferManager.ChunkSSBO;
import bufferManager.ChunkSSBO.Slot;
import bufferManager.SceneBufferManager;
import main.Chunk;
import main.Settings;
import main.WorldMap;

public abstract class MeshThread implements Runnable {
	// --- Infrastructure ---
    protected SceneBufferManager bufferManager;
    protected ChunkSSBO renderTorroid;
    protected BlockingQueue<Slot> queue;

    // --- Block cache ---
    protected final short[][][] localBlockCache = new short[Settings.blockCacheSize][Settings.blockCacheSize][Settings.blockCacheSize];
    
    //Vertex Indices Stuff
    protected int[] vertices = new int[120000];
    protected int[] indices = new int[80000];
	
	protected int vertexPtr = 0;
	protected int indexPtr = 0;
    
    // Texture Consts
    
    protected static final int TOP = 0;
    protected static final int BOTTOM = 1;
    protected static final int NORTH = 2;
    protected static final int SOUTH = 3;
    protected static final int EAST = 4;
    protected static final int WEST = 5;
	
    protected final int ATLAS_SIZE = 16;   // tiles per row
    protected final float TILE_SIZE = 1f / ATLAS_SIZE;
    
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
            	vertexPtr = 0;
                indexPtr  = 0;
                
            	Slot slot = queue.take();
            	
            	Chunk chunk = WorldMap.getChunkDirect(slot.x, slot.y, slot.z);
            	if (chunk != null) {
            	    WorldMap.gen.generate(chunk, slot.x, slot.y, slot.z);
            	    meshChunk(slot, chunk); 
            	}
            	uploadToGPU(slot);
            	slot.queued = false;
            } catch (InterruptedException e) {
                break;
            }
        }
    }

	protected abstract void meshChunk(Slot slot, Chunk chunk);
	
	protected void uploadToGPU(Slot slot) {

		if (vertexPtr == 0 && indexPtr == 0) {  
		    if (slot.allocation != null) {
		        bufferManager.free(slot.allocation);
		        slot.allocation = null;
		    }
		    renderTorroid.clear(slot);
		    return;
		} else {
            int vertexSizeBytes = vertexPtr * 4;
            int indexSizeBytes  = indexPtr  * 4;
            int paddedVertex = bufferManager.alignVertex((int)(vertexSizeBytes));
            int paddedIndex  = bufferManager.alignIndex ((int)(indexSizeBytes));
            
            
            if (slot.allocation != null) {
                int allocV = slot.allocation.vertexLimit - slot.allocation.vertexOffset;
                int allocI = slot.allocation.indexLimit  - slot.allocation.indexOffset;

                if (allocV >= paddedVertex && allocI >= paddedIndex
                		&& allocV <= paddedVertex + (paddedVertex >> 2)  
                	    && allocI <= paddedIndex  + (paddedIndex  >> 2)) {
                    slot.allocation.setCounts(0);
                } else {
                    bufferManager.free(slot.allocation);
                    slot.allocation = bufferManager.getAllocation(paddedVertex, paddedIndex);
                }
            } else {
                slot.allocation = bufferManager.getAllocation(paddedVertex, paddedIndex);
            }

            ByteBuffer vbo = bufferManager.getVBOSlice(slot.allocation).order(ByteOrder.nativeOrder());
            vbo.asIntBuffer().put(vertices, 0, vertexPtr);
            
            ByteBuffer ebo = bufferManager.getEBOSlice(slot.allocation).order(ByteOrder.nativeOrder());
            ebo.asIntBuffer().put(indices, 0, indexPtr);

            slot.allocation.setCounts(indexPtr);
            
            //System.out.println("uploadToGPU slot.ssboIndex=" + slot.ssboIndex + " slot.x=" + slot.x);
            renderTorroid.commit(slot,
            	    slot.allocation.indexOffset / 4,
            	    slot.allocation.vertexOffset / Settings.stride,
            	    indexPtr);
		}
    }
}
