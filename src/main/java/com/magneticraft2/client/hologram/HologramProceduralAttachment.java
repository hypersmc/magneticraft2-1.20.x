package com.magneticraft2.client.hologram;

import com.magneticraft2.client.animation.AnimatedComponentRenderer;
import com.magneticraft2.client.animation.AnimationDefinition;
import com.magneticraft2.client.animation.AnimationFrame;
import com.magneticraft2.client.render.blocks.stage.copper.MechanicalBrakeBlockEntity_woodRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Bridges authored hologram films to real machine procedural geometry.
 *
 * This intentionally does NOT instantiate any real or fake block entity:
 * mechanical motion, gear overload and brake engagement are sampled from
 * the prerecorded timeline and are not game simulation.
 *
 * Components are drawn at the existing PR #12 scene graph's evaluated
 * transform so author-defined parent pivots and the physical projector
 * facing all apply correctly. The live Magneticraft2 brake and the archive
 * share exactly the same Java geometry routine, textures and dimensions.
 */
public final class HologramProceduralAttachment
        implements AnimatedComponentRenderer.ComponentAttachment {

    private final Map<String, HologramRecordingResourceLoader.ProceduralPart> parts;
    private final HologramBufferSource projection;
    private final double filmTick;

    public HologramProceduralAttachment(
            HologramRecordingResourceLoader.Recording film,
            HologramBufferSource projection, double filmTick) {
        Map<String, HologramRecordingResourceLoader.ProceduralPart> byComponent =
                new HashMap<>();
        for (HologramRecordingResourceLoader.ProceduralPart part
                : film.proceduralParts()) {
            byComponent.put(part.component(), part);
        }
        this.parts = Map.copyOf(byComponent);
        this.projection = projection;
        this.filmTick = filmTick;
    }

    @Override
    public void render(String name, AnimationDefinition.Component component,
                       AnimationFrame frame, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        HologramRecordingResourceLoader.ProceduralPart part = parts.get(name);
        if (part == null) return;

        if (part.visibleChannel() != null) {
            Integer stage = frame.integer(part.visibleChannel());
            if (stage == null || stage.intValue() != part.visibleStage()) return;
        }

        Float recordedAngle = frame.angle(part.angleChannel());
        if (recordedAngle == null || !Float.isFinite(recordedAngle)) return;
        float rotation = recordedAngle * part.angleScale();
        if (!Float.isFinite(rotation)) return;

        boolean braking = filmTick >= part.clampFromTick()
                && filmTick < part.clampUntilTick();

        // Parser restricts type to this explicitly reviewed machine visual.
        // More real procedural machines can be whitelisted individually.
        if ("magneticraft2:mechanical_brake_wood".equals(part.type())) {
            MechanicalBrakeBlockEntity_woodRenderer.renderProcedural(
                    pose, projection::textured, light, part.axis(), rotation, braking);
        }
    }
}
