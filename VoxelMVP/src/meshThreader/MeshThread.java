package meshThreader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BlockingQueue;

import bufferManager.SceneBufferManager;
import main.Chunk;
import main.Settings;

public abstract class MeshThread implements Runnable {
	// --- Infrastructure ---
    protected SceneBufferManager bufferManager;
    protected BlockingQueue<Chunk> queue;

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
                Chunk chunk = queue.take();
                meshChunk(chunk);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

	protected abstract void meshChunk(Chunk chunk);
	
	protected void uploadToGPU(Chunk chunk) {

        if (vertexPtr == 0 && indexPtr == 0) {
            chunk.hasBlocks = false;
            
            //
            if (chunk.previousAllocation != null) {
                bufferManager.free(chunk.previousAllocation);
                chunk.previousAllocation = null;
            }
            
        } else {
            chunk.hasBlocks = true;

            int vertexSizeBytes = vertexPtr * 4;
            int indexSizeBytes  = indexPtr  * 4;
            int paddedVertex = bufferManager.alignVertex((int)(vertexSizeBytes * 1.1));
            int paddedIndex  = bufferManager.alignIndex ((int)(indexSizeBytes  * 1.1));
            
            //int paddedVertex = bufferManager.alignVertex((int)(vertexSizeBytes));
            //int paddedIndex  = bufferManager.alignIndex ((int)(indexSizeBytes));

            if (chunk.previousAllocation != null) {
                int allocV = chunk.previousAllocation.vertexLimit - chunk.previousAllocation.vertexOffset;
                int allocI = chunk.previousAllocation.indexLimit  - chunk.previousAllocation.indexOffset;

                if (allocV >= paddedVertex && allocI >= paddedIndex) {
                    chunk.previousAllocation.setCounts(0);
                    chunk.allocation = chunk.previousAllocation;
                    chunk.previousAllocation = null;
                } else {
                    bufferManager.free(chunk.previousAllocation);
                    chunk.previousAllocation = null;
                    chunk.allocation = bufferManager.getAllocation(paddedVertex, paddedIndex);
                }
            } else {
                chunk.allocation = bufferManager.getAllocation(paddedVertex, paddedIndex);
            }

            ByteBuffer vbo = bufferManager.getVBOSlice(chunk.allocation).order(ByteOrder.nativeOrder());
            for (int i = 0; i < vertexPtr; i++) vbo.putFloat(vertices[i]);

            ByteBuffer ebo = bufferManager.getEBOSlice(chunk.allocation).order(ByteOrder.nativeOrder());
            for (int i = 0; i < indexPtr; i++) ebo.putInt(indices[i]);

            chunk.allocation.setCounts(indexPtr);
        }
        
        chunk.queuedForMeshing = false;
        chunk.previousAllocation = null;
        if (chunk.pendingDisposal) {
            chunk.dispose();
        }
    }
}
