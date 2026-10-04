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
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/**
 * Renders only the liquid visible behind the four inspection windows.
 * The copper frame and glass remain ordinary chunk-rendered block geometry.
 */
public class CopperFluidTankBlockEntityRenderer
        implements BlockEntityRenderer<CopperFluidTankBlockEntity> {

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
        FluidStack fluid =
                tank.getFluidForRender();

        if (fluid.isEmpty()) {
            return;
        }

        float fill =
                tank.getFillRatio();

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
                                InventoryMenu.BLOCK_ATLAS
                        )
                        .apply(texture);

        int tint =
                clientFluid.getTintColor(fluid);

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

        float min =
                3.25F / 16.0F;
        float max =
                12.75F / 16.0F;
        float bottom =
                3.25F / 16.0F;
        float top =
                bottom
                        + (9.5F / 16.0F)
                        * fill;

        float northZ =
                1.30F / 16.0F;
        float southZ =
                14.70F / 16.0F;
        float westX =
                1.30F / 16.0F;
        float eastX =
                14.70F / 16.0F;

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();
        float v0 =
                v1
                        - (v1 - sprite.getV0())
                        * fill;

        VertexConsumer consumer =
                buffer.getBuffer(
                        RenderType.translucent()
                );
        PoseStack.Pose pose =
                poseStack.last();

        // North window.
        quad(
                consumer,
                pose,
                min, bottom, northZ,
                max, bottom, northZ,
                max, top, northZ,
                min, top, northZ,
                0.0F, 0.0F, -1.0F,
                u0, v1, u1, v0,
                red, green, blue, alpha,
                packedLight
        );

        // South window.
        quad(
                consumer,
                pose,
                max, bottom, southZ,
                min, bottom, southZ,
                min, top, southZ,
                max, top, southZ,
                0.0F, 0.0F, 1.0F,
                u0, v1, u1, v0,
                red, green, blue, alpha,
                packedLight
        );

        // West window.
        quad(
                consumer,
                pose,
                westX, bottom, max,
                westX, bottom, min,
                westX, top, min,
                westX, top, max,
                -1.0F, 0.0F, 0.0F,
                u0, v1, u1, v0,
                red, green, blue, alpha,
                packedLight
        );

        // East window.
        quad(
                consumer,
                pose,
                eastX, bottom, min,
                eastX, bottom, max,
                eastX, top, max,
                eastX, top, min,
                1.0F, 0.0F, 0.0F,
                u0, v1, u1, v0,
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
            float u0, float vBottom,
            float u1, float vTop,
            float red, float green, float blue, float alpha,
            int packedLight) {
        vertex(
                consumer, pose,
                x0, y0, z0,
                nx, ny, nz,
                u0, vBottom,
                red, green, blue, alpha,
                packedLight
        );
        vertex(
                consumer, pose,
                x1, y1, z1,
                nx, ny, nz,
                u1, vBottom,
                red, green, blue, alpha,
                packedLight
        );
        vertex(
                consumer, pose,
                x2, y2, z2,
                nx, ny, nz,
                u1, vTop,
                red, green, blue, alpha,
                packedLight
        );
        vertex(
                consumer, pose,
                x3, y3, z3,
                nx, ny, nz,
                u0, vTop,
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
