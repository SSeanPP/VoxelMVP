package saveHelper;

import java.util.Random;

import main.Block;
import main.Chunk;

public class TerrainGeneratorClaude {

    private final int[] perm = new int[512];

    // Tune these to taste
    private static final int   SEA_LEVEL        = 128;
    private static final float TERRAIN_SCALE_XZ = 200.0f;
    private static final float TERRAIN_SCALE_Y  = 80.0f;
    private static final float DETAIL_SCALE_XZ  = 50.0f;
    private static final float DETAIL_SCALE_Y   = 20.0f;
    private static final float CAVE_SCALE        = 5f;
    private static final float CAVE_THRESHOLD    = 0.01f;
    private static final float OVERHANG_SCALE_XZ = 80.0f;
    private static final float OVERHANG_SCALE_Y  = 15.0f;

    public TerrainGeneratorClaude(long seed) {
        // Build a seeded permutation table — same seed = same terrain always
        int[] base = new int[256];
        for (int i = 0; i < 256; i++) base[i] = i;

        Random rng = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = base[i];
            base[i] = base[j];
            base[j] = tmp;
        }
        // Double the table to avoid index wrapping
        for (int i = 0; i < 512; i++) perm[i] = base[i & 255];
    }

    /**
     * Fill a chunk's block array based on world-space coordinates.
     * cx, cy, cz are chunk coordinates (not block coordinates).
     */
    public void generate(Chunk chunk, int cx, int cy, int cz) {
        short[][][] blocks = chunk.getBlocks();
        int chunkSize = chunk.chunkSize;

        for (int lx = 0; lx < chunkSize; lx++) {
            for (int lz = 0; lz < chunkSize; lz++) {

                // World-space XZ for this column
                int wx = cx * chunkSize + lx;
                int wz = cz * chunkSize + lz;

                // 2D heightmap noise — sets the broad terrain shape
                float heightNoise = octaveNoise2D(wx, wz, TERRAIN_SCALE_XZ, 4, 0.5f);
                int surfaceY = SEA_LEVEL + (int)(heightNoise * TERRAIN_SCALE_Y);

                // 2D overhang selector — controls where 3D noise dominates
                float overhangSelector = octaveNoise2D(wx + 1000, wz + 1000,
                        OVERHANG_SCALE_XZ, 2, 0.5f);
                // Remap to 0..1
                overhangSelector = (overhangSelector + 1.0f) * 0.5f;

                for (int ly = 0; ly < chunkSize; ly++) {
                    int wy = cy * chunkSize + ly;

                    // Base density: negative above surface, positive below
                    float density = (surfaceY - wy) / TERRAIN_SCALE_Y;

                    // 3D detail noise — creates overhangs where selector is high
                    float detail = noise3D(
                            wx / DETAIL_SCALE_XZ,
                            wy / DETAIL_SCALE_Y,
                            wz / DETAIL_SCALE_XZ);

                    // Blend: high overhangSelector = more 3D influence = overhangs
                    density += detail * overhangSelector * 1.2f;
                    
                    if (wy >= surfaceY-3) density = Math.max(density, 0.1f);
                    
                    if (density <= 0.0f) {
                        blocks[lx][ly][lz] = Block.air.id;
                        continue;
                    }

                    // Cave carving — two offset noise samples, classic worm caves
                    float cave1 = noise3D(
                            wx / CAVE_SCALE,
                            wy / CAVE_SCALE,
                            wz / CAVE_SCALE);
                    float cave2 = noise3D(
                            wx / CAVE_SCALE + 100,
                            wy / CAVE_SCALE + 100,
                            wz / CAVE_SCALE + 100);

                    // Carve when both samples are near zero (tube intersection)
                    float caveDensity = cave1 * cave1 + cave2 * cave2;
                    if (caveDensity < CAVE_THRESHOLD) {
                        blocks[lx][ly][lz] = Block.air.id;
                        continue;
                    }

                    // Block type by depth
                    if (wy > surfaceY - 4) {
                        blocks[lx][ly][lz] = Block.dirt.id;
                    } else {
                        blocks[lx][ly][lz] = Block.stone.id;
                    }
                }
            }
        }
        
        for (int lx=0; lx<chunkSize; lx++) {
            for (int lz=0; lz<chunkSize; lz++) {
                for (int ly=chunkSize-1; ly>=0; ly--) {
                    if (blocks[lx][ly][lz] != Block.air.id) {
                        blocks[lx][ly][lz] = Block.grass.id;
                        break;
                    }
                }
            }
        }
        
        chunk.needsUpdate = true;
    }

    // --- Noise internals ---

    private float octaveNoise2D(float x, float z, float scale, int octaves, float persistence) {
        float total = 0;
        float amplitude = 1.0f;
        float frequency = 1.0f;
        float maxValue = 0;
        for (int i = 0; i < octaves; i++) {
            total += noise2D(x * frequency / scale, z * frequency / scale) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2.0f;
        }
        return total / maxValue;
    }

    private float noise2D(float x, float z) {
        int X = (int)Math.floor(x) & 255;
        int Z = (int)Math.floor(z) & 255;
        x -= (float)Math.floor(x);
        z -= (float)Math.floor(z);
        float u = fade(x);
        float w = fade(z);
        int a  = perm[X]   + Z;
        int b  = perm[X+1] + Z;
        return lerp(w,
                lerp(u, grad2(perm[a],   x,   z),
                        grad2(perm[b],   x-1, z)),
                lerp(u, grad2(perm[a+1], x,   z-1),
                        grad2(perm[b+1], x-1, z-1)));
    }

    private float noise3D(float x, float y, float z) {
        int X = (int)Math.floor(x) & 255;
        int Y = (int)Math.floor(y) & 255;
        int Z = (int)Math.floor(z) & 255;
        x -= (float)Math.floor(x);
        y -= (float)Math.floor(y);
        z -= (float)Math.floor(z);
        float u = fade(x), v = fade(y), w = fade(z);
        int A  = perm[X]+Y,   AA = perm[A]+Z,   AB = perm[A+1]+Z;
        int B  = perm[X+1]+Y, BA = perm[B]+Z,   BB = perm[B+1]+Z;
        return lerp(w,
                lerp(v, lerp(u, grad(perm[AA],  x,  y,  z),
                                grad(perm[BA],  x-1,y,  z)),
                        lerp(u, grad(perm[AB],  x,  y-1,z),
                                grad(perm[BB],  x-1,y-1,z))),
                lerp(v, lerp(u, grad(perm[AA+1],x,  y,  z-1),
                                grad(perm[BA+1],x-1,y,  z-1)),
                        lerp(u, grad(perm[AB+1],x,  y-1,z-1),
                                grad(perm[BB+1],x-1,y-1,z-1))));
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    private static float grad(int hash, float x, float y, float z) {
        int h = hash & 15;
        float u = h < 8 ? x : y;
        float v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    private static float grad2(int hash, float x, float z) {
        int h = hash & 3;
        float u = (h & 1) == 0 ? x : -x;
        float v = (h & 2) == 0 ? z : -z;
        return u + v;
    }
}