#version 460

layout(local_size_x = 64) in;

struct Slot {
    int worldX, worldY, worldZ;
    int pad;
    int firstIndex, baseVertex, indexCount;
    int pad2;
};

struct DrawCmd {
    uint count;
    uint instanceCount;
    uint firstIndex;
    int  baseVertex;
    uint baseInstance;
    int  worldX, worldY, worldZ;
};

layout(std430, binding = 0) buffer SlotBuffer {
    Slot slots[];
};

layout(std430, binding = 1) buffer DrawBuffer {
    DrawCmd draws[];
};

layout(std430, binding = 2) buffer Counter {
    uint drawCount;
};

uniform int slotCount;

void main() {
    uint i = gl_GlobalInvocationID.x;
    if (int(i) >= slotCount) return;

    Slot s = slots[i];
    if (s.indexCount == 0) return;

    uint dst = atomicAdd(drawCount, 1);
    draws[dst].count         = uint(s.indexCount);
    draws[dst].instanceCount = 1;
    draws[dst].firstIndex    = uint(s.firstIndex);
    draws[dst].baseVertex    = s.baseVertex;
    draws[dst].baseInstance  = 0;
    draws[dst].worldX        = s.worldX;
    draws[dst].worldY        = s.worldY;
    draws[dst].worldZ        = s.worldZ;
}