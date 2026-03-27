package main;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import bufferManager.ChunkSSBO.Slot;
import saveHelper.TerrainGeneratorGPT;

public class WorldMap {
	private static final ConcurrentHashMap<Long, Chunk> chunks = new ConcurrentHashMap<Long, Chunk>();
	private static final ConcurrentLinkedQueue<short[]> blockPool = 
		    new ConcurrentLinkedQueue<short[]>();

    public static long key(int x, int y, int z) {
        return ((long)(x & 0xFFFFF) << 40) | ((long)(y & 0xFFFFF) << 20) | (z & 0xFFFFF);
    }
    
	//Sean
	private final long seed = 5116345970222046394L;
	
	private Random random = new Random();
	public static TerrainGeneratorGPT gen;
	
	public WorldMap() {
		gen = new TerrainGeneratorGPT(seed);
		
	}
	/*
	public void addToWorldMapViaIndex(int Index, Chunk chunk) {
		chunks[Index] = chunk;
	}
	
	public static int chunkIndex(int x, int y, int z) {
	    return x * Settings.WORLD_SIZE_HEIGHT * Settings.WORLD_SIZE_WIDTH + y * Settings.WORLD_SIZE_WIDTH + z;
	}*/

	public static Chunk getChunkDirect(int x, int y, int z) {
	    if (y < 0 || y >= Settings.WORLD_SIZE_HEIGHT) return null;
	    
	    long k = key(x, y, z);
	    Chunk existing = chunks.get(k);
	    if (existing != null) return existing;
	    
	    // Generate candidate
	    Chunk chunk = new Chunk();
	    
	    // Only put if absent - if another thread beat us, use theirs
	    Chunk winner = chunks.putIfAbsent(k, chunk);
	    if (winner != null) {
	        chunk.dispose(); // release the block array we just acquired
	        return winner;
	    }
	    return chunk;
	}
	
	public static void removeChunk(long key) {
		Chunk c = chunks.remove(key);
	    if (c != null) {
	    	c.dispose();
	    }
	}
	
	public static ConcurrentHashMap<Long, Chunk> getChunks() {
		return chunks;
	}
	
	public static Chunk getChunkIfExists(int x, int y, int z) {
	    if (y < 0 || y >= Settings.WORLD_SIZE_HEIGHT) return null;
	    return chunks.get(key(x, y, z));
	}
	
