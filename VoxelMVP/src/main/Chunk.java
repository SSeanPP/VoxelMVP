package main;

public class Chunk {
	
	public boolean hasBlocks = false;
	public boolean pendingDisposal = false;
	
	
	public final int chunkSize = 16;
	//public boolean needsUpdate;
	
	public short[] blocks;
	public boolean hasGenned;
	
	public Chunk() {
		
		this.hasGenned = false;
		blocks = WorldMap.acquireBlocks();
	}
	
	
	public short[] getBlocks() {
		return blocks;
	}
	
	public static int blockIndex(int x, int y, int z) {
	    return x << 8 | y << 4 | z;
	}
	
	public void dispose() {
		if (blocks != null) {
		    WorldMap.releaseBlocks(blocks);
		    blocks = null;
		}
	}
}
