#version 450

layout(local_size_x = 64) in;

struct ChunkSlot {
    int worldX, worldY, worldZ;
    int pad;
    int firstIndex, baseVertex, indexCount;
    int pad2;
};

struct DrawCommand {
    uint count;
    uint instanceCount;
    uint firstIndex;
    int  baseVertex;
    uint baseInstance;
    int  worldX, worldY, worldZ;
};

layout(std430, binding = 0) readonly buffer ChunkBuffer {
    ChunkSlot slots[];
};

layout(std430, binding = 1) writeonly buffer DrawBuffer {
    DrawCommand commands[];
};

layout(std430, binding = 2) buffer CountBuffer {
    uint drawCount;
};

uniform int slotCount;
uniform vec4 frustumPlanes[6];

bool inFrustum(int wx, int wy, int wz) {
    vec3 bmin = vec3(wx * 16.0, wy * 16.0, wz * 16.0);
    vec3 bmax = bmin + vec3(16.0);
    for (int i = 0; i < 6; i++) {
        vec3 p;
        p.x = frustumPlanes[i].x > 0.0 ? bmax.x : bmin.x;
        p.y = frustumPlanes[i].y > 0.0 ? bmax.y : bmin.y;
        p.z = frustumPlanes[i].z > 0.0 ? bmax.z : bmin.z;
        if (dot(frustumPlanes[i].xyz, p) + frustumPlanes[i].w < 0.0) return false;
    }
    return true;
}

void main() {
    uint slot = gl_GlobalInvocationID.x;
    if (int(slot) >= slotCount) return;

    ChunkSlot s = slots[slot];
    if (s.indexCount == 0) return;
    if (!inFrustum(s.worldX, s.worldY, s.worldZ)) return;

    uint idx = atomicAdd(drawCount, 1);

    commands[idx].count        = uint(s.indexCount);
    commands[idx].instanceCount = 1;
    commands[idx].firstIndex   = uint(s.firstIndex);
    commands[idx].baseVertex   = s.baseVertex;
    commands[idx].baseInstance = 0;
    commands[idx].worldX       = s.worldX;
    commands[idx].worldY       = s.worldY;
    commands[idx].worldZ       = s.worldZ;
}