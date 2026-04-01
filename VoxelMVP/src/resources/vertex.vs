#version 460

layout(location = 0) in uvec2 packedVertex;

out vec2 outTileOrigin;
out vec2 outBlockUV;

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
    int bu       = int((p0 >> 15) & 0x1Fu);
    int bv       = int((p0 >> 20) & 0x1Fu);
    // face at bits 25-27, unused for now
    int texIndex = int(p1 & 0xFFu);

    DrawCmd cmd = draws[gl_DrawID];
    vec3 chunkWorld = vec3(cmd.worldX * 16, cmd.worldY * 16, cmd.worldZ * 16);
    vec3 relPos = vec3(x, y, z) + chunkWorld - cameraPos;
    gl_Position = projectionMatrix * viewMatrix * vec4(relPos, 1.0);

    int tileX = texIndex % 16;
    int tileY = texIndex / 16;

    float u0 = float(tileX) * TILE_SIZE;
    float v0 = 1.0 - float(tileY) * TILE_SIZE - TILE_SIZE;

    outTileOrigin = vec2(u0, v0);
    outBlockUV    = vec2(float(bu), float(bv));
}