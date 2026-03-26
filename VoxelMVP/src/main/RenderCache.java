package main;

import org.joml.Vector3f;
/*
public class RenderCache {
    private final int WIDTH  = Settings.RENDER_DISTANCE * 2 + 1;
    private final int HEIGHT = Settings.RENDER_HEIGHT   * 2 + 1;
    private final int DEPTH  = Settings.RENDER_DISTANCE * 2 + 1;

    private final Chunk[] array = new Chunk[WIDTH * HEIGHT * DEPTH];

    public RenderCache(Vector3f pos) {
    	
    }

    public Chunk getSlot(int wx, int wy, int wz) {
        return array[index(wx, wy, wz)];
    }
    
    private int index(int wx, int wy, int wz) {
        int tx = floorMod(wx, WIDTH);
        int ty = floorMod(wy, HEIGHT);
        int tz = floorMod(wz, DEPTH);
        
        return tx * (HEIGHT * DEPTH) + ty * DEPTH + tz;
    }

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
    
    public static int floorMod(int x, int y) {
        int r = x % y;
        if ((x ^ y) < 0 && r != 0) {
            r += y;
        }
        return r;
    }
}*/