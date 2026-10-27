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


    // Supplier, rather than a captured instance, survives resource reloads.
    private static final ShaderStateShard HOLOGRAM_SHADER =
            new ShaderStateShard(() -> hologramShader);

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

    public static RenderType projection() {
        // Safe during startup or an interrupted reload. Client fallback
        // remains vanilla depth-tested translucent rendering.
        return hologramShader != null ? PROJECTION : RenderType.translucent();
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
            return RenderType.entityTranslucent(texture);
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
