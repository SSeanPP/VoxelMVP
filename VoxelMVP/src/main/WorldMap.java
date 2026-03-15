package main;

import java.util.Random;

import saveHelper.TerrainGeneratorClaude;
import saveHelper.TerrainGeneratorGPT;

public class WorldMap {
	public final static int worldSize = 32;
	public final static int worldHeight = 16;
	
	private static final Chunk[] chunks = new Chunk[worldSize * worldHeight * worldSize];
	public static final int blockCacheSize = 18;
	private final long seed = 12345L;
	
	private Random random = new Random();
	
	public WorldMap() {
		TerrainGeneratorGPT gen = new TerrainGeneratorGPT(seed);
		
		for(int x = 0; x <worldSize; x++) {
        	for(int y = 0; y < worldHeight; y++) {
        		for (int z = 0; z < worldSize; z++) {
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
	    return x * worldHeight * worldSize + y * worldSize + z;
	}

	public static Chunk getChunkDirect(int x, int y, int z) {
	    if (x < 0 || x >= worldSize || y < 0 || y >= worldHeight || z < 0 || z >= worldSize)
	        return null;
	    return chunks[chunkIndex(x, y, z)];
	}
	
	public static Chunk[] getChunks() {
		return chunks;
	}
	
	public static short[][][] blockCache(long chunkCoords, Chunk centreChunk, short[][][] blockCache) {
		
		for (int x = 0; x < WorldMap.blockCacheSize; x++) {
		    for (int y = 0; y < WorldMap.blockCacheSize; y++) {
		        java.util.Arrays.fill(blockCache[x][y], (short) 0);
		    }
		}
		
		int centreX = centreChunk.x;
		int centreY = centreChunk.y;
		int centreZ = centreChunk.z;
		
		short[][][] centreBlocks = centreChunk.getBlocks();

		for (int x = 0; x < 16; x++)
		for (int y = 0; y < 16; y++)
		for (int z = 0; z < 16; z++)
		{
		    blockCache[x+1][y+1][z+1] = centreBlocks[x][y][z];
		}
		
		//top
		Chunk top = getChunkDirect(centreX, centreY+1, centreZ);

		if (top != null)
		{
			short[][][] topBlocks = top.getBlocks();

		    for (int x = 0; x < 16; x++)
		    for (int z = 0; z < 16; z++)
		    {
		        blockCache[x+1][17][z+1] = topBlocks[x][0][z];
		    }
		}
		//bottom
		
		Chunk bottom = getChunkDirect(centreX, centreY-1, centreZ);

		if (bottom != null)
		{
			short[][][] bottomBlocks = bottom.getBlocks();

		    for (int x = 0; x < 16; x++)
		    for (int z = 0; z < 16; z++)
		    {
		        blockCache[x+1][0][z+1] = bottomBlocks[x][15][z];
		    }
		}
		//side
		
		//N
		Chunk north = getChunkDirect(centreX, centreY, centreZ+1);

		if (north != null)
		{
			short[][][] northBlocks = north.getBlocks();

		    for (int x = 0; x < 16; x++)
	    	for (int y = 0; y < 16; y++)
	    	{
	    	    blockCache[x+1][y+1][17] = northBlocks[x][y][0];
	    	}
		}
		
		//E
		Chunk east = getChunkDirect(centreX+1, centreY, centreZ);

		if (east != null)
		{
			short[][][] eastBlocks = east.getBlocks();

		    for (int y = 0; y < 16; y++)
	    	for (int z = 0; z < 16; z++)
	    	{
	    	    blockCache[17][y+1][z+1] = eastBlocks[0][y][z];
	    	}
		}
		//S
		Chunk south = getChunkDirect(centreX, centreY, centreZ-1);

		if (south != null)
		{
			short[][][] southBlocks = south.getBlocks();

		    for (int x = 0; x < 16; x++)
	    	for (int y = 0; y < 16; y++)
	    	{
	    	    blockCache[x+1][y+1][0] = southBlocks[x][y][15];
	    	}
		}
		
		//W
		Chunk west = getChunkDirect(centreX-1, centreY, centreZ);

		if (west != null)
		{
			short[][][] westBlocks = west.getBlocks();

		    for (int y = 0; y < 16; y++)
	    	for (int z = 0; z < 16; z++)
	    	{
	    	    blockCache[0][y+1][z+1] = westBlocks[15][y][z];
	    	}
		}
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
	        	if (chunk.needsUpdate || chunk.queuedForMeshing) {
		            return false;
		        }
	        }
	    }

	    return true;
	}
}
