package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.OverloadDisconnectBlock_wood;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.OverloadDisconnectBlockEntity_wood;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

public class OverloadDisconnectBlockEntity_woodRenderer
        implements BlockEntityRenderer<OverloadDisconnectBlockEntity_wood> {

    private static final ResourceLocation SHAFT_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/stripped_oak_log.png"
            );

    private static final ResourceLocation WOOD_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/spruce_planks.png"
            );

    private static final ResourceLocation COPPER_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/copper_block.png"
            );

    private static final ResourceLocation TRIP_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/redstone_block.png"
            );

    public OverloadDisconnectBlockEntity_woodRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            OverloadDisconnectBlockEntity_wood disconnect,
            float partialTicks,
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        if (disconnect.getLevel() == null
                || !disconnect.getBlockState().getValue(
                        OverloadDisconnectBlock_wood.ROTATING
                )) {
            return;
        }

        Direction.Axis axis = disconnect.getBlockState()
                .getValue(DirectionalBlock.FACING)
                .getAxis();

        Direction positive = positiveDirection(axis);
        Direction negative = positive.getOpposite();

        float positiveAngle = sideRotation(
                disconnect,
                positive,
                partialTicks
        );

        float negativeAngle = sideRotation(
                disconnect,
                negative,
                partialTicks
        );

        if (disconnect.isEngaged()) {
            float locked =
                    disconnect.getVisualRotationDegrees(
                            partialTicks
                    );
            positiveAngle = locked;
            negativeAngle = locked;
        }

        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);
        orientLocalXToDirection(stack, positive);

        VertexConsumer shaftConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                SHAFT_TEXTURE
                        )
                );

        renderRotatingBox(
                stack,
                shaftConsumer,
                packedLight,
                -0.285D,
                0.43D,
                0.375D,
                negativeAngle
        );

        renderRotatingBox(
                stack,
                shaftConsumer,
                packedLight,
                0.285D,
                0.43D,
                0.375D,
                positiveAngle
        );

        VertexConsumer woodConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                WOOD_TEXTURE
                        )
                );

        drawBox(
                stack,
                woodConsumer,
                packedLight,
                0.30D,
                0.54D,
                0.54D
        );

        VertexConsumer copperConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                COPPER_TEXTURE
                        )
                );

        for (double x : new double[]{-0.115D, 0.115D}) {
            stack.pushPose();
            stack.translate(x, 0.0D, 0.0D);
            drawBox(
                    stack,
                    copperConsumer,
                    packedLight,
                    0.04D,
                    0.575D,
                    0.575D
            );
            stack.popPose();
        }

        ResourceLocation indicatorTexture =
                disconnect.isTripped()
                        ? TRIP_TEXTURE
                        : COPPER_TEXTURE;

        VertexConsumer indicatorConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                indicatorTexture
                        )
                );

        stack.pushPose();
        stack.translate(
                0.0D,
                0.34D,
                0.0D
        );
        drawBox(
                stack,
                indicatorConsumer,
                packedLight,
                0.14D,
                0.12D,
                0.18D
        );
        stack.popPose();

        stack.popPose();
    }

    private float sideRotation(
            OverloadDisconnectBlockEntity_wood disconnect,
            Direction side,
            float partialTicks) {

        BlockPos neighborPos =
                disconnect.getBlockPos().relative(side);

        BlockEntity neighbor =
                disconnect.getLevel().getBlockEntity(
                        neighborPos
                );

        if (neighbor instanceof GearBlockEntity gear
                && gear.getGearAxis()
                == disconnect.getGearAxis()) {
            return gear.getVisualRotationDegrees(
                    partialTicks
            );
        }

        return disconnect.getVisualRotationDegrees(
                partialTicks
        );
    }

    private void renderRotatingBox(
            PoseStack stack,
            VertexConsumer consumer,
            int packedLight,
            double centerX,
            double length,
            double thickness,
            float rotation) {

        stack.pushPose();
        stack.translate(centerX, 0.0D, 0.0D);
        stack.mulPose(
                Axis.XP.rotationDegrees(rotation)
        );

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

    private void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            float normalX,
            float normalY,
            float normalZ) {

        vertex(consumer, pose, packedLight,
                x0, y0, z0, 0.0F, 0.0F,
                normalX, normalY, normalZ);
        vertex(consumer, pose, packedLight,
                x1, y1, z1, 0.0F, 1.0F,
                normalX, normalY, normalZ);
        vertex(consumer, pose, packedLight,
                x2, y2, z2, 1.0F, 1.0F,
                normalX, normalY, normalZ);
        vertex(consumer, pose, packedLight,
                x3, y3, z3, 1.0F, 0.0F,
                normalX, normalY, normalZ);
    }

    private void vertex(
            VertexConsumer consumer,
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
                .overlayCoords(
                        OverlayTexture.NO_OVERLAY
                )
                .uv2(packedLight)
                .normal(
                        pose.normal(),
                        normalX,
                        normalY,
                        normalZ
                )
                .endVertex();
    }
}
