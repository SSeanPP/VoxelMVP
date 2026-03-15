package main;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WorldMap {
	private static final Map<Long, Chunk> chunks = new HashMap<Long, Chunk>();
	public static final int blockCacheSize = 18;
	
	private final int worldSize = 32;
	private final int worldHeight = 16;
	
	private Random random = new Random();
	
	public WorldMap() {
		for(int x = 0; x <worldSize; x++) {
        	for(int y = 0; y < worldHeight; y++) {
        		for (int z = 0; z < worldSize; z++) {
            		Chunk chunk = new Chunk(ChunkCoord.pack(x,y,z));
        			addToWorldMap(ChunkCoord.pack(x,y,z), chunk);
            	}
        	}
        	
        }
	}
	
	public void addToWorldMap(long coords, Chunk chunk) {
		chunks.put(coords, chunk);
	}
	
	public Chunk getChunk(long coords) {
		return chunks.get(coords);
	}
	
	public static Map<Long, Chunk> getChunks() {
		return chunks;
	}
	
	public static short[][][] blockCache(long chunkCoords, Chunk centreChunk, short[][][] blockCache) {
		
		for (int x = 0; x < WorldMap.blockCacheSize; x++) {
		    for (int y = 0; y < WorldMap.blockCacheSize; y++) {
		        java.util.Arrays.fill(blockCache[x][y], (short) 0);
		    }
		}
		
		int centreX = ChunkCoord.unpackX(chunkCoords);
		int centreY = ChunkCoord.unpackY(chunkCoords);
		int centreZ = ChunkCoord.unpackZ(chunkCoords);
		
		short[][][] centreBlocks = centreChunk.getBlocks();

		for (int x = 0; x < 16; x++)
		for (int y = 0; y < 16; y++)
		for (int z = 0; z < 16; z++)
		{
		    blockCache[x+1][y+1][z+1] = centreBlocks[x][y][z];
		}
		
		//top
		Chunk top = chunks.get(ChunkCoord.pack(centreX, centreY+1, centreZ));

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
		
		Chunk bottom = chunks.get(ChunkCoord.pack(centreX, centreY-1, centreZ));

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
		Chunk north = chunks.get(ChunkCoord.pack(centreX, centreY, centreZ+1));

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
		Chunk east = chunks.get(ChunkCoord.pack(centreX+1, centreY, centreZ));

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
		Chunk south = chunks.get(ChunkCoord.pack(centreX, centreY, centreZ-1));

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
		Chunk west = chunks.get(ChunkCoord.pack(centreX-1, centreY, centreZ));

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
		
		return getChunk(ChunkCoord.pack(x, y, -z));
	}
	
	public static boolean allMeshed() {

	    for (Chunk chunk : chunks.values()) {
	        if (chunk.needsUpdate || chunk.queuedForMeshing) {
	            return false;
	        }
	    }

	    return true;
	}
}
