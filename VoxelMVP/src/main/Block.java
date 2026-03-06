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
	
	
	/*
	 * void addFace(int tileIndex) {

    int tileX = tileIndex % atlasWidth;
    int tileY = tileIndex / atlasWidth;

    float u0 = tileX * tileUV;
    float v0 = tileY * tileUV;

    float u1 = u0 + tileUV;
    float v1 = v0 + tileUV;

    vertices.put(x).put(y).put(z).put(u0).put(v0);
    vertices.put(x).put(y).put(z).put(u1).put(v0);
    vertices.put(x).put(y).put(z).put(u1).put(v1);
    vertices.put(x).put(y).put(z).put(u0).put(v1);
}
	 * 
	 */
}
