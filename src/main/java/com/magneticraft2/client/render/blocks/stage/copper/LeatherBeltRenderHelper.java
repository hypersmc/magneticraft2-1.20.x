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

    private static final double WEAR_SAMPLE_LENGTH = 0.125D;
    // A nick is not repeated at every texture tile. Keep damage spaced
    // along a continuous 2-block leather loop, including moving UV offsets.
    private static final double WEAR_PATTERN_LENGTH = 2.0D;

    /**
     * Default path remains completely unchanged for all existing external
     * leather-belt networks. Internal machine components opt into wear.
     */
    public static void renderSegments(
            List<BeltPath.Segment> segments,
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight,
            Vec3 renderOrigin,
            double textureOffset) {
        renderSegments(segments, stack, bufferSource, packedLight,
                renderOrigin, textureOffset, 0);
    }

    /**
     * 0 = pristine, 1..4 = increasingly abraded and frayed, 5 = snapped.
     * The texture always uses the original physical 0.5-block repeat.
     * Shorter edges crop their UVs, never stretch or compress the leather.
     */
    public static void renderSegments(
            List<BeltPath.Segment> segments,
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight,
            Vec3 renderOrigin,
            double textureOffset,
            int wearStage) {
        if (segments == null || segments.isEmpty()) {
            return;
        }

        int stage = Math.max(0, Math.min(5, wearStage));
        VertexConsumer consumer =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(BELT_TEXTURE));
        PoseStack.Pose pose = stack.last();

        for (BeltPath.Segment segment : segments) {
            if (stage == 0) {
                drawTiledBeltSegment(
                        consumer, pose, segment, renderOrigin,
                        packedLight, textureOffset + segment.startDistance());
            } else {
                drawWornBeltSegment(
                        consumer, pose, segment, renderOrigin,
                        packedLight, textureOffset + segment.startDistance(), stage);
            }
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

    /**
     * Wear travels with the belt rather than sliding across a fixed machine.
     * 1/8-block-long pieces provide worn patches, clipped/ragged edges and
     * real gaps at high damage levels. There is no second floating item.
     */
    private static void drawWornBeltSegment(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BeltPath.Segment segment,
            Vec3 renderOrigin,
            int packedLight,
            double textureDistance,
            int stage) {
        Vec3 delta = segment.to().subtract(segment.from());
        double length = delta.length();
        if (length < 0.00001D) {
            return;
        }
        Vec3 direction = delta.scale(1.0D / length);
        double cursor = 0.0D;

        while (cursor < length - 0.000001D) {
            double along = textureDistance + cursor;
            double samplePhase = positiveModulo(along, WEAR_SAMPLE_LENGTH);
            double step = Math.min(length - cursor,
                    WEAR_SAMPLE_LENGTH - samplePhase);
            if (step < 0.000001D) {
                step = Math.min(length - cursor, WEAR_SAMPLE_LENGTH);
            }

            double tilePhase = positiveModulo(along, TEXTURE_REPEAT_LENGTH);
            int section = Math.min(15, (int) Math.floor(
                    positiveModulo(along, WEAR_PATTERN_LENGTH)
                            / WEAR_SAMPLE_LENGTH + 0.0000001D));

            // Severed sections are physically absent, not transparent faces
            // over an intact leather strip.
            boolean snapped = (stage == 4 && (section == 5 || section == 13))
                    || (stage == 5 && (section == 3 || section == 4
                    || section == 9 || section == 10 || section == 13));
            if (!snapped) {
                double left = 1.0D;
                double right = 1.0D;
                if (stage >= 1 && (section == 4 || section == 12)) {
                    left = 0.92D;
                }
                if (stage >= 2 && (section == 7 || section == 14)) {
                    right = 0.68D;
                }
                if (stage >= 3 && (section == 2 || section == 11)) {
                    left = 0.43D;
                }
                if (stage >= 3 && (section == 5 || section == 13)) {
                    right = 0.52D;
                }
                if (stage >= 4 && (section == 6 || section == 14)) {
                    left = 0.30D;
                    right = 0.62D;
                }
                if (stage >= 5 && (section == 2 || section == 5
                        || section == 8 || section == 11)) {
                    left = 0.31D;
                    right = 0.43D;
                }

                // Vertex tint adds weathering and darkened abrasion without
                // requiring texture changes to the existing leather belt.
                float faded = switch (stage) {
                    case 1 -> 0.91F;
                    case 2 -> 0.83F;
                    case 3 -> 0.74F;
                    case 4 -> 0.65F;
                    default -> 0.55F;
                };
                float abrasion = (section == 1 || section == 6
                        || section == 11 || section == 15) ? 0.77F : 1.0F;
                float red = faded * abrasion;
                float green = red * (stage >= 3 ? 0.82F : 0.94F);
                float blue = red * (stage >= 3 ? 0.69F : 0.88F);

                Vec3 a = segment.from().add(direction.scale(cursor))
                        .subtract(renderOrigin);
                Vec3 b = segment.from().add(direction.scale(cursor + step))
                        .subtract(renderOrigin);

                drawWornBeltPrism(
                        consumer, pose, a, b,
                        segment.widthDirection(), segment.thicknessDirection(),
                        packedLight,
                        (float) (tilePhase / TEXTURE_REPEAT_LENGTH),
                        (float) ((tilePhase + step) / TEXTURE_REPEAT_LENGTH),
                        left, right, red, green, blue);
            }
            cursor += step;
        }
    }

    private static void drawWornBeltPrism(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 from,
            Vec3 to,
            Vec3 widthDirection,
            Vec3 thicknessDirection,
            int packedLight,
            float u0, float u1,
            double leftFraction,
            double rightFraction,
            float red, float green, float blue) {
        Vec3 width = widthDirection.normalize()
                .scale(BeltPath.RENDER_HALF_WIDTH);
        Vec3 thickness = thicknessDirection.normalize()
                .scale(BeltPath.RENDER_HALF_THICKNESS);
        Vec3 leftWidth = width.scale(leftFraction);
        Vec3 rightWidth = width.scale(rightFraction);

        Vec3 a0 = from.subtract(leftWidth).subtract(thickness);
        Vec3 a1 = from.add(rightWidth).subtract(thickness);
        Vec3 a2 = from.add(rightWidth).add(thickness);
        Vec3 a3 = from.subtract(leftWidth).add(thickness);
        Vec3 b0 = to.subtract(leftWidth).subtract(thickness);
        Vec3 b1 = to.add(rightWidth).subtract(thickness);
        Vec3 b2 = to.add(rightWidth).add(thickness);
        Vec3 b3 = to.subtract(leftWidth).add(thickness);

        // Cropped V coordinates keep texture pixels the same physical size
        // after a torn edge shortens the belt surface.
        float vLeft = (float) ((1.0D - leftFraction) * 0.5D);
        float vRight = (float) ((1.0D + rightFraction) * 0.5D);

        drawWornQuad(consumer, pose, a3, a2, b2, b3, thicknessDirection,
                u0, vLeft, u0, vRight, u1, vRight, u1, vLeft,
                packedLight, red, green, blue);
        drawWornQuad(consumer, pose, a0, b0, b1, a1, thicknessDirection.scale(-1),
                u0, vLeft, u1, vLeft, u1, vRight, u0, vRight,
                packedLight, red, green, blue);
        drawWornQuad(consumer, pose, a1, b1, b2, a2, widthDirection,
                u0, 0.0F, u1, 0.0F, u1, 1.0F, u0, 1.0F,
                packedLight, red, green, blue);
        drawWornQuad(consumer, pose, a0, a3, b3, b0, widthDirection.scale(-1),
                u0, 0.0F, u0, 1.0F, u1, 1.0F, u1, 0.0F,
                packedLight, red, green, blue);
    }

    private static void drawWornQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 a, Vec3 b, Vec3 c, Vec3 d,
            Vec3 normal,
            float u0, float v0, float u1, float v1,
            float u2, float v2, float u3, float v3,
            int light, float red, float green, float blue) {
        Vec3 n = normal.lengthSqr() < 0.0001D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : normal.normalize();
        drawWornVertex(consumer, pose, a, n, u0, v0, light, red, green, blue);
        drawWornVertex(consumer, pose, b, n, u1, v1, light, red, green, blue);
        drawWornVertex(consumer, pose, c, n, u2, v2, light, red, green, blue);
        drawWornVertex(consumer, pose, d, n, u3, v3, light, red, green, blue);
    }

    private static void drawWornVertex(
            VertexConsumer consumer, PoseStack.Pose pose,
            Vec3 position, Vec3 normal, float u, float v, int packedLight,
            float red, float green, float blue) {
        consumer.vertex(pose.pose(),
                (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(),
                        (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
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
