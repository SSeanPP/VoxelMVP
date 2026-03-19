package saveHelper;


import main.Block;
import main.Chunk;

public class TerrainGeneratorGPT {

    private final long seed;

    private static final int SEA_LEVEL = 128+64;
    private static final float TERRAIN_SCALE = 50f;   // bigger = smoother hills
    private static final float HEIGHT_AMPLITUDE = 50f; // max height variation
    private static final float CAVE_SCALE = 15f;
    private static final float CAVE_THRESHOLD = 0.25f;

    public TerrainGeneratorGPT(long seed) {
        this.seed = seed;
    }

    public void generate(Chunk chunk, int cx, int cy, int cz) {
        short[] blocks = chunk.getBlocks();
        int size = chunk.chunkSize;

        for (int lx = 0; lx < size; lx++) {
            for (int lz = 0; lz < size; lz++) {
                int wx = cx * size + lx;
                int wz = cz * size + lz;

                // Smooth 2D heightmap
                float h = smoothNoise2D(wx / TERRAIN_SCALE, wz / TERRAIN_SCALE);
                int surfaceY = SEA_LEVEL + (int)(h * HEIGHT_AMPLITUDE);

                for (int ly = 0; ly < size; ly++) {
                    int wy = cy * size + ly;

                    // Air above surface
                    if (wy > surfaceY) {
                        blocks[Chunk.blockIndex(lx,ly,lz)] = Block.air.id;
                        continue;
                    }

                    // Caves only below surface
                    if (wy < surfaceY - 2) {
                        float cave = smoothNoise3D(wx / CAVE_SCALE, wy / CAVE_SCALE, wz / CAVE_SCALE);
                        if (cave > CAVE_THRESHOLD) {
                            blocks[Chunk.blockIndex(lx,ly,lz)] = Block.air.id;
                            continue;
                        }
                    }

                    // Block type
                    if (wy == surfaceY) {
                        blocks[Chunk.blockIndex(lx,ly,lz)] = Block.grass.id;
                    } else if (wy >= surfaceY - 3) {
                        blocks[Chunk.blockIndex(lx,ly,lz)] = Block.dirt.id;
                    } else {
                        blocks[Chunk.blockIndex(lx,ly,lz)] = Block.stone.id;
                    }
                }
            }
        }

        //chunk.needsUpdate = true;
    }

    // --- Smooth deterministic noise ---

    private float smoothNoise2D(float x, float z) {
        int xi = (int)Math.floor(x);
        int zi = (int)Math.floor(z);
        float xf = x - xi;
        float zf = z - zi;

        float n00 = hashNoise(xi,     zi);
        float n10 = hashNoise(xi + 1, zi);
        float n01 = hashNoise(xi,     zi + 1);
        float n11 = hashNoise(xi + 1, zi + 1);

        float u = fade(xf);
        float v = fade(zf);

        return lerp(v,
                lerp(u, n00, n10),
                lerp(u, n01, n11)
        );
    }

    private float smoothNoise3D(float x, float y, float z) {
        int xi = (int)Math.floor(x);
        int yi = (int)Math.floor(y);
        int zi = (int)Math.floor(z);

        float xf = x - xi;
        float yf = y - yi;
        float zf = z - zi;

        float n000 = hashNoise(xi,     yi,     zi);
        float n100 = hashNoise(xi + 1, yi,     zi);
        float n010 = hashNoise(xi,     yi + 1, zi);
        float n110 = hashNoise(xi + 1, yi + 1, zi);
        float n001 = hashNoise(xi,     yi,     zi + 1);
        float n101 = hashNoise(xi + 1, yi,     zi + 1);
        float n011 = hashNoise(xi,     yi + 1, zi + 1);
        float n111 = hashNoise(xi + 1, yi + 1, zi + 1);

        float u = fade(xf);
        float v = fade(yf);
        float w = fade(zf);

        float x00 = lerp(u, n000, n100);
        float x10 = lerp(u, n010, n110);
        float x01 = lerp(u, n001, n101);
        float x11 = lerp(u, n011, n111);

        float y0 = lerp(v, x00, x10);
        float y1 = lerp(v, x01, x11);

        return lerp(w, y0, y1);
    }

    // Deterministic hash-based noise [-1,1]
    private float hashNoise(int x, int z) {
        int h = x * 374761393 + z * 668265263 + (int)seed * 31;
        h = (h ^ (h >> 13)) * 1274126177;
        return 1.0f - ((h & 0x7fffffff) / 1073741824.0f);
    }

    private float hashNoise(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 83492791 + (int)seed * 31;
        h = (h ^ (h >> 13)) * 1274126177;
        return 1.0f - ((h & 0x7fffffff) / 1073741824.0f);
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }
}