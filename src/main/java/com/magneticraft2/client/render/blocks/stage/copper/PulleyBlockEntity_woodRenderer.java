package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.PulleyBlockEntity_wood;
import com.magneticraft2.common.systems.GEAR.BeltConnectionManager;
import com.magneticraft2.common.systems.GEAR.BeltPath;
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
 * All geometry comes from BeltPath, which is also used by physical collision and will be
 * used by item transport. Rendering is therefore only responsible for drawing the path,
 * not deciding where the belt exists.
 */
public class PulleyBlockEntity_woodRenderer implements BlockEntityRenderer<PulleyBlockEntity_wood> {
    private static final ResourceLocation BELT_TEXTURE =
            new ResourceLocation("magneticraft2", "textures/block/leather_belt.png");

    private static final double TEXTURE_REPEAT_LENGTH = 0.50D;

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

        BeltConnectionManager.ensureRegistered(blockEntity);
        BeltPath path = BeltConnectionManager.getPath(
                blockEntity.getLevel(),
                blockEntity.getBlockPos(),
                partnerPos
        );

        if (path == null || !path.startPulley().equals(blockEntity.getBlockPos())) {
            return;
        }

        VertexConsumer consumer =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(BELT_TEXTURE));
        PoseStack.Pose pose = stack.last();

        Vec3 renderOrigin = new Vec3(
                blockEntity.getBlockPos().getX(),
                blockEntity.getBlockPos().getY(),
                blockEntity.getBlockPos().getZ()
        );

        double textureOffset = -blockEntity.getVisualBeltTravelDistance(partialTicks);

        for (BeltPath.Segment segment : path.segments()) {
            drawTiledBeltSegment(
                    consumer,
                    pose,
                    segment,
                    renderOrigin,
                    packedLight,
                    textureOffset + segment.startDistance()
            );
        }
    }

    /**
     * Tile the custom leather texture by physical path length. A long belt repeats the
     * texture instead of stretching one 16x16 image over the entire span.
     */
    private void drawTiledBeltSegment(VertexConsumer consumer,
                                      PoseStack.Pose pose,
                                      BeltPath.Segment segment,
                                      Vec3 renderOrigin,
                                      int packedLight,
                                      double textureDistance) {
        Vec3 worldDelta = segment.to().subtract(segment.from());
        double length = worldDelta.length();
        if (length < 0.00001D) {
            return;
        }

        Vec3 direction = worldDelta.scale(1.0D / length);
        double cursor = 0.0D;

        while (cursor < length - 0.000001D) {
            double phase = positiveModulo(textureDistance + cursor, TEXTURE_REPEAT_LENGTH);
            double untilRepeat = TEXTURE_REPEAT_LENGTH - phase;
            if (untilRepeat < 0.000001D) {
                untilRepeat = TEXTURE_REPEAT_LENGTH;
            }

            double step = Math.min(length - cursor, untilRepeat);

            Vec3 segmentStart = segment.from()
                    .add(direction.scale(cursor))
                    .subtract(renderOrigin);
            Vec3 segmentEnd = segment.from()
                    .add(direction.scale(cursor + step))
                    .subtract(renderOrigin);

            float u0 = (float) (phase / TEXTURE_REPEAT_LENGTH);
            float u1 = (float) ((phase + step) / TEXTURE_REPEAT_LENGTH);

            drawOpenBeltPrism(
                    consumer,
                    pose,
                    segmentStart,
                    segmentEnd,
                    segment.widthDirection(),
                    segment.thicknessDirection(),
                    packedLight,
                    u0,
                    u1
            );

            cursor += step;
        }
    }

    /**
     * Four-sided open prism: no end caps. Adjacent BeltPath segments share their edge
     * instead of drawing coplanar faces on top of each other.
     */
    private void drawOpenBeltPrism(VertexConsumer consumer,
                                   PoseStack.Pose pose,
                                   Vec3 from,
                                   Vec3 to,
                                   Vec3 widthDirection,
                                   Vec3 thicknessDirection,
                                   int packedLight,
                                   float u0,
                                   float u1) {
        Vec3 width = widthDirection.normalize().scale(BeltPath.RENDER_HALF_WIDTH);
        Vec3 thickness = thicknessDirection.normalize().scale(BeltPath.RENDER_HALF_THICKNESS);

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

    private double positiveModulo(double value, double divisor) {
        double result = value % divisor;
        return result < 0.0D ? result + divisor : result;
    }
}
