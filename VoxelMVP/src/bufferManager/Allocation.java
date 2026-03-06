package bufferManager;

public class Allocation {
	public int vertexOffset;
	public int indexOffset;
	public int indexCount;
	public int vertexCount;
	public int vertexLimit;
	public int indexLimit;
	
	public int baseIndex;
	public int baseVertex;
	
	public Allocation(int vo, int io, int vertLimit, int indLimit, int stride) {
		this.vertexOffset = vo;
		this.vertexCount = 0;
		this.indexOffset = io;
		this.indexCount = 0;
		this.vertexLimit = vertLimit + vertexOffset;
		this.indexLimit = indLimit + indexOffset;
		this.baseIndex = indexOffset / stride;
		this.baseVertex = indexOffset / stride;
	}
	
	public void setCounts(int vertCount, int indCount) {
		this.vertexCount = vertCount;
		this.indexCount = indCount;
	}
}
