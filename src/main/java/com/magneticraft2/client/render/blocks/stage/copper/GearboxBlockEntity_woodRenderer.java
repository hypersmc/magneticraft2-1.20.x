package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.GearboxBlockEntity_wood;
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
 * Visible wooden miter gears + shaft stubs inside the right-angle gearbox casing.
 */
public class GearboxBlockEntity_woodRenderer implements BlockEntityRenderer<GearboxBlockEntity_wood> {
    private static final ResourceLocation OAK =
            new ResourceLocation("minecraft", "textures/block/oak_planks.png");
    private static final ResourceLocation SHAFT =
            new ResourceLocation("minecraft", "textures/block/stripped_oak_log.png");

    public GearboxBlockEntity_woodRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GearboxBlockEntity_wood gearbox,
                       float partialTicks,
                       PoseStack stack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);

        VertexConsumer oak =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(OAK));
        VertexConsumer shaft =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(SHAFT));

        Direction input = gearbox.getInputDirection();
        Direction output = gearbox.getOutputDirection();

        renderPortShaft(stack, shaft, packedLight, input);
        renderPortShaft(stack, shaft, packedLight, output);

        float inputAngle = gearbox.getVisualRotationDegrees(partialTicks);
        float outputAngle = gearbox.getOutputVisualRotationDegrees(partialTicks);

        renderBevelGear(
                stack,
                oak,
                packedLight,
                input,
                inputAngle,
                false
        );
        renderBevelGear(
                stack,
                oak,
                packedLight,
                output,
                outputAngle + 22.5F,
                true
        );

        stack.popPose();
    }

    private void renderPortShaft(PoseStack stack,
                                 VertexConsumer consumer,
                                 int packedLight,
                                 Direction port) {
        stack.pushPose();

        double offset = 0.31D;
        stack.translate(
                port.getStepX() * offset,
                port.getStepY() * offset,
                port.getStepZ() * offset
        );

        Direction.Axis axis = port.getAxis();
        if (axis == Direction.Axis.X) {
            drawBox(stack, consumer, packedLight, 0.62D, 0.375D, 0.375D);
        } else if (axis == Direction.Axis.Y) {
            drawBox(stack, consumer, packedLight, 0.375D, 0.62D, 0.375D);
        } else {
            drawBox(stack, consumer, packedLight, 0.375D, 0.375D, 0.62D);
        }

        stack.popPose();
    }

    private void renderBevelGear(PoseStack stack,
                                 VertexConsumer consumer,
                                 int packedLight,
                                 Direction port,
                                 float rotation,
                                 boolean secondary) {
        stack.pushPose();

        // Work in local space where +X points out through this gearbox port. That makes
        // both miter gears taper toward the shared block center regardless of whether the
        // real port is EAST/WEST, UP/DOWN or NORTH/SOUTH.
        orientLocalXToDirection(stack, port);

        // A 1:1 miter pair has both pitch cones meeting at the intersection of the
        // two shaft axes. Keep each wheel close to that intersection instead of
        // rendering two large spur-like crowns on opposite sides of the housing.
        double gearCenter = 0.105D;
        stack.translate(gearCenter, 0.0D, 0.0D);
        stack.mulPose(Axis.XP.rotationDegrees(rotation));

        // Compact hub on the shaft side of the wheel.
        drawBox(
                stack,
                consumer,
                packedLight,
                0.10D,
                0.14D,
                0.14D
        );

        double radius = 0.145D;
        double radialDepth = 0.070D;
        double tangentWidth = 0.082D;
        double axialDepth = 0.070D;

        for (int i = 0; i < 8; i++) {
            stack.pushPose();
            stack.mulPose(Axis.XP.rotationDegrees(i * 45.0F));

            // For equal-size bevel gears the tooth face is approximately a 45 degree
            // cone. Move the tooth ring inward and lean it toward the common shaft
            // intersection so the two wheels visibly mesh at the corner.
            stack.translate(-0.040D, radius, 0.0D);
            stack.mulPose(Axis.ZP.rotationDegrees(45.0F));

            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    axialDepth,
                    radialDepth,
                    tangentWidth
            );

            stack.popPose();
        }

        // Four short spokes make the wheel read as a gear rather than eight floating
        // wooden blocks. They stay on the shaft side of the bevel tooth ring.
        for (int i = 0; i < 4; i++) {
            stack.pushPose();
            stack.mulPose(Axis.XP.rotationDegrees(i * 90.0F));
            stack.translate(0.020D, 0.085D, 0.0D);
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    0.050D,
                    0.105D,
                    0.045D
            );
            stack.popPose();
        }

        stack.popPose();
    }

    private void orientLocalXToDirection(PoseStack stack, Direction direction) {
        switch (direction) {
            case EAST -> {
                // Local +X already points east.
            }
            case WEST -> stack.mulPose(
                    Axis.YP.rotationDegrees(180.0F)
            );
            case UP -> stack.mulPose(
                    Axis.ZP.rotationDegrees(90.0F)
            );
            case DOWN -> stack.mulPose(
                    Axis.ZN.rotationDegrees(90.0F)
            );
            case SOUTH -> stack.mulPose(
                    Axis.YN.rotationDegrees(90.0F)
            );
            case NORTH -> stack.mulPose(
                    Axis.YP.rotationDegrees(90.0F)
            );
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

        quad(consumer, pose, packedLight,
                x0, y1, z0,  x0, y1, z1,  x1, y1, z1,  x1, y1, z0,
                0.0F, 1.0F, 0.0F);
        quad(consumer, pose, packedLight,
                x0, y0, z1,  x0, y0, z0,  x1, y0, z0,  x1, y0, z1,
                0.0F, -1.0F, 0.0F);
        quad(consumer, pose, packedLight,
                x0, y0, z1,  x1, y0, z1,  x1, y1, z1,  x0, y1, z1,
                0.0F, 0.0F, 1.0F);
        quad(consumer, pose, packedLight,
                x1, y0, z0,  x0, y0, z0,  x0, y1, z0,  x1, y1, z0,
                0.0F, 0.0F, -1.0F);
        quad(consumer, pose, packedLight,
                x1, y0, z1,  x1, y0, z0,  x1, y1, z0,  x1, y1, z1,
                1.0F, 0.0F, 0.0F);
        quad(consumer, pose, packedLight,
                x0, y0, z0,  x0, y0, z1,  x0, y1, z1,  x0, y1, z0,
                -1.0F, 0.0F, 0.0F);
    }

    private void quad(VertexConsumer consumer,
                      PoseStack.Pose pose,
                      int packedLight,
                      double x0, double y0, double z0,
                      double x1, double y1, double z1,
                      double x2, double y2, double z2,
                      double x3, double y3, double z3,
                      float normalX,
                      float normalY,
                      float normalZ) {
        vertex(consumer, pose, packedLight, x0, y0, z0, 0.0F, 0.0F, normalX, normalY, normalZ);
        vertex(consumer, pose, packedLight, x1, y1, z1, 0.0F, 1.0F, normalX, normalY, normalZ);
        vertex(consumer, pose, packedLight, x2, y2, z2, 1.0F, 1.0F, normalX, normalY, normalZ);
        vertex(consumer, pose, packedLight, x3, y3, z3, 1.0F, 0.0F, normalX, normalY, normalZ);
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
