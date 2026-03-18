package main;

import org.joml.Vector3f;

public class RenderCache {
    private final int WIDTH  = Settings.RENDER_DISTANCE * 2 + 1;
    private final int HEIGHT = Settings.RENDER_HEIGHT   * 2 + 1;
    private final int DEPTH  = Settings.RENDER_DISTANCE * 2 + 1;

    private Chunk[] renderToroidalArray = new Chunk[WIDTH * HEIGHT * DEPTH];
    private volatile Vector3f playerChunkPos;

    RenderCache(Vector3f pos) {
        playerChunkPos = pos;
    }

    public Chunk[] getRenderToroid() {
        return renderToroidalArray;
    }

    public int torroidIndex(int x, int y, int z) {
        int rx = (int)(x - playerChunkPos.x);
        int ry = (int)(y - playerChunkPos.y);
        int rz = (int)(z - playerChunkPos.z);

        int tx = ((rx % WIDTH)  + WIDTH)  % WIDTH;
        int ty = ((ry % HEIGHT) + HEIGHT) % HEIGHT;
        int tz = ((rz % DEPTH)  + DEPTH)  % DEPTH;

        return tx * HEIGHT * DEPTH + ty * DEPTH + tz;
    }

    public void updateChunkPos(Vector3f newChunkPos) {
        this.playerChunkPos = newChunkPos;
    }

    public void updateTorroid(Chunk chunk) {
        renderToroidalArray[torroidIndex(chunk.x, chunk.y, chunk.z)] = chunk;
    }

	public Vector3f getPlayerChunkPos() {
		// TODO Auto-generated method stub
		return playerChunkPos;
	}
}