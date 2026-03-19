package meshThreader;

import java.util.concurrent.BlockingQueue;

import bufferManager.SceneBufferManager;
import main.Block;
import main.Chunk;
import main.WorldMap;

public class MeshThreadCulling extends MeshThread {

	private int bx = 0;
	private int by = 0;
	private int bz = 0;
	
	public MeshThreadCulling (SceneBufferManager manager, BlockingQueue<Chunk> queueInput) {
		this.bufferManager = manager;
		this.queue = queueInput;
	}
	
	
	public void meshChunk(Chunk chunk) {
		
		//chunk.needsUpdate = false;
		chunk.queuedForMeshing = false;
		
		vertexPtr = 0;
		indexPtr = 0;
		
		WorldMap.blockCache(WorldMap.chunkIndex(chunk.x, chunk.y, chunk.z), chunk, localBlockCache);
		int chunkSize = chunk.chunkSize;
		
		for(int x = 1; x < chunkSize+1; x++) {
			for (int y = 1; y < chunkSize+1; y++) {
				for (int z = 1; z < chunkSize+1; z++) {
					short id = localBlockCache[x][y][z];
					if(id == Block.air.id) continue;

					Block block = Block.blockRegister[id];
					
					bx = x - 1;
					by = y - 1;
					bz = z - 1;
					//top
					if(localBlockCache[x][y+1][z] == Block.air.id) {
						addFace(TOP, bx, by, bz, block);
					}
					
					//bottom
					if(localBlockCache[x][y-1][z] == Block.air.id) {
						addFace(BOTTOM, bx, by, bz, block);
					}
					
					//N
					if(localBlockCache[x][y][z+1] == Block.air.id) {
						addFace(NORTH, bx, by, bz, block);
					}
					
					//E
					if(localBlockCache[x+1][y][z] == Block.air.id) {
						addFace(EAST, bx, by, bz, block);
					}
					
					//S
					if(localBlockCache[x][y][z-1] == Block.air.id) {
						addFace(SOUTH, bx, by, bz, block);
					}
					
					//W
					if(localBlockCache[x-1][y][z] == Block.air.id) {
						addFace(WEST, bx, by, bz, block);
					}
				}
			}
		}
		
		uploadToGPU(chunk);
		
	}
	
	private static final float[][][] FACE_VERTICES = {
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
	
	private static final int[] FACE_INDICES = {
		    0,2,1,
		    0,3,2
		};
	
	private void addFace(int face, int x, int y, int z, Block block)
	{
		
		int offset = vertexPtr / 5;

	    float[][] verts = FACE_VERTICES[face];

	    int texIndex = getFaceTexture(block, face);

	    int tileX = texIndex % ATLAS_SIZE;
	    int tileY = texIndex / ATLAS_SIZE;

	    float u = tileX * TILE_SIZE;
	    float v = 1.0f - (tileY * TILE_SIZE) - TILE_SIZE;

	    float[][] rawUVs = FACE_UVS[face];

	    for (int i = 0; i < 4; i++)
	    {
	        vertices[vertexPtr++] = x + verts[i][0];
	        vertices[vertexPtr++] = y + verts[i][1];
	        vertices[vertexPtr++] = z + verts[i][2];

	        vertices[vertexPtr++] = u + rawUVs[i][0] * TILE_SIZE;
	        vertices[vertexPtr++] = v + rawUVs[i][1] * TILE_SIZE;
	    }

	    for(int i : FACE_INDICES)
	    	indices[indexPtr++] = offset + i;
	}
	
	private static int getFaceTexture(Block block, int face)
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

	private static final float[][][] FACE_UVS = {
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
