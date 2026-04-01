#version 460

in vec2 outTileOrigin;
in vec2 outBlockUV;

out vec4 fragColor;

uniform sampler2D txtSampler;

const float TILE_SIZE = 1.0 / 16.0;

void main() {
    vec2 uv = fract(outBlockUV);
    uv = clamp(uv, 0.001, 0.999);
    vec2 withinTile = uv * TILE_SIZE;
    fragColor = texture(txtSampler, outTileOrigin + withinTile);
}