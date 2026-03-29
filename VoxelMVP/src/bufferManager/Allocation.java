package bufferManager;

public class Allocation {
	public volatile int vertexOffset;
	public volatile int indexOffset;
	public volatile int indexCount;
	public volatile int vertexLimit; // no. bytes
	public volatile int indexLimit;	// no. bytes
	
	public void setAlloc(int vertexOffset, int indexOffset, int vertexSizeBytes, int indexSizeBytes) {
	    this.vertexOffset = vertexOffset;
	    this.indexOffset = indexOffset;
	    this.indexCount = 0;
	    this.vertexLimit = vertexOffset + vertexSizeBytes;
	    this.indexLimit = indexOffset + indexSizeBytes;

	}
	
	public Allocation() {
		this.vertexOffset = 0;
	    this.indexOffset = 0;
	    this.indexCount = 0;
	    this.vertexLimit = 0;
	    this.indexLimit = 0;
	}

	public void setCounts(int indCount) {
		this.indexCount = indCount;
	}
	
	public int getCounts() {
		return this.indexCount;
	}
}
