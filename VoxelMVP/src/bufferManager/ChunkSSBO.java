package bufferManager;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBMapBufferRange;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;

import main.Chunk;
import main.Settings;

public class ChunkSSBO {
    // Slot layout - 32 bytes per slot
    // [worldX, worldY, worldZ, pad, firstIndex, baseVertex, indexCount, chunkId]
    
	private final int DRAWCMD_SIZE = 20;
	
    private final int SLOT_SIZE     = 32;
    private final int OFF_WORLD_X   = 0;
    private final int OFF_WORLD_Y   = 4;
    private final int OFF_WORLD_Z   = 8;
    private final int OFF_FIRST_IDX = 16;
    private final int OFF_BASE_VTX  = 20;
    private final int OFF_COUNT     = 24;
    
    private final int chunkSSBOid;
    private final int drawSSBOid;
    private final ByteBuffer chunkSSBO;
    
    private final int slotCount;
    
    private final int WIDTH  = Settings.RENDER_DISTANCE * 2 + 1;
    private final int HEIGHT = Settings.RENDER_HEIGHT   * 2 + 1;
    
    // CPU mirror - so game thread can read back chunkId without touching GL
    private final Chunk[] renderTorroid;
    
    public ChunkSSBO() {
    	slotCount = WIDTH * HEIGHT * WIDTH;
    	renderTorroid = new Chunk[slotCount];
    	
    	chunkSSBOid = GL15.glGenBuffers();
    	GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, chunkSSBOid);

    	GL44.glBufferStorage(GL43.GL_SHADER_STORAGE_BUFFER, 
    			slotCount * SLOT_SIZE,
    			GL30.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT);
    	
    	
    	chunkSSBO = ARBMapBufferRange.glMapBufferRange(GL43.GL_SHADER_STORAGE_BUFFER, 
    			0, 
    			slotCount * SLOT_SIZE,
	    	    GL30.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT, 
	    	    null);

    	
    	drawSSBOid = GL15.glGenBuffers();
    	GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, drawSSBOid);
    	GL44.glBufferStorage(GL43.GL_SHADER_STORAGE_BUFFER, 
    			slotCount * DRAWCMD_SIZE, 
    			GL44.GL_DYNAMIC_STORAGE_BIT);
    }
    
    private int index(int wx, int wy, int wz) {
        int tx = floorMod(wx, WIDTH);
        int ty = floorMod(wy, HEIGHT);
        int tz = floorMod(wz, WIDTH);
        
        return tx * (HEIGHT * WIDTH) + ty * WIDTH + tz;
    }

    public int getIndexAt(int wx, int wy, int wz) {
        return index(wx, wy, wz);
    }
    
    public void write(Chunk chunk) {
        int slot = getIndexAt(chunk.x, chunk.y, chunk.z);
        writeSlotMeta(slot, chunk.x, chunk.y, chunk.z);
        renderTorroid[slot] = chunk;
    }
    
    public void commit(Chunk chunk, int firstIndex, int baseVertex, int indexCount) {
    	commitSlot(getIndexAt(chunk.x, chunk.y, chunk.z), firstIndex, baseVertex, indexCount);
    }
    
    public void clear(Chunk chunk) {
    	clearSlot(getIndexAt(chunk.x, chunk.y, chunk.z));
    }

    public Chunk[] getRenderToroid() {
        return renderTorroid;
    }
    
    private void writeSlotMeta(int slot, int wx, int wy, int wz) {
        int base = slot * SLOT_SIZE;
        chunkSSBO.putInt(base + OFF_WORLD_X,  wx);
        chunkSSBO.putInt(base + OFF_WORLD_Y,  wy);
        chunkSSBO.putInt(base + OFF_WORLD_Z,  wz);
        chunkSSBO.putInt  (base + OFF_COUNT,    0);
    }

    private void commitSlot(int slot, int firstIndex, int baseVertex, int indexCount) {
        int base = slot * SLOT_SIZE;
        chunkSSBO.putInt(base + OFF_FIRST_IDX, firstIndex);
        chunkSSBO.putInt(base + OFF_BASE_VTX,  baseVertex);
        chunkSSBO.putInt(base + OFF_COUNT,     indexCount);
    }

    private void clearSlot(int slot) {
        int base = slot * SLOT_SIZE;
        chunkSSBO.putInt(base + OFF_COUNT,    0);
        renderTorroid[slot] = null;
    }

    
    public static int floorMod(int x, int y) {
        int r = x % y;
        if ((x ^ y) < 0 && r != 0) {
            r += y;
        }
        return r;
    }
}