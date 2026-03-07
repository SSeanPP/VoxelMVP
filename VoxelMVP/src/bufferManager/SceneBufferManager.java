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
import java.util.concurrent.atomic.AtomicInteger;

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
	
	public SceneBufferManager() {
		
		vertexOffset.set(0);
		indexOffset.set(0);
		
		vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        
        long bufferSize = 1024L * 1024L * 256L; // 256MB example
		
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
	
	public Allocation getAllocation(int vertexSize, int indicesSize) {
		return new Allocation(vertexOffset.getAndAdd(vertexSize * 4), indexOffset.getAndAdd(indicesSize * 4), (vertexSize * 4), (indicesSize * 4), stride);
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
}
