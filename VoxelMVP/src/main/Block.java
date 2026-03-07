package main;

public class Block {
	public int topTexture;
	public int bottomTexture;
	public int sideTexture;
	
	public Block(int top, int bottom, int side) {
		this.topTexture = top;
		this.bottomTexture = bottom;
		this.sideTexture = side;
	}
	
	public static final Block grass = new Block(0,1,16);
	public static final Block dirt = new Block(16,16,16);
	public static final Block wood = new Block(2,2,3);
	public static final Block stone = new Block(4,4,4);
	
}
