package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.systems.GEAR.BeltPath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * One continuous, visually moving leather loop around the Stamp Mill's two
 * bronze-rimmed wooden drums. It is a rendering detail, not another Gear V2
 * node or item transport system.
 *
 * Uses the same BeltPath segments and texture-boundary tiling technique as
 * LeatherBeltRenderHelper, without changing the shared external belt and Ore
 * Washer renderer. Coordinates are controller-relative, BEFORE the machine
 * facing transform that BronzeStampMillRenderer already applies.
 */
final class BronzeStampMillBeltRenderer {
    private static final ResourceLocation LEATHER =
            new ResourceLocation("magneticraft2",
                    "textures/block/bronze_stamp_mill_drive_belt.png");

    // All dimensions are in blocks. The authored former belt was 1.59 px
    // wide along the pulley axis and 1.37 px thick in the radial direction.
    // These bounds exactly match its radius=4.08..5.45 model-pixel envelope.
    private static final double BELT_CENTER_RADIUS = (4.08D + 5.45D) / 32D;
    private static final double BELT_HALF_WIDTH = (8.72D - 7.13D) / 32D;
    private static final double BELT_HALF_THICKNESS = (5.45D - 4.08D) / 32D;
    // Native texel density: sixteen texture pixels per sixteen world pixels.
    private static final double TEXTURE_REPEAT_LENGTH = 1.0D;
    private static final double EPSILON = 0.000001D;

    private static final List<BeltPath.Segment> BELT_LOOP =
            BeltPath.createVisualSegments(
                    new Vec3(47.2D / 16D, 8D / 16D, -10D / 16D),
                    new Vec3(47.2D / 16D, 38.5D / 16D, -10D / 16D),
                    Direction.Axis.X,
                    BELT_CENTER_RADIUS - BeltPath.BELT_CLEARANCE,
                    BELT_CENTER_RADIUS - BeltPath.BELT_CLEARANCE
            );

    private BronzeStampMillBeltRenderer() {}

    /** Offset for an angular delta; positive X rotation moves the +Z run down. */
    static double distanceForDegrees(double degrees) {
        return Math.toRadians(degrees) * BELT_CENTER_RADIUS;
    }

    static void render(PoseStack stack, MultiBufferSource buffers,
                       int packedLight, double textureDistance) {
        if (BELT_LOOP.isEmpty()) return;
        VertexConsumer consumer =
                buffers.getBuffer(RenderType.entityCutoutNoCull(LEATHER));
        PoseStack.Pose pose = stack.last();

        for (BeltPath.Segment segment : BELT_LOOP) {
            Vec3 delta = segment.to().subtract(segment.from());
            double length = delta.length();
            if (length < EPSILON) continue;

            Vec3 tangent = delta.scale(1.0D / length);
            double cursor = 0.0D;
            while (cursor < length - EPSILON) {
                // No GL texture-wrap assumptions: split exactly where the
                // moving pattern reaches a 16-pixel tile boundary.
                double phase = positiveModulo(
                        textureDistance + segment.startDistance() + cursor,
                        TEXTURE_REPEAT_LENGTH);
                double remainingTile = TEXTURE_REPEAT_LENGTH - phase;
                if (remainingTile < EPSILON)
                    remainingTile = TEXTURE_REPEAT_LENGTH;
                double step = Math.min(length - cursor, remainingTile);

                Vec3 from = segment.from().add(tangent.scale(cursor));
                Vec3 to = segment.from().add(tangent.scale(cursor + step));
                drawBeltSection(consumer, pose, from, to,
                        segment.widthDirection(), segment.thicknessDirection(),
                        (float) (phase / TEXTURE_REPEAT_LENGTH),
                        (float) ((phase + step) / TEXTURE_REPEAT_LENGTH),
                        packedLight);
                cursor += step;
            }
        }
    }

    private static void drawBeltSection(
            VertexConsumer consumer, PoseStack.Pose pose,
            Vec3 from, Vec3 to, Vec3 axis, Vec3 radial,
            float u0, float u1, int light) {
        Vec3 width = axis.normalize().scale(BELT_HALF_WIDTH);
        Vec3 thickness = radial.normalize().scale(BELT_HALF_THICKNESS);
        Vec3 a0 = from.subtract(width).subtract(thickness);
        Vec3 a1 = from.add(width).subtract(thickness);
        Vec3 a2 = from.add(width).add(thickness);
        Vec3 a3 = from.subtract(width).add(thickness);
        Vec3 b0 = to.subtract(width).subtract(thickness);
        Vec3 b1 = to.add(width).subtract(thickness);
        Vec3 b2 = to.add(width).add(thickness);
        Vec3 b3 = to.subtract(width).add(thickness);

        // Crop UVs across the actual 1.59px and 1.37px dimensions.
        // Mapping the entire 16x16 image over such a thin strip would
        // severely stretch the leather artwork (a past model bug).
        float width0 = 0.5F - (float) BELT_HALF_WIDTH;
        float width1 = 0.5F + (float) BELT_HALF_WIDTH;
        float thick0 = 0.5F - (float) BELT_HALF_THICKNESS;
        float thick1 = 0.5F + (float) BELT_HALF_THICKNESS;

        quad(consumer, pose, a3, a2, b2, b3, radial,
                u0, width0, u0, width1, u1, width1, u1, width0, light);
        quad(consumer, pose, a0, b0, b1, a1, radial.scale(-1D),
                u0, width0, u1, width0, u1, width1, u0, width1, light);
        quad(consumer, pose, a1, b1, b2, a2, axis,
                u0, thick0, u1, thick0, u1, thick1, u0, thick1, light);
        quad(consumer, pose, a0, a3, b3, b0, axis.scale(-1D),
                u0, thick0, u0, thick1, u1, thick1, u1, thick0, light);
    }

    private static void quad(
            VertexConsumer consumer, PoseStack.Pose pose,
            Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal,
            float u0, float v0, float u1, float v1,
            float u2, float v2, float u3, float v3,
            int light) {
        Vec3 n = normal.normalize();
        vertex(consumer, pose, a, n, u0, v0, light);
        vertex(consumer, pose, b, n, u1, v1, light);
        vertex(consumer, pose, c, n, u2, v2, light);
        vertex(consumer, pose, d, n, u3, v3, light);
    }

    private static void vertex(
            VertexConsumer consumer, PoseStack.Pose pose,
            Vec3 at, Vec3 normal, float u, float v, int light) {
        consumer.vertex(pose.pose(),
                        (float) at.x, (float) at.y, (float) at.z)
                .color(1F, 1F, 1F, 1F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(),
                        (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private static double positiveModulo(double value, double modulus) {
        double result = value % modulus;
        return result < 0D ? result + modulus : result;
    }
}
