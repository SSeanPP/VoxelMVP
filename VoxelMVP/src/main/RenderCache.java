package main;

import java.util.concurrent.ConcurrentHashMap;
import org.joml.Vector3f;

public class RenderCache {
    private final ConcurrentHashMap<Long, Chunk> loadedChunks = new ConcurrentHashMap<Long, Chunk>();
    
    private volatile int playerChunkX;
    private volatile int playerChunkY;
    private volatile int playerChunkZ;

    RenderCache(Vector3f pos) {
        playerChunkX = (int) pos.x;
        playerChunkY = (int) pos.y;
        playerChunkZ = (int) pos.z;
    }

    private static long key(int x, int y, int z) {
        return ((long)(x & 0xFFFFF) << 40) | ((long)(y & 0xFFFFF) << 20) | (z & 0xFFFFF);
    }

    public void updateTorroid(Chunk chunk) {
        loadedChunks.put(key(chunk.x, chunk.y, chunk.z), chunk);
    }

    public void clearSlot(int x, int y, int z) {
        loadedChunks.remove(key(x, y, z));
    }

    public void clearAll() {
        loadedChunks.clear();
    }

    public Iterable<Chunk> getRenderToroid() {
        return loadedChunks.values();
    }

    public void updateChunkPos(Vector3f pos) {
        playerChunkX = (int) pos.x;
        playerChunkY = (int) pos.y;
        playerChunkZ = (int) pos.z;
    }

    public int getPlayerChunkX() { return playerChunkX; }
    public int getPlayerChunkY() { return playerChunkY; }
    public int getPlayerChunkZ() { return playerChunkZ; }
    
    public boolean contains(long key) {
        return loadedChunks.containsKey(key);
    }
}