package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.ClutchBlock_wood;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.ClutchBlockEntity_wood;
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

/**
 * Renders two independently rotating shaft halves and a sliding wooden clutch
 * collar. When open, each half can visibly follow the network on its own side;
 * when engaged the collar bridges them and Gear V2 locks both sides 1:1.
 */
public class ClutchBlockEntity_woodRenderer
        implements BlockEntityRenderer<ClutchBlockEntity_wood> {
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
    private static final ResourceLocation BAND_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/copper_block.png"
            );

    public ClutchBlockEntity_woodRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ClutchBlockEntity_wood clutch,
                       float partialTicks,
                       PoseStack stack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        if (clutch.getLevel() == null
                || !clutch.getBlockState()
                        .getValue(ClutchBlock_wood.ROTATING)) {
            return;
        }

        Direction facing = clutch.getBlockState()
                .getValue(DirectionalBlock.FACING);
        boolean engaged = clutch.getBlockState()
                .getValue(ClutchBlock_wood.ENGAGED);

        VertexConsumer shaftConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                SHAFT_TEXTURE
                        )
                );
        VertexConsumer woodConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                WOOD_TEXTURE
                        )
                );
        VertexConsumer bandConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                BAND_TEXTURE
                        )
                );

        float positiveAngle = sideRotation(
                clutch,
                facing,
                partialTicks
        );
        float negativeAngle = sideRotation(
                clutch,
                facing.getOpposite(),
                partialTicks
        );

        if (engaged) {
            float lockedAngle =
                    clutch.getVisualRotationDegrees(
                            partialTicks
                    )
                            * axisDirectionSign(facing);

            positiveAngle = lockedAngle;
            negativeAngle = lockedAngle;
        }

        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);
        orientLocalXToDirection(stack, facing);

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

        float collarAngle = positiveAngle;

        double collarX = engaged
                ? 0.0D
                : 0.205D;

        renderClutchCollar(
                stack,
                woodConsumer,
                bandConsumer,
                packedLight,
                collarX,
                collarAngle,
                engaged
        );

        stack.popPose();
    }

    private float sideRotation(ClutchBlockEntity_wood clutch,
                               Direction side,
                               float partialTicks) {
        BlockPos neighborPos =
                clutch.getBlockPos().relative(side);

        BlockEntity neighbor =
                clutch.getLevel().getBlockEntity(
                        neighborPos
                );

        if (neighbor
                instanceof GearBlockEntity gear
                && gear.getGearAxis()
                == clutch.getGearAxis()) {
            return gear.getVisualRotationDegrees(
                    partialTicks
            ) * axisDirectionSign(
                    clutch.getBlockState()
                            .getValue(
                                    DirectionalBlock.FACING
                            )
            );
        }

        return clutch.getVisualRotationDegrees(
                partialTicks
        ) * axisDirectionSign(
                clutch.getBlockState()
                        .getValue(
                                DirectionalBlock.FACING
                        )
        );
    }

    private void renderRotatingBox(PoseStack stack,
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

    private void renderClutchCollar(PoseStack stack,
                                    VertexConsumer woodConsumer,
                                    VertexConsumer bandConsumer,
                                    int packedLight,
                                    double centerX,
                                    float rotation,
                                    boolean engaged) {
        stack.pushPose();
        stack.translate(centerX, 0.0D, 0.0D);
        stack.mulPose(
                Axis.XP.rotationDegrees(rotation)
        );

        double collarLength = engaged
                ? 0.34D
                : 0.27D;

        drawBox(
                stack,
                woodConsumer,
                packedLight,
                collarLength,
                0.50D,
                0.50D
        );

        // Narrow metal bands make the moving sleeve read as a mechanical
        // coupling rather than another oversized section of shaft.
        for (double bandX : new double[]{
                -collarLength * 0.34D,
                collarLength * 0.34D
        }) {
            stack.pushPose();
            stack.translate(
                    bandX,
                    0.0D,
                    0.0D
            );
            drawBox(
                    stack,
                    bandConsumer,
                    packedLight,
                    0.045D,
                    0.535D,
                    0.535D
            );
            stack.popPose();
        }

        stack.popPose();
    }

    private int axisDirectionSign(Direction direction) {
        return switch (direction) {
            case EAST, UP, SOUTH -> 1;
            case WEST, DOWN, NORTH -> -1;
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

    private void quad(VertexConsumer consumer,
                      PoseStack.Pose pose,
                      int packedLight,
                      double x0,
                      double y0,
                      double z0,
                      double x1,
                      double y1,
                      double z1,
                      double x2,
                      double y2,
                      double z2,
                      double x3,
                      double y3,
                      double z3,
                      float normalX,
                      float normalY,
                      float normalZ) {
        vertex(
                consumer,
                pose,
                packedLight,
                x0, y0, z0,
                0.0F, 0.0F,
                normalX, normalY, normalZ
        );
        vertex(
                consumer,
                pose,
                packedLight,
                x1, y1, z1,
                0.0F, 1.0F,
                normalX, normalY, normalZ
        );
        vertex(
                consumer,
                pose,
                packedLight,
                x2, y2, z2,
                1.0F, 1.0F,
                normalX, normalY, normalZ
        );
        vertex(
                consumer,
                pose,
                packedLight,
                x3, y3, z3,
                1.0F, 0.0F,
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
                .color(
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F
                )
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
