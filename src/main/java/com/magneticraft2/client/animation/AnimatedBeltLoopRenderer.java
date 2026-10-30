package com.magneticraft2.client.animation;

import com.magneticraft2.common.systems.GEAR.BeltPath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/**
 * Client-only UV-tiled belt-loop renderer shared by machine animations.
 *
 * The immutable BeltPath is constructed once during JSON resource reload.
 * Every visible tile is clipped at its precise physical texture-repeat
 * boundary, so long loops never stretch a 16x16 image over their entire run.
 * Reverse drive changes the SIGNED surface displacement, not the geometry.
 *
 * Does not own any gameplay belt, collision entity or Gear V2 state.
 */
public final class AnimatedBeltLoopRenderer {
    private static final double EPS = 0.000001D;

    private AnimatedBeltLoopRenderer() {}

    /**
     * Convert a continuous (unwrapped) Gear V2 shaft angle into signed
     * surface distance, applying the authored driving pulley radius.
     */
    public static double surfaceDistance(AnimationDefinition.BeltLoop belt,
                                         double cumulativeDegrees) {
        return Math.toRadians(cumulativeDegrees) * belt.driveRadius()
                * belt.speedScale() + belt.textureOffset();
    }

    public static void render(AnimationDefinition.BeltLoop belt,
                              double cumulativeDegrees,
                              PoseStack poseStack, MultiBufferSource buffers,
                              int packedLight) {
        if (!Double.isFinite(cumulativeDegrees) || belt.segments().isEmpty()) {
            return;
        }
        // Periodic reduction BEFORE generating float UVs. Avoid precision
        // loss after long sessions, while preserving sign under reversals.
        double textureDistance = positiveModulo(
                surfaceDistance(belt, cumulativeDegrees), belt.textureRepeat());
        VertexConsumer consumer =
                buffers.getBuffer(RenderType.entityCutoutNoCull(belt.texture()));
        PoseStack.Pose pose = poseStack.last();
        double repeat = belt.textureRepeat();

        for (BeltPath.Segment segment : belt.segments()) {
            Vec3 delta = segment.to().subtract(segment.from());
            double length = delta.length();
            if (length < EPS) continue;

            Vec3 tangent = delta.scale(1.0D / length);
            double cursor = 0.0D;
            while (cursor < length - EPS) {
                double phase = positiveModulo(
                        textureDistance + segment.startDistance() + cursor,
                        repeat);
                // A value 0.999999999... is the start of the NEXT repeat,
                // not permission to emit a quad whose UVs run from 1 to 2.
                // Normalize the tiny rounding tail to the seam at zero.
                if (repeat - phase < EPS) phase = 0.0D;
                double step = Math.min(length - cursor, repeat - phase);
                Vec3 from = segment.from().add(tangent.scale(cursor));
                Vec3 to = segment.from().add(tangent.scale(cursor + step));
                renderSection(consumer, pose, from, to,
                        segment.widthDirection(), segment.thicknessDirection(),
                        belt.halfWidth(), belt.halfThickness(), repeat,
                        (float) (phase / repeat),
                        (float) ((phase + step) / repeat),
                        packedLight);
                cursor += step;
            }
        }
    }

    private static void renderSection(VertexConsumer consumer, PoseStack.Pose pose,
                                      Vec3 from, Vec3 to, Vec3 axis, Vec3 radial,
                                      double halfWidth, double halfThickness,
                                      double repeat, float u0, float u1,
                                      int light) {
        Vec3 w = axis.normalize().scale(halfWidth);
        Vec3 t = radial.normalize().scale(halfThickness);
        Vec3 a0 = from.subtract(w).subtract(t);
        Vec3 a1 = from.add(w).subtract(t);
        Vec3 a2 = from.add(w).add(t);
        Vec3 a3 = from.subtract(w).add(t);
        Vec3 b0 = to.subtract(w).subtract(t);
        Vec3 b1 = to.add(w).subtract(t);
        Vec3 b2 = to.add(w).add(t);
        Vec3 b3 = to.subtract(w).add(t);

        // Match physical texel density on BOTH along-belt and cross-belt
        // faces. A pack may change repeat length without compressing width.
        float vWidth0 = 0.5F - (float) (halfWidth / repeat);
        float vWidth1 = 0.5F + (float) (halfWidth / repeat);
        float vThick0 = 0.5F - (float) (halfThickness / repeat);
        float vThick1 = 0.5F + (float) (halfThickness / repeat);

        quad(consumer, pose, a3, a2, b2, b3, radial,
                u0, vWidth0, u0, vWidth1, u1, vWidth1, u1, vWidth0, light);
        quad(consumer, pose, a0, b0, b1, a1, radial.scale(-1.0D),
                u0, vWidth0, u1, vWidth0, u1, vWidth1, u0, vWidth1, light);
        quad(consumer, pose, a1, b1, b2, a2, axis,
                u0, vThick0, u1, vThick0, u1, vThick1, u0, vThick1, light);
        quad(consumer, pose, a0, a3, b3, b0, axis.scale(-1.0D),
                u0, vThick0, u0, vThick1, u1, vThick1, u1, vThick0, light);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
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

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               Vec3 at, Vec3 normal,
                               float u, float v, int light) {
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

    private static double positiveModulo(double value, double divisor) {
        double result = value % divisor;
        return result < 0D ? result + divisor : result;
    }
}
