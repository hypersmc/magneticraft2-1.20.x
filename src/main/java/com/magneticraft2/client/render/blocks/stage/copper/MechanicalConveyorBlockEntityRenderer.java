package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.MechanicalConveyorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Moving top surface for the early Mechanical Conveyor.
 *
 * The wooden frame and rollers are normal block-model geometry. Only the leather transport
 * surface is redrawn here so its UVs can move continuously with the Gear V2 roller speed.
 */
public class MechanicalConveyorBlockEntityRenderer implements BlockEntityRenderer<MechanicalConveyorBlockEntity> {
    private static final ResourceLocation BELT_TEXTURE =
            new ResourceLocation("magneticraft2", "textures/block/leather_belt.png");

    private static final double BELT_MIN = 0.125D;
    private static final double BELT_MAX = 0.875D;
    private static final double BELT_Y = 0.376D;

    public MechanicalConveyorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MechanicalConveyorBlockEntity conveyor,
                       float partialTicks,
                       PoseStack stack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        VertexConsumer consumer =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(BELT_TEXTURE));
        PoseStack.Pose pose = stack.last();

        double phase = positiveModulo(conveyor.getVisualBeltTravel(partialTicks), 1.0D);
        Direction facing = conveyor.getConveyorFacing();

        // Split at the texture wrap point so all UVs remain inside 0..1. This avoids
        // depending on texture wrap state and prevents the moving leather from smearing.
        double firstLength = 1.0D - phase;
        if (firstLength > 0.0001D) {
            drawSurfaceSection(
                    consumer,
                    pose,
                    facing,
                    0.0D,
                    firstLength,
                    (float) phase,
                    1.0F,
                    packedLight
            );
        }

        if (phase > 0.0001D) {
            drawSurfaceSection(
                    consumer,
                    pose,
                    facing,
                    firstLength,
                    1.0D,
                    0.0F,
                    (float) phase,
                    packedLight
            );
        }
    }

    /**
     * fromDistance/toDistance are measured from the back of the conveyor toward its FACING.
     */
    private void drawSurfaceSection(VertexConsumer consumer,
                                    PoseStack.Pose pose,
                                    Direction facing,
                                    double fromDistance,
                                    double toDistance,
                                    float u0,
                                    float u1,
                                    int packedLight) {
        Vec3 leftStart = pointOnBelt(facing, fromDistance, true);
        Vec3 rightStart = pointOnBelt(facing, fromDistance, false);
        Vec3 rightEnd = pointOnBelt(facing, toDistance, false);
        Vec3 leftEnd = pointOnBelt(facing, toDistance, true);

        vertex(consumer, pose, leftStart, u0, 0.0F, packedLight);
        vertex(consumer, pose, rightStart, u0, 1.0F, packedLight);
        vertex(consumer, pose, rightEnd, u1, 1.0F, packedLight);
        vertex(consumer, pose, leftEnd, u1, 0.0F, packedLight);
    }

    private Vec3 pointOnBelt(Direction facing, double distance, boolean left) {
        return switch (facing) {
            case NORTH -> new Vec3(
                    left ? BELT_MIN : BELT_MAX,
                    BELT_Y,
                    1.0D - distance
            );
            case SOUTH -> new Vec3(
                    left ? BELT_MAX : BELT_MIN,
                    BELT_Y,
                    distance
            );
            case EAST -> new Vec3(
                    distance,
                    BELT_Y,
                    left ? BELT_MIN : BELT_MAX
            );
            case WEST -> new Vec3(
                    1.0D - distance,
                    BELT_Y,
                    left ? BELT_MAX : BELT_MIN
            );
            default -> new Vec3(0.5D, BELT_Y, 0.5D);
        };
    }

    private void vertex(VertexConsumer consumer,
                        PoseStack.Pose pose,
                        Vec3 position,
                        float u,
                        float v,
                        int packedLight) {
        consumer.vertex(
                        pose.pose(),
                        (float) position.x,
                        (float) position.y,
                        (float) position.z
                )
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private double positiveModulo(double value, double divisor) {
        double result = value % divisor;
        return result < 0.0D ? result + divisor : result;
    }
}
