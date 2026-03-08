package main;

import org.joml.Matrix4f;

import bufferManager.Allocation;

public class Chunk {
	public Allocation allocation = null; 
	
	public final ChunkCoord chunkCoord;
	public final Matrix4f modelMatrix;
	
	public final int chunkSize = 16;
	
	public short[][][] blocks = new short[chunkSize][chunkSize][chunkSize];
	
	public Chunk(ChunkCoord coord) {
		this.chunkCoord = coord;
		
		for(int i = 0; i < chunkSize; i++) {
			for (int k = 0; k < chunkSize; k++) {
				blocks[i][15][k] = Block.grass.id;
				blocks[i][14][k] = Block.dirt.id;
				blocks[i][13][k] = Block.dirt.id;
				blocks[i][12][k] = Block.dirt.id;
				blocks[i][11][k] = Block.dirt.id;
				blocks[i][10][k] = Block.stone.id;
				blocks[i][9][k] = Block.stone.id;
				blocks[i][8][k] = Block.stone.id;
				blocks[i][7][k] = Block.stone.id;
				blocks[i][6][k] = Block.stone.id;
				blocks[i][5][k] = Block.stone.id;
				blocks[i][4][k] = Block.stone.id;
				blocks[i][3][k] = Block.stone.id;
				blocks[i][2][k] = Block.stone.id;
				blocks[i][1][k] = Block.stone.id;
				blocks[i][0][k] = Block.stone.id;
				
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
	
	public short[][][] getBlocks() {
		return blocks;
	}
}
