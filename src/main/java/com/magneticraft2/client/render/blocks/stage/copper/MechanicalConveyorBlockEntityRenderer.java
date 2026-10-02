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

        double travel = conveyor.getVisualBeltTravel(partialTicks);
        float u0 = (float) positiveModulo(travel, 1.0D);
        float u1 = u0 + 1.0F;

        Direction facing = conveyor.getConveyorFacing();

        Vec3 leftStart;
        Vec3 rightStart;
        Vec3 rightEnd;
        Vec3 leftEnd;

        switch (facing) {
            case SOUTH -> {
                leftStart = new Vec3(BELT_MAX, BELT_Y, 0.0D);
                rightStart = new Vec3(BELT_MIN, BELT_Y, 0.0D);
                rightEnd = new Vec3(BELT_MIN, BELT_Y, 1.0D);
                leftEnd = new Vec3(BELT_MAX, BELT_Y, 1.0D);
            }
            case EAST -> {
                leftStart = new Vec3(0.0D, BELT_Y, BELT_MIN);
                rightStart = new Vec3(0.0D, BELT_Y, BELT_MAX);
                rightEnd = new Vec3(1.0D, BELT_Y, BELT_MAX);
                leftEnd = new Vec3(1.0D, BELT_Y, BELT_MIN);
            }
            case WEST -> {
                leftStart = new Vec3(1.0D, BELT_Y, BELT_MAX);
                rightStart = new Vec3(1.0D, BELT_Y, BELT_MIN);
                rightEnd = new Vec3(0.0D, BELT_Y, BELT_MIN);
                leftEnd = new Vec3(0.0D, BELT_Y, BELT_MAX);
            }
            case NORTH -> {
                leftStart = new Vec3(BELT_MIN, BELT_Y, 1.0D);
                rightStart = new Vec3(BELT_MAX, BELT_Y, 1.0D);
                rightEnd = new Vec3(BELT_MAX, BELT_Y, 0.0D);
                leftEnd = new Vec3(BELT_MIN, BELT_Y, 0.0D);
            }
            default -> {
                return;
            }
        }

        vertex(consumer, pose, leftStart, u0, 0.0F, packedLight);
        vertex(consumer, pose, rightStart, u0, 1.0F, packedLight);
        vertex(consumer, pose, rightEnd, u1, 1.0F, packedLight);
        vertex(consumer, pose, leftEnd, u1, 0.0F, packedLight);
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
