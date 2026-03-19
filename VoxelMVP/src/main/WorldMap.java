package main;

import java.util.Random;

import saveHelper.TerrainGeneratorClaude;
import saveHelper.TerrainGeneratorGPT;

public class WorldMap {
	private static final Chunk[] chunks = new Chunk[Settings.WORLD_SIZE_WIDTH * Settings.WORLD_SIZE_HEIGHT * Settings.WORLD_SIZE_WIDTH];
	
	//Sean
	private final long seed = 5116345970222046394L;
	
	private Random random = new Random();
	
	public WorldMap() {
		TerrainGeneratorGPT gen = new TerrainGeneratorGPT(seed);
		
		for(int x = 0; x <Settings.WORLD_SIZE_WIDTH; x++) {
        	for(int y = 0; y < Settings.WORLD_SIZE_HEIGHT ; y++) {
        		for (int z = 0; z < Settings.WORLD_SIZE_WIDTH; z++) {
        			Chunk chunk = new Chunk(x, y, z);
        			gen.generate(chunk, x, y, z);
                    chunks[chunkIndex(x, y, z)] = chunk;
            	}
        	}
        	
        }
	}
	
	public void addToWorldMapViaIndex(int Index, Chunk chunk) {
		chunks[Index] = chunk;
	}
	
	public static int chunkIndex(int x, int y, int z) {
	    return x * Settings.WORLD_SIZE_HEIGHT * Settings.WORLD_SIZE_WIDTH + y * Settings.WORLD_SIZE_WIDTH + z;
	}

	public static Chunk getChunkDirect(int x, int y, int z) {
	    if (x < 0 || x >= Settings.WORLD_SIZE_WIDTH || y < 0 || y >= Settings.WORLD_SIZE_HEIGHT || z < 0 || z >= Settings.WORLD_SIZE_WIDTH)
	        return null;
	    return chunks[chunkIndex(x, y, z)];
	}
	
	public static Chunk[] getChunks() {
		return chunks;
	}
	
