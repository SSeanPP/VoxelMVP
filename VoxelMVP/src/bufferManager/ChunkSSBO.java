package bufferManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.joml.Vector3f;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBMapBufferRange;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;

import main.Settings;

public class ChunkSSBO {
    // Slot layout - 32 bytes per slot
    // [worldX, worldY, worldZ, pad, firstIndex, baseVertex, indexCount, chunkId]
    
	private final int DRAWCMD_SIZE = 32;
	
	private final int SLOT_SIZE      = 32;
	private final int OFF_WORLD_X    = 0;
	private final int OFF_WORLD_Y    = 4;
	private final int OFF_WORLD_Z    = 8;
	// offset 12 = pad (unused)
	private final int OFF_FIRST_IDX  = 16;
	private final int OFF_BASE_VTX   = 20;
	private final int OFF_COUNT      = 24; 
	
	
    private final int chunkSSBOid;
    private final int drawSSBOid;
    private final ByteBuffer chunkSSBO;
    private final ByteBuffer zero = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder());
    
    private final int[] countBufIds = new int[2];
    private final ByteBuffer[] countBufs = new ByteBuffer[2];
    private int countBufWrite = 0; // GPU writes to this
    private int countBufRead  = 1; // CPU reads from this
    
    public final int slotCount;
    
    private final static int WIDTH  = Settings.RENDER_DISTANCE * 2 + 1;
    private final static int HEIGHT = Settings.RENDER_HEIGHT   * 2 + 1;
    
    // CPU mirror - so game thread can read back chunkId without touching GL
    private final Slot[] renderTorroid;
    
    public ChunkSSBO() {
    	slotCount = WIDTH * HEIGHT * WIDTH;
    	renderTorroid = new Slot[slotCount];
    	System.out.println("slotCount=" + slotCount + " WIDTH=" + WIDTH + " HEIGHT=" + HEIGHT);
    	int i = 0;
        for (int x = 0; x < WIDTH; x++)
        for (int y = 0; y < HEIGHT; y++)
        for (int z = 0; z < WIDTH; z++) {
            Slot s = new Slot();
            s.ssboIndex = i;
            s.x = (int)Settings.spawnChunk.x - Settings.RENDER_DISTANCE + x;
            s.y = (int)Settings.spawnChunk.y - Settings.RENDER_HEIGHT   + y;
            s.z = (int)Settings.spawnChunk.z - Settings.RENDER_DISTANCE + z;
            renderTorroid[i++] = s;
        }
        
        zero.putInt(0, 0);
        zero.putInt(4, 0);

        chunkSSBOid = GL15.glGenBuffers();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, chunkSSBOid);
        GL44.glBufferStorage(GL43.GL_SHADER_STORAGE_BUFFER,
            slotCount * SLOT_SIZE,
            GL30.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT);

        chunkSSBO = ARBMapBufferRange.glMapBufferRange(GL43.GL_SHADER_STORAGE_BUFFER,
            0, slotCount * SLOT_SIZE,
            GL30.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT,
            null);

        for (int j = 0; j < slotCount * SLOT_SIZE; j += 4)
            chunkSSBO.putInt(j, 0);

    	/*
    	drawSSBOid = GL15.glGenBuffers();
    	GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, drawSSBOid);
    	GL44.glBufferStorage(GL43.GL_SHADER_STORAGE_BUFFER,
    	    slotCount * DRAWCMD_SIZE,
    	    GL30.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT);
    	drawSSBO = ARBMapBufferRange.glMapBufferRange(GL43.GL_SHADER_STORAGE_BUFFER,
    	    0, slotCount * DRAWCMD_SIZE,
    	    GL30.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT,
    	    null);*/
    	
    	drawSSBOid = GL15.glGenBuffers();
    	GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, drawSSBOid);
    	GL44.glBufferStorage(GL43.GL_SHADER_STORAGE_BUFFER,
    	    slotCount * DRAWCMD_SIZE,
    	    GL44.GL_DYNAMIC_STORAGE_BIT); 
    	
    	for (int b = 0; b < 2; b++) {
    	    countBufIds[b] = GL15.glGenBuffers();
    	    GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, countBufIds[b]);
    	    GL44.glBufferStorage(GL43.GL_SHADER_STORAGE_BUFFER, 8,
    	    	    GL30.GL_MAP_READ_BIT |
    	    	    ARBBufferStorage.GL_MAP_PERSISTENT_BIT |
    	    	    ARBBufferStorage.GL_MAP_COHERENT_BIT |
    	    	    GL44.GL_DYNAMIC_STORAGE_BIT);
    	    countBufs[b] = ARBMapBufferRange.glMapBufferRange(GL43.GL_SHADER_STORAGE_BUFFER, 0, 8,
    	        GL30.GL_MAP_READ_BIT |
    	        ARBBufferStorage.GL_MAP_PERSISTENT_BIT |
    	        ARBBufferStorage.GL_MAP_COHERENT_BIT, null);
    	}
    	
    	
    	// After creating chunkSSBOid:
    	System.out.println("chunkSSBOid=" + chunkSSBOid);
    	// After creating chunkSSBOid:
    	System.out.println("drawSSBOid=" + drawSSBOid);
    	
    }
    
    public static class Slot {
    	public int x,y,z;
    	public int ssboIndex;
    	public Allocation allocation;
    	public volatile boolean queued;
    	
    	public Slot() {
    		this.queued = false;
    	}
    	
    	public boolean escaped(Vector3f playerPos) {
    	    return Math.abs(x - playerPos.x) > Settings.RENDER_DISTANCE ||
    	           Math.abs(y - playerPos.y) > Settings.RENDER_HEIGHT ||
    	           Math.abs(z - playerPos.z) > Settings.RENDER_DISTANCE;
    	}
    	
    	public void newPos(Vector3f p) {
    	    this.x = (int) (p.x + floorMod((int) (this.x - p.x + Settings.RENDER_DISTANCE), WIDTH)  - Settings.RENDER_DISTANCE);
    	    this.y = (int) (p.y + floorMod((int) (this.y - p.y + Settings.RENDER_HEIGHT),   HEIGHT) - Settings.RENDER_HEIGHT);
    	    this.z = (int) (p.z + floorMod((int) (this.z - p.z + Settings.RENDER_DISTANCE), WIDTH)  - Settings.RENDER_DISTANCE);
    	}
    	
    	@Override
    	public int hashCode() {
    	    return x * 31 * 31 + y * 31 + z;
    	}

    	@Override
    	public boolean equals(Object o) {
    	    if (!(o instanceof Slot)) return false;
    	    Slot other = (Slot) o;
    	    return x == other.x && y == other.y && z == other.z;
    	}
    }
    
    public void write(Slot slot) {
        writeSlotMeta(slot.ssboIndex, slot.x, slot.y, slot.z);
    }

    public void commit(Slot slot, int firstIndex, int baseVertex, int indexCount) {
        commitSlot(slot.ssboIndex, firstIndex, baseVertex, indexCount);
    }

    public void clear(Slot slot) {
        clearSlot(slot.ssboIndex);
    }

    public Slot[] getRenderToroid() {
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
    }

    
    public static int floorMod(int x, int y) {
        int r = x % y;
        if ((x ^ y) < 0 && r != 0) {
            r += y;
        }
        return r;
    }
    
    public void resetCount() {
        countBufWrite ^= 1;
        countBufRead  ^= 1;
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, countBufIds[countBufWrite]);
        GL43.glClearBufferSubData(
            GL43.GL_SHADER_STORAGE_BUFFER,
            GL30.GL_R32UI,
            0L, 8L,
            GL30.GL_RED_INTEGER,
            GL11.GL_UNSIGNED_INT,
            zero
        );
    }
    
    private int stableDrawCount = 0;
    private int stableMeshedCount = 0;
    private int drawCountFrames = 0;

    public int readDrawCount() {
        if (++drawCountFrames >= 60) {
            drawCountFrames = 0;
            stableDrawCount   = countBufs[countBufRead].getInt(0);
            stableMeshedCount = countBufs[countBufRead].getInt(4);
        }
        return stableDrawCount;
    }

    public int readMeshedCount() {
        return stableMeshedCount;
    }
    
    public int getChunkSSBOid() { return chunkSSBOid; }
    public int getDrawSSBOid()  { return drawSSBOid; }
    public int getCountBufId() { return countBufIds[countBufWrite]; }
    public int getSlotCount()   { return slotCount; }
}