package bufferManager;

public class Allocation {
	public int vertexOffset;
	public int indexOffset;
	public int indexCount;
	public int vertexLimit; // no. bytes
	public int indexLimit;	// no. bytes
	
	public Allocation(int vertexOffset, int indexOffset, int vertexSizeBytes, int indexSizeBytes) {
	    this.vertexOffset = vertexOffset;
	    this.indexOffset = indexOffset;

	    this.vertexLimit = vertexOffset + vertexSizeBytes;
	    this.indexLimit = indexOffset + indexSizeBytes;

	}
	
	public void setCounts(int indCount) {
		this.indexCount = indCount;
	}
}
