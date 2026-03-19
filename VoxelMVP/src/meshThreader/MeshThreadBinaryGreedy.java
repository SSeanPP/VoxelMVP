package meshThreader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BlockingQueue;

import bufferManager.SceneBufferManager;
import main.Block;
import main.Chunk;
import main.Settings;
import main.WorldMap;

public class MeshThreadBinaryGreedy implements Runnable {

    // --- Infrastructure ---
    private final SceneBufferManager bufferManager;
    private final BlockingQueue<Chunk> queue;

    // --- Block cache ---
    private final short[][][] localBlockCache = new short[Settings.blockCacheSize][Settings.blockCacheSize][Settings.blockCacheSize];

    // --- Binary meshing working data - all instance fields, zero allocation per chunk ---
    private final long[][][] axisCols    = new long[3][18][18];
    private final long[][][] faceMasks   = new long[6][18][18];
    private final int[][][][] planes     = new int[6][32][16][16];
    private final int[][] planeMask = new int[6][32];

    // --- Block type compaction ---
    private final int[] blockIdToLocal  = new int[Block.blockRegister.length];
    private final int[] localToBlockId  = new int[32];
    private int localTypeCount = 0;

    // --- Greedy quad output ---
    private final GreedyQuad[] quadPool = new GreedyQuad[512];
    private int quadCount = 0;

    // --- Vertex/index output ---
    private final float[] vertices = new float[300000];
    private final int[]   indices  = new int[200000];
    private int vertexPtr = 0;
    private int indexPtr  = 0;

    // --- Atlas constants ---
    private static final int   ATLAS_SIZE = 16;
    private static final float TILE_SIZE  = 1f / ATLAS_SIZE;

    // --- Face constants - match your original ordering ---
    private static final int TOP    = 0;
    private static final int BOTTOM = 1;
    private static final int NORTH  = 2; // +Z
    private static final int SOUTH  = 3; // -Z
    private static final int EAST   = 4; // +X
    private static final int WEST   = 5; // -X

    public MeshThreadBinaryGreedy(SceneBufferManager manager, BlockingQueue<Chunk> queueInput) {
        this.bufferManager = manager;
        this.queue = queueInput;
        // pre-allocate quad pool
        for (int i = 0; i < quadPool.length; i++)
            quadPool[i] = new GreedyQuad();
    }

    // -------------------------------------------------------------------------
    // Runnable
    // -------------------------------------------------------------------------

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Chunk chunk = queue.take();
                meshChunk(chunk);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Main entry point
    // -------------------------------------------------------------------------

    public void meshChunk(Chunk chunk) {
        vertexPtr = 0;
        indexPtr  = 0;
        
        WorldMap.blockCache(WorldMap.chunkIndex(chunk.x, chunk.y, chunk.z), chunk, localBlockCache);

        buildAxisCols();
        buildFaceMasks();
        buildPlanes();
        emitAllQuads();
        uploadToGPU(chunk);
    }

    // -------------------------------------------------------------------------
    // Stage 1 — build axis-aligned column bitmasks from block cache
    // Each long represents one column of 18 blocks (16 + 1 padding each side)
    // Bit N is set if block N in that column is solid
    // -------------------------------------------------------------------------

    private void buildAxisCols() {
        // clear
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 18; j++) {
            	long[] row = axisCols[i][j];
            	for (int k = 0; k < 18; k++) row[k] = 0L;
            }


