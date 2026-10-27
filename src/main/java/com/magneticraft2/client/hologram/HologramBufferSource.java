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
 * <p>The neutral atlas texel prevents the source wood/stone pixels from
 * overpowering the cyan material while preserving their detailed geometry.
 * It is resolved per render to remain correct after F3+T resource reloads.
 * Currently intended for baked models using the block atlas.
 */
public final class HologramBufferSource implements MultiBufferSource {
    private static final ResourceLocation NEUTRAL_SPRITE =
            new ResourceLocation("minecraft", "block/white_concrete");
    private final VertexConsumer consumer;

    public HologramBufferSource(MultiBufferSource downstream, float opacity,
                               float red, float green, float blue) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(NEUTRAL_SPRITE);

        float centerU = (sprite.getU0() + sprite.getU1()) * 0.5F;
        float centerV = (sprite.getV0() + sprite.getV1()) * 0.5F;
        consumer = new HologramVertexConsumer(
                downstream.getBuffer(RenderType.translucent()),
                centerU, centerV, Math.max(0F, Math.min(1F, opacity)),
                red, green, blue);
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
        private final float red;
        private final float green;
        private final float blue;

        private HologramVertexConsumer(VertexConsumer delegate, float spriteU,
                                       float spriteV, float opacity,
                                       float red, float green, float blue) {
            this.delegate = delegate;
            this.spriteU = spriteU;
            this.spriteV = spriteV;
            this.opacity = opacity;
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
            // Preserve vanilla per-face/AO light shading, but not the
            // underlying material's brown/gray color.
            delegate.color(channel(red * this.red),
                    channel(green * this.green),
                    channel(blue * this.blue),
                    channel(alpha * opacity));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(spriteU, spriteV);
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
