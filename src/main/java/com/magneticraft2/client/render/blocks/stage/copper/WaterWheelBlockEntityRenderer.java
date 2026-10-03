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
            // Mechanical output direction is already correct. The wheel model's local
            // positive visual rotation is opposite Gear V2's positive axle convention.
            stack.mulPose(Axis.XP.rotationDegrees(
                    -blockEntity.getSmoothWheelVisualRotationDegrees(partialTicks)
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
        // Axle + hub.
        drawBox(stack, axle, packedLight, 0.72D, 0.12D, 0.12D);
        drawBox(stack, oak, packedLight, 0.30D, 0.24D, 0.24D);

        // Four spokes and eight rim/paddle positions.
        renderSpokes(stack, oak, packedLight, 4, 0.31D, 0.26D, 0.07D, 0.055D);
        renderRim(stack, oak, packedLight, 8, 0.34D, 0.08D, 0.26D, 0.18D);

        // Clear, broad fins around the circumference rather than decorative spoke blocks.
        renderPaddles(stack, spruce, packedLight, 8, 0.455D, 0.075D, 0.29D, 0.46D);
    }

    private void renderLargeWheel(PoseStack stack,
                                  VertexConsumer oak,
                                  VertexConsumer spruce,
                                  VertexConsumer axle,
                                  int packedLight) {
        // Long axle through the one-block-thick 3x3 wheel and a large central hub.
        drawBox(stack, axle, packedLight, 1.35D, 0.16D, 0.16D);
        drawBox(stack, oak, packedLight, 0.42D, 0.34D, 0.34D);

        // Eight real spokes into a 16-segment wooden rim.
        renderSpokes(stack, oak, packedLight, 8, 1.07D, 0.88D, 0.12D, 0.085D);
        renderRim(stack, oak, packedLight, 16, 1.12D, 0.15D, 0.44D, 0.28D);

        // Sixteen independent water-catching fins. These sit outside the rim and are
        // intentionally wider along the axle so they read as paddles, not more spokes.
        renderPaddles(stack, spruce, packedLight, 16, 1.365D, 0.09D, 0.46D, 0.78D);
    }

    private void renderSpokes(PoseStack stack,
                              VertexConsumer consumer,
                              int packedLight,
                              int count,
                              double radialCenter,
                              double radialLength,
                              double tangentialThickness,
                              double axleThickness) {
        for (int i = 0; i < count; i++) {
            double angle = 360.0D * i / count;

            stack.pushPose();
            stack.mulPose(Axis.XP.rotationDegrees((float) angle));
            stack.translate(0.0D, radialCenter, 0.0D);
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    axleThickness * 2.0D,
                    radialLength,
                    tangentialThickness * 2.0D
            );
            stack.popPose();
        }
    }

    private void renderRim(PoseStack stack,
                           VertexConsumer consumer,
                           int packedLight,
                           int count,
                           double radius,
                           double radialThickness,
                           double tangentialLength,
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
                               double radialDepth,
                               double tangentialWidth,
                               double axleWidth) {
        for (int i = 0; i < count; i++) {
            double angle = 360.0D * i / count;

            stack.pushPose();
            stack.mulPose(Axis.XP.rotationDegrees((float) angle));
            stack.translate(0.0D, radius, 0.0D);

            // Pitch every paddle slightly backwards relative to the radial line. Combined
            // with a very thin radial depth and a broad tangential face, this makes the
            // pieces read as actual water-catching boards instead of chunky rim segments.
            stack.mulPose(Axis.XP.rotationDegrees(-12.0F));

            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    axleWidth,
                    radialDepth,
                    tangentialWidth
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
