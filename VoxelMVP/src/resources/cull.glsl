#version 430

layout(local_size_x = 64) in;

// ----------------------
// INPUT (matches Java)
// ----------------------
struct Slot {
    int worldX;
    int worldY;
    int worldZ;
    int pad;

    uint firstIndex;
    uint baseVertex;
    uint indexCount;
    uint chunkId;
};

// ----------------------
// OUTPUT DRAW COMMAND
// ----------------------
struct DrawCmd {
    uint count;
    uint instanceCount;
    uint firstIndex;
    uint baseVertex;
    uint baseInstance;
};

// ----------------------
// OUTPUT TRANSFORMS
// ----------------------
struct ChunkData {
    vec4 position; // xyz = chunk origin, w unused (alignment)
};

// ----------------------
// BUFFERS
// ----------------------
layout(std430, binding = 0) readonly buffer SlotBuffer {
    Slot slots[];
};

layout(std430, binding = 1) writeonly buffer DrawBuffer {
    DrawCmd draws[];
};

layout(std430, binding = 2) buffer Counter {
    uint drawCount;
};

layout(std430, binding = 3) writeonly buffer ChunkBuffer {
    ChunkData chunks[];
};

// ----------------------
// UNIFORMS
// ----------------------
uniform float chunkSize;

// (for later frustum culling)
uniform mat4 viewProj;

// ----------------------
// OPTIONAL CULLING HOOK
// ----------------------
bool visible(Slot s) {
    // Stage 1: no culling yet
    return true;

    // Stage 2 will go here:
    // - build AABB from worldX/Y/Z
    // - test vs frustum planes
}

// ----------------------
// MAIN
// ----------------------
void main() {
    uint i = gl_GlobalInvocationID.x;

    if (i >= slots.length())
        return;

    Slot s = slots[i];

    if (s.indexCount == 0)
        return;

    if (!visible(s))
        return;

    // Append draw
    uint dst = atomicAdd(drawCount, 1);

    // ---- Draw command ----
    draws[dst].count         = s.indexCount;
    draws[dst].instanceCount = 1;
    draws[dst].firstIndex    = s.firstIndex;
    draws[dst].baseVertex    = s.baseVertex;
    draws[dst].baseInstance  = dst; // index into chunk buffer

    // ---- Chunk transform data ----
    chunks[dst].position = vec4(
        float(s.worldX) * chunkSize,
        float(s.worldY) * chunkSize,
        float(s.worldZ) * chunkSize,
        0.0
    );
}