package com.magneticraft2.client.hologram;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean WARNED_MISSING_SPRITE = new AtomicBoolean();
    private static final ResourceLocation NEUTRAL_SPRITE =
            new ResourceLocation("minecraft", "block/white_concrete");
    private static final ResourceLocation FLAT_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/white_concrete.png");
    private final MultiBufferSource downstream;
    private final VertexConsumer consumer;
    private final float opacity;
    private final float red;
    private final float green;
    private final float blue;
    private final boolean preserveSourceTexture;
    private final boolean shaderEffect;

    /** Called on the client recording resource-reload path. */
    public static void resetMissingSpriteWarning() {
        WARNED_MISSING_SPRITE.set(false);
    }

    public HologramBufferSource(MultiBufferSource downstream, float opacity,
                               float red, float green, float blue,
                               boolean preserveSourceTexture,
                               boolean shaderEffect) {
        // Avoid texture lookups in the common detailed mode. The original
        // atlas UVs are already encoded in the baked quads from PR #12.
        // Broken or unbaked block models can provide the distinctive
        // magenta/black missing-texture atlas sprite. A recording must
        // remain a readable *blue hologram* instead of projecting that debug
        // checkerboard into the world. This is a fallback, not a replacement
        // for fixing the offending source model.
        this.downstream = downstream;
        this.opacity = Math.max(0F, Math.min(1F, opacity));
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.preserveSourceTexture = preserveSourceTexture;
        this.shaderEffect = shaderEffect;
        var atlas = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite neutral = atlas.apply(NEUTRAL_SPRITE);
        TextureAtlasSprite missing = atlas.apply(MissingTextureAtlasSprite.getLocation());
        float centerU = (neutral.getU0() + neutral.getU1()) * 0.5F;
        float centerV = (neutral.getV0() + neutral.getV1()) * 0.5F;
        consumer = new HologramVertexConsumer(
                downstream.getBuffer(shaderEffect
                        ? HologramRenderType.projection()
                        : RenderType.translucent()),
                centerU, centerV,
                missing.getU0(), missing.getU1(),
                missing.getV0(), missing.getV1(),
                this.opacity, red, green, blue, preserveSourceTexture, true);
    }

    /**
     * Requesting a SOLID/CUTOUT scene model must still produce a ghost.
     * This wrapper never changes other models' material or the original buffer.
     */
    @Override
    public VertexConsumer getBuffer(RenderType requested) {
        return consumer;
    }

    /**
     * The real machine's procedural geometry uses normalized 0..1 UVs and
     * separate PNG textures, unlike baked atlas models. Rebind the original
     * machine texture to our holographic shader and apply the SAME translucent
     * material settings. Never transform these UVs as block atlas samples.
     */
    public VertexConsumer textured(ResourceLocation machineTexture) {
        ResourceLocation texture = preserveSourceTexture ? machineTexture : FLAT_TEXTURE;
        VertexConsumer material = downstream.getBuffer(shaderEffect
                ? HologramRenderType.projection(texture)
                : RenderType.entityTranslucent(texture));
        return new HologramVertexConsumer(
                material, 0F, 0F, 0F, -1F, 0F, -1F,
                opacity, red, green, blue, true, false);
    }

    private static final class HologramVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float spriteU;
        private final float spriteV;
        private final float missingU0;
        private final float missingU1;
        private final float missingV0;
        private final float missingV1;
        private final float opacity;
        private final boolean preserveSourceTexture;
        private final boolean atlasUV;
        private final float red;
        private final float green;
        private final float blue;

        private HologramVertexConsumer(VertexConsumer delegate, float spriteU,
                                       float spriteV,
                                       float missingU0, float missingU1,
                                       float missingV0, float missingV1,
                                       float opacity,
                                       float red, float green, float blue,
                                       boolean preserveSourceTexture,
                                       boolean atlasUV) {
            this.delegate = delegate;
            this.spriteU = spriteU;
            this.spriteV = spriteV;
            this.missingU0 = missingU0;
            this.missingU1 = missingU1;
            this.missingV0 = missingV0;
            this.missingV1 = missingV1;
            this.opacity = opacity;
            this.preserveSourceTexture = preserveSourceTexture;
            this.atlasUV = atlasUV;
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
            // Atlas coordinates are already supplied by baked quads. If a
            // specific quad uses the missing atlas sprite, redirect only
            // THAT quad's samples to a neutral real texture. Keep all valid
            // gear, shaft and wood UVs untouched.
            boolean missingSprite = atlasUV && preserveSourceTexture
                    && u >= missingU0 && u <= missingU1
                    && v >= missingV0 && v <= missingV1;
            if (missingSprite && WARNED_MISSING_SPRITE.compareAndSet(false, true)) {
                LOGGER.warn("Hologram encountered a missing block atlas texture. "
                        + "Using a neutral hologram sample; check baked model or texture logs.");
            }
            delegate.uv(preserveSourceTexture && !missingSprite ? u : spriteU,
                    preserveSourceTexture && !missingSprite ? v : spriteV);
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
