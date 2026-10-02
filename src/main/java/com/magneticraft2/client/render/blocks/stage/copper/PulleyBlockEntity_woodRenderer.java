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
 * Wooden pulley + open leather-belt renderer.
 *
 * The belt uses true external tangent points, tiled custom leather UVs, and a small
 * radial clearance from the pulley rim. Belt geometry is open-ended between segments
 * so touching straight/arc sections share an edge instead of z-fighting through
 * overlapping end caps.
 */
public class PulleyBlockEntity_woodRenderer implements BlockEntityRenderer<PulleyBlockEntity_wood> {
    private static final ResourceLocation BELT_TEXTURE =
            new ResourceLocation("magneticraft2", "textures/block/leather_belt.png");

    private static final double BELT_HALF_WIDTH = 0.085D;
    private static final double BELT_HALF_THICKNESS = 0.018D;
    private static final double BELT_CLEARANCE = 0.030D;
    private static final double TEXTURE_REPEAT_LENGTH = 0.50D;
    private static final int ARC_SEGMENTS_PER_HALF_TURN = 16;

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

        // Render each complete belt exactly once.
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
        double centerDistance = centerLine.length();
        if (centerDistance < 0.0001D) {
            return;
        }

        Vec3 runDirection = centerLine.scale(1.0D / centerDistance);
        Vec3 axis = axisVector(blockEntity.getGearAxis());

        Vec3 sideDirection = axis.cross(runDirection);
        if (sideDirection.lengthSqr() < 0.0001D) {
            return;
        }
        sideDirection = sideDirection.normalize();

        double startRadius = blockEntity.getPulleyRadius() + BELT_CLEARANCE;
        double endRadius = partner.getPulleyRadius() + BELT_CLEARANCE;

        // True external tangents for unequal pulley radii.
        //
        // Let n be the radial vector to a tangent point. For the segment between
        // C1 + r1*n and C2 + r2*n to be tangent, that segment must be perpendicular
        // to n, yielding n.run = (r1-r2)/distance.
        double radiusDifference = startRadius - endRadius;
        double tangentRunComponent = radiusDifference / centerDistance;
        tangentRunComponent = Math.max(-0.999D, Math.min(0.999D, tangentRunComponent));

        double tangentSideComponent =
                Math.sqrt(Math.max(0.0D, 1.0D - tangentRunComponent * tangentRunComponent));

        Vec3 topRadial = runDirection.scale(tangentRunComponent)
                .add(sideDirection.scale(tangentSideComponent))
                .normalize();
        Vec3 bottomRadial = runDirection.scale(tangentRunComponent)
                .subtract(sideDirection.scale(tangentSideComponent))
                .normalize();

        Vec3 startTop = startCenter.add(topRadial.scale(startRadius));
        Vec3 endTop = endCenter.add(topRadial.scale(endRadius));
        Vec3 startBottom = startCenter.add(bottomRadial.scale(startRadius));
        Vec3 endBottom = endCenter.add(bottomRadial.scale(endRadius));

