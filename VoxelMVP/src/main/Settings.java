package main;

import org.joml.Vector3f;

// Immutable at runtime - buffer sizes, GL limits, fixed world dimensions
public final class Settings {
    public static final int RENDER_DISTANCE   = 32;
    public static final int RENDER_HEIGHT = 16;
    public static final int WORLD_SIZE_WIDTH      = 32;
    public static final int WORLD_SIZE_HEIGHT     = 16;
    public static final int CHUNK_SIZE        = 16;
    public static final long VBO_SIZE_BYTES    = 1024*1024*512; 
    public static final long EBO_SIZE_BYTES    = (512L * 1024L * 1024L * 3L/4l) + 1L;
    public static final int blockCacheSize = CHUNK_SIZE + 2;
    public static final int stride = 5 * 4;
    public static final Vector3f spawnPoint = new Vector3f((WORLD_SIZE_WIDTH * CHUNK_SIZE / 2),((WORLD_SIZE_HEIGHT * CHUNK_SIZE)+16),(WORLD_SIZE_WIDTH * CHUNK_SIZE / 2));
    public static final Vector3f spawnChunk = new Vector3f(spawnPoint.x/CHUNK_SIZE,spawnPoint.y/CHUNK_SIZE,spawnPoint.z/CHUNK_SIZE);

    private Settings() {} // uninstantiable
}
