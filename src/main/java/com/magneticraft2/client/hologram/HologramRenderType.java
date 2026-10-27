package com.magneticraft2.client.hologram;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;

import java.io.IOException;

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
                    .setCullState(NO_CULL)
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

    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation("magneticraft2", "hologram"),
                DefaultVertexFormat.BLOCK), shader -> hologramShader = shader);
    }
}
