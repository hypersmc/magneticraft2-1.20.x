package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.PulleyBlockEntity_wood;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import static com.magneticraft2.common.block.stage.copper.PulleyBlock_wood.POWERED;
import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * Dynamic wooden pulley + open leather-belt renderer.
 *
 * The belt is rendered as an actual narrow textured ribbon wrapped around both
 * pulleys rather than as debug lines. One endpoint renders each connection to
 * avoid duplicate geometry.
 */
public class PulleyBlockEntity_woodRenderer implements BlockEntityRenderer<PulleyBlockEntity_wood> {
    private static final ResourceLocation BELT_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/brown_wool.png");

    private static final double BELT_HALF_WIDTH = 0.075D;
    private static final double BELT_HALF_THICKNESS = 0.022D;
    private static final int ARC_SEGMENTS = 10;

    public PulleyBlockEntity_woodRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PulleyBlockEntity_wood blockEntity,
                       float partialTicks,
                       PoseStack stack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        if (blockEntity.getBlockState().getValue(POWERED)) {
            stack.pushPose();
            stack.translate(0.5D, 0.5D, 0.5D);
            applyPulleyRotation(blockEntity, partialTicks, stack);
            stack.translate(-0.5D, -0.5D, -0.5D);

            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    blockEntity.getBlockState().setValue(POWERED, false),
                    stack,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
            stack.popPose();
        }

