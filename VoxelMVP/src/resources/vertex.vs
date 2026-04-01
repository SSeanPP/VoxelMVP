#version 460

layout (location=0) in vec3 position;
layout (location=1) in vec2 texCoord;

out vec2 outTextCoord;

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

void main() {
    DrawCmd cmd = draws[gl_DrawID];
    vec3 chunkWorld = vec3(cmd.worldX * 16, cmd.worldY * 16, cmd.worldZ * 16);
    vec3 relPos = position + chunkWorld - cameraPos;
    gl_Position = projectionMatrix * viewMatrix * vec4(relPos, 1.0);
    outTextCoord = texCoord;
}