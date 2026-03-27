package bufferManager;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL44;

import main.Settings;

import org.lwjgl.opengl.ARBMapBufferRange;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.lwjgl.opengl.ARBBufferStorage;

public class SceneBufferManager {
	private int megaVBOid;
	private int megaEBOid;
	private int vaoId;
	
	private AtomicInteger vertexOffset = new AtomicInteger();
	private AtomicInteger indexOffset = new AtomicInteger();
	
	private AtomicLong totalAllocated = new AtomicLong();
	private AtomicLong totalFreed = new AtomicLong();
	
	public static volatile long vboFreeBytes = 0;
	public static volatile long eboFreeBytes = 0;
	public static volatile int vboFreeRegions = 1;
	public static volatile int eboFreeRegions = 1;
	
	private final ByteBuffer megaVBO;
	private final ByteBuffer megaEBO;
	
	private class FreeRegion {
		public int offset;
		public int size;
		
		private FreeRegion(int off, int s) {
			this.offset = off;
			this.size = s;
		}
	}
	
	TreeMap<Integer, FreeRegion> vboFreeList;
	TreeMap<Integer, FreeRegion> eboFreeList;
	
	private final ReentrantLock allocLock = new ReentrantLock();
	
	public SceneBufferManager() {
		
		vboFreeBytes = Settings.VBO_SIZE_BYTES;
		eboFreeBytes = Settings.EBO_SIZE_BYTES;
		
		System.out.println("Size"+Settings.VBO_SIZE_BYTES);
		
		vertexOffset.set(0);
		indexOffset.set(0);
		
		vboFreeList = new TreeMap<Integer, FreeRegion>();
		vboFreeList.put(0, new FreeRegion(0, (int)Settings.VBO_SIZE_BYTES));

		eboFreeList = new TreeMap<Integer, FreeRegion>();
		eboFreeList.put(0, new FreeRegion(0, (int)Settings.EBO_SIZE_BYTES));
		
		
		vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
		
        //VBO
        megaVBOid = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, megaVBOid);
		
		GL44.glBufferStorage(
				GL15.GL_ARRAY_BUFFER,
			Settings.VBO_SIZE_BYTES,
		    GL30.GL_MAP_WRITE_BIT |
		    ARBBufferStorage.GL_MAP_PERSISTENT_BIT |
		    ARBBufferStorage.GL_MAP_COHERENT_BIT
		);
		
		glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, Settings.stride, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, Settings.stride, 3*4);
		
		megaVBO = ARBMapBufferRange.glMapBufferRange( GL15.GL_ARRAY_BUFFER, 0, Settings.VBO_SIZE_BYTES, GL30.GL_MAP_WRITE_BIT  | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT, null);
	
		//EBO
		megaEBOid = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, megaEBOid);

		GL44.glBufferStorage(
				GL15.GL_ELEMENT_ARRAY_BUFFER,
				Settings.EBO_SIZE_BYTES,
		    GL30.GL_MAP_WRITE_BIT |
		    ARBBufferStorage.GL_MAP_PERSISTENT_BIT |
		    ARBBufferStorage.GL_MAP_COHERENT_BIT
		);
		
		megaEBO = ARBMapBufferRange.glMapBufferRange( GL15.GL_ELEMENT_ARRAY_BUFFER, 0, Settings.EBO_SIZE_BYTES, GL30.GL_MAP_WRITE_BIT  | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT, null);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}
	
	public Allocation getAllocation(int vertexSizeBytes, int indexSizeBytes) {
	    allocLock.lock();
	    try {
	        FreeRegion vboRegion = findFit(vboFreeList, vertexSizeBytes, true);
	        FreeRegion eboRegion = findFit(eboFreeList, indexSizeBytes, false);
	        
	        if (vboRegion == null || eboRegion == null) {
	            throw new RuntimeException("Out of GPU buffer space");
	        }
	        
	        //totalAllocated.addAndGet(vboRegion.size);
	        //System.out.println("alloc: +" + vboRegion.size + " total=" + totalAllocated.get() + " freed=" + totalFreed.get() + " diff=" + (totalAllocated.get() - totalFreed.get()));
	        
	        // use vboRegion.size and eboRegion.size, not the requested sizes
	        return new Allocation(vboRegion.offset, eboRegion.offset, vboRegion.size, eboRegion.size);
	    } finally {
	        allocLock.unlock();
	    }
	}
	
	public ByteBuffer getVBOSlice(Allocation allocation) {
		ByteBuffer vVBO = megaVBO.duplicate();
		vVBO.position(allocation.vertexOffset);
		vVBO.limit(allocation.vertexLimit);
		return vVBO.slice();
	}
	
	public ByteBuffer getEBOSlice(Allocation allocation) {
		ByteBuffer eEBO = megaEBO.duplicate();
		eEBO.position(allocation.indexOffset);
		eEBO.limit(allocation.indexLimit);
		return eEBO.slice();
	}
	
	
	public void bind() {
		glBindVertexArray(vaoId);
	}
	
	public void unbind() {
		glBindVertexArray(0);
	}
	
	public int alignVertex(int bytes) {
	    return ((bytes + Settings.stride - 1) / Settings.stride) * Settings.stride;
	}
	
	public int alignIndex(int bytes) {
	    return (bytes + 3) & ~3;
	}
	
	public void free(Allocation alloc) {
		allocLock.lock();
		try {
			FreeRegion vboRegion = new FreeRegion(alloc.vertexOffset, alloc.vertexLimit - alloc.vertexOffset);
			vboFreeList.put(vboRegion.offset, vboRegion);
			coalesce(vboFreeList, vboRegion);
			vboFreeBytes += (alloc.vertexLimit - alloc.vertexOffset);
			vboFreeRegions = vboFreeList.size();
			
			//totalFreed.addAndGet(alloc.vertexLimit - alloc.vertexOffset);
			//System.out.println("free: +" + (alloc.vertexLimit - alloc.vertexOffset) + " total=" + totalAllocated.get() + " freed=" + totalFreed.get() + " diff=" + (totalAllocated.get() - totalFreed.get()));

			FreeRegion eboRegion = new FreeRegion(alloc.indexOffset, alloc.indexLimit - alloc.indexOffset);
			eboFreeList.put(eboRegion.offset, eboRegion);
			coalesce(eboFreeList, eboRegion);
			eboFreeBytes += (alloc.indexLimit - alloc.indexOffset);
			eboFreeRegions = eboFreeList.size();
		} finally {
			allocLock.unlock();
		}
	}

	private void coalesce(TreeMap<Integer, FreeRegion> freeList, FreeRegion region) {
		// Check if region immediately before this one is also free
	    Map.Entry<Integer, FreeRegion> lower = freeList.lowerEntry(region.offset);
	    if (lower != null && lower.getValue().offset + lower.getValue().size == region.offset) {
	        freeList.remove(lower.getKey());
	        freeList.remove(region.offset);
	        region = new FreeRegion(lower.getValue().offset, lower.getValue().size + region.size);
	        freeList.put(region.offset, region);
	    }

	    // Check if region immediately after this one is also free
	    Map.Entry<Integer, FreeRegion> higher = freeList.higherEntry(region.offset);
	    if (higher != null && region.offset + region.size == higher.getValue().offset) {
	        freeList.remove(higher.getKey());
	        freeList.remove(region.offset);
	        region = new FreeRegion(region.offset, region.size + higher.getValue().size);
	        freeList.put(region.offset, region);
	    }
	}
	
	private FreeRegion findFit(TreeMap<Integer, FreeRegion> freeList, int sizeBytes, boolean isVBO) {
	    FreeRegion bestFit = null;
	    int bestWaste = Integer.MAX_VALUE;

	    for (FreeRegion region : freeList.values()) {
	        if (region.size >= sizeBytes) {
	            int waste = region.size - sizeBytes;
	            if (waste < bestWaste) {
	                bestWaste = waste;
	                bestFit = region;
	                if (waste == 0) break;
	            }
	        }
	    }

	    if (bestFit == null) return null;

	    freeList.remove(bestFit.offset);

	    int actualSize;
	    int remainder = bestFit.size - sizeBytes;

	    if (remainder > Settings.stride) {
	        actualSize = sizeBytes;

	        freeList.put(
	            bestFit.offset + actualSize,
	            new FreeRegion(bestFit.offset + actualSize, remainder)
	        );
	    } else {
	        // absorb sliver
	        actualSize = bestFit.size;
	    }

	    if (isVBO) {
	        vboFreeBytes -= actualSize;
	        vboFreeRegions = freeList.size();
	    } else {
	        eboFreeBytes -= actualSize;
	        eboFreeRegions = freeList.size();
	    }

	    return new FreeRegion(bestFit.offset, actualSize);
	}
	
}
