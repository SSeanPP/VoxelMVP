package bufferManager;

import java.nio.ByteBuffer;

import main.Chunk;
/*
public class ChunkSSBO {
    // Slot layout - 32 bytes per slot
    // [worldX, worldY, worldZ, pad, firstIndex, baseVertex, indexCount, chunkId]
    
    private static final int SLOT_SIZE     = 32;
    private static final int OFF_WORLD_X   = 0;
    private static final int OFF_WORLD_Y   = 4;
    private static final int OFF_WORLD_Z   = 8;
    private static final int OFF_FIRST_IDX = 16;
    private static final int OFF_BASE_VTX  = 20;
    private static final int OFF_COUNT     = 24;
    private static final int OFF_CHUNK_ID  = 28;
    
    private final int ssboId;
    private final ByteBuffer mapped;
    private final int slotCount;
    
    // CPU mirror - so game thread can read back chunkId without touching GL
    private final int[] chunkIds;
    private final Chunk[] slotToChunk;
    
    public void writeSlotMeta(int slot, float wx, float wy, float wz, int chunkId) {
        int base = slot * SLOT_SIZE;
        mapped.putFloat(base + OFF_WORLD_X,  wx);
        mapped.putFloat(base + OFF_WORLD_Y,  wy);
        mapped.putFloat(base + OFF_WORLD_Z,  wz);
        mapped.putInt  (base + OFF_CHUNK_ID, chunkId);
        mapped.putInt  (base + OFF_COUNT,    0); // invisible until mesh done
        chunkIds[slot] = chunkId;
    }

    public void commitSlot(int slot, int firstIndex, int baseVertex, int indexCount) {
        int base = slot * SLOT_SIZE;
        mapped.putInt(base + OFF_FIRST_IDX, firstIndex);
        mapped.putInt(base + OFF_BASE_VTX,  baseVertex);
        mapped.putInt(base + OFF_COUNT,     indexCount); // last - visibility gate
    }

    public void clearSlot(int slot) {
        int base = slot * SLOT_SIZE;
        mapped.putInt(base + OFF_COUNT,    0);
        mapped.putInt(base + OFF_CHUNK_ID, 0);
        chunkIds[slot] = 0;
        slotToChunk[slot] = null;
    }

    public int getChunkId(int slot) { return chunkIds[slot]; }
    public Chunk getChunk(int slot)  { return slotToChunk[slot]; }
}*/