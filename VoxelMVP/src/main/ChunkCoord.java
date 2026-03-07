package main;

public class ChunkCoord {
    public final int x, y, z;

    public ChunkCoord(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkCoord)) return false;
        ChunkCoord other = (ChunkCoord) o;
        return x == other.x && y == other.y && z == other.z;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = h * 31 + x;
        h = h * 31 + y;
        h = h * 31 + z;
        return h;
    }
}