        VertexConsumer consumer =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(BELT_TEXTURE));
        PoseStack.Pose pose = stack.last();

        // UV distance follows the actual physical belt loop. Moving the offset in the
        // opposite direction of belt travel makes texture details move with the leather.
        double textureDistance = -blockEntity.getVisualBeltTravelDistance(partialTicks);

        // Upper run: start pulley -> end pulley.
        textureDistance = drawTiledBeltSegment(
                consumer,
                pose,
                startTop,
                endTop,
                axis,
                topRadial,
                packedLight,
                textureDistance
        );

        // Wrap outside of end pulley: top -> bottom.
        textureDistance = drawPulleyArc(
                consumer,
                pose,
                endCenter,
                topRadial,
                bottomRadial,
                runDirection,
                runDirection,
                sideDirection,
                axis,
                endRadius,
                packedLight,
                textureDistance
        );

        // Return run: end pulley -> start pulley.
        textureDistance = drawTiledBeltSegment(
                consumer,
                pose,
                endBottom,
                startBottom,
                axis,
                bottomRadial,
                packedLight,
                textureDistance
        );

        // Wrap outside of start pulley: bottom -> top.
        drawPulleyArc(
                consumer,
                pose,
                startCenter,
                bottomRadial,
                topRadial,
                runDirection.scale(-1.0D),
                runDirection,
                sideDirection,
                axis,
                startRadius,
                packedLight,
                textureDistance
        );
    }

    private Vec3 axisVector(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Vec3(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3(0.0D, 0.0D, 1.0D);
        };
    }

    private double drawPulleyArc(VertexConsumer consumer,
                                 PoseStack.Pose pose,
                                 Vec3 center,
                                 Vec3 startRadial,
                                 Vec3 endRadial,
                                 Vec3 wantedOuterDirection,
                                 Vec3 runBasis,
                                 Vec3 sideBasis,
                                 Vec3 axis,
                                 double radius,
                                 int packedLight,
                                 double textureDistance) {
        double startAngle = Math.atan2(startRadial.dot(sideBasis), startRadial.dot(runBasis));
        double endAngle = Math.atan2(endRadial.dot(sideBasis), endRadial.dot(runBasis));

        double shortDelta = wrapRadians(endAngle - startAngle);
        double longDelta = shortDelta >= 0.0D
                ? shortDelta - Math.PI * 2.0D
                : shortDelta + Math.PI * 2.0D;

        double shortScore = radialAt(
                startAngle + shortDelta * 0.5D,
                runBasis,
                sideBasis
        ).dot(wantedOuterDirection);

        double longScore = radialAt(
                startAngle + longDelta * 0.5D,
                runBasis,
                sideBasis
        ).dot(wantedOuterDirection);

        double delta = longScore > shortScore ? longDelta : shortDelta;
        int segments = Math.max(
                4,
                (int) Math.ceil(
                        ARC_SEGMENTS_PER_HALF_TURN * Math.abs(delta) / Math.PI
                )
        );

        Vec3 previousRadial = startRadial;
        Vec3 previousPoint = center.add(previousRadial.scale(radius));

        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 radial = radialAt(
                    startAngle + delta * t,
                    runBasis,
                    sideBasis
            );
            Vec3 nextPoint = center.add(radial.scale(radius));

            Vec3 thicknessDirection = previousRadial.add(radial);
            if (thicknessDirection.lengthSqr() < 0.0001D) {
                thicknessDirection = radial;
            } else {
                thicknessDirection = thicknessDirection.normalize();
            }

            textureDistance = drawTiledBeltSegment(
                    consumer,
                    pose,
                    previousPoint,
                    nextPoint,
                    axis,
                    thicknessDirection,
                    packedLight,
                    textureDistance
            );

            previousRadial = radial;
            previousPoint = nextPoint;
        }

        return textureDistance;
    }

    private Vec3 radialAt(double angle, Vec3 runBasis, Vec3 sideBasis) {
        return runBasis.scale(Math.cos(angle))
                .add(sideBasis.scale(Math.sin(angle)))
                .normalize();
    }

    private double wrapRadians(double radians) {
        double wrapped = radians % (Math.PI * 2.0D);
        if (wrapped > Math.PI) {
            wrapped -= Math.PI * 2.0D;
        } else if (wrapped < -Math.PI) {
            wrapped += Math.PI * 2.0D;
        }
        return wrapped;
    }

    /**
     * Draw a belt path using fixed physical UV scale. The texture restarts only at an
     * exact repeat boundary, so a six-block belt receives twelve half-block repeats
     * rather than one texture stretched across all six blocks.
     */
    private double drawTiledBeltSegment(VertexConsumer consumer,
                                        PoseStack.Pose pose,
                                        Vec3 from,
                                        Vec3 to,
                                        Vec3 widthDirection,
                                        Vec3 thicknessDirection,
                                        int packedLight,
                                        double textureDistance) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.00001D) {
            return textureDistance;
        }

        Vec3 direction = delta.scale(1.0D / length);
        double cursor = 0.0D;

        while (cursor < length - 0.000001D) {
            double phase = positiveModulo(textureDistance, TEXTURE_REPEAT_LENGTH);
            double untilRepeat = TEXTURE_REPEAT_LENGTH - phase;

            if (untilRepeat < 0.000001D) {
                untilRepeat = TEXTURE_REPEAT_LENGTH;
            }

            double step = Math.min(length - cursor, untilRepeat);
            Vec3 segmentStart = from.add(direction.scale(cursor));
            Vec3 segmentEnd = from.add(direction.scale(cursor + step));

            float u0 = (float) (phase / TEXTURE_REPEAT_LENGTH);
            float u1 = (float) ((phase + step) / TEXTURE_REPEAT_LENGTH);

            drawBeltSegment(
                    consumer,
                    pose,
                    segmentStart,
                    segmentEnd,
                    widthDirection,
                    thicknessDirection,
                    packedLight,
                    u0,
                    u1
            );

            cursor += step;
            textureDistance += step;
        }

        return textureDistance;
    }

    private double positiveModulo(double value, double divisor) {
        double result = value % divisor;
        return result < 0.0D ? result + divisor : result;
    }

    /**
     * Four-sided open belt prism. There are deliberately no end caps: adjacent straight
     * and arc pieces meet exactly at their shared edge, eliminating the coplanar faces
     * that caused the previous z-fighting.
     */
    private void drawBeltSegment(VertexConsumer consumer,
                                 PoseStack.Pose pose,
                                 Vec3 from,
                                 Vec3 to,
                                 Vec3 widthDirection,
                                 Vec3 thicknessDirection,
                                 int packedLight,
                                 float u0,
                                 float u1) {
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

        drawQuad(
                consumer, pose,
                a3, a2, b2, b3,
                thicknessDirection,
                u0, 0.0F,
                u0, 1.0F,
                u1, 1.0F,
                u1, 0.0F,
                packedLight
        );

        drawQuad(
                consumer, pose,
                a0, b0, b1, a1,
                thicknessDirection.scale(-1.0D),
                u0, 0.0F,
                u1, 0.0F,
                u1, 1.0F,
                u0, 1.0F,
                packedLight
        );

        drawQuad(
                consumer, pose,
                a1, b1, b2, a2,
                widthDirection,
                u0, 0.0F,
                u1, 0.0F,
                u1, 1.0F,
                u0, 1.0F,
                packedLight
        );

        drawQuad(
                consumer, pose,
                a0, a3, b3, b0,
                widthDirection.scale(-1.0D),
                u0, 0.0F,
                u0, 1.0F,
                u1, 1.0F,
                u1, 0.0F,
                packedLight
        );
    }

    private void drawQuad(VertexConsumer consumer,
                          PoseStack.Pose pose,
                          Vec3 vertex0,
                          Vec3 vertex1,
                          Vec3 vertex2,
                          Vec3 vertex3,
                          Vec3 normal,
                          float texU0,
                          float texV0,
                          float texU1,
                          float texV1,
                          float texU2,
                          float texV2,
                          float texU3,
                          float texV3,
                          int packedLight) {
        Vec3 n = normal.lengthSqr() < 0.0001D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : normal.normalize();

        beltVertex(consumer, pose, vertex0, n, texU0, texV0, packedLight);
        beltVertex(consumer, pose, vertex1, n, texU1, texV1, packedLight);
        beltVertex(consumer, pose, vertex2, n, texU2, texV2, packedLight);
        beltVertex(consumer, pose, vertex3, n, texU3, texV3, packedLight);
    }

    private void beltVertex(VertexConsumer consumer,
                            PoseStack.Pose pose,
                            Vec3 position,
                            Vec3 normal,
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
                .normal(
                        pose.normal(),
                        (float) normal.x,
                        (float) normal.y,
                        (float) normal.z
                )
                .endVertex();
    }
}
