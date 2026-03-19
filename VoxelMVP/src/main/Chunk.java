package main;

import org.joml.Matrix4f;

import bufferManager.Allocation;

public class Chunk {
	
	public boolean hasBlocks = false;
	public Allocation allocation = null; 
	public Chunk Previous = null;
	
	public final int x;
	public final int y;
	public final int z;
	
	public final Matrix4f modelMatrix;
	
	public final int chunkSize = 16;
	//public boolean needsUpdate;
	public boolean queuedForMeshing;
	public int cacheIndex;
	
	public short[] blocks = new short[16 * 16 * 16];
	
	public Chunk(int xi, int yj, int zk) {
		//this.needsUpdate = true;
		this.queuedForMeshing = false;
		
		this.x = xi;
		this.y = yj;
		this.z= zk;
		
		modelMatrix = new Matrix4f().translation(x * chunkSize, y * chunkSize, z * chunkSize);
	}
	
	public Allocation getAllocation() {
		return allocation;
	}
	
	public void setAllocation(Allocation newAlloc) {
		this.allocation = newAlloc;
	}
	
	public short[] getBlocks() {
		return blocks;
	}
	
	public static int blockIndex(int x, int y, int z) {
	    return x << 8 | y << 4 | z;
	}
	
	@Override
	public int hashCode() {
	    return x * 31 * 31 + y * 31 + z;
	}

	@Override
	public boolean equals(Object o) {
	    if (!(o instanceof Chunk)) return false;
	    Chunk other = (Chunk) o;
	    return x == other.x && y == other.y && z == other.z;
	}
}
