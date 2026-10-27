package com.magneticraft2.client.hologram;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * Opt-in, client-only material override for already baked PR #12 scenes.
 *
 * <p>Models and animation transforms are completely unchanged. Only the
 * buffer/material supplied to the shared scene renderer is substituted.
 * The vanilla translucent block layer uses normal depth testing, so solid
 * world geometry hides the projection. Never use see-through/outline types.
 *
 * <p>An optional Forge-registered GLSL material adds a scrolling raster and
 * silhouette highlights. Vanilla RenderType.translucent() remains available
 * as a per-recording fallback if the shader is disabled or not registered.
 *
 * <p>Normal recordings retain the baked model's original UVs, allowing the
 * underlying wood, stone and gear textures to remain readable through the
 * tint. Flat/silhouette mode can still sample a neutral atlas texel instead.
 * That sprite is resolved anew after F3+T resource reloads. This material is
 * intended for models baked into Minecraft's block atlas.
 */
public final class HologramBufferSource implements MultiBufferSource {
    private static final ResourceLocation NEUTRAL_SPRITE =
            new ResourceLocation("minecraft", "block/white_concrete");
    private final VertexConsumer consumer;

    public HologramBufferSource(MultiBufferSource downstream, float opacity,
                               float red, float green, float blue,
                               boolean preserveSourceTexture,
                               boolean shaderEffect) {
        // Avoid texture lookups in the common detailed mode. The original
        // atlas UVs are already encoded in the baked quads from PR #12.
        float centerU = 0F;
        float centerV = 0F;
        if (!preserveSourceTexture) {
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(NEUTRAL_SPRITE);
            centerU = (sprite.getU0() + sprite.getU1()) * 0.5F;
            centerV = (sprite.getV0() + sprite.getV1()) * 0.5F;
        }
        consumer = new HologramVertexConsumer(
                downstream.getBuffer(shaderEffect
                        ? HologramRenderType.projection()
                        : RenderType.translucent()),
                centerU, centerV, Math.max(0F, Math.min(1F, opacity)),
                red, green, blue, preserveSourceTexture);
    }

    /**
     * Requesting a SOLID/CUTOUT scene model must still produce a ghost.
     * This wrapper never changes other models' material or the original buffer.
     */
    @Override
    public VertexConsumer getBuffer(RenderType requested) {
        return consumer;
    }

    private static final class HologramVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float spriteU;
        private final float spriteV;
        private final float opacity;
        private final boolean preserveSourceTexture;
        private final float red;
        private final float green;
        private final float blue;

        private HologramVertexConsumer(VertexConsumer delegate, float spriteU,
                                       float spriteV, float opacity,
                                       float red, float green, float blue,
                                       boolean preserveSourceTexture) {
            this.delegate = delegate;
            this.spriteU = spriteU;
            this.spriteV = spriteV;
            this.opacity = opacity;
            this.preserveSourceTexture = preserveSourceTexture;
            this.red = Math.max(0F, Math.min(1F, red));
            this.green = Math.max(0F, Math.min(1F, green));
            this.blue = Math.max(0F, Math.min(1F, blue));
        }

        private static int channel(float value) {
            return Math.max(0, Math.min(255, Math.round(value)));
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            // Preserve the baked model's vertex lighting. Its original
            // atlas texture provides surface details in detailed mode.
            delegate.color(channel(red * this.red),
                    channel(green * this.green),
                    channel(blue * this.blue),
                    channel(alpha * opacity));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(preserveSourceTexture ? u : spriteU,
                    preserveSourceTexture ? v : spriteV);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            delegate.defaultColor(channel(red * this.red),
                    channel(green * this.green),
                    channel(blue * this.blue),
                    channel(alpha * opacity));
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }
}
