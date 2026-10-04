package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.CrankBlock_wood;
import com.magneticraft2.common.blockentity.stage.copper.CrankBlockEntity_wood;
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
 * Visible slider-crank mechanism.
 *
 * Local X is the rotating shaft and local +Y is the reciprocating output. The
 * connecting rod uses real slider-crank geometry instead of simply bobbing a
 * cube back and forth, so the motion reads correctly at low RPM.
 */
public class CrankBlockEntity_woodRenderer
        implements BlockEntityRenderer<CrankBlockEntity_wood> {

    private static final ResourceLocation SHAFT_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/stripped_oak_log.png"
            );

    private static final ResourceLocation WOOD_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/oak_planks.png"
            );

    private static final ResourceLocation COPPER_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/copper_block.png"
            );

    private static final double WHEEL_RADIUS = 0.30D;
    private static final double WHEEL_HALF_LENGTH = 0.075D;
    private static final int WHEEL_SIDES = 12;

    private static final double CRANK_RADIUS = 0.16D;
    private static final double CONNECTING_ROD_LENGTH = 0.34D;

    public CrankBlockEntity_woodRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            CrankBlockEntity_wood crank,
            float partialTicks,
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        Direction shaftDirection =
                positiveDirection(
                        crank.getBlockState()
                                .getValue(
                                        DirectionalBlock.FACING
                                )
                                .getAxis()
                );

        Direction rodDirection =
                crank.getBlockState()
                        .getValue(
                                CrankBlock_wood.ROD_DIRECTION
                        );

        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);
        orientLocalXAndY(
                stack,
                shaftDirection,
                rodDirection
        );

        renderDirectionalFrame(
                stack,
                bufferSource,
                packedLight
        );

        float angleDegrees =
                crank.getVisualRotationDegrees(
                        partialTicks
                );

        double angle = Math.toRadians(
                angleDegrees
        );

        double pinY =
                Math.cos(angle)
                        * CRANK_RADIUS;
        double pinZ =
                Math.sin(angle)
                        * CRANK_RADIUS;

        double sliderY =
                pinY
                        + Math.sqrt(
                                Math.max(
                                        0.0D,
                                        CONNECTING_ROD_LENGTH
                                                * CONNECTING_ROD_LENGTH
                                                - pinZ * pinZ
                                )
                        );

        VertexConsumer shaftConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                SHAFT_TEXTURE
                        )
                );

        stack.pushPose();
        stack.mulPose(
                Axis.XP.rotationDegrees(
                        angleDegrees
                )
        );
        drawBox(
                stack,
                shaftConsumer,
                packedLight,
                0.92D,
                0.22D,
                0.22D
        );
        stack.popPose();

        VertexConsumer woodConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                WOOD_TEXTURE
                        )
                );

        stack.pushPose();
        stack.mulPose(
                Axis.XP.rotationDegrees(
                        angleDegrees
                )
        );

        drawCylinder(
                stack.last(),
                woodConsumer,
                packedLight,
                WHEEL_HALF_LENGTH,
                WHEEL_RADIUS,
                WHEEL_SIDES
        );

        stack.pushPose();
        stack.translate(
                0.10D,
                CRANK_RADIUS * 0.5D,
                0.0D
        );
        drawBox(
                stack,
                woodConsumer,
                packedLight,
                0.10D,
                CRANK_RADIUS,
                0.09D
        );
        stack.popPose();

        stack.popPose();

        renderRodBetween(
                stack,
                woodConsumer,
                packedLight,
                pinY,
                pinZ,
                sliderY,
                0.0D
        );

        // Two fixed guide rails make it immediately obvious that the far end is
        // a linear output, not another rotating shaft.
        for (double guideZ :
                new double[]{-0.13D, 0.13D}) {
            stack.pushPose();
            stack.translate(
                    0.0D,
                    0.35D,
                    guideZ
            );
            drawBox(
                    stack,
                    woodConsumer,
                    packedLight,
                    0.12D,
                    0.38D,
                    0.06D
            );
            stack.popPose();
        }

        VertexConsumer copperConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                COPPER_TEXTURE
                        )
                );

        stack.pushPose();
        stack.translate(
                0.10D,
                pinY,
                pinZ
        );
        drawBox(
                stack,
                copperConsumer,
                packedLight,
                0.16D,
                0.10D,
                0.10D
        );
        stack.popPose();

        stack.pushPose();
        stack.translate(
                0.0D,
                sliderY,
                0.0D
        );
        drawBox(
                stack,
                copperConsumer,
                packedLight,
                0.20D,
                0.13D,
                0.22D
        );
        stack.popPose();

        double outputEnd = 0.54D;
        double outputLength =
                Math.max(
                        0.02D,
                        outputEnd - sliderY
                );

        stack.pushPose();
        stack.translate(
                0.0D,
                sliderY
                        + outputLength * 0.5D,
                0.0D
        );
        drawBox(
                stack,
                copperConsumer,
                packedLight,
                0.10D,
                outputLength,
                0.10D
        );
        stack.popPose();

        stack.popPose();
    }

    private void renderDirectionalFrame(
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight) {
        // The complete support follows local +Y, which is the selected linear
        // output direction. The old JSON frame only followed the shaft axis and
        // could end up 90/180 degrees away from its own moving mechanism.
        VertexConsumer woodConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                WOOD_TEXTURE
                        )
                );

        stack.pushPose();
        stack.translate(
                0.0D,
                -0.43D,
                0.0D
        );
        drawBox(
                stack,
                woodConsumer,
                packedLight,
                0.94D,
                0.12D,
                0.78D
        );
        stack.popPose();

        VertexConsumer postConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                SHAFT_TEXTURE
                        )
                );

        // Two U-shaped bearing cheeks. They deliberately stay open toward +Y
        // so the slider/crosshead has a clear path toward the driven machine.
        for (double x : new double[]{-0.39D, 0.39D}) {
            for (double z : new double[]{-0.31D, 0.31D}) {
                stack.pushPose();
                stack.translate(
                        x,
                        -0.08D,
                        z
                );
                drawBox(
                        stack,
                        postConsumer,
                        packedLight,
                        0.10D,
                        0.68D,
                        0.10D
                );
                stack.popPose();
            }

            stack.pushPose();
            stack.translate(
                    x,
                    -0.38D,
                    0.0D
            );
            drawBox(
                    stack,
                    postConsumer,
                    packedLight,
                    0.10D,
                    0.10D,
                    0.72D
            );
            stack.popPose();
        }

        VertexConsumer copperConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                COPPER_TEXTURE
                        )
                );

        for (double x : new double[]{-0.405D, 0.405D}) {
            stack.pushPose();
            stack.translate(
                    x,
                    0.0D,
                    0.0D
            );
            drawBox(
                    stack,
                    copperConsumer,
                    packedLight,
                    0.09D,
                    0.25D,
                    0.25D
            );
            stack.popPose();
        }
    }

    private void renderRodBetween(
            PoseStack stack,
            VertexConsumer consumer,
            int packedLight,
            double fromY,
            double fromZ,
            double toY,
            double toZ) {

        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double length = Math.sqrt(
                dy * dy + dz * dz
        );

        if (length < 0.0001D) {
            return;
        }

        double midY =
                (fromY + toY) * 0.5D;
        double midZ =
                (fromZ + toZ) * 0.5D;

        float rodAngle =
                (float) Math.toDegrees(
                        Math.atan2(
                                dz,
                                dy
                        )
                );

        stack.pushPose();
        stack.translate(
                0.04D,
                midY,
                midZ
        );
        stack.mulPose(
                Axis.XP.rotationDegrees(
                        rodAngle
                )
        );

        drawBox(
                stack,
                consumer,
                packedLight,
                0.08D,
                length,
                0.07D
        );

        stack.popPose();
    }

    private void orientLocalXAndY(
            PoseStack stack,
            Direction shaftDirection,
            Direction rodDirection) {

        orientLocalXToDirection(
                stack,
                shaftDirection
        );

        Direction baseY =
                baseLocalY(
                        shaftDirection
                );
        Direction baseZ =
                baseLocalZ(
                        shaftDirection
                );

        float roll;

        if (rodDirection == baseY) {
            roll = 0.0F;
        } else if (rodDirection == baseZ) {
            roll = 90.0F;
        } else if (rodDirection
                == baseY.getOpposite()) {
            roll = 180.0F;
        } else if (rodDirection
                == baseZ.getOpposite()) {
            roll = 270.0F;
        } else {
            roll = 0.0F;
        }

        stack.mulPose(
                Axis.XP.rotationDegrees(roll)
        );
    }

    private Direction baseLocalY(
            Direction shaftDirection) {
        return switch (shaftDirection) {
            case EAST, WEST, SOUTH, NORTH ->
                    Direction.UP;
            case UP -> Direction.WEST;
            case DOWN -> Direction.EAST;
        };
    }

    private Direction baseLocalZ(
            Direction shaftDirection) {
        return switch (shaftDirection) {
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.NORTH;
            case UP, DOWN -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case NORTH -> Direction.EAST;
        };
    }

    private Direction positiveDirection(
            Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.EAST;
            case Y -> Direction.UP;
            case Z -> Direction.SOUTH;
        };
    }

    private void orientLocalXToDirection(
            PoseStack stack,
            Direction direction) {
        switch (direction) {
            case EAST -> {
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

    private void drawCylinder(
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

            quad(
                    consumer,
                    pose,
                    packedLight,
                    -halfLength, y0, z0,
                    halfLength, y0, z0,
                    halfLength, y1, z1,
                    -halfLength, y1, z1,
                    0.0F,
                    (float) Math.cos(middle),
                    (float) Math.sin(middle)
            );

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
