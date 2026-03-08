package main;

import java.util.HashMap;
import java.util.Map;

public class WorldMap {
	private static final Map<ChunkCoord, Chunk> chunks = new HashMap<ChunkCoord, Chunk>();
	private static final int blockCacheSize = 18;
	
	public WorldMap() {
		
	}
	
	public void addToWorldMap(ChunkCoord coords, Chunk chunk) {
		chunks.put(coords, chunk);
	}
	
	public Chunk getChunk(ChunkCoord coords) {
		return chunks.get(coords);
	}
	
	public static Map<ChunkCoord, Chunk> getChunks() {
		return chunks;
	}
	
	public static short[][][] blockCache(ChunkCoord chunkCoords, Chunk centreChunk) {
		short[][][] blockCache = new short[blockCacheSize][blockCacheSize][blockCacheSize];
		
		short[][][] centreBlocks = centreChunk.getBlocks();

		for (int x = 0; x < 16; x++)
		for (int y = 0; y < 16; y++)
		for (int z = 0; z < 16; z++)
		{
		    blockCache[x+1][y+1][z+1] = centreBlocks[x][y][z];
		}
		
		//top
		Chunk top = chunks.get(new ChunkCoord(chunkCoords.x, chunkCoords.y+1, chunkCoords.z));

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
		
		Chunk bottom = chunks.get(new ChunkCoord(chunkCoords.x, chunkCoords.y-1, chunkCoords.z));

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
		Chunk north = chunks.get(new ChunkCoord(chunkCoords.x, chunkCoords.y, chunkCoords.z+1));

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
		Chunk east = chunks.get(new ChunkCoord(chunkCoords.x+1, chunkCoords.y, chunkCoords.z));

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
		Chunk south = chunks.get(new ChunkCoord(chunkCoords.x, chunkCoords.y, chunkCoords.z-1));

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
		Chunk west = chunks.get(new ChunkCoord(chunkCoords.x-1, chunkCoords.y, chunkCoords.z));

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
}
