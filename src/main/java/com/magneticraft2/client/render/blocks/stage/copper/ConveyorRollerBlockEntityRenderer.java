package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.ConveyorRollerBlockEntity;
import com.magneticraft2.common.systems.GEAR.BeltPath;
import com.magneticraft2.common.systems.GEAR.ItemBeltConnectionManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Renders one continuous wide item belt between two Conveyor Rollers.
 *
 * Only the canonical start roller renders the span, so the connection is never drawn twice.
 */
public class ConveyorRollerBlockEntityRenderer implements BlockEntityRenderer<ConveyorRollerBlockEntity> {
    private static final ResourceLocation BELT_TEXTURE =
            new ResourceLocation("magneticraft2", "textures/block/leather_belt.png");

    private static final double TEXTURE_REPEAT_LENGTH = 1.0D;

    public ConveyorRollerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ConveyorRollerBlockEntity roller,
                       float partialTicks,
                       PoseStack stack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        BlockPos partnerPos = roller.getItemBeltPartner();
        if (partnerPos == null || roller.getLevel() == null) {
            return;
        }

        ItemBeltConnectionManager.ensureRegistered(roller);
        BeltPath path = ItemBeltConnectionManager.getPath(
                roller.getLevel(),
                roller.getBlockPos(),
                partnerPos
        );

        if (path == null || !path.startPulley().equals(roller.getBlockPos())) {
            return;
        }

