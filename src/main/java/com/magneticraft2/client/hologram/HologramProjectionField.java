package com.magneticraft2.client.hologram;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * A lightweight visual connection between the small physical ancient artifact
 * and its distant, full-size recorded scene.
 *
 * Geometry: eight extremely faint curved-envelope projection sheets, four
 * pencil-thin light strands, one advancing circular raster/sync ring and a
 * tiny animated emitter ring. These are translucent quads, not extra entities,
 * point lights, particles, fog, world blocks or ray tracing.
 *
 * The caller has already applied the projector-facing rotation but has NOT
 * translated the pose into the scene's placement offset. Everything here is
 * drawn in canonical south-facing block coordinates.
 *
 * Bounded cost: 33 quads / 132 vertices per visible projector per frame.
 * Nothing is submitted when recording projection_field is absent or faded.
 */
public final class HologramProjectionField {
    private static final ResourceLocation WHITE =
            new ResourceLocation("minecraft", "textures/block/white_concrete.png");
    private static final Vec3 EMITTER = new Vec3(0.5D, 0.272D, 0.5D);
    private static final int EMITTER_SEGMENTS = 12;
    private static final int FIELD_SEGMENTS = 8;

    private HologramProjectionField() {}

    public static void render(PoseStack pose, MultiBufferSource buffers,
                              HologramRecordingResourceLoader.Recording recording,
                              double filmTick, float sceneOpacity) {
        HologramRecordingResourceLoader.ProjectionField settings =
                recording.projectionField();
        if (settings == null || settings.intensity() <= 0F
                || recording.appearance().opacity() <= 0F) {
            return;
        }

        // Fade with the recording, including reconstruction signal dropouts.
        // The beam's own opacity is independent of the scene's material
        // opacity so increasing projector light never washes out the gears.
        float fade = Math.min(1F, sceneOpacity / recording.appearance().opacity());
        float strength = settings.intensity() * fade;
        if (strength < 0.001F) return;

        Vec3 focus = settings.focus().add(recording.offsetX(),
                recording.offsetY(), recording.offsetZ());
        if (focus.distanceToSqr(EMITTER) < 0.5D
                || focus.distanceToSqr(EMITTER) > 64D * 64D) return;

        VertexConsumer sink = buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        double t = filmTick / settings.pulseTicks();
        double pulse = t - Math.floor(t);
        double shimmer = 0.92D + 0.08D * Math.sin(filmTick * 0.09D);

        double halfWidth = settings.width() * 0.5D;
        double halfHeight = settings.height() * 0.5D;

        // A rounded, eight-sided light cone instead of a rectangular
        // frame around the machinery. It emerges through the small blue
        // aperture and spreads toward the recording's front plane.
        Vec3[] near = ellipse(EMITTER, 0.05D, 0.035D, FIELD_SEGMENTS);
        Vec3[] far = ellipse(focus, halfWidth, halfHeight, FIELD_SEGMENTS);

        // Very faint envelope sheets: this should read as light in the air,
        // not another set of glass blocks hiding the mechanism.
        for (int i = 0; i < FIELD_SEGMENTS; i++) {
            int next = (i + 1) % FIELD_SEGMENTS;
            quadGradient(pose, sink, near[i], near[next], far[next], far[i],
                    0.09F * strength, 0.024F * strength,
                    0.31F, 0.81F, 1.0F);
        }

        // Four brighter paths connect the emitter to the projection.
        // Narrow ribbons are much cheaper than long particle trails.
        double beamThickness = 0.012D;
        for (int i = 0; i < FIELD_SEGMENTS; i += 2) {
            Vec3 a = near[i];
            Vec3 b = far[i];
            quadGradient(pose, sink, a.add(-beamThickness, 0, 0),
                    a.add(beamThickness, 0, 0),
                    b.add(beamThickness, 0, 0),
                    b.add(-beamThickness, 0, 0),
                    (float) (0.52D * strength * shimmer),
                    0.095F * strength,
                    0.25F, 0.88F, 1.0F);
        }

        // An expanding elliptical signal ring moves from emitter to the
        // recording front. An open ring reads more like a scan wave than
        // an opaque glass wall crossing the machine.
        Vec3 plane = EMITTER.lerp(focus, pulse);
        double w = 0.05D + halfWidth * pulse;
        double h = 0.04D + halfHeight * pulse;
        Vec3[] outer = ellipse(plane, w, h, FIELD_SEGMENTS);
        Vec3[] inner = ellipse(plane, Math.max(0.008D, w - 0.05D),
                Math.max(0.008D, h - 0.05D), FIELD_SEGMENTS);
        for (int i = 0; i < FIELD_SEGMENTS; i++) {
            int next = (i + 1) % FIELD_SEGMENTS;
            quad(pose, sink, inner[i], outer[i], outer[next], inner[next],
                    (float) (0.26D * strength * (1D - 0.30D * pulse)),
                    0.46F, 0.95F, 1.0F);
        }

        // A second tiny raster stroke lies horizontally. The elliptical
        // ring is viewed edge-on from overhead; this stroke keeps the
        // travelling data pulse readable from the player's top-down view.
        double lineHalfDepth = 0.035D;
        quad(pose, sink,
                new Vec3(plane.x - w, plane.y, plane.z - lineHalfDepth),
                new Vec3(plane.x + w, plane.y, plane.z - lineHalfDepth),
                new Vec3(plane.x + w, plane.y, plane.z + lineHalfDepth),
                new Vec3(plane.x - w, plane.y, plane.z + lineHalfDepth),
                (float) (0.18D * strength * (1D - 0.35D * pulse)),
                0.43F, 0.95F, 1.0F);

        // A fine cyan ring on the disc's actual upper emitter surface. This
        // remains the most obvious light source when viewed from above.
        double radius = 0.16D + 0.015D * Math.sin(filmTick * 0.07D);
        double inner = radius - 0.015D;
        for (int i = 0; i < EMITTER_SEGMENTS; i++) {
            double a = i * 2D * Math.PI / EMITTER_SEGMENTS;
            double b = (i + 1) * 2D * Math.PI / EMITTER_SEGMENTS;
            Vec3 innerA = ringPoint(inner, a);
            Vec3 outerA = ringPoint(radius, a);
            Vec3 outerB = ringPoint(radius, b);
            Vec3 innerB = ringPoint(inner, b);
            quad(pose, sink, innerA, outerA, outerB, innerB,
                    0.55F * strength, 0.34F, 0.92F, 1.0F);
        }
    }

    private static Vec3 ringPoint(double r, double angle) {
        return new Vec3(EMITTER.x + Math.cos(angle) * r,
                EMITTER.y + 0.009D, EMITTER.z + Math.sin(angle) * r);
    }

    private static Vec3[] ellipse(Vec3 center, double xRadius,
                                  double yRadius, int segments) {
        Vec3[] vertices = new Vec3[segments];
        for (int i = 0; i < segments; i++) {
            double angle = 2D * Math.PI * i / segments;
            vertices[i] = center.add(Math.cos(angle) * xRadius,
                    Math.sin(angle) * yRadius, 0D);
        }
        return vertices;
    }

    private static void quad(PoseStack pose, VertexConsumer sink, Vec3 a, Vec3 b,
                             Vec3 c, Vec3 d, float alpha,
                             float red, float green, float blue) {
        quadGradient(pose, sink, a, b, c, d, alpha, alpha, red, green, blue);
    }

    /**
     * The beam is brighter by the physical emitter and falls off before
     * reaching the hologram, rather than tinting the entire scene blue.
     * All UVs sample the same neutral texel: unlike mechanical block models,
     * no bitmap is stretched over the cone's many-block-wide surfaces.
     */
    private static void quadGradient(PoseStack pose, VertexConsumer sink,
                                     Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                                     float nearAlpha, float farAlpha,
                                     float red, float green, float blue) {
        int near = opacity(nearAlpha);
        int far = opacity(farAlpha);
        vertex(pose, sink, a, red, green, blue, near, 0.5F, 0.5F);
        vertex(pose, sink, b, red, green, blue, near, 0.5F, 0.5F);
        vertex(pose, sink, c, red, green, blue, far, 0.5F, 0.5F);
        vertex(pose, sink, d, red, green, blue, far, 0.5F, 0.5F);
    }

    private static int opacity(float alpha) {
        return Math.max(0, Math.min(255, Math.round(alpha * 255F)));
    }

    private static void vertex(PoseStack pose, VertexConsumer sink, Vec3 point,
                               float red, float green, float blue, int opacity,
                               float u, float v) {
        sink.vertex(pose.last().pose(),
                        (float) point.x, (float) point.y, (float) point.z)
                .color(Math.round(red * 255F), Math.round(green * 255F),
                        Math.round(blue * 255F), opacity)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.last().normal(), 0F, 1F, 0F)
                .endVertex();
    }
}
