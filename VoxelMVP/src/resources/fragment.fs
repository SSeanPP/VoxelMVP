#version 460

in vec2 outTileOrigin;
in vec2 outBlockUV;

out vec4 fragColor;

uniform sampler2D txtSampler;

const float TILE_SIZE = 1.0 / 16.0;

void main() {
    vec2 withinTile = fract(outBlockUV) * TILE_SIZE;
	vec2 uv = outTileOrigin + withinTile;
	
	// Clamp to tile boundary with half-texel inset
	vec2 tileMin = outTileOrigin + vec2(0.5 / 256.0);
	vec2 tileMax = outTileOrigin + vec2(TILE_SIZE) - vec2(0.5 / 256.0);
	uv = clamp(uv, tileMin, tileMax);
	
	fragColor = texture(txtSampler, uv);
}