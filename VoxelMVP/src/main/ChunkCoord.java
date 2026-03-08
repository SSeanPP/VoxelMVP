package main;

public final class ChunkCoord {

    private ChunkCoord() {}

    // X: 28 bits, Z: 28 bits, Y: 8 bits
    public static long pack(int x, int y, int z) {
        long lx = ((long)x & 0xFFFFFFFL); // 28 bits (Mask: 7 F's)
        long lz = ((long)z & 0xFFFFFFFL); // 28 bits
        long ly = ((long)y & 0xFFL);       // 8 bits (Mask: 2 F's)

        // Shift X by 36 (28 for Z + 8 for Y)
        // Shift Z by 8 (to make room for Y)
        return (lx << 36) | (lz << 8) | ly;
    }

    public static int unpackX(long key) {
        int x = (int)(key >> 36);
        // Check 28th bit (1 << 27) for sign extension
        if ((x & (1 << 27)) != 0) x |= ~0xFFFFFFF;
        return x;
    }

    public static int unpackZ(long key) {
        int z = (int)((key >> 8) & 0xFFFFFFF);
        if ((z & (1 << 27)) != 0) z |= ~0xFFFFFFF;
        return z;
    }

    public static int unpackY(long key) {
        // If Y can be negative, you'd apply the same sign-extension logic here
        return (int)(key & 0xFF);
    }
}