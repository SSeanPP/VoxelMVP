package meshThreader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import bufferManager.SceneBufferManager;
import main.Block;
import main.Chunk;
import main.WorldMap;

public class MeshThread {
	
	private ArrayList<Integer> indices = new ArrayList<Integer>();
	private ArrayList<Float> vertices = new ArrayList<Float>();
	
	private SceneBufferManager bufferManager;
	
	private int bx = 0;
	private int by = 0;
	private int bz = 0;
	
	private final int TOP = 0;
	private final int BOTTOM = 1;
	private final int NORTH = 2;
	private final int SOUTH = 3;
	private final int EAST = 4;
	private final int WEST = 5;
	
	private final int ATLAS_SIZE = 16;   // tiles per row
	private final float TILE_SIZE = 1f / ATLAS_SIZE;
	
	public MeshThread (SceneBufferManager manager) {
		this.bufferManager = manager;
	}
	
	/*
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (true) {
            Chunk chunk = MeshQueue.meshInputQueue.poll();
            if (chunk != null) {
                meshChunk(chunk);
            } else {
            	
            }
        }
	}*/
	
	public void meshChunk(Chunk chunk) {
		indices.clear();
		vertices.clear();
		
		short[][][] blocks = WorldMap.blockCache(chunk.chunkCoord, chunk);
		int chunkSize = chunk.chunkSize;
		
		for(int x = 1; x < chunkSize+1; x++) {
			for (int y = 1; y < chunkSize+1; y++) {
				for (int z = 1; z < chunkSize+1; z++) {
					short id = blocks[x][y][z];
					if(id == Block.air.id) continue;

					Block block = Block.blockRegister[id];
					
					bx = x - 1;
					by = y - 1;
					bz = z - 1;
					//top
					if(blocks[x][y+1][z] == Block.air.id) {
						addFace(TOP, bx, by, bz, block);
					}
					
					//bottom
					if(blocks[x][y-1][z] == Block.air.id) {
						addFace(BOTTOM, bx, by, bz, block);
					}
					
					//N
					if(blocks[x][y][z+1] == Block.air.id) {
						addFace(NORTH, bx, by, bz, block);
					}
					
					//E
					if(blocks[x+1][y][z] == Block.air.id) {
						addFace(EAST, bx, by, bz, block);
					}
					
					//S
					if(blocks[x][y][z-1] == Block.air.id) {
						addFace(SOUTH, bx, by, bz, block);
					}
					
					//W
					if(blocks[x-1][y][z] == Block.air.id) {
						addFace(WEST, bx, by, bz, block);
					}
				}
			}
		}
		
		int vertexSizeBytes = vertices.size() * 4;
		int indexSizeBytes = indices.size() * 4;
		
		int paddedVertexBytes = bufferManager.alignVertex((int)(vertexSizeBytes * 1.1));
		int paddedIndexBytes  = bufferManager.alignIndex((int)(indexSizeBytes * 1.1));
		
		if (chunk.allocation == null) {
		    chunk.allocation = bufferManager.getAllocation(paddedVertexBytes, paddedIndexBytes);
		}

		int allocatedVertexSize = chunk.allocation.vertexLimit - chunk.allocation.vertexOffset;
		int allocatedIndexSize = chunk.allocation.indexLimit - chunk.allocation.indexOffset;
		if (allocatedVertexSize < paddedVertexBytes || allocatedIndexSize < paddedIndexBytes) {
			
			bufferManager.free(chunk.allocation);
		    chunk.allocation = bufferManager.getAllocation(
		    		paddedVertexBytes, paddedIndexBytes
		    );
		}

		ByteBuffer sliceVBO = bufferManager.getVBOSlice(chunk.allocation);
		//System.out.println("Slice position: " + sliceVBO.position() + " limit: " + sliceVBO.limit());
		ByteBuffer sliceEBO = bufferManager.getEBOSlice(chunk.allocation);
		
		
		FloatBuffer fb = sliceVBO.order(ByteOrder.nativeOrder()).asFloatBuffer();
		for(float f : vertices) fb.put(f);
		
		//System.out.println("FB after write: " + fb.get(0) + ", " + fb.get(1) + ", " + fb.get(2));
		
		IntBuffer ib = sliceEBO.order(ByteOrder.nativeOrder()).asIntBuffer();
		for(int i : indices) ib.put(i);
		//ib.flip();
		
		chunk.allocation.setCounts(indices.size());
	}
	
	private final float[][][] FACE_VERTICES = {
	    // TOP
	    {
	        {0,1,0},
	        {1,1,0},
	        {1,1,1},
	        {0,1,1}
	    },

	    // BOTTOM
	    {
	        {0,0,0},
	        {0,0,1},
	        {1,0,1},
	        {1,0,0}
	    },

	    // NORTH (+Z)
	    {
	        {0,0,1},
	        {0,1,1},
	        {1,1,1},
	        {1,0,1}
	    },

	    // SOUTH (-Z)
	    {
	        {0,0,0},
	        {1,0,0},
	        {1,1,0},
	        {0,1,0}
	    },

	    // EAST (+X)
	    {
	        {1,0,0},
	        {1,0,1},
	        {1,1,1},
	        {1,1,0}
	    },

	    // WEST (-X)
	    {
	        {0,0,0},
	        {0,1,0},
	        {0,1,1},
	        {0,0,1}
	    }
	};
	
	private final int[] FACE_INDICES = {
		    0,2,1,
		    0,3,2
		};
	
	private void addFace(int face, int x, int y, int z, Block block)
	{
		
	    int offset = vertices.size() / 5;

	    float[][] verts = FACE_VERTICES[face];

	    int texIndex = getFaceTexture(block, face);

	    int tileX = texIndex % ATLAS_SIZE;
	    int tileY = texIndex / ATLAS_SIZE;

	    float u = tileX * TILE_SIZE;
	    float v = 1.0f - (tileY * TILE_SIZE) - TILE_SIZE;

	    float[][] rawUVs = FACE_UVS[face];
	    float[][] uvs = new float[4][2];
	    for (int i = 0; i < 4; i++) {
	        uvs[i][0] = u + rawUVs[i][0] * TILE_SIZE;
	        uvs[i][1] = v + rawUVs[i][1] * TILE_SIZE;
	    }

	    for(int i=0;i<4;i++)
	    {
	        vertices.add(x + verts[i][0]);
	        vertices.add(y + verts[i][1]);
	        vertices.add(z + verts[i][2]);

	        vertices.add(uvs[i][0]);
	        vertices.add(uvs[i][1]);
	    }

	    for(int i : FACE_INDICES)
	        indices.add(offset + i);
	}
	
	private int getFaceTexture(Block block, int face)
	{
	    switch(face)
	    {
	        case TOP:
	            return block.topTexture;

	        case BOTTOM:
	            return block.bottomTexture;

	        default:
	            return block.sideTexture;
	    }
	}

	private final float[][][] FACE_UVS = {
		    // TOP - looking down, x increases right, z increases down
		    {{0,1},{1,1},{1,0},{0,0}},
		    // BOTTOM - looking up, x increases right, z increases up  
		    {{0,0},{1,0},{1,1},{0,1}},
		    // NORTH (+Z) - x increases right, y increases up
		    {{0,0},{0,1},{1,1},{1,0}},
		    // SOUTH (-Z) - x increases left, y increases up
		    {{1,0},{0,0},{0,1},{1,1}},
		    // EAST (+X) - z increases left, y increases up
		    {{1,0},{0,0},{0,1},{1,1}},
		    // WEST (-X) - z increases right, y increases up
		    {{0,0},{0,1},{1,1},{1,0}},
		};
	
	
}
