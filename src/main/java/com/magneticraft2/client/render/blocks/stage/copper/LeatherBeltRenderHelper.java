package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.systems.GEAR.BeltPath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class LeatherBeltRenderHelper {
    private static final ResourceLocation BELT_TEXTURE =
            new ResourceLocation("magneticraft2", "textures/block/leather_belt.png");
    private static final double TEXTURE_REPEAT_LENGTH = 0.50D;

    private LeatherBeltRenderHelper() {
    }

    public static void renderSegments(
            List<BeltPath.Segment> segments,
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight,
            Vec3 renderOrigin,
            double textureOffset) {
        if (segments == null || segments.isEmpty()) {
            return;
        }

        VertexConsumer consumer =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(BELT_TEXTURE));
        PoseStack.Pose pose = stack.last();

        for (BeltPath.Segment segment : segments) {
            drawTiledBeltSegment(
                    consumer, pose, segment, renderOrigin,
                    packedLight, textureOffset + segment.startDistance()
            );
        }
    }

    private static void drawTiledBeltSegment(
            VertexConsumer consumer,
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
            double phase = positiveModulo(
                    textureDistance + cursor,
                    TEXTURE_REPEAT_LENGTH
            );
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
                    consumer, pose, segmentStart, segmentEnd,
                    segment.widthDirection(), segment.thicknessDirection(),
                    packedLight, u0, u1
            );
            cursor += step;
        }
    }

    private static void drawOpenBeltPrism(
            VertexConsumer consumer,
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

        drawQuad(consumer, pose, a3, a2, b2, b3, thicknessDirection,
                u0, 0.0F, u0, 1.0F, u1, 1.0F, u1, 0.0F, packedLight);
        drawQuad(consumer, pose, a0, b0, b1, a1, thicknessDirection.scale(-1.0D),
                u0, 0.0F, u1, 0.0F, u1, 1.0F, u0, 1.0F, packedLight);
        drawQuad(consumer, pose, a1, b1, b2, a2, widthDirection,
                u0, 0.0F, u1, 0.0F, u1, 1.0F, u0, 1.0F, packedLight);
        drawQuad(consumer, pose, a0, a3, b3, b0, widthDirection.scale(-1.0D),
                u0, 0.0F, u0, 1.0F, u1, 1.0F, u1, 0.0F, packedLight);
    }

    private static void drawQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 v0, Vec3 v1, Vec3 v2, Vec3 v3,
            Vec3 normal,
            float u0, float vv0,
            float u1, float vv1,
            float u2, float vv2,
            float u3, float vv3,
            int packedLight) {
        Vec3 n = normal.lengthSqr() < 0.0001D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : normal.normalize();

        beltVertex(consumer, pose, v0, n, u0, vv0, packedLight);
        beltVertex(consumer, pose, v1, n, u1, vv1, packedLight);
        beltVertex(consumer, pose, v2, n, u2, vv2, packedLight);
        beltVertex(consumer, pose, v3, n, u3, vv3, packedLight);
    }

    private static void beltVertex(
            VertexConsumer consumer,
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

    private static double positiveModulo(double value, double divisor) {
        double result = value % divisor;
        return result < 0.0D ? result + divisor : result;
    }
}
