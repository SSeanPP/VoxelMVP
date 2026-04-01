#version 460
#extension GL_KHR_shader_subgroup_arithmetic : enable
#extension GL_KHR_shader_subgroup_ballot : enable


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

layout(std430, binding = 0) buffer SlotBuffer { Slot slots[]; };
layout(std430, binding = 1) buffer DrawBuffer { DrawCmd draws[]; };
layout(std430, binding = 2) buffer Counter {
    uint drawCount;
    uint meshedCount;
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
    uint i = gl_GlobalInvocationID.x;
    if (int(i) >= slotCount) return;

    Slot s = slots[i];
    if (s.indexCount == 0) return;

    // Subgroup reduction for meshedCount — one atomic per subgroup
    uint meshedContrib = subgroupAdd(1u);
    if (subgroupElect()) atomicAdd(meshedCount, meshedContrib);

    if (!inFrustum(s.worldX, s.worldY, s.worldZ)) return;

    // Subgroup reduction for drawCount
    uint drawContrib = subgroupAdd(1u);
    uint base;
    if (subgroupElect()) base = atomicAdd(drawCount, drawContrib);
    base = subgroupBroadcastFirst(base);
    uint dst = base + subgroupExclusiveAdd(1u);

    draws[dst].count         = uint(s.indexCount);
    draws[dst].instanceCount = 1;
    draws[dst].firstIndex    = uint(s.firstIndex);
    draws[dst].baseVertex    = s.baseVertex;
    draws[dst].baseInstance  = 0;
    draws[dst].worldX        = s.worldX;
    draws[dst].worldY        = s.worldY;
    draws[dst].worldZ        = s.worldZ;
}