        renderBelt(blockEntity, partialTicks, stack, bufferSource, packedLight);
    }

    private void applyPulleyRotation(PulleyBlockEntity_wood blockEntity,
                                     float partialTicks,
                                     PoseStack stack) {
        float rotation = blockEntity.getVisualRotationDegrees(partialTicks);
        Direction facing = blockEntity.getBlockState().getValue(FACING);

        if (facing.getAxis() == Direction.Axis.Y) {
            stack.mulPose(Axis.YP.rotationDegrees(rotation));
        } else if (facing.getAxis() == Direction.Axis.X) {
            stack.mulPose(Axis.XP.rotationDegrees(rotation));
        } else {
            stack.mulPose(Axis.ZP.rotationDegrees(rotation));
        }
    }

    private void renderBelt(PulleyBlockEntity_wood blockEntity,
                            float partialTicks,
                            PoseStack stack,
                            MultiBufferSource bufferSource,
                            int packedLight) {
        BlockPos partnerPos = blockEntity.getBeltPartner();
        if (partnerPos == null || blockEntity.getLevel() == null) {
            return;
        }

        // Render once from the deterministically lower endpoint.
        if (blockEntity.getBlockPos().asLong() > partnerPos.asLong()) {
            return;
        }

        if (!(blockEntity.getLevel().getBlockEntity(partnerPos) instanceof PulleyBlockEntity_wood partner)
                || !partner.isLinkedTo(blockEntity.getBlockPos())
                || partner.getGearAxis() != blockEntity.getGearAxis()) {
            return;
        }

        Vec3 startCenter = new Vec3(0.5D, 0.5D, 0.5D);
        BlockPos delta = partnerPos.subtract(blockEntity.getBlockPos());
        Vec3 endCenter = new Vec3(
                delta.getX() + 0.5D,
                delta.getY() + 0.5D,
                delta.getZ() + 0.5D
        );

        Vec3 centerLine = endCenter.subtract(startCenter);
        if (centerLine.lengthSqr() < 0.0001D) {
            return;
        }

        Vec3 runDirection = centerLine.normalize();
        Vec3 axis = switch (blockEntity.getGearAxis()) {
            case X -> new Vec3(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3(0.0D, 0.0D, 1.0D);
        };

        Vec3 radialSide = axis.cross(runDirection);
        if (radialSide.lengthSqr() < 0.0001D) {
            return;
        }
        radialSide = radialSide.normalize();

        double startRadius = blockEntity.getPulleyRadius();
        double endRadius = partner.getPulleyRadius();

        Vec3 startA = startCenter.add(radialSide.scale(startRadius));
        Vec3 endA = endCenter.add(radialSide.scale(endRadius));
        Vec3 startB = startCenter.subtract(radialSide.scale(startRadius));
        Vec3 endB = endCenter.subtract(radialSide.scale(endRadius));

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(BELT_TEXTURE));
        PoseStack.Pose pose = stack.last();

        // Straight upper/lower runs.
        drawBeltPrism(
                consumer,
                pose,
                startA,
                endA,
                axis,
                radialSide,
                packedLight,
                0.56F,
                0.34F,
                0.18F
        );
        drawBeltPrism(
                consumer,
                pose,
                startB,
                endB,
                axis,
                radialSide.scale(-1.0D),
                packedLight,
                0.56F,
                0.34F,
                0.18F
        );

        // Wrap the belt around the outer half of each pulley so it reads as one loop
        // rather than two disconnected rails.
        drawPulleyArc(
                consumer,
                pose,
                startCenter,
                radialSide,
                runDirection.scale(-1.0D),
                axis,
                startRadius,
                packedLight
        );
        drawPulleyArc(
                consumer,
                pose,
                endCenter,
                radialSide.scale(-1.0D),
                runDirection,
                axis,
                endRadius,
                packedLight
        );

        // The tangent velocity is opposite the positive angular direction at startA:
        // v = omega x r, and r = axis x run. Therefore belt travel uses -rotation here.
        // This fixes the previous visual where a CCW pulley appeared to drive the belt CW.
        double phase = -blockEntity.getVisualRotationDegrees(partialTicks) / 360.0D;
        phase = phase - Math.floor(phase);

        for (int i = 0; i < 3; i++) {
            double spacing = i / 3.0D;
            double forwardT = (phase + spacing) % 1.0D;
            double returnT = (1.0D - phase + spacing) % 1.0D;

            Vec3 forwardPoint = startA.lerp(endA, forwardT);
            Vec3 returnPoint = startB.lerp(endB, returnT);

            drawTravelMarker(
                    consumer,
                    pose,
                    forwardPoint,
                    endA.subtract(startA).normalize(),
                    axis,
                    radialSide,
                    packedLight
            );
            drawTravelMarker(
                    consumer,
                    pose,
                    returnPoint,
                    startB.subtract(endB).normalize(),
                    axis,
                    radialSide.scale(-1.0D),
                    packedLight
            );
        }
    }

    private void drawPulleyArc(VertexConsumer consumer,
                               PoseStack.Pose pose,
                               Vec3 center,
                               Vec3 firstRadial,
                               Vec3 outerDirection,
                               Vec3 axis,
                               double radius,
                               int packedLight) {
        Vec3 previous = center.add(firstRadial.scale(radius));

        for (int i = 1; i <= ARC_SEGMENTS; i++) {
            double t = i / (double) ARC_SEGMENTS;
            double angle = Math.PI * t;

            Vec3 radial = firstRadial.scale(Math.cos(angle))
                    .add(outerDirection.scale(Math.sin(angle)))
                    .normalize();

            Vec3 next = center.add(radial.scale(radius));
            Vec3 midpointRadial = previous.add(next).scale(0.5D).subtract(center);
            if (midpointRadial.lengthSqr() < 0.0001D) {
                midpointRadial = radial;
            } else {
                midpointRadial = midpointRadial.normalize();
            }

            drawBeltPrism(
                    consumer,
                    pose,
                    previous,
                    next,
                    axis,
                    midpointRadial,
                    packedLight,
                    0.56F,
                    0.34F,
                    0.18F
            );

            previous = next;
        }
    }

    private void drawTravelMarker(VertexConsumer consumer,
                                  PoseStack.Pose pose,
                                  Vec3 center,
                                  Vec3 travelDirection,
                                  Vec3 axis,
                                  Vec3 thicknessDirection,
                                  int packedLight) {
        Vec3 halfTravel = travelDirection.scale(0.035D);
        drawBeltPrism(
                consumer,
                pose,
                center.subtract(halfTravel),
                center.add(halfTravel),
                axis,
                thicknessDirection,
                packedLight,
                0.82F,
                0.58F,
                0.30F
        );
    }

    private void drawBeltPrism(VertexConsumer consumer,
                               PoseStack.Pose pose,
                               Vec3 from,
                               Vec3 to,
                               Vec3 widthDirection,
                               Vec3 thicknessDirection,
                               int packedLight,
                               float red,
                               float green,
                               float blue) {
        Vec3 width = widthDirection.normalize().scale(BELT_HALF_WIDTH);
        Vec3 thickness = thicknessDirection.normalize().scale(BELT_HALF_THICKNESS);

        Vec3 a0 = from.subtract(width).subtract(thickness);
        Vec3 a1 = from.add(width).subtract(thickness);
        Vec3 a2 = from.add(width).add(thickness);
        Vec3 a3 = from.subtract(width).add(thickness);

        Vec3 b0 = to.subtract(width).subtract(thickness);
        Vec3 b1 = to.add(width).subtract(thickness);
        Vec3 b2 = to.add(width).add(thickness);
        Vec3 b3 = to.subtract(width).add(thickness);

        Vec3 run = to.subtract(from);
        Vec3 runNormal = run.lengthSqr() < 0.0001D ? new Vec3(0.0D, 1.0D, 0.0D) : run.normalize();

        drawQuad(consumer, pose, a3, a2, b2, b3, thicknessDirection, packedLight, red, green, blue);
        drawQuad(consumer, pose, a0, b0, b1, a1, thicknessDirection.scale(-1.0D), packedLight, red, green, blue);
        drawQuad(consumer, pose, a1, b1, b2, a2, widthDirection, packedLight, red, green, blue);
        drawQuad(consumer, pose, a0, a3, b3, b0, widthDirection.scale(-1.0D), packedLight, red, green, blue);
        drawQuad(consumer, pose, a0, a1, a2, a3, runNormal.scale(-1.0D), packedLight, red, green, blue);
        drawQuad(consumer, pose, b3, b2, b1, b0, runNormal, packedLight, red, green, blue);
    }

    private void drawQuad(VertexConsumer consumer,
                          PoseStack.Pose pose,
                          Vec3 v0,
                          Vec3 v1,
                          Vec3 v2,
                          Vec3 v3,
                          Vec3 normal,
                          int packedLight,
                          float red,
                          float green,
                          float blue) {
        Vec3 n = normal.lengthSqr() < 0.0001D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : normal.normalize();

        beltVertex(consumer, pose, v0, n, 0.0F, 0.0F, packedLight, red, green, blue);
        beltVertex(consumer, pose, v1, n, 1.0F, 0.0F, packedLight, red, green, blue);
        beltVertex(consumer, pose, v2, n, 1.0F, 1.0F, packedLight, red, green, blue);
        beltVertex(consumer, pose, v3, n, 0.0F, 1.0F, packedLight, red, green, blue);
    }

    private void beltVertex(VertexConsumer consumer,
                            PoseStack.Pose pose,
                            Vec3 position,
                            Vec3 normal,
                            float u,
                            float v,
                            int packedLight,
                            float red,
                            float green,
                            float blue) {
        consumer.vertex(pose.pose(), (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }
}