	public static short[][][] blockCache(long chunkCoords, Chunk centreChunk, short[][][] blockCache) {

	    for (int x = 0; x < Settings.blockCacheSize; x++)
	        for (int y = 0; y < Settings.blockCacheSize; y++)
	            java.util.Arrays.fill(blockCache[x][y], (short) 0);

	    int cx = centreChunk.x;
	    int cy = centreChunk.y;
	    int cz = centreChunk.z;

	    // --- CENTRE ---
	    short[] c = centreChunk.getBlocks();
	    for (int x = 0; x < 16; x++)
	    for (int y = 0; y < 16; y++)
	    for (int z = 0; z < 16; z++)
	        blockCache[x+1][y+1][z+1] = c[Chunk.blockIndex(x,y,z)];

	    // --- 6 FACES ---
	    Chunk top    = getChunkDirect(cx,   cy+1, cz  );
	    Chunk bottom = getChunkDirect(cx,   cy-1, cz  );
	    Chunk north  = getChunkDirect(cx,   cy,   cz+1);
	    Chunk south  = getChunkDirect(cx,   cy,   cz-1);
	    Chunk east   = getChunkDirect(cx+1, cy,   cz  );
	    Chunk west   = getChunkDirect(cx-1, cy,   cz  );

	    if (top != null) {
	        short[] b = top.getBlocks();
	        for (int x = 0; x < 16; x++)
	        for (int z = 0; z < 16; z++)
	            blockCache[x+1][17][z+1] = b[Chunk.blockIndex(x,0,z)];
	    }
	    if (bottom != null) {
	        short[] b = bottom.getBlocks();
	        for (int x = 0; x < 16; x++)
	        for (int z = 0; z < 16; z++)
	            blockCache[x+1][0][z+1] = b[Chunk.blockIndex(x,15,z)];
	    }
	    if (north != null) {
	        short[] b = north.getBlocks();
	        for (int x = 0; x < 16; x++)
	        for (int y = 0; y < 16; y++)
	            blockCache[x+1][y+1][17] = b[Chunk.blockIndex(x,y,0)];
	    }
	    if (south != null) {
	        short[] b = south.getBlocks();
	        for (int x = 0; x < 16; x++)
	        for (int y = 0; y < 16; y++)
	            blockCache[x+1][y+1][0] = b[Chunk.blockIndex(x,y,15)];
	    }
	    if (east != null) {
	        short[] b = east.getBlocks();
	        for (int y = 0; y < 16; y++)
	        for (int z = 0; z < 16; z++)
	            blockCache[17][y+1][z+1] = b[Chunk.blockIndex(0,y,z)];
	    }
	    if (west != null) {
	        short[] b = west.getBlocks();
	        for (int y = 0; y < 16; y++)
	        for (int z = 0; z < 16; z++)
	            blockCache[0][y+1][z+1] = b[Chunk.blockIndex(15,y,z)];
	    }

	    // --- 12 EDGES ---
	    // 4 vertical edges (Y axis edges, varying X and Z)
	    Chunk ne = getChunkDirect(cx+1, cy, cz+1);
	    Chunk nw = getChunkDirect(cx-1, cy, cz+1);
	    Chunk se = getChunkDirect(cx+1, cy, cz-1);
	    Chunk sw = getChunkDirect(cx-1, cy, cz-1);

	    if (ne != null) {
	        short[] b = ne.getBlocks();
	        for (int y = 0; y < 16; y++)
	            blockCache[17][y+1][17] = b[Chunk.blockIndex(0,y,0)];
	    }
	    if (nw != null) {
	        short[] b = nw.getBlocks();
	        for (int y = 0; y < 16; y++)
	            blockCache[0][y+1][17] = b[Chunk.blockIndex(15,y,0)];
	    }
	    if (se != null) {
	        short[] b = se.getBlocks();
	        for (int y = 0; y < 16; y++)
	            blockCache[17][y+1][0] = b[Chunk.blockIndex(0,y,15)];
	    }
	    if (sw != null) {
	        short[]b = sw.getBlocks();
	        for (int y = 0; y < 16; y++)
	            blockCache[0][y+1][0] = b[Chunk.blockIndex(15,y,15)];
	    }

	    // 4 top edges
	    Chunk topNorth = getChunkDirect(cx,   cy+1, cz+1);
	    Chunk topSouth = getChunkDirect(cx,   cy+1, cz-1);
	    Chunk topEast  = getChunkDirect(cx+1, cy+1, cz  );
	    Chunk topWest  = getChunkDirect(cx-1, cy+1, cz  );

	    if (topNorth != null) {
	        short[] b = topNorth.getBlocks();
	        for (int x = 0; x < 16; x++)
	            blockCache[x+1][17][17] = b[Chunk.blockIndex(x,0,0)];
	    }
	    if (topSouth != null) {
	        short[] b = topSouth.getBlocks();
	        for (int x = 0; x < 16; x++)
	            blockCache[x+1][17][0] = b[Chunk.blockIndex(x,0,15)];
	    }
	    if (topEast != null) {
	        short[] b = topEast.getBlocks();
	        for (int z = 0; z < 16; z++)
	            blockCache[17][17][z+1] = b[Chunk.blockIndex(0,0,z)];
	    }
	    if (topWest != null) {
	        short[] b = topWest.getBlocks();
	        for (int z = 0; z < 16; z++)
	            blockCache[0][17][z+1] = b[Chunk.blockIndex(15,0,z)];
	    }

	    // 4 bottom edges
	    Chunk botNorth = getChunkDirect(cx,   cy-1, cz+1);
	    Chunk botSouth = getChunkDirect(cx,   cy-1, cz-1);
	    Chunk botEast  = getChunkDirect(cx+1, cy-1, cz  );
	    Chunk botWest  = getChunkDirect(cx-1, cy-1, cz  );

	    if (botNorth != null) {
	        short[] b = botNorth.getBlocks();
	        for (int x = 0; x < 16; x++)
	            blockCache[x+1][0][17] = b[Chunk.blockIndex(x,15,0)];
	    }
	    if (botSouth != null) {
	        short[] b = botSouth.getBlocks();
	        for (int x = 0; x < 16; x++)
	            blockCache[x+1][0][0] = b[Chunk.blockIndex(x,15,15)];
	    }
	    if (botEast != null) {
	        short[] b = botEast.getBlocks();
	        for (int z = 0; z < 16; z++)
	            blockCache[17][0][z+1] = b[Chunk.blockIndex(0,15,z)];
	    }
	    if (botWest != null) {
	        short[] b = botWest.getBlocks();
	        for (int z = 0; z < 16; z++)
	            blockCache[0][0][z+1] = b[Chunk.blockIndex(15,15,z)];
	    }

	    // --- 8 CORNERS ---
	    Chunk topNE = getChunkDirect(cx+1, cy+1, cz+1);
	    Chunk topNW = getChunkDirect(cx-1, cy+1, cz+1);
	    Chunk topSE = getChunkDirect(cx+1, cy+1, cz-1);
	    Chunk topSW = getChunkDirect(cx-1, cy+1, cz-1);
	    Chunk botNE = getChunkDirect(cx+1, cy-1, cz+1);
	    Chunk botNW = getChunkDirect(cx-1, cy-1, cz+1);
	    Chunk botSE = getChunkDirect(cx+1, cy-1, cz-1);
	    Chunk botSW = getChunkDirect(cx-1, cy-1, cz-1);

	    if (topNE != null) blockCache[17][17][17] = topNE.getBlocks()[Chunk.blockIndex(0,0,0)];
	    if (topNW != null) blockCache[0][17][17]  = topNW.getBlocks()[Chunk.blockIndex(15,0,0)];
	    if (topSE != null) blockCache[17][17][0]  = topSE.getBlocks()[Chunk.blockIndex(0,0,15)];
	    if (topSW != null) blockCache[0][17][0]   = topSW.getBlocks()[Chunk.blockIndex(0,0,0)];
	    if (botNE != null) blockCache[17][0][17]  = botNE.getBlocks()[Chunk.blockIndex(0,15,0)];
	    if (botNW != null) blockCache[0][0][17]   = botNW.getBlocks()[Chunk.blockIndex(15,15,0)];
	    if (botSE != null) blockCache[17][0][0]   = botSE.getBlocks()[Chunk.blockIndex(0,15,15)];
	    if (botSW != null) blockCache[0][0][0]    = botSW.getBlocks()[Chunk.blockIndex(15,15,15)];

	    return blockCache;
	}
	 
	public Chunk getRandomChunk() {
		int x = random.nextInt(32);
		int y = random.nextInt(16);
		int z = random.nextInt(32);
		
		return getChunkDirect(x, y, -z);
	}
	
	public static boolean allMeshed() {

	    for (Chunk chunk : chunks) {
	        if(chunk != null) {
	        	if (chunk.queuedForMeshing) {
		            return false;
		        }
	        }
	    }

	    return true;
	}
	
	public static Chunk getChunkByKey(long key) {
	    int x = (int)((key >> 40) & 0xFFFFF);
	    int y = (int)((key >> 20) & 0xFFFFF);
	    int z = (int)(key & 0xFFFFF);
	    return getChunkDirect(x, y, z);
	}
}