	public static short[][][] blockCache(Slot slot, Chunk centreChunk, short[][][] blockCache) {

	    for (int x = 0; x < Settings.blockCacheSize; x++)
	        for (int y = 0; y < Settings.blockCacheSize; y++)
	            java.util.Arrays.fill(blockCache[x][y], (short) 0);

	    int cx = slot.x;
	    int cy = slot.y;
	    int cz = slot.z;

	    // --- CENTRE ---
	    short[] c = centreChunk.blocks;
	    if (c == null) return blockCache;
	    for (int x = 0; x < 16; x++)
	    for (int y = 0; y < 16; y++)
	    for (int z = 0; z < 16; z++)
	        blockCache[x+1][y+1][z+1] = c[Chunk.blockIndex(x,y,z)];

	    // --- 6 FACES ---
	    Chunk top    = getChunkIfExists(cx,   cy+1, cz  );
	    Chunk bottom = getChunkIfExists(cx,   cy-1, cz  );
	    Chunk north  = getChunkIfExists(cx,   cy,   cz+1);
	    Chunk south  = getChunkIfExists(cx,   cy,   cz-1);
	    Chunk east   = getChunkIfExists(cx+1, cy,   cz  );
	    Chunk west   = getChunkIfExists(cx-1, cy,   cz  );

	    for (int x = 0; x < 16; x++)
	    for (int z = 0; z < 16; z++) {
	        blockCache[x+1][17][z+1] = getBlock(top,    x,  0,  z);
	        blockCache[x+1][0] [z+1] = getBlock(bottom, x,  15, z);
	    }
	    for (int x = 0; x < 16; x++)
	    for (int y = 0; y < 16; y++) {
	        blockCache[x+1][y+1][17] = getBlock(north, x, y, 0 );
	        blockCache[x+1][y+1][0]  = getBlock(south, x, y, 15);
	    }
	    for (int y = 0; y < 16; y++)
	    for (int z = 0; z < 16; z++) {
	        blockCache[17][y+1][z+1] = getBlock(east, 0,  y, z);
	        blockCache[0] [y+1][z+1] = getBlock(west, 15, y, z);
	    }

	    // --- 12 EDGES ---
	    Chunk ne = getChunkIfExists(cx+1, cy, cz+1);
	    Chunk nw = getChunkIfExists(cx-1, cy, cz+1);
	    Chunk se = getChunkIfExists(cx+1, cy, cz-1);
	    Chunk sw = getChunkIfExists(cx-1, cy, cz-1);

	    for (int y = 0; y < 16; y++) {
	        blockCache[17][y+1][17] = getBlock(ne, 0,  y, 0 );
	        blockCache[0] [y+1][17] = getBlock(nw, 15, y, 0 );
	        blockCache[17][y+1][0]  = getBlock(se, 0,  y, 15);
	        blockCache[0] [y+1][0]  = getBlock(sw, 15, y, 15);
	    }

	    Chunk topNorth = getChunkIfExists(cx,   cy+1, cz+1);
	    Chunk topSouth = getChunkIfExists(cx,   cy+1, cz-1);
	    Chunk topEast  = getChunkIfExists(cx+1, cy+1, cz  );
	    Chunk topWest  = getChunkIfExists(cx-1, cy+1, cz  );

	    for (int x = 0; x < 16; x++) {
	        blockCache[x+1][17][17] = getBlock(topNorth, x, 0, 0 );
	        blockCache[x+1][17][0]  = getBlock(topSouth, x, 0, 15);
	    }
	    for (int z = 0; z < 16; z++) {
	        blockCache[17][17][z+1] = getBlock(topEast, 0,  0, z);
	        blockCache[0] [17][z+1] = getBlock(topWest, 15, 0, z);
	    }

	    Chunk botNorth = getChunkIfExists(cx,   cy-1, cz+1);
	    Chunk botSouth = getChunkIfExists(cx,   cy-1, cz-1);
	    Chunk botEast  = getChunkIfExists(cx+1, cy-1, cz  );
	    Chunk botWest  = getChunkIfExists(cx-1, cy-1, cz  );

	    for (int x = 0; x < 16; x++) {
	        blockCache[x+1][0][17] = getBlock(botNorth, x, 15, 0 );
	        blockCache[x+1][0][0]  = getBlock(botSouth, x, 15, 15);
	    }
	    for (int z = 0; z < 16; z++) {
	        blockCache[17][0][z+1] = getBlock(botEast, 0,  15, z);
	        blockCache[0] [0][z+1] = getBlock(botWest, 15, 15, z);
	    }

	    // --- 8 CORNERS ---
	    Chunk topNE = getChunkIfExists(cx+1, cy+1, cz+1);
	    Chunk topNW = getChunkIfExists(cx-1, cy+1, cz+1);
	    Chunk topSE = getChunkIfExists(cx+1, cy+1, cz-1);
	    Chunk topSW = getChunkIfExists(cx-1, cy+1, cz-1);
	    Chunk botNE = getChunkIfExists(cx+1, cy-1, cz+1);
	    Chunk botNW = getChunkIfExists(cx-1, cy-1, cz+1);
	    Chunk botSE = getChunkIfExists(cx+1, cy-1, cz-1);
	    Chunk botSW = getChunkIfExists(cx-1, cy-1, cz-1);

	    blockCache[17][17][17] = getBlock(topNE, 0,  0,  0 );
	    blockCache[0] [17][17] = getBlock(topNW, 15, 0,  0 );
	    blockCache[17][17][0]  = getBlock(topSE, 0,  0,  15);
	    blockCache[0] [17][0]  = getBlock(topSW, 15, 0,  0 );
	    blockCache[17][0] [17] = getBlock(botNE, 0,  15, 0 );
	    blockCache[0] [0] [17] = getBlock(botNW, 15, 15, 0 );
	    blockCache[17][0] [0]  = getBlock(botSE, 0,  15, 15);
	    blockCache[0] [0] [0]  = getBlock(botSW, 15, 15, 15);

	    return blockCache;
	}
	
	private static short getBlock(Chunk chunk, int x, int y, int z) {
	    if (chunk == null) return 0;
	    short[] b = chunk.blocks;
	    if (b == null) return 0;
	    return b[Chunk.blockIndex(x, y, z)];
	}

	public static short[] acquireBlocks() {
	    short[] blocks = blockPool.poll();
	    if (blocks == null) {
	        blocks = new short[16 * 16 * 16];
	    } else {
	        java.util.Arrays.fill(blocks, (short)0); // clear before reuse
	    }
	    return blocks;
	}
	
	public static void releaseBlocks(short[] blocks) {
	    blockPool.offer(blocks);
	}
}
