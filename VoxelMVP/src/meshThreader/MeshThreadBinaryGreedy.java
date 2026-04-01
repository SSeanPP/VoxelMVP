package meshThreader;

import java.util.concurrent.BlockingQueue;

import bufferManager.ChunkSSBO;
import bufferManager.ChunkSSBO.Slot;
import bufferManager.SceneBufferManager;
import main.Block;
import main.Chunk;
import main.WorldMap;

public class MeshThreadBinaryGreedy extends MeshThread {

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


    public MeshThreadBinaryGreedy(SceneBufferManager manager, BlockingQueue<Slot> queueInput, ChunkSSBO torroid) {
        this.bufferManager = manager;
        this.queue = queueInput;
        this.renderTorroid = torroid;
        // pre-allocate quad pool
        for (int i = 0; i < quadPool.length; i++)
            quadPool[i] = new GreedyQuad();
    }

    // -------------------------------------------------------------------------
    // Main entry point
    // -------------------------------------------------------------------------

    public void meshChunk(Slot slot, Chunk chunk) {
        
        WorldMap.blockCache(slot, chunk, localBlockCache);

        buildAxisCols();
        buildFaceMasks();
        buildPlanes();
        emitAllQuads();
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
        if (vertexPtr + 4 > vertices.length || indexPtr + 6 > indices.length) return;

        Block block = Block.blockRegister[blockId];
        int texIndex = getFaceTexture(block, face);

        int r0 = q.x, c0 = q.y, s = axisPos;
        int qw = q.w, qh = q.h;

        switch (face) {
	        case TOP: {
	            int x0=r0, x1=r0+qw, z0=c0, z1=c0+qh, y=s+1;
	            putVertex(x0,y,z0,  0,  0,  face, texIndex);
	            putVertex(x1,y,z0, qw,  0,  face, texIndex);
	            putVertex(x1,y,z1, qw, qh,  face, texIndex);
	            putVertex(x0,y,z1,  0, qh,  face, texIndex);
	            break;
	        }
	        case BOTTOM: {
	            int x0=r0, x1=r0+qw, z0=c0, z1=c0+qh, y=s;
	            putVertex(x0,y,z0,  0,  0,  face, texIndex);
	            putVertex(x0,y,z1,  0, qh,  face, texIndex);
	            putVertex(x1,y,z1, qw, qh,  face, texIndex);
	            putVertex(x1,y,z0, qw,  0,  face, texIndex);
	            break;
	        }
	        case NORTH: {
	            int y0=r0, y1=r0+qw, x0=c0, x1=c0+qh, z=s+1;
	            putVertex(x0,y0,z,  0,  0,  face, texIndex);
	            putVertex(x0,y1,z,  0, qw,  face, texIndex);
	            putVertex(x1,y1,z, qh, qw,  face, texIndex);
	            putVertex(x1,y0,z, qh,  0,  face, texIndex);
	            break;
	        }
	        case SOUTH: {
	            int y0=r0, y1=r0+qw, x0=c0, x1=c0+qh, z=s;
	            putVertex(x0,y0,z,  0,  0,  face, texIndex);
	            putVertex(x1,y0,z, qh,  0,  face, texIndex);
	            putVertex(x1,y1,z, qh, qw,  face, texIndex);
	            putVertex(x0,y1,z,  0, qw,  face, texIndex);
	            break;
	        }
	        case EAST: {
	            int z0=r0, z1=r0+qw, y0=c0, y1=c0+qh, x=s+1;
	            putVertex(x,y0,z0,  0,  0,  face, texIndex);
	            putVertex(x,y0,z1, qw,  0,  face, texIndex);
	            putVertex(x,y1,z1, qw, qh,  face, texIndex);
	            putVertex(x,y1,z0,  0, qh,  face, texIndex);
	            break;
	        }
	        case WEST: {
	            int z0=r0, z1=r0+qw, y0=c0, y1=c0+qh, x=s;
	            putVertex(x,y0,z0,  0,  0,  face, texIndex);
	            putVertex(x,y1,z0,  0, qh,  face, texIndex);
	            putVertex(x,y1,z1, qw, qh,  face, texIndex);
	            putVertex(x,y0,z1, qw,  0,  face, texIndex);
	            break;
	        }
        }

        int base = vertexPtr / 2 - 4; // vertexPtr now counts uints, not floats
        indices[indexPtr++] = base;
        indices[indexPtr++] = base + 2;
        indices[indexPtr++] = base + 1;
        indices[indexPtr++] = base;
        indices[indexPtr++] = base + 3;
        indices[indexPtr++] = base + 2;
    }

 // Replace putVertex entirely
    private void putVertex(int x, int y, int z, int bu, int bv, int face, int texIndex) {
        int packed0 = (x & 0x1F)
                    | ((y      & 0x1F) << 5)
                    | ((z      & 0x1F) << 10)
                    | ((bu     & 0x1F) << 15)  // block U directly, 0..quadW
                    | ((bv     & 0x1F) << 20)  // block V directly, 0..quadH
                    | ((face   & 0x7)  << 25);

        int packed1 = (texIndex & 0xFF);

        vertices[vertexPtr++] = packed0;
        vertices[vertexPtr++] = packed1;
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