        for (int z = 0; z < 18; z++) {
            for (int y = 0; y < 18; y++) {
                for (int x = 0; x < 18; x++) {
                    if (localBlockCache[x][y][z] == Block.air.id) continue;
                    axisCols[0][z][x] |= 1L << y; // Y columns — for TOP/BOTTOM
                    axisCols[1][y][x] |= 1L << z; // Z columns — for NORTH/SOUTH
                    axisCols[2][y][z] |= 1L << x; // X columns — for EAST/WEST
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Stage 2 — derive face masks from axis columns
    // A face exists where a solid block is adjacent to an air block
    // col & ~(col << 1) = solid block with air BELOW it  → face on bottom
    // col & ~(col >> 1) = solid block with air ABOVE it  → face on top
    // -------------------------------------------------------------------------

    private void buildFaceMasks() {
        // clear
        for (int i = 0; i < 6; i++)
            for (int j = 0; j < 18; j++)
                java.util.Arrays.fill(faceMasks[i][j], 0L);

        // Y axis — TOP (face points +Y) and BOTTOM (face points -Y)
        // axisCols[0] indexed [z][x], bit = y
        for (int z = 0; z < 18; z++) {
            for (int x = 0; x < 18; x++) {
                long col = axisCols[0][z][x];
                faceMasks[TOP   ][z][x] = col & ~(col >>> 1); // solid with air above
                faceMasks[BOTTOM][z][x] = col & ~(col << 1);  // solid with air below
            }
        }

        // Z axis — NORTH (face points +Z) and SOUTH (face points -Z)
        // axisCols[1] indexed [y][x], bit = z
        for (int y = 0; y < 18; y++) {
            for (int x = 0; x < 18; x++) {
                long col = axisCols[1][y][x];
                faceMasks[NORTH][y][x] = col & ~(col >>> 1); // solid with air at +Z
                faceMasks[SOUTH][y][x] = col & ~(col << 1);  // solid with air at -Z
            }
        }

        // X axis — EAST (face points +X) and WEST (face points -X)
        // axisCols[2] indexed [y][z], bit = x
        for (int y = 0; y < 18; y++) {
            for (int z = 0; z < 18; z++) {
                long col = axisCols[2][y][z];
                faceMasks[EAST][y][z] = col & ~(col >>> 1); // solid with air at +X
                faceMasks[WEST][y][z] = col & ~(col << 1);  // solid with air at -X
            }
        }
    }

    // -------------------------------------------------------------------------
    // Stage 3 — build 2D binary planes from face masks
    // For each exposed face, set a bit in the plane for its (blockType, slice)
    // The plane is a 16x16 bitmask — one int per row, one bit per column
    // -------------------------------------------------------------------------

    private void buildPlanes() {
        java.util.Arrays.fill(blockIdToLocal, -1);
        localTypeCount = 0;

        // TOP / BOTTOM — face mask indexed [z][x], bit = y, slice = y
        for (int face = TOP; face <= BOTTOM; face++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    long col = faceMasks[face][z + 1][x + 1];
                    col >>>= 1;
                    col &= ~(1L << 16);
                    while (col != 0) {
                        int y = Long.numberOfTrailingZeros(col);
                        col &= col - 1;
                        short blockId = localBlockCache[x + 1][y + 1][z + 1];
                        int localId = getOrAddLocalId(blockId);
                        planes[face][localId][y][x] |= 1 << z;
                        planeMask[face][localId] |= 1 << y;
                    }
                }
            }
        }

        // NORTH / SOUTH — face mask indexed [y][x], bit = z, slice = z
        for (int face = NORTH; face <= SOUTH; face++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    long col = faceMasks[face][y + 1][x + 1];
                    col >>>= 1;
                    col &= ~(1L << 16);
                    while (col != 0) {
                        int z = Long.numberOfTrailingZeros(col);
                        col &= col - 1;
                        short blockId = localBlockCache[x + 1][y + 1][z + 1];
                        int localId = getOrAddLocalId(blockId);
                        planes[face][localId][z][y] |= 1 << x;
                        planeMask[face][localId] |= 1 << z;
                    }
                }
            }
        }

