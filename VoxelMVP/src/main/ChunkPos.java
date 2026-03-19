package main;

class ChunkPos {
    int x, y, z;

    ChunkPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChunkPos)) return false;
        ChunkPos other = (ChunkPos) o;
        return x == other.x && y == other.y && z == other.z;
    }

    @Override
    public int hashCode() {
        return x * 73856093 ^ y * 19349663 ^ z * 83492791;
    }
}