package meshThreader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BlockingQueue;

import bufferManager.ChunkSSBO.Slot;
import bufferManager.SceneBufferManager;
import main.Chunk;
import main.Settings;
import main.WorldMap;

public abstract class MeshThread implements Runnable {
	// --- Infrastructure ---
    protected SceneBufferManager bufferManager;
    protected BlockingQueue<Slot> queue;

    // --- Block cache ---
    protected final short[][][] localBlockCache = new short[Settings.blockCacheSize][Settings.blockCacheSize][Settings.blockCacheSize];
    
    //Vertex Indices Stuff
    protected float[] vertices = new float[120000];
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
                Slot slot = queue.take();
                if(!slot.chunk.hasGenned) {
                	slot.chunk.hasGenned = true;
                	WorldMap.gen.generate(slot.chunk, slot.x, slot.y, slot.z);
                }
                meshChunk(slot);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

	protected abstract void meshChunk(Slot slot);
	
	protected void uploadToGPU(Slot slot) {

        if (vertexPtr == 0 && indexPtr == 0) {
            slot.chunk.hasBlocks = false;
            
            
        } else {
            slot.chunk.hasBlocks = true;

            int vertexSizeBytes = vertexPtr * 4;
            int indexSizeBytes  = indexPtr  * 4;
            int paddedVertex = bufferManager.alignVertex((int)(vertexSizeBytes * 1.1));
            int paddedIndex  = bufferManager.alignIndex ((int)(indexSizeBytes  * 1.1));
            
            //int paddedVertex = bufferManager.alignVertex((int)(vertexSizeBytes));
            //int paddedIndex  = bufferManager.alignIndex ((int)(indexSizeBytes));

            if (slot.allocation != null) {
                int allocV = slot.allocation.vertexLimit - slot.allocation.vertexOffset;
                int allocI = slot.allocation.indexLimit  - slot.allocation.indexOffset;

                if (allocV >= paddedVertex && allocI >= paddedIndex) {
                    slot.allocation.setCounts(0);
                } else {
                    bufferManager.free(slot.allocation);
                    slot.allocation = bufferManager.getAllocation(paddedVertex, paddedIndex);
                }
            } else {
                slot.allocation = bufferManager.getAllocation(paddedVertex, paddedIndex);
            }

            ByteBuffer vbo = bufferManager.getVBOSlice(slot.allocation).order(ByteOrder.nativeOrder());
            for (int i = 0; i < vertexPtr; i++) vbo.putFloat(vertices[i]);

            ByteBuffer ebo = bufferManager.getEBOSlice(slot.allocation).order(ByteOrder.nativeOrder());
            for (int i = 0; i < indexPtr; i++) ebo.putInt(indices[i]);

            slot.allocation.setCounts(indexPtr);
        }
        
        slot.queued = false;
        if (slot.pendingDisposal) {
            slot.chunk.dispose();
        }
    }
}
