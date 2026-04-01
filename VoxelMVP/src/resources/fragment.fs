#version 460

in vec2 outTileOrigin;
in vec2 outBlockUV;

out vec4 fragColor;

uniform sampler2D txtSampler;

const float TILE_SIZE = 1.0 / 16.0;

void main() {
    vec2 withinTile = fract(outBlockUV) * TILE_SIZE;
    fragColor = texture(txtSampler, outTileOrigin + withinTile);
}