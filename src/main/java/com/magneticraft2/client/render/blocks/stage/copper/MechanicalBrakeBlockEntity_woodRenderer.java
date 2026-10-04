package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalBrakeBlock_wood;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalBrakeBlockEntity_wood;
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
import net.minecraft.world.level.block.DirectionalBlock;

/**
 * Open band/shoe brake: a large wooden drum rotates continuously inside the
 * static frame while two leather-lined shoes visibly clamp onto it.
 */
public class MechanicalBrakeBlockEntity_woodRenderer
        implements BlockEntityRenderer<MechanicalBrakeBlockEntity_wood> {

    private static final ResourceLocation DRUM_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/oak_planks.png"
            );

    private static final ResourceLocation LEATHER_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/brown_wool.png"
            );

    private static final ResourceLocation COPPER_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/copper_block.png"
            );

    private static final double DRUM_RADIUS = 0.31D;
    private static final double DRUM_HALF_LENGTH = 0.25D;
    private static final int DRUM_SIDES = 12;

    public MechanicalBrakeBlockEntity_woodRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            MechanicalBrakeBlockEntity_wood brake,
            float partialTicks,
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        Direction.Axis axis = brake.getBlockState()
                .getValue(DirectionalBlock.FACING)
                .getAxis();

        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);
        orientLocalXToAxis(stack, axis);

        float angle = brake.getVisualRotationDegrees(
                partialTicks
        );

        stack.pushPose();
        stack.mulPose(
                Axis.XP.rotationDegrees(angle)
        );

        VertexConsumer drumConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                DRUM_TEXTURE
                        )
                );

        drawCylinderSides(
                stack.last(),
                drumConsumer,
                packedLight,
                DRUM_HALF_LENGTH,
                DRUM_RADIUS,
                DRUM_SIDES
        );
        drawCylinderEnds(
                stack.last(),
                drumConsumer,
                packedLight,
                DRUM_HALF_LENGTH,
                DRUM_RADIUS,
                DRUM_SIDES
        );

        stack.popPose();

        double shoeY = brake.isBraking()
                ? 0.345D
                : 0.435D;

        VertexConsumer leatherConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                LEATHER_TEXTURE
                        )
                );

        renderBrakeShoe(
                stack,
                leatherConsumer,
                packedLight,
                shoeY
        );
        renderBrakeShoe(
                stack,
                leatherConsumer,
                packedLight,
                -shoeY
        );

        VertexConsumer copperConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                COPPER_TEXTURE
                        )
                );

        renderClampArm(
                stack,
                copperConsumer,
                packedLight,
                shoeY,
                true
        );
        renderClampArm(
                stack,
                copperConsumer,
                packedLight,
                -shoeY,
                false
        );

        stack.popPose();
    }

    private void renderBrakeShoe(
            PoseStack stack,
            VertexConsumer consumer,
            int packedLight,
            double y) {

        stack.pushPose();
        stack.translate(0.0D, y, 0.0D);

        drawBox(
                stack,
                consumer,
                packedLight,
                0.58D,
                0.07D,
                0.54D
        );

        stack.popPose();
    }

    private void renderClampArm(
            PoseStack stack,
            VertexConsumer consumer,
            int packedLight,
            double shoeY,
            boolean upper) {

        double outerY = upper ? 0.445D : -0.445D;
        double midpoint =
                (shoeY + outerY) * 0.5D;
        double length =
                Math.abs(outerY - shoeY)
                        + 0.12D;

        stack.pushPose();
        stack.translate(
                0.0D,
                midpoint,
                0.33D
        );

        drawBox(
                stack,
                consumer,
                packedLight,
                0.12D,
                length,
                0.10D
        );

        stack.popPose();

        stack.pushPose();
        stack.translate(
                0.0D,
                midpoint,
                -0.33D
        );

        drawBox(
                stack,
                consumer,
                packedLight,
                0.12D,
                length,
                0.10D
        );

        stack.popPose();
    }

    private void orientLocalXToAxis(
            PoseStack stack,
            Direction.Axis axis) {
        switch (axis) {
            case X -> {
            }
            case Y -> stack.mulPose(
                    Axis.ZP.rotationDegrees(90.0F)
            );
            case Z -> stack.mulPose(
                    Axis.YN.rotationDegrees(90.0F)
            );
        }
    }

    private void drawCylinderSides(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int packedLight,
            double halfLength,
            double radius,
            int sides) {

        double offset = Math.PI / sides;

        for (int i = 0; i < sides; i++) {
            double a0 =
                    offset
                            + i * Math.PI * 2.0D
                            / sides;
            double a1 =
                    offset
                            + (i + 1) * Math.PI * 2.0D
                            / sides;

            double y0 = Math.cos(a0) * radius;
            double z0 = Math.sin(a0) * radius;
            double y1 = Math.cos(a1) * radius;
            double z1 = Math.sin(a1) * radius;

            double middle = (a0 + a1) * 0.5D;
            float normalY = (float) Math.cos(middle);
            float normalZ = (float) Math.sin(middle);

            quad(
                    consumer,
                    pose,
                    packedLight,
                    -halfLength, y0, z0,
                    halfLength, y0, z0,
                    halfLength, y1, z1,
                    -halfLength, y1, z1,
                    0.0F,
                    normalY,
                    normalZ
            );
        }
    }

    private void drawCylinderEnds(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int packedLight,
            double halfLength,
            double radius,
            int sides) {

        double offset = Math.PI / sides;

        for (int i = 0; i < sides; i++) {
            double a0 =
                    offset
                            + i * Math.PI * 2.0D
                            / sides;
            double a1 =
                    offset
                            + (i + 1) * Math.PI * 2.0D
                            / sides;

            double y0 = Math.cos(a0) * radius;
            double z0 = Math.sin(a0) * radius;
            double y1 = Math.cos(a1) * radius;
            double z1 = Math.sin(a1) * radius;

            triangle(
                    consumer,
                    pose,
                    packedLight,
                    -halfLength, 0.0D, 0.0D,
                    -halfLength, y1, z1,
                    -halfLength, y0, z0,
                    -1.0F, 0.0F, 0.0F
            );

            triangle(
                    consumer,
                    pose,
                    packedLight,
                    halfLength, 0.0D, 0.0D,
                    halfLength, y0, z0,
                    halfLength, y1, z1,
                    1.0F, 0.0F, 0.0F
            );
        }
    }

    private void drawBox(
            PoseStack stack,
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
                x0, y1, z0, x0, y1, z1,
                x1, y1, z1, x1, y1, z0,
                0.0F, 1.0F, 0.0F);
        quad(consumer, pose, packedLight,
                x0, y0, z1, x0, y0, z0,
                x1, y0, z0, x1, y0, z1,
                0.0F, -1.0F, 0.0F);
        quad(consumer, pose, packedLight,
                x0, y0, z1, x1, y0, z1,
                x1, y1, z1, x0, y1, z1,
                0.0F, 0.0F, 1.0F);
        quad(consumer, pose, packedLight,
                x1, y0, z0, x0, y0, z0,
                x0, y1, z0, x1, y1, z0,
                0.0F, 0.0F, -1.0F);
        quad(consumer, pose, packedLight,
                x1, y0, z1, x1, y0, z0,
                x1, y1, z0, x1, y1, z1,
                1.0F, 0.0F, 0.0F);
        quad(consumer, pose, packedLight,
                x0, y0, z0, x0, y0, z1,
                x0, y1, z1, x0, y1, z0,
                -1.0F, 0.0F, 0.0F);
    }

    private void triangle(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            float nx, float ny, float nz) {

        vertex(consumer, pose, packedLight,
                x0, y0, z0, 0.5F, 0.5F,
                nx, ny, nz);
        vertex(consumer, pose, packedLight,
                x1, y1, z1, 0.0F, 1.0F,
                nx, ny, nz);
        vertex(consumer, pose, packedLight,
                x2, y2, z2, 1.0F, 1.0F,
                nx, ny, nz);
        // entityCutoutNoCull is emitted as quads. Repeat the center vertex so
        // this triangular fan slice is a valid degenerate quad.
        vertex(consumer, pose, packedLight,
                x0, y0, z0, 0.5F, 0.5F,
                nx, ny, nz);
    }

    private void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            float nx, float ny, float nz) {

        vertex(consumer, pose, packedLight,
                x0, y0, z0, 0.0F, 0.0F,
                nx, ny, nz);
        vertex(consumer, pose, packedLight,
                x1, y1, z1, 1.0F, 0.0F,
                nx, ny, nz);
        vertex(consumer, pose, packedLight,
                x2, y2, z2, 1.0F, 1.0F,
                nx, ny, nz);
        vertex(consumer, pose, packedLight,
                x3, y3, z3, 0.0F, 1.0F,
                nx, ny, nz);
    }

    private void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            double x, double y, double z,
            float u, float v,
            float nx, float ny, float nz) {

        consumer.vertex(
                        pose.pose(),
                        (float) x,
                        (float) y,
                        (float) z
                )
                .color(1.0F, 1.0F, 1.0F, 1.0F)
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
