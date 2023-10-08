$input v_texcoord0, v_color0

#include <bgfx_shader.sh>

SAMPLER2D(s_GlyphTexture, 0);
uniform vec4 TintColor;
uniform vec4 HudOpacity;
uniform vec4 OutlineCutoff;
uniform vec4 GlyphCutoff;
uniform vec4 GlyphSmoothRadius;
uniform vec4 ShadowSmoothRadius;
uniform vec4 ShadowColor;
uniform vec4 OutlineColor;
uniform vec4 ShadowOffset;
float median(float a, float b, float c) {
    return max(min(a, b), min(max(a, b), c));
}
void main() {
    vec4 glyphColor;
    vec4 diffuse;
    vec2 newpos;

#if BGFX_SHADER_LANGUAGE_GLSL < 300 && BGFX_SHADER_LANGUAGE_GLSL
    glyphColor = texture2D(s_GlyphTexture, v_texcoord0);
    newpos = v_texcoord0;
#else
    vec2 size = vec2(textureSize(s_GlyphTexture, 0));
    int hx = int(floor(v_texcoord0.x*size.x))*2;
    int hy = int(floor(v_texcoord0.y*size.y))*2;
    newpos = vec2((float(hx+1)/(size.x*2.)), (float(hy+1)/(size.y*2.)));
    glyphColor = texture2D(s_GlyphTexture, newpos);
#endif

#if SMOOTH
    const float center = 0.5;
    const float radius = 0.0;
    glyphColor = smoothstep(center - radius, center + radius, glyphColor);
#endif
#if ALPHA_TEST
    if(glyphColor.a < 0.5) {
        discard;
    }
#endif
#if MSDF
    vec4 resultColor = v_color0;
    vec2 uv = newpos;
    float sampleDistance = median(glyphColor.r, glyphColor.g, glyphColor.b);

    float innerEdgeAlpha = smoothstep(max(0.0, GlyphCutoff.x - GlyphSmoothRadius.x), min(1.0, GlyphCutoff.x + GlyphSmoothRadius.x), sampleDistance);
    resultColor = mix(OutlineColor, resultColor, innerEdgeAlpha);
    float outerEdgeAlpha = smoothstep(max(0.0, OutlineCutoff.x - GlyphSmoothRadius.x), min(1.0, OutlineCutoff.x + GlyphSmoothRadius.x), sampleDistance);
    resultColor = vec4(resultColor.rgb, resultColor.a * outerEdgeAlpha);
    const float GlyphUvSize = 1.0 / 16.0;

    vec2 topLeft = floor(uv / GlyphUvSize) * GlyphUvSize;
    vec2 bottomRight = topLeft + vec2(GlyphUvSize, GlyphUvSize);

    vec4 shadowSample = texture2D(s_GlyphTexture, clamp(uv - ShadowOffset.xy, topLeft, bottomRight));
    float shadowAlpha = smoothstep(max(0.0, OutlineCutoff.x - ShadowSmoothRadius.x), min(1.0, OutlineCutoff.x + ShadowSmoothRadius.x), shadowSample.a);

    diffuse = mix(vec4(ShadowColor.rgb, ShadowColor.a * shadowAlpha), resultColor, outerEdgeAlpha) * TintColor;
    diffuse.a = diffuse.a * HudOpacity.x;
#else
    diffuse = v_color0 * glyphColor * TintColor;
    diffuse.a = diffuse.a * HudOpacity.x;
#endif
    gl_FragColor = diffuse;
}
