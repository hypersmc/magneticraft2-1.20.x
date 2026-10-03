package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.WaterWheelBlock;
import com.magneticraft2.common.blockentity.stage.copper.WaterWheelBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Procedural wooden Water Wheel renderer.
 *
 * The wheel is drawn as one lightweight mesh rather than re-rendering nine separate
 * block models. This gives us real paddles/fins, avoids block-model UV/bounds problems,
 * and keeps the complete 3x3 wheel under one smooth rotation transform.
 */
public class WaterWheelBlockEntityRenderer implements BlockEntityRenderer<WaterWheelBlockEntity> {
    private static final ResourceLocation OAK_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/oak_planks.png");
    private static final ResourceLocation SPRUCE_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/spruce_planks.png");
    private static final ResourceLocation AXLE_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/stripped_oak_log.png");

    public WaterWheelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WaterWheelBlockEntity blockEntity,
                       float partialTicks,
                       PoseStack stack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);

        Direction.Axis axis = blockEntity.getGearAxis();

        // Geometry is authored around local +X. Turn that local axle onto world Z when
        // needed; X needs no basis change.
        if (axis == Direction.Axis.Z) {
            stack.mulPose(Axis.YN.rotationDegrees(90.0F));
        }

        if (blockEntity.getBlockState().getValue(WaterWheelBlock.ACTIVE)) {
            // Use the exact same authoritative Gear V2 angle/direction that every connected
            // shaft and gear receives. The renderer must not invent a second sign convention.
            stack.mulPose(Axis.XP.rotationDegrees(
                    blockEntity.getVisualRotationDegrees(partialTicks)
            ));
        }

        VertexConsumer oak =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(OAK_TEXTURE));
        VertexConsumer spruce =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(SPRUCE_TEXTURE));
        VertexConsumer axle =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(AXLE_TEXTURE));

        if (blockEntity.isLarge()) {
            renderLargeWheel(stack, oak, spruce, axle, packedLight);
        } else {
            renderSmallWheel(stack, oak, spruce, axle, packedLight);
        }

        stack.popPose();
    }

    private void renderSmallWheel(PoseStack stack,
                                  VertexConsumer oak,
                                  VertexConsumer spruce,
                                  VertexConsumer axle,
                                  int packedLight) {
        // Classic narrow wooden water wheel: axle/hub in the middle, two side rims,
        // spokes on each rim, and boards spanning the two rims as actual paddles.
        // Exact match for shaft_wood.json: 6x6 px = 0.375x0.375 blocks.
        drawBox(stack, axle, packedLight, 1.00D, 0.375D, 0.375D);
        drawBox(stack, oak, packedLight, 0.44D, 0.30D, 0.30D);

        double sideOffset = 0.275D;
        renderSideSpokes(stack, oak, packedLight, 4, sideOffset, 0.275D, 0.30D, 0.075D, 0.085D);
        renderSideSpokes(stack, oak, packedLight, 4, -sideOffset, 0.275D, 0.30D, 0.075D, 0.085D);

        renderSideRim(stack, oak, packedLight, 8, sideOffset, 0.365D, 0.11D, 0.30D, 0.10D);
        renderSideRim(stack, oak, packedLight, 8, -sideOffset, 0.365D, 0.11D, 0.30D, 0.10D);

        // Eight full-depth paddle boards. The rim still stays inside the 1x1 footprint,
        // but the complete wheel now uses nearly the full block in both diameter and depth.
        // Extend slightly beyond the nominal 0.5-block radius so the paddle visually
        // enters the adjacent water block instead of stopping on the exact voxel border.
        renderPaddles(stack, spruce, packedLight, 8, 0.500D, 0.115D, 0.11D, 0.78D);
    }

    private void renderLargeWheel(PoseStack stack,
                                  VertexConsumer oak,
                                  VertexConsumer spruce,
                                  VertexConsumer axle,
                                  int packedLight) {
        // Same cross-section as a normal Wooden Shaft.
        drawBox(stack, axle, packedLight, 1.00D, 0.375D, 0.375D);
        drawBox(stack, oak, packedLight, 0.58D, 0.42D, 0.42D);

        double sideOffset = 0.43D;

        // The large wheel now really fills its 3x3x1 footprint: two substantial side rims
        // sit close to the front/back faces and the paddles bridge almost the whole depth.
        renderSideSpokes(stack, oak, packedLight, 8, sideOffset, 0.72D, 1.04D, 0.10D, 0.11D);
        renderSideSpokes(stack, oak, packedLight, 8, -sideOffset, 0.72D, 1.04D, 0.10D, 0.11D);

        renderSideRim(stack, oak, packedLight, 16, sideOffset, 1.20D, 0.20D, 0.50D, 0.12D);
        renderSideRim(stack, oak, packedLight, 16, -sideOffset, 1.20D, 0.20D, 0.50D, 0.12D);

        // Sixteen wide, thin paddles reach the edge of the 3-block diameter and span
        // essentially the full one-block shaft depth.
        // Nominal 3-block wheel radius is 1.5. Give the outer board a small overlap into
        // the adjacent water cell so the powered water and visible paddle occupy the same
        // space rather than merely touching at an invisible block boundary.
        renderPaddles(stack, spruce, packedLight, 16, 1.505D, 0.15D, 0.13D, 0.98D);
    }

    private void renderSideSpokes(PoseStack stack,
                                  VertexConsumer consumer,
                                  int packedLight,
                                  int count,
                                  double xOffset,
                                  double radialCenter,
                                  double radialLength,
                                  double tangentialThickness,
                                  double sideThickness) {
        for (int i = 0; i < count; i++) {
            double angle = 360.0D * i / count;

            stack.pushPose();
            stack.mulPose(Axis.XP.rotationDegrees((float) angle));
            stack.translate(xOffset, radialCenter, 0.0D);
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    sideThickness,
                    radialLength,
                    tangentialThickness
            );
            stack.popPose();
        }
    }

    private void renderSideRim(PoseStack stack,
                               VertexConsumer consumer,
                               int packedLight,
                               int count,
                               double xOffset,
                               double radius,
                               double radialThickness,
                               double tangentialLength,
                               double sideThickness) {
        for (int i = 0; i < count; i++) {
            double angle = 360.0D * i / count;

            stack.pushPose();
            stack.mulPose(Axis.XP.rotationDegrees((float) angle));
            stack.translate(xOffset, radius, 0.0D);
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    sideThickness,
                    radialThickness,
                    tangentialLength
            );
            stack.popPose();
        }
    }

    private void renderPaddles(PoseStack stack,
                               VertexConsumer consumer,
                               int packedLight,
                               int count,
                               double radius,
                               double radialLength,
                               double tangentialThickness,
                               double axleWidth) {
        for (int i = 0; i < count; i++) {
            double angle = 360.0D * i / count;

            stack.pushPose();
            stack.mulPose(Axis.XP.rotationDegrees((float) angle));
            stack.translate(0.0D, radius, 0.0D);

            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    axleWidth,
                    radialLength,
                    tangentialThickness
            );
            stack.popPose();
        }
    }

    private void drawBox(PoseStack stack,
                         VertexConsumer consumer,
                         int packedLight,
                         double sizeX,
                         double sizeY,
                         double sizeZ) {
        double x0 = -sizeX * 0.5D;
        double x1 = sizeX * 0.5D;
        double y0 = -sizeY * 0.5D;
        double y1 = sizeY * 0.5D;
        double z0 = -sizeZ * 0.5D;
        double z1 = sizeZ * 0.5D;

        PoseStack.Pose pose = stack.last();

        // +Y
        quad(consumer, pose, packedLight,
                x0, y1, z0, 0.0F, 0.0F,
                x0, y1, z1, 0.0F, 1.0F,
                x1, y1, z1, 1.0F, 1.0F,
                x1, y1, z0, 1.0F, 0.0F,
                0.0F, 1.0F, 0.0F);

        // -Y
        quad(consumer, pose, packedLight,
                x0, y0, z1, 0.0F, 0.0F,
                x0, y0, z0, 0.0F, 1.0F,
                x1, y0, z0, 1.0F, 1.0F,
                x1, y0, z1, 1.0F, 0.0F,
                0.0F, -1.0F, 0.0F);

        // +Z
        quad(consumer, pose, packedLight,
                x0, y0, z1, 0.0F, 0.0F,
                x1, y0, z1, 1.0F, 0.0F,
                x1, y1, z1, 1.0F, 1.0F,
                x0, y1, z1, 0.0F, 1.0F,
                0.0F, 0.0F, 1.0F);

        // -Z
        quad(consumer, pose, packedLight,
                x1, y0, z0, 0.0F, 0.0F,
                x0, y0, z0, 1.0F, 0.0F,
                x0, y1, z0, 1.0F, 1.0F,
                x1, y1, z0, 0.0F, 1.0F,
                0.0F, 0.0F, -1.0F);

        // +X
        quad(consumer, pose, packedLight,
                x1, y0, z1, 0.0F, 0.0F,
                x1, y0, z0, 1.0F, 0.0F,
                x1, y1, z0, 1.0F, 1.0F,
                x1, y1, z1, 0.0F, 1.0F,
                1.0F, 0.0F, 0.0F);

        // -X
        quad(consumer, pose, packedLight,
                x0, y0, z0, 0.0F, 0.0F,
                x0, y0, z1, 1.0F, 0.0F,
                x0, y1, z1, 1.0F, 1.0F,
                x0, y1, z0, 0.0F, 1.0F,
                -1.0F, 0.0F, 0.0F);
    }

    private void quad(VertexConsumer consumer,
                      PoseStack.Pose pose,
                      int packedLight,
                      double x0, double y0, double z0, float u0, float v0,
                      double x1, double y1, double z1, float u1, float v1,
                      double x2, double y2, double z2, float u2, float v2,
                      double x3, double y3, double z3, float u3, float v3,
                      float normalX, float normalY, float normalZ) {
        vertex(consumer, pose, packedLight, x0, y0, z0, u0, v0, normalX, normalY, normalZ);
        vertex(consumer, pose, packedLight, x1, y1, z1, u1, v1, normalX, normalY, normalZ);
        vertex(consumer, pose, packedLight, x2, y2, z2, u2, v2, normalX, normalY, normalZ);
        vertex(consumer, pose, packedLight, x3, y3, z3, u3, v3, normalX, normalY, normalZ);
    }

    private void vertex(VertexConsumer consumer,
                        PoseStack.Pose pose,
                        int packedLight,
                        double x,
                        double y,
                        double z,
                        float u,
                        float v,
                        float normalX,
                        float normalY,
                        float normalZ) {
        consumer.vertex(
                        pose.pose(),
                        (float) x,
                        (float) y,
                        (float) z
                )
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), normalX, normalY, normalZ)
                .endVertex();
    }
}
