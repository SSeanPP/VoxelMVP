package main;

import org.joml.Vector3f;

public class RenderCache {
    private final int WIDTH  = Settings.RENDER_DISTANCE * 2 + 1;
    private final int HEIGHT = Settings.RENDER_HEIGHT   * 2 + 1;
    private final int DEPTH  = Settings.RENDER_DISTANCE * 2 + 1;

    private final Chunk[] array = new Chunk[WIDTH * HEIGHT * DEPTH];

    public volatile int playerChunkX;
    public volatile int playerChunkY;
    public volatile int playerChunkZ;

    public RenderCache(Vector3f pos) {
        playerChunkX = (int)pos.x;
        playerChunkY = (int)pos.y;
        playerChunkZ = (int)pos.z;
    }

    /*private int index(int wx, int wy, int wz) {
        int rx = wx - playerChunkX;
        int ry = wy - playerChunkY;
        int rz = wz - playerChunkZ;

        int tx = ((rx % WIDTH)  + WIDTH)  % WIDTH;
        int ty = ((ry % HEIGHT) + HEIGHT) % HEIGHT;
        int tz = ((rz % DEPTH)  + DEPTH)  % DEPTH;

        return tx * HEIGHT * DEPTH + ty * DEPTH + tz;
    }
*/
    public Chunk getSlot(int wx, int wy, int wz) {
        return array[index(wx, wy, wz)];
    }
    
    public Chunk getSlotAt(int wx, int wy, int wz, int playerX, int playerY, int playerZ) {
        int rx = wx - playerX;
        int ry = wy - playerY;
        int rz = wz - playerZ;

        int tx = ((rx % WIDTH)  + WIDTH)  % WIDTH;
        int ty = ((ry % HEIGHT) + HEIGHT) % HEIGHT;
        int tz = ((rz % DEPTH)  + DEPTH)  % DEPTH;

        return array[tx * HEIGHT * DEPTH + ty * DEPTH + tz];
    }
    
 // Replace your existing index method with this
    private int index(int wx, int wy, int wz) {
        // Math.floorMod handles negative world coordinates correctly
        int tx = floorMod(wx, WIDTH);
        int ty = floorMod(wy, HEIGHT);
        int tz = floorMod(wz, DEPTH);
        
        return tx * (HEIGHT * DEPTH) + ty * DEPTH + tz;
    }

    // Simplify this so it doesn't care about player position for the index
    public int getIndexAt(int wx, int wy, int wz) {
        return index(wx, wy, wz);
    }
    
    public void setSlot(int wx, int wy, int wz, Chunk chunk) {
        array[index(wx, wy, wz)] = chunk;
    }

    public void updateTorroid(Chunk chunk) {
    	int idx = this.index(chunk.x, chunk.y, chunk.z);
    	chunk.cacheIndex = idx;
    	array[idx] = chunk;
    }
    
    public void updateTorroidAtIndex(Chunk chunk) {
    	array[chunk.cacheIndex] = chunk;
    }

    public void clearSlot(int index) {
        array[index] = null;
    }

    public Chunk[] getRenderToroid() {
        return array;
    }

    public void updateChunkPos(Vector3f pos) {
        playerChunkX = (int)pos.x;
        playerChunkY = (int)pos.y;
        playerChunkZ = (int)pos.z;
    }
    
    public static int floorMod(int x, int y) {
        int r = x % y;
        // If the signs are different and the remainder is non-zero, 
        // we need to adjust the result.
        if ((x ^ y) < 0 && r != 0) {
            r += y;
        }
        return r;
    }
}