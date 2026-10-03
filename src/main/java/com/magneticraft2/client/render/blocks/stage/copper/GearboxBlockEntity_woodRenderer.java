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

        // Move the two miter gears slightly toward their own shaft faces so the rims meet
        // around the block center rather than occupying exactly the same plane.
        double centerOffset = secondary ? 0.08D : 0.05D;
        stack.translate(
                port.getStepX() * centerOffset,
                port.getStepY() * centerOffset,
                port.getStepZ() * centerOffset
        );

        orientLocalXToAxis(stack, port.getAxis());
        stack.mulPose(Axis.XP.rotationDegrees(rotation));

        // Compact hub.
        drawBox(stack, consumer, packedLight, 0.18D, 0.22D, 0.22D);

        // Eight chunky wooden miter teeth. The secondary gear is slightly smaller so the
        // two perpendicular sets read cleanly inside the open frame.
        double radius = secondary ? 0.235D : 0.255D;
        double radialDepth = secondary ? 0.10D : 0.11D;
        double tangentWidth = secondary ? 0.14D : 0.15D;
        double axialDepth = secondary ? 0.15D : 0.17D;

        for (int i = 0; i < 8; i++) {
            stack.pushPose();
            stack.mulPose(Axis.XP.rotationDegrees(i * 45.0F));
            stack.translate(0.0D, radius, 0.0D);

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

        stack.popPose();
    }

    private void orientLocalXToAxis(PoseStack stack, Direction.Axis axis) {
        if (axis == Direction.Axis.Y) {
            stack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        } else if (axis == Direction.Axis.Z) {
            stack.mulPose(Axis.YN.rotationDegrees(90.0F));
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