        // EAST / WEST — face mask indexed [y][z], bit = x, slice = x
        for (int face = EAST; face <= WEST; face++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    long col = faceMasks[face][y + 1][z + 1];
                    col >>>= 1;
                    col &= ~(1L << 16);
                    while (col != 0) {
                        int x = Long.numberOfTrailingZeros(col);
                        col &= col - 1;
                        short blockId = localBlockCache[x + 1][y + 1][z + 1];
                        int localId = getOrAddLocalId(blockId);
                        planes[face][localId][x][z] |= 1 << y;
                        planeMask[face][localId] |= 1 << x;
                    }
                }
            }
        }
    }

    private int getOrAddLocalId(short blockId) {
        if (blockIdToLocal[blockId] == -1) {
            blockIdToLocal[blockId] = localTypeCount;
            localToBlockId[localTypeCount] = blockId;
            localTypeCount++;
        }
        return blockIdToLocal[blockId];
    }

    // -------------------------------------------------------------------------
    // Stage 4 — greedy mesh each plane and emit quads
    // -------------------------------------------------------------------------

    private void emitAllQuads() {
        for (int face = 0; face < 6; face++) {
            for (int i = 0; i < localTypeCount; i++) {

                short blockId = (short) localToBlockId[i];
                int slices = planeMask[face][i];

                while (slices != 0) {

                    int slice = Integer.numberOfTrailingZeros(slices);
                    slices &= slices - 1;

                    quadCount = 0;
                    greedyMeshBinaryPlane(planes[face][i][slice], 16);

                    for (int q = 0; q < quadCount; q++) {
                        emitQuad(face, slice, quadPool[q], blockId);
                    }
                    
                    java.util.Arrays.fill(planes[face][i][slice], 0);
                }

                planeMask[face][i] = 0;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Greedy mesh one binary plane — results written into quadPool
    // -------------------------------------------------------------------------

    private void greedyMeshBinaryPlane(int[] data, int size) {

        for (int row = 0; row < size; row++) {

            int bits = data[row];

            while (bits != 0) {

                // find first filled column
                int col = Integer.numberOfTrailingZeros(bits);

                // shift so run starts at bit 0
                int span = bits >>> col;

                // count contiguous 1s
                int width = Integer.numberOfTrailingZeros(~span);

                if (width == 0) width = 1;

                // build mask for that horizontal run
                int mask = ((1 << width) - 1) << col;

                // grow rectangle vertically
                int height = 1;

                while (row + height < size &&
                       (data[row + height] & mask) == mask) {

                    data[row + height] &= ~mask;
                    height++;
                }

                // emit quad
                if (quadCount >= quadPool.length) break;
                quadPool[quadCount++].set(row, col, height, width);

                // remove processed bits from this row
                bits &= ~mask;
                data[row] &= ~mask;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Emit one quad into vertex/index buffers
    // axisPos = which slice (Y for top/bottom, Z for north/south, X for east/west)
    // q.x = row in the plane, q.y = col in the plane, q.w = width, q.h = height
    // -------------------------------------------------------------------------

    private void emitQuad(int face, int axisPos, GreedyQuad q, short blockId) {
        if (vertexPtr + 20 > vertices.length || indexPtr + 6 > indices.length) return;

        Block block = Block.blockRegister[blockId];
        int texIndex = getFaceTexture(block, face);
        int tileX = texIndex % ATLAS_SIZE;
        int tileY = texIndex / ATLAS_SIZE;
        float u0 = tileX * TILE_SIZE;
        float v0 = 1.0f - (tileY * TILE_SIZE) - TILE_SIZE;
        float u1 = u0 + TILE_SIZE;
        float v1 = v0 + TILE_SIZE;

        // q.x = row axis, q.y = column axis within the plane
        // expand to world coords depending on face orientation
        int r0 = q.x;
        int c0 = q.y;
        int s  = axisPos;

        switch (face) {
	        case TOP: {
	            // slice=Y, row=X, bit=Z
	            int x0=r0, x1=r0+q.w, z0=c0, z1=c0+q.h, y=s+1;
	            putVertex(x0,y,z0, u0,v0);
	            putVertex(x1,y,z0, u1,v0);
	            putVertex(x1,y,z1, u1,v1);
	            putVertex(x0,y,z1, u0,v1);
	            break;
	        }
	        case BOTTOM: {
	            // slice=Y, row=X, bit=Z
	            int x0=r0, x1=r0+q.w, z0=c0, z1=c0+q.h, y=s;
	            putVertex(x0,y,z0, u0,v0);
	            putVertex(x0,y,z1, u0,v1);
	            putVertex(x1,y,z1, u1,v1);
	            putVertex(x1,y,z0, u1,v0);
	            break;
	        }
	        case NORTH: {
	            // slice=Z, row=Y, bit=X
	            int y0=r0, y1=r0+q.w, x0=c0, x1=c0+q.h, z=s+1;
	            putVertex(x0,y0,z, u0,v0);
	            putVertex(x0,y1,z, u0,v1);
	            putVertex(x1,y1,z, u1,v1);
	            putVertex(x1,y0,z, u1,v0);
	            break;
	        }
	        case SOUTH: {
	            // slice=Z, row=Y, bit=X
	            int y0=r0, y1=r0+q.w, x0=c0, x1=c0+q.h, z=s;
	            putVertex(x0,y0,z, u0,v0);
	            putVertex(x1,y0,z, u1,v0);
	            putVertex(x1,y1,z, u1,v1);
	            putVertex(x0,y1,z, u0,v1);
	            break;
	        }
	        case EAST: {
	            // slice=X, row=Z, bit=Y
	            int z0=r0, z1=r0+q.w, y0=c0, y1=c0+q.h, x=s+1;
	            putVertex(x,y0,z0, u0,v0);
	            putVertex(x,y0,z1, u1,v0);
	            putVertex(x,y1,z1, u1,v1);
	            putVertex(x,y1,z0, u0,v1);
	            break;
	        }
	        case WEST: {
	            // slice=X, row=Z, bit=Y
	            int z0=r0, z1=r0+q.w, y0=c0, y1=c0+q.h, x=s;
	            putVertex(x,y0,z0, u0,v0);
	            putVertex(x,y1,z0, u0,v1);
	            putVertex(x,y1,z1, u1,v1);
	            putVertex(x,y0,z1, u1,v0);
	            break;
	        }
        }

        int base = vertexPtr / 5 - 4;
        indices[indexPtr++] = base;
        indices[indexPtr++] = base + 2;
        indices[indexPtr++] = base + 1;
        indices[indexPtr++] = base;
        indices[indexPtr++] = base + 3;
        indices[indexPtr++] = base + 2;
    }

    private void putVertex(int x, int y, int z, float u, float v) {
        vertices[vertexPtr++] = x;
        vertices[vertexPtr++] = y;
        vertices[vertexPtr++] = z;
        vertices[vertexPtr++] = u;
        vertices[vertexPtr++] = v;
    }
    
    // -------------------------------------------------------------------------
    // Upload to GPU
    // -------------------------------------------------------------------------

    private void uploadToGPU(Chunk chunk) {
    	if (vertexPtr != 0 || indexPtr != 0) {
    		chunk.hasBlocks = true;
    	} else {
    		chunk.hasBlocks = false;
    		return;
    	}
    	
        int vertexSizeBytes = vertexPtr * 4;
        int indexSizeBytes  = indexPtr  * 4;

        int paddedVertex = bufferManager.alignVertex((int)(vertexSizeBytes * 1.1));
        int paddedIndex  = bufferManager.alignIndex ((int)(indexSizeBytes  * 1.1));

        if (chunk.allocation == null) {
            chunk.allocation = bufferManager.getAllocation(paddedVertex, paddedIndex);
        } else {
            int allocV = chunk.allocation.vertexLimit - chunk.allocation.vertexOffset;
            int allocI = chunk.allocation.indexLimit  - chunk.allocation.indexOffset;
            if (allocV < paddedVertex || allocI < paddedIndex) {
                bufferManager.free(chunk.allocation);
                chunk.allocation = bufferManager.getAllocation(paddedVertex, paddedIndex);
            }
        }
        
        ByteBuffer vbo = bufferManager.getVBOSlice(chunk.allocation).order(ByteOrder.nativeOrder());
        for (int i = 0; i < vertexPtr; i++) vbo.putFloat(vertices[i]);
        
        ByteBuffer ebo = bufferManager.getEBOSlice(chunk.allocation).order(ByteOrder.nativeOrder());
        for (int i = 0; i < indexPtr; i++) ebo.putInt(indices[i]);
        
        chunk.allocation.setCounts(indexPtr);
        //chunk.needsUpdate       = false;
        chunk.queuedForMeshing  = false;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static int getFaceTexture(Block block, int face) {
        switch (face) {
            case TOP:    return block.topTexture;
            case BOTTOM: return block.bottomTexture;
            default:     return block.sideTexture;
        }
    }

    private static class GreedyQuad {
        int x, y, w, h;
        void set(int x, int y, int w, int h) {
            this.x=x; this.y=y; this.w=w; this.h=h;
        }
    }
}