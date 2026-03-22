#version 450

layout (location=0) in vec3 position;
layout (location=1) in vec2 texCoord;

out vec2 outTextCoord;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform vec3 cameraPos;

void main()
{
    vec4 worldPos = modelMatrix * vec4(position, 1.0);
    worldPos.xyz -= cameraPos;
    gl_Position = projectionMatrix * viewMatrix * worldPos;
    outTextCoord = texCoord;
}