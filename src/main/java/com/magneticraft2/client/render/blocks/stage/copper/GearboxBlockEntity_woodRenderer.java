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
            new ResourceLocation("minecraft", "textures/block/oak_planks.png");

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

        // Fixed support tied into the cage. The wooden axle rotates inside it.
        renderPortSupport(
                stack,
                support,
                packedLight
        );

        stack.mulPose(Axis.XP.rotationDegrees(rotation));

        // Continuous axle from the gear hub to the outer coupling.
        drawBoxAtLocalX(
                stack,
                consumer,
                packedLight,
                0.265D,
                0.470D,
                0.105D
        );

        // Full-size coupling at the block face, matching a normal Wooden Shaft.
        drawBoxAtLocalX(
                stack,
                consumer,
                packedLight,
                0.455D,
                0.090D,
                0.375D
        );

        stack.popPose();
    }

    private void renderPortSupport(
            PoseStack stack,
            VertexConsumer consumer,
            int packedLight) {
        /*
         * Local +X points toward the active gearbox face.
         *
         * This is a real structural cross-member, not a small decorative
         * collar. It spans from one side of the wooden cage to the other and
         * carries the bearing at its center, so the shaft/gear assembly has an
         * obvious physical support path from every normal viewing angle.
         */
        stack.pushPose();

        // Full-width wooden cross-brace. The static housing rails sit near
        // +/-0.44 from center, so this visibly terminates inside both rails.
        stack.pushPose();
        stack.translate(0.405D, 0.0D, 0.0D);
        drawBox(
                stack,
                consumer,
                packedLight,
                0.095D,
                0.095D,
                0.875D
        );
        stack.popPose();

        // Thicker center bearing block where the rotating axle passes through
        // the cross-member.
        stack.pushPose();
        stack.translate(0.405D, 0.0D, 0.0D);
        drawBox(
                stack,
                consumer,
                packedLight,
                0.125D,
                0.245D,
                0.245D
        );
        stack.popPose();

        // Small upper/lower cheeks make the bearing read as captured by the
        // brace rather than pasted onto its front face.
        stack.pushPose();
        stack.translate(0.405D, 0.170D, 0.0D);
        drawBox(
                stack,
                consumer,
                packedLight,
                0.105D,
                0.095D,
                0.310D
        );
        stack.popPose();

        stack.pushPose();
        stack.translate(0.405D, -0.170D, 0.0D);
        drawBox(
                stack,
                consumer,
                packedLight,
                0.105D,
                0.095D,
                0.310D
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

        /*
         * Do not stretch a whole 16x16 block texture across tiny mechanical
         * parts, but also do not sample a microscopic 1-2% strip of it.
         *
         * Quantise each face to an integer Minecraft texel window and keep a
         * minimum 2x2 texel patch. This gives small teeth/spokes readable wood
         * grain without smearing or over-compressing the texture.
         */
        float uvX = texelWindow(sizeX);
        float uvY = texelWindow(sizeY);
        float uvZ = texelWindow(sizeZ);

        PoseStack.Pose pose = stack.last();

        // Top / bottom: X by Z.
        quad(consumer, pose, packedLight,
                x0, y1, z0,  x0, y1, z1,  x1, y1, z1,  x1, y1, z0,
                0.0F, 1.0F, 0.0F,
                uvX, uvZ);
        quad(consumer, pose, packedLight,
                x0, y0, z1,  x0, y0, z0,  x1, y0, z0,  x1, y0, z1,
                0.0F, -1.0F, 0.0F,
                uvX, uvZ);

        // North / south: X by Y.
        quad(consumer, pose, packedLight,
                x0, y0, z1,  x1, y0, z1,  x1, y1, z1,  x0, y1, z1,
                0.0F, 0.0F, 1.0F,
                uvX, uvY);
        quad(consumer, pose, packedLight,
                x1, y0, z0,  x0, y0, z0,  x0, y1, z0,  x1, y1, z0,
                0.0F, 0.0F, -1.0F,
                uvX, uvY);

        // East / west: Z by Y.
        quad(consumer, pose, packedLight,
                x1, y0, z1,  x1, y0, z0,  x1, y1, z0,  x1, y1, z1,
                1.0F, 0.0F, 0.0F,
                uvZ, uvY);
        quad(consumer, pose, packedLight,
                x0, y0, z0,  x0, y0, z1,  x0, y1, z1,  x0, y1, z0,
                -1.0F, 0.0F, 0.0F,
                uvZ, uvY);
    }

    private float texelWindow(double blockSize) {
        int texels =
                Math.max(
                        2,
                        Math.min(
                                16,
                                (int) Math.round(
                                        blockSize * 16.0D
                                )
                        )
                );

        return texels / 16.0F;
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
                      float normalZ,
                      float maxU,
                      float maxV) {
        vertex(
                consumer, pose, packedLight,
                x0, y0, z0,
                0.0F, 0.0F,
                normalX, normalY, normalZ
        );
        vertex(
                consumer, pose, packedLight,
                x1, y1, z1,
                0.0F, maxV,
                normalX, normalY, normalZ
        );
        vertex(
                consumer, pose, packedLight,
                x2, y2, z2,
                maxU, maxV,
                normalX, normalY, normalZ
        );
        vertex(
                consumer, pose, packedLight,
                x3, y3, z3,
                maxU, 0.0F,
                normalX, normalY, normalZ
        );
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
