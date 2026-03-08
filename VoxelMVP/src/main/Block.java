package main;

public class Block {
	public int topTexture;
	public int bottomTexture;
	public int sideTexture;
	
	public final short id;

	public static final Block blockRegister[] = new Block[16];
	
	public Block(int top, int bottom, int side, int newID) {
		this.topTexture = top;
		this.bottomTexture = bottom;
		this.sideTexture = side;
		this.id = (short) newID;
		
		blockRegister[newID] = this;
	}
	
	private int noBlocks = 0;
	public static final Block air = new Block(255,255,255,0);
	public static final Block grass = new Block(0,16,1,1);
	public static final Block dirt = new Block(16,16,16,2);
	public static final Block wood = new Block(2,2,3,3);
	public static final Block stone = new Block(3,3,3,4);
	
	
	
}
