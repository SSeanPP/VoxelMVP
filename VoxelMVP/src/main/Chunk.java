package main;

import org.joml.Matrix4f;

import bufferManager.Allocation;

public class Chunk {
	public Allocation allocation = null; 
	
	public final ChunkCoord chunkCoord;
	public final Matrix4f modelMatrix;
	
	public final int chunkSize = 16;
	
	public Block[][][] blocks = new Block[chunkSize][chunkSize][chunkSize];
	
	public Chunk(ChunkCoord coord) {
		this.chunkCoord = coord;
		
		for(int i = 0; i < chunkSize; i++) {
			for (int k = 0; k < chunkSize; k++) {
				blocks[i][5][k] = Block.grass;
				blocks[i][4][k] = Block.dirt;
				blocks[i][3][k] = Block.dirt;
				blocks[i][2][k] = Block.dirt;
				blocks[i][1][k] = Block.stone;
				blocks[i][0][k] = Block.stone;
			}
		}
		
		modelMatrix = new Matrix4f().translation(coord.x * chunkSize, coord.y * chunkSize,coord.z * chunkSize);
	}
	
	public Allocation getAllocation() {
		return allocation;
	}
	
	public void setAllocation(Allocation newAlloc) {
		this.allocation = newAlloc;
	}
	
	public Block[][][] getBlocks() {
		return blocks;
	}
}
