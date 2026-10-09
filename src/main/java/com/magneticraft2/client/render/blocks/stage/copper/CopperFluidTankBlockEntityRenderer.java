package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.CopperFluidTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/**
 * Renders the actual stored fluid volume inside the Copper Fluid Tank.
 *
 * The baked tank model is only the copper vessel/frame. Its four inspection
 * openings expose this cuboid directly, so the visible level is the real tank
 * fill ratio rather than four independent fake window quads.
 */
public class CopperFluidTankBlockEntityRenderer
        implements BlockEntityRenderer<CopperFluidTankBlockEntity> {

    private static final float MIN_XZ = 3.05F / 16.0F;
    private static final float MAX_XZ = 12.95F / 16.0F;
    private static final float BOTTOM = 2.05F / 16.0F;
    private static final float FULL_TOP = 13.85F / 16.0F;

    public CopperFluidTankBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            CopperFluidTankBlockEntity tank,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        FluidStack fluid = tank.getFluidForRender();

        if (fluid.isEmpty()) {
            return;
        }

        float fill = tank.getFillRatio();
        if (fill <= 0.0F) {
            return;
        }

        IClientFluidTypeExtensions clientFluid =
                IClientFluidTypeExtensions.of(
                        fluid.getFluid()
                );

        ResourceLocation texture =
                clientFluid.getStillTexture(fluid);

        if (texture == null) {
            return;
        }

        TextureAtlasSprite sprite =
                Minecraft.getInstance()
                        .getTextureAtlas(
                                TextureAtlas.LOCATION_BLOCKS
                        )
                        .apply(texture);

        int tint = clientFluid.getTintColor(fluid);

        float alpha =
                ((tint >>> 24) & 0xFF) / 255.0F;
        float red =
                ((tint >>> 16) & 0xFF) / 255.0F;
        float green =
                ((tint >>> 8) & 0xFF) / 255.0F;
        float blue =
                (tint & 0xFF) / 255.0F;

        if (alpha <= 0.0F) {
            alpha = 1.0F;
        }

        // Keep the liquid just inside the vessel surfaces. Besides looking like
        // a real contained volume, the small inset prevents coplanar z-fighting
        // with the copper frame.
        float top =
                BOTTOM
                        + (FULL_TOP - BOTTOM)
                        * fill;

        if (top <= BOTTOM) {
            return;
        }

        VertexConsumer consumer =
                buffer.getBuffer(
                        RenderType.translucent()
                );
        PoseStack.Pose pose =
                poseStack.last();

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float sideVTop =
                v1 - (v1 - v0) * fill;

        // Top surface.
        quad(
                consumer, pose,
                MIN_XZ, top, MIN_XZ,
                MIN_XZ, top, MAX_XZ,
                MAX_XZ, top, MAX_XZ,
                MAX_XZ, top, MIN_XZ,
                0.0F, 1.0F, 0.0F,
                u0, v0, u1, v1,
                red, green, blue, alpha,
                packedLight
        );

        // North side.
        quad(
                consumer, pose,
                MAX_XZ, BOTTOM, MIN_XZ,
                MIN_XZ, BOTTOM, MIN_XZ,
                MIN_XZ, top, MIN_XZ,
                MAX_XZ, top, MIN_XZ,
                0.0F, 0.0F, -1.0F,
                u0, v1, u1, sideVTop,
                red, green, blue, alpha,
                packedLight
        );

        // South side.
        quad(
                consumer, pose,
                MIN_XZ, BOTTOM, MAX_XZ,
                MAX_XZ, BOTTOM, MAX_XZ,
                MAX_XZ, top, MAX_XZ,
                MIN_XZ, top, MAX_XZ,
                0.0F, 0.0F, 1.0F,
                u0, v1, u1, sideVTop,
                red, green, blue, alpha,
                packedLight
        );

        // West side.
        quad(
                consumer, pose,
                MIN_XZ, BOTTOM, MIN_XZ,
                MIN_XZ, BOTTOM, MAX_XZ,
                MIN_XZ, top, MAX_XZ,
                MIN_XZ, top, MIN_XZ,
                -1.0F, 0.0F, 0.0F,
                u0, v1, u1, sideVTop,
                red, green, blue, alpha,
                packedLight
        );

        // East side.
        quad(
                consumer, pose,
                MAX_XZ, BOTTOM, MAX_XZ,
                MAX_XZ, BOTTOM, MIN_XZ,
                MAX_XZ, top, MIN_XZ,
                MAX_XZ, top, MAX_XZ,
                1.0F, 0.0F, 0.0F,
                u0, v1, u1, sideVTop,
                red, green, blue, alpha,
                packedLight
        );
    }

    private void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float nx, float ny, float nz,
            float u0, float v0,
            float u1, float v1,
            float red, float green, float blue, float alpha,
            int packedLight) {
        vertex(
                consumer, pose,
                x0, y0, z0,
                nx, ny, nz,
                u0, v0,
                red, green, blue, alpha,
                packedLight
        );
        vertex(
                consumer, pose,
                x1, y1, z1,
                nx, ny, nz,
                u1, v0,
                red, green, blue, alpha,
                packedLight
        );
        vertex(
                consumer, pose,
                x2, y2, z2,
                nx, ny, nz,
                u1, v1,
                red, green, blue, alpha,
                packedLight
        );
        vertex(
                consumer, pose,
                x3, y3, z3,
                nx, ny, nz,
                u0, v1,
                red, green, blue, alpha,
                packedLight
        );
    }

    private void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x, float y, float z,
            float nx, float ny, float nz,
            float u, float v,
            float red, float green, float blue, float alpha,
            int packedLight) {
        consumer.vertex(
                        pose.pose(),
                        x, y, z
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .uv(u, v)
                .overlayCoords(
                        OverlayTexture.NO_OVERLAY
                )
                .uv2(packedLight)
                .normal(
                        pose.normal(),
                        nx, ny, nz
                )
                .endVertex();
    }
}
