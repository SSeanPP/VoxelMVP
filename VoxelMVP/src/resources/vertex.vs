#version 460

layout(location = 0) in uvec2 packedVertex;

out vec2 outTexCoord;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform vec3 cameraPos;

struct DrawCmd {
    uint count;
    uint instanceCount;
    uint firstIndex;
    int  baseVertex;
    uint baseInstance;
    int  worldX, worldY, worldZ;
};

layout(std430, binding = 1) readonly buffer DrawBuffer {
    DrawCmd draws[];
};

const float ATLAS_TILES = 16.0;
const float TILE_SIZE   = 1.0 / ATLAS_TILES;

void main() {
    uint p0 = packedVertex.x;
    uint p1 = packedVertex.y;

    int x        = int( p0        & 0x1Fu);
    int y        = int((p0 >>  5) & 0x1Fu);
    int z        = int((p0 >> 10) & 0x1Fu);
    int corner   = int((p0 >> 15) & 0x3u);
    int quadW    = int((p0 >> 17) & 0x1Fu);
    int quadH    = int((p0 >> 22) & 0x1Fu);
    // face at bits 27-29 unused for now
    int texIndex = int(p1 & 0xFFu);

    DrawCmd cmd = draws[gl_DrawID];
    vec3 chunkWorld = vec3(cmd.worldX * 16, cmd.worldY * 16, cmd.worldZ * 16);
    vec3 relPos = vec3(x, y, z) + chunkWorld - cameraPos;
    gl_Position = projectionMatrix * viewMatrix * vec4(relPos, 1.0);

    // Match the Y-flip your original Java code used:
    // float v0 = 1.0f - (tileY * TILE_SIZE) - TILE_SIZE;
    int tileX = texIndex % 16;
    int tileY = texIndex / 16;

    float u0 = float(tileX) * TILE_SIZE;
    float v0 = 1.0 - float(tileY) * TILE_SIZE - TILE_SIZE;

    // Corner offsets within one tile — U goes right, V goes up
    vec2 offsets[4] = vec2[](
        vec2(0.0,       0.0      ),  // corner 0: u0, v0
        vec2(TILE_SIZE, 0.0      ),  // corner 1: u1, v0
        vec2(TILE_SIZE, TILE_SIZE),  // corner 2: u1, v1
        vec2(0.0,       TILE_SIZE)   // corner 3: u0, v1
    );

    outTexCoord = vec2(u0, v0) + offsets[corner];
}