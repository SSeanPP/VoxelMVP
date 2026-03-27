package main;

public class Chunk {
	
	public volatile boolean hasBlocks = false;
	public volatile boolean pendingDisposal = false;
	//public boolean needsUpdate;
	
	public volatile short[] blocks;
	public volatile boolean hasGenned;
	
	public Chunk() {
		this.hasGenned = false;
		blocks = WorldMap.acquireBlocks();
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