        VertexConsumer consumer =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(BELT_TEXTURE));
        PoseStack.Pose pose = stack.last();

        Vec3 renderOrigin = new Vec3(
                roller.getBlockPos().getX(),
                roller.getBlockPos().getY(),
                roller.getBlockPos().getZ()
        );

        double textureOffset = roller.getVisualItemBeltTravelDistance(partialTicks);

        for (BeltPath.Segment segment : path.segments()) {
            drawTiledSegment(
                    consumer,
                    pose,
                    segment,
                    renderOrigin,
                    packedLight,
                    textureOffset + segment.startDistance()
            );
        }

        renderTransportedItems(
                roller,
                partnerPos,
                partialTicks,
                stack,
                bufferSource,
                packedLight,
                renderOrigin
        );
    }

    private void renderTransportedItems(ConveyorRollerBlockEntity roller,
                                        BlockPos partnerPos,
                                        float partialTicks,
                                        PoseStack stack,
                                        MultiBufferSource bufferSource,
                                        int packedLight,
                                        Vec3 renderOrigin) {
        if (roller.getTransportedItems().isEmpty() || roller.getLevel() == null) {
            return;
        }

        int seed = 0;
        for (ConveyorRollerBlockEntity.TransportedItem transportedItem
                : roller.getTransportedItems()) {
            if (transportedItem.getStack().isEmpty()) {
                continue;
            }

            double distance = roller.getClientTransportDistance(
                    transportedItem,
                    partialTicks
            );

            ItemBeltConnectionManager.CarryingSample sample =
                    ItemBeltConnectionManager.sampleCarryingSurface(
                            roller.getLevel(),
                            roller.getBlockPos(),
                            partnerPos,
                            distance
                    );
            if (sample == null) {
                continue;
            }

            Vec3 surfacePosition = sample.position()
                    .add(sample.surfaceNormal().scale(
                            ItemBeltConnectionManager.BELT_HALF_THICKNESS + 0.025D
                    ))
                    .subtract(renderOrigin);

            Vec3 tangent = sample.tangent();
            double horizontalLength = Math.sqrt(
                    tangent.x * tangent.x + tangent.z * tangent.z
            );

            float yawDegrees = horizontalLength < 0.0001D
                    ? 0.0F
                    : (float) Math.toDegrees(Math.atan2(tangent.x, tangent.z));
            float pitchDegrees = (float) -Math.toDegrees(
                    Math.atan2(tangent.y, horizontalLength)
            );

            stack.pushPose();
            stack.translate(
                    surfacePosition.x,
                    surfacePosition.y,
                    surfacePosition.z
            );

            // Local Z follows belt travel and local Y follows the carrying surface
            // normal. ItemDisplayContext.GROUND then places the model on that plane
            // without the bob/spin behavior of a dropped ItemEntity.
            stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yawDegrees));
            stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitchDegrees));
            stack.scale(0.55F, 0.55F, 0.55F);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    transportedItem.getStack(),
                    ItemDisplayContext.GROUND,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    stack,
                    bufferSource,
                    roller.getLevel(),
                    seed++
            );

            stack.popPose();
        }
    }

    private void drawTiledSegment(VertexConsumer consumer,
                                  PoseStack.Pose pose,
                                  BeltPath.Segment segment,
                                  Vec3 renderOrigin,
                                  int packedLight,
                                  double textureDistance) {
        Vec3 delta = segment.to().subtract(segment.from());
        double length = delta.length();
        if (length < 0.00001D) {
            return;
        }

        Vec3 direction = delta.scale(1.0D / length);
        double cursor = 0.0D;

        while (cursor < length - 0.000001D) {
            double phase = positiveModulo(textureDistance + cursor, TEXTURE_REPEAT_LENGTH);
            double untilRepeat = TEXTURE_REPEAT_LENGTH - phase;
            if (untilRepeat < 0.000001D) {
                untilRepeat = TEXTURE_REPEAT_LENGTH;
            }

            double step = Math.min(length - cursor, untilRepeat);

            Vec3 from = segment.from()
                    .add(direction.scale(cursor))
                    .subtract(renderOrigin);
            Vec3 to = segment.from()
                    .add(direction.scale(cursor + step))
                    .subtract(renderOrigin);

            float u0 = (float) (phase / TEXTURE_REPEAT_LENGTH);
            float u1 = (float) ((phase + step) / TEXTURE_REPEAT_LENGTH);

            drawOpenPrism(
                    consumer,
                    pose,
                    from,
                    to,
                    segment.widthDirection(),
                    segment.thicknessDirection(),
                    packedLight,
                    u0,
                    u1
            );

            cursor += step;
        }
    }

    private void drawOpenPrism(VertexConsumer consumer,
                               PoseStack.Pose pose,
                               Vec3 from,
                               Vec3 to,
                               Vec3 widthDirection,
                               Vec3 thicknessDirection,
                               int packedLight,
                               float u0,
                               float u1) {
        Vec3 width = widthDirection.normalize()
                .scale(ItemBeltConnectionManager.BELT_HALF_WIDTH);
        Vec3 thickness = thicknessDirection.normalize()
                .scale(ItemBeltConnectionManager.BELT_HALF_THICKNESS);

        Vec3 a0 = from.subtract(width).subtract(thickness);
        Vec3 a1 = from.add(width).subtract(thickness);
        Vec3 a2 = from.add(width).add(thickness);
        Vec3 a3 = from.subtract(width).add(thickness);

        Vec3 b0 = to.subtract(width).subtract(thickness);
        Vec3 b1 = to.add(width).subtract(thickness);
        Vec3 b2 = to.add(width).add(thickness);
        Vec3 b3 = to.subtract(width).add(thickness);

        drawQuad(consumer, pose, a3, a2, b2, b3, thicknessDirection,
                u0, 0.0F, u0, 1.0F, u1, 1.0F, u1, 0.0F, packedLight);
        drawQuad(consumer, pose, a0, b0, b1, a1, thicknessDirection.scale(-1.0D),
                u0, 0.0F, u1, 0.0F, u1, 1.0F, u0, 1.0F, packedLight);
        drawQuad(consumer, pose, a1, b1, b2, a2, widthDirection,
                u0, 0.0F, u1, 0.0F, u1, 1.0F, u0, 1.0F, packedLight);
        drawQuad(consumer, pose, a0, a3, b3, b0, widthDirection.scale(-1.0D),
                u0, 0.0F, u0, 1.0F, u1, 1.0F, u1, 0.0F, packedLight);
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

        vertex(consumer, pose, vertex0, n, texU0, texV0, packedLight);
        vertex(consumer, pose, vertex1, n, texU1, texV1, packedLight);
        vertex(consumer, pose, vertex2, n, texU2, texV2, packedLight);
        vertex(consumer, pose, vertex3, n, texU3, texV3, packedLight);
    }

    private void vertex(VertexConsumer consumer,
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
