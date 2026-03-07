package bufferManager;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.ARBMapBufferRange;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.lwjgl.opengl.ARBBufferStorage;

public class SceneBufferManager {
	private int megaVBOid;
	private int megaEBOid;
	private int vaoId;
	
	private AtomicInteger vertexOffset = new AtomicInteger();
	private AtomicInteger indexOffset = new AtomicInteger();
	
	private final ByteBuffer megaVBO;
	private final ByteBuffer megaEBO;
	
	private int stride = 5 * 4;
	
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
		
		long bufferSize = 1024L * 1024L * 256L; // 256MB example
		
		vertexOffset.set(0);
		indexOffset.set(0);
		
		vboFreeList = new TreeMap<Integer, FreeRegion>();
		vboFreeList.put(0, new FreeRegion(0, (int)bufferSize));

		eboFreeList = new TreeMap<Integer, FreeRegion>();
		eboFreeList.put(0, new FreeRegion(0, (int)bufferSize));
		
		
		vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
		
        //VBO
        megaVBOid = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, megaVBOid);
		
		GL44.glBufferStorage(
				GL15.GL_ARRAY_BUFFER,
		    bufferSize,
		    GL30.GL_MAP_WRITE_BIT |
		    ARBBufferStorage.GL_MAP_PERSISTENT_BIT |
		    ARBBufferStorage.GL_MAP_COHERENT_BIT
		);
		
		glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3*4);
		
		megaVBO = ARBMapBufferRange.glMapBufferRange( GL15.GL_ARRAY_BUFFER, 0, bufferSize, GL30.GL_MAP_WRITE_BIT  | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT, null);
	
		//EBO
		megaEBOid = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, megaEBOid);

		GL44.glBufferStorage(
				GL15.GL_ELEMENT_ARRAY_BUFFER,
		    bufferSize,
		    GL30.GL_MAP_WRITE_BIT |
		    ARBBufferStorage.GL_MAP_PERSISTENT_BIT |
		    ARBBufferStorage.GL_MAP_COHERENT_BIT
		);
		
		megaEBO = ARBMapBufferRange.glMapBufferRange( GL15.GL_ELEMENT_ARRAY_BUFFER, 0, bufferSize, GL30.GL_MAP_WRITE_BIT  | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT, null);
		
		glBindVertexArray(0);
	}
	
	public Allocation getAllocation(int vertexFloatCount, int indexCount) {
		allocLock.lock();
		try {
			int vertexSizeBytes = vertexFloatCount * 4; // size_of(float)
		    int indexSizeBytes  = indexCount * 4; // size_of(int)
		    FreeRegion vboRegion = findFit(vboFreeList, vertexSizeBytes);
		    FreeRegion eboRegion = findFit(eboFreeList, indexSizeBytes);

		    if (vboRegion == null || eboRegion == null) {
		        throw new RuntimeException("Out of GPU buffer space");
		    }

		    return new Allocation(vboRegion.offset, eboRegion.offset, vertexSizeBytes, indexSizeBytes);
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
	
	public int getStride() {
		return stride;
	}
	
	public void free(Allocation alloc) {
		allocLock.lock();
		try {
			// Return VBO region
		    FreeRegion vboRegion = new FreeRegion(alloc.vertexOffset, alloc.indexLimit);
		    vboFreeList.put(vboRegion.offset, vboRegion);
		    coalesce(vboFreeList, vboRegion);

		    // Return EBO region
		    FreeRegion eboRegion = new FreeRegion(alloc.indexOffset, alloc.indexLimit);
		    eboFreeList.put(eboRegion.offset, eboRegion);
		    coalesce(eboFreeList, eboRegion);
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
	
	private FreeRegion findFit(TreeMap<Integer, FreeRegion> freeList, int sizeBytes) {

		Iterator<FreeRegion> it = freeList.values().iterator();

		while (it.hasNext()) {
		    FreeRegion region = it.next();

		    if (region.size >= sizeBytes) {
		        it.remove();

		        if (region.size > sizeBytes) {
		            FreeRegion remainder = new FreeRegion(
		                    region.offset + sizeBytes,
		                    region.size - sizeBytes
		            );
		            freeList.put(remainder.offset, remainder);
		        }

		        return new FreeRegion(region.offset, sizeBytes);
		    }
		}
	    return null;
	}
}
