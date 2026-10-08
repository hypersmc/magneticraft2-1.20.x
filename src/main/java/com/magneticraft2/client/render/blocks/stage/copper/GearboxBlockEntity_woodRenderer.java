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
    private static final ResourceLocation SUPPORT =
            new ResourceLocation("minecraft", "textures/block/copper_block.png");

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
        VertexConsumer support =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(SUPPORT));

        Direction input = gearbox.getInputDirection();
        Direction output = gearbox.getOutputDirection();

        float inputAngle = gearbox.getVisualRotationDegrees(partialTicks);
        float outputAngle = gearbox.getOutputVisualRotationDegrees(partialTicks);

        renderPortShaft(
                stack,
                shaft,
                support,
                packedLight,
                input,
                inputAngle * axisDirectionSign(input)
        );
        renderPortShaft(
                stack,
                shaft,
                support,
                packedLight,
                output,
                outputAngle * axisDirectionSign(output)
        );

        renderBevelGear(
                stack,
                oak,
                packedLight,
                input,
                inputAngle * axisDirectionSign(input),
                false
        );
        renderBevelGear(
                stack,
                oak,
                packedLight,
                output,
                outputAngle * axisDirectionSign(output) + 22.5F,
                true
        );

        stack.popPose();
    }

    /**
     * Connects a normal 6/16 Wooden Shaft at the block face to the compact
     * internal bevel-gear axle. The previous renderer used one 6/16-thick box
     * all the way through the gearbox; on UP/DOWN ports that appeared as a huge
     * wooden cube sitting directly on top of the gears.
     */
    private void renderPortShaft(PoseStack stack,
                                 VertexConsumer consumer,
                                 VertexConsumer support,
                                 int packedLight,
                                 Direction port,
                                 float rotation) {
        stack.pushPose();
        orientLocalXToDirection(stack, port);

        // Fixed bearing attached to the gearbox frame. The wooden axle rotates
        // inside this ring, so the gear no longer looks suspended in open air.
        renderBearingCollar(
                stack,
                support,
                packedLight,
                0.335D
        );

        stack.mulPose(Axis.XP.rotationDegrees(rotation));

        // Thin internal axle into the gear hub.
        drawBoxAtLocalX(
                stack,
                consumer,
                packedLight,
                0.115D,
                0.160D,
                0.100D
        );

        // Two stepped shoulders make the transition readable instead of hiding
        // the mechanism inside one oversized square shaft.
        drawBoxAtLocalX(
                stack,
                consumer,
                packedLight,
                0.225D,
                0.120D,
                0.160D
        );
        drawBoxAtLocalX(
                stack,
                consumer,
                packedLight,
                0.325D,
                0.120D,
                0.255D
        );

        // Match the normal Wooden Shaft exactly at the outer block face.
        drawBoxAtLocalX(
                stack,
                consumer,
                packedLight,
                0.445D,
                0.110D,
                0.375D
        );

        stack.popPose();
    }

    private void renderPortSupport(
            PoseStack stack,
            VertexConsumer consumer,
            int packedLight) {
        /*
         * Local +X points toward the gearbox face. Build a fixed center
         * bearing and two vertical straps that physically reach the cage rails.
         * The rotating wooden axle passes through the middle.
         */
        stack.pushPose();

        // Center bearing block around the axle.
        stack.pushPose();
        stack.translate(0.405D, 0.0D, 0.0D);
        drawBox(
                stack,
                consumer,
                packedLight,
                0.105D,
                0.245D,
                0.245D
        );
        stack.popPose();

        // Upper strap to the frame.
        stack.pushPose();
        stack.translate(0.405D, 0.285D, 0.0D);
        drawBox(
                stack,
                consumer,
                packedLight,
                0.095D,
                0.325D,
                0.080D
        );
        stack.popPose();

        // Lower strap to the frame.
        stack.pushPose();
        stack.translate(0.405D, -0.285D, 0.0D);
        drawBox(
                stack,
                consumer,
                packedLight,
                0.095D,
                0.325D,
                0.080D
        );
        stack.popPose();

        stack.popPose();
    }

    private void drawBoxAtLocalX(PoseStack stack,
                                 VertexConsumer consumer,
                                 int packedLight,
                                 double centerX,
                                 double length,
                                 double thickness) {
        stack.pushPose();
        stack.translate(centerX, 0.0D, 0.0D);
        drawBox(
                stack,
                consumer,
                packedLight,
                length,
                thickness,
                thickness
        );
        stack.popPose();
    }

    private void renderBevelGear(PoseStack stack,
                                 VertexConsumer consumer,
                                 int packedLight,
                                 Direction port,
                                 float rotation,
                                 boolean secondary) {
        stack.pushPose();

        // Local +X points out through the shaft. The miter apex is therefore
        // toward local -X at the block center.
        orientLocalXToDirection(stack, port);
        stack.translate(0.135D, 0.0D, 0.0D);
        stack.mulPose(Axis.XP.rotationDegrees(rotation));

        drawBox(
                stack,
                consumer,
                packedLight,
                0.13D,
                0.14D,
                0.14D
        );

        // Three connected stepped rings form a readable wooden bevel wheel.
        // The previous version was eight oversized floating cubes and looked
        // more like paddles than a gear.
        renderRing(
                stack,
                consumer,
                packedLight,
                0.150D,
                0.040D,
                0.052D,
                0.055D,
                16,
                0.035D
        );
        renderRing(
                stack,
                consumer,
                packedLight,
                0.116D,
                0.036D,
                0.047D,
                0.048D,
                16,
                -0.005D
        );
        renderRing(
                stack,
                consumer,
                packedLight,
                0.082D,
                0.030D,
                0.040D,
                0.040D,
                16,
                -0.045D
        );

        for (int spoke = 0; spoke < 4; spoke++) {
            stack.pushPose();
            stack.mulPose(
                    Axis.XP.rotationDegrees(spoke * 90.0F)
            );
            stack.translate(
                    0.005D,
                    0.082D,
                    0.0D
            );
            stack.mulPose(
                    Axis.ZP.rotationDegrees(18.0F)
            );
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    0.050D,
                    0.115D,
                    0.036D
            );
            stack.popPose();
        }

        for (int tooth = 0; tooth < 8; tooth++) {
            stack.pushPose();
            stack.mulPose(
                    Axis.XP.rotationDegrees(tooth * 45.0F)
            );
            stack.translate(
                    0.040D,
                    0.188D,
                    0.0D
            );
            stack.mulPose(
                    Axis.ZP.rotationDegrees(38.0F)
            );
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    0.082D,
                    0.070D,
                    0.060D
            );
            stack.popPose();
        }

        stack.popPose();
    }

    private void renderRing(PoseStack stack,
                            VertexConsumer consumer,
                            int packedLight,
                            double radius,
                            double radialDepth,
                            double tangentWidth,
                            double axialDepth,
                            int segments,
                            double axialOffset) {
        for (int segment = 0; segment < segments; segment++) {
            stack.pushPose();
            stack.mulPose(
                    Axis.XP.rotationDegrees(
                            segment * (360.0F / segments)
                    )
            );
            stack.translate(
                    axialOffset,
                    radius,
                    0.0D
            );
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
    }

    private int axisDirectionSign(Direction direction) {
        return switch (direction) {
            case EAST, UP, SOUTH -> 1;
            case WEST, DOWN, NORTH -> -1;
        };
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
