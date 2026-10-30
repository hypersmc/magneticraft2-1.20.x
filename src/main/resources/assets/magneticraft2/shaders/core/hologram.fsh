#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float GameTime;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in float projectedY;
in vec3 viewNormal;
in vec3 viewDirection;

out vec4 fragColor;

void main() {
    vec4 material = texture(Sampler0, texCoord0);
    if (material.a < 0.10) {
        discard;
    }

    // Original gear/shaft texture + Java-controlled tint, fade, and flicker.
    vec4 color = material * vertexColor * ColorModulator;

    // Local horizontal raster pattern, slowly travelling along the Y axis.
    // The line phase is continuous across independent component models.
    float phase = projectedY * 13.0 - GameTime * 250.0;
    float line = 0.5 + 0.5 * cos(phase * 6.2831853);
    float scanline = pow(line, 10.0);

    // One broader, softly illuminated sweep across the projected machinery.
    float bandPhase = projectedY * 0.32 - GameTime * 26.0;
    float scanBand = pow(0.5 + 0.5 * cos(bandPhase * 6.2831853), 24.0);

    // Screen-space silhouette emphasis: edges become a brighter blue,
    // which makes thin gear teeth readable despite translucent overlap.
    float facing = abs(dot(normalize(viewNormal), normalize(viewDirection)));
    float rim = pow(1.0 - clamp(facing, 0.0, 1.0), 2.4);

    color.rgb = mix(color.rgb, vec3(0.24, 0.83, 1.0), rim * 0.37);
    color.rgb *= 0.84 + 0.23 * scanline + 0.23 * scanBand;
    color.a *= 0.72 + 0.19 * scanline + 0.17 * scanBand + 0.38 * rim;

    // Keep conventional world depth + fog. This is NOT an X-ray overlay.
    fragColor = linear_fog(color, vertexDistance,
            FogStart, FogEnd, FogColor);
}
