package com.magneticraft2.client.hologram;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Projection-only material; ordinary PR #12 machines keep the vanilla shader.
 *
 * <p>Uses the BLOCK vertex format expected by AnimatedComponentRenderer's
 * baked model quads. The custom core shader adds scanlines, a moving scan
 * band, and fresnel-ish silhouette highlights in screen space, without
 * additional ghost entities or a second geometry render.
 */
public final class HologramRenderType extends RenderType {
    private static ShaderInstance hologramShader;
    // Procedural machine renderers use individual textures rather than the
    // block atlas. Each texture gets one cached projection render layer; no
    // RenderType objects are constructed every rendered frame.
    private static final Map<ResourceLocation, RenderType> TEXTURED = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, RenderType> TEXTURED_FALLBACK =
            new ConcurrentHashMap<>();


    // Supplier, rather than a captured instance, survives resource reloads.
    private static final ShaderStateShard HOLOGRAM_SHADER =
            new ShaderStateShard(() -> hologramShader);

    /**
     * The beam is a visual overlay INSIDE the world, not solid geometry.
     *
     * Vanilla entityTranslucent writes depth in this pipeline, causing its
     * nearly invisible cone faces to occlude later gears, shafts and terrain.
     * Keep LEQUAL depth TESTING for world occlusion, but write COLOR ONLY.
     * NEW_ENTITY matches the beam vertex layout (UV0, overlay, UV2, normal).
     */
    private static final RenderType PROJECTION_BEAM = RenderType.create(
            "magneticraft2:hologram_projection_beam",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            2048,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new TextureStateShard(
                            new ResourceLocation("minecraft",
                                    "textures/block/white_concrete.png"), false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setCullState(NO_CULL)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .createCompositeState(true));

    /**
     * Shader-disabled recordings must still be true ghosts. Minecraft's
     * normal translucent block layer may write depth, so do NOT fall back
     * to RenderType.translucent() for an off-world hologram.
     */
    private static final RenderType PROJECTION_FALLBACK = RenderType.create(
            "magneticraft2:hologram_projection_fallback",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            131072,
            true,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setLightmapState(LIGHTMAP)
                    .setCullState(CULL)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .createCompositeState(true));

    private static final RenderType PROJECTION = RenderType.create(
            "magneticraft2:hologram_projection",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            131072,
            true,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(HOLOGRAM_SHADER)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    // The scene must not write into the depth buffer and
                    // hide later transparent world geometry.
                    .setWriteMaskState(COLOR_WRITE)
                    .setLightmapState(LIGHTMAP)
                    .setCullState(CULL)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .createCompositeState(true));

    private HologramRenderType(String name, VertexFormat format, VertexFormat.Mode mode,
                              int bufferSize, boolean affectsCrumbling,
                              boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload,
                setup, clear);
    }

    /** A depth-tested, no-depth-write material specifically for light cones. */
    public static RenderType projectionBeam() {
        return PROJECTION_BEAM;
    }

    public static RenderType projection() {
        // Safe during startup or an interrupted reload. Client fallback
        // remains vanilla depth-tested translucent rendering.
        return hologramShader != null ? PROJECTION : PROJECTION_FALLBACK;
    }

    /** Use when a recording explicitly disables the scanline shader. */
    public static RenderType projectionFallback() {
        return PROJECTION_FALLBACK;
    }

    /** Procedural brake fallback that also preserves the world depth buffer. */
    public static RenderType projectionTexturedFallback(ResourceLocation texture) {
        return TEXTURED_FALLBACK.computeIfAbsent(texture, id -> RenderType.create(
                "magneticraft2:hologram_procedural_fallback/" + id.toString().replace(':', '_'),
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                32768,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(id, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .setLightmapState(LIGHTMAP)
                        .setCullState(NO_CULL)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .createCompositeState(true)));
    }

    /**
     * Same shader, depth, fog, tint and translucency as baked projections,
     * but with an individual machine texture bound to Sampler0.
     *
     * The real brake renderer produces normalized UVs for oak, leather and
     * copper. Those UVs are NOT block-atlas coordinates; they must not be
     * routed through the block-atlas projection layer.
     */
    public static RenderType projection(ResourceLocation texture) {
        if (hologramShader == null) {
            return projectionTexturedFallback(texture);
        }
        return TEXTURED.computeIfAbsent(texture, id -> RenderType.create(
                "magneticraft2:hologram_procedural/" + id.toString().replace(':', '_'),
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                32768,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(HOLOGRAM_SHADER)
                        .setTextureState(new TextureStateShard(id, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .setLightmapState(LIGHTMAP)
                        .setCullState(NO_CULL)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .createCompositeState(true)));
    }

    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation("magneticraft2", "hologram"),
                DefaultVertexFormat.BLOCK), shader -> hologramShader = shader);
    }
}
