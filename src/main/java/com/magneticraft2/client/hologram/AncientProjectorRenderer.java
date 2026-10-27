package com.magneticraft2.client.hologram;

import com.magneticraft2.client.animation.AnimatedComponentRenderer;
import com.magneticraft2.common.hologram.AncientProjectorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Full-size, non-interactive projected recordings. Reuses PR #12's animation
 * evaluator and baked models, supplying only an opt-in hologram material.
 */
public final class AncientProjectorRenderer implements BlockEntityRenderer<AncientProjectorBlockEntity> {
    public AncientProjectorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(AncientProjectorBlockEntity projector, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (projector.getLevel() == null || projector.startTime() < 0) return;
        HologramRecordingResourceLoader.Recording recording =
                HologramRecordingResourceLoader.get(projector.recording());
        if (recording == null) return;
        int serverDuration = projector.durationTicks();
        if (serverDuration <= 0) return;
        // Every client receives the server's latched start time and duration
        // through the BE update packet, including players joining mid-film.
        double elapsed = projector.getLevel().getGameTime() - projector.startTime() + partialTick;
        if (elapsed < 0D || elapsed >= serverDuration) return;
        // A datapack can override playback length without modifying the
        // corresponding client resource-pack animation. The visual timeline
        // is rescaled, while completion ALWAYS belongs to the server.
        double sceneTick = elapsed * recording.durationTicks() / serverDuration;
        float opacity = projectionOpacity(sceneTick, recording.durationTicks(),
                recording.appearance()) * recording.visibility(sceneTick);
        if (opacity <= 0F) return;
        // Validate archive/animation cross-references once per resource set.
        // A broken authored film never submits partial or misleading geometry.
        if (!HologramRecordingValidator.isUsable(projector.recording(), recording)) return;
        // Once per scene/reload: log the exact baked model responsible if
        // its atlas sprite is the purple/black missing-texture checkerboard.
        HologramModelDiagnostics.check(recording.animation());
        Direction facing = projector.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        pose.pushPose();
        try {
            pose.translate(.5D, 0D, .5D);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            pose.translate(-.5D, 0D, -.5D);
            pose.translate(recording.offsetX(), recording.offsetY(), recording.offsetZ());
            AnimatedComponentRenderer.renderScene(recording.animation(),
                    HologramAnimationBridge.frame(recording.channels(),
                            recording.visualChannels(), sceneTick),
                    Blocks.STONE.defaultBlockState(), pose,
                    new HologramBufferSource(buffers, opacity,
                            recording.appearance().red(),
                            recording.appearance().green(),
                            recording.appearance().blue(),
                            recording.appearance().preserveSourceTexture(),
                            recording.appearance().shaderEffect()),
                    LightTexture.FULL_BRIGHT, overlay);
        } finally {
            pose.popPose();
        }
    }

    /**
     * Materialization and dissolve, with occasional smoothly eased recording
     * signal dips. All effects depend only on synchronized playback time and
     * cannot alter the machine simulation.
     */
    static float projectionOpacity(double elapsed, int duration,
                                   HologramRecordingResourceLoader.Appearance appearance) {
        if (!Double.isFinite(elapsed) || duration <= 0
                || elapsed <= 0D || elapsed >= duration) {
            return 0F;
        }
        double fadeIn = appearance.fadeInTicks() == 0 ? 1D
                : Math.min(1D, elapsed / appearance.fadeInTicks());
        double fadeOut = appearance.fadeOutTicks() == 0 ? 1D
                : Math.min(1D, (duration - elapsed) / appearance.fadeOutTicks());
        fadeIn = fadeIn * fadeIn * (3D - 2D * fadeIn);
        fadeOut = fadeOut * fadeOut * (3D - 2D * fadeOut);

        // The old +/-2% oscillation was almost invisible at low-alpha
        // hologram settings. Keep a very gentle continuous drift and add
        // an occasional *brief, smoothly eased* signal dip. This happens
        // once per roughly six seconds, never rapid full-frame flashing.
        //
        // All timing is based on the server-synchronized recording clock:
        // two clients rendering the same tick see the same signal quality.
        // A zero flicker setting disables both effects.
        double drift = 0.5D + 0.5D * Math.sin(
                elapsed * 0.12D + 1.4D * Math.sin(elapsed * 0.031D));
        long cycle = (long) Math.floor(elapsed / 117D);
        double cycleTime = elapsed - cycle * 117D;
        double dipCenter = 31D + (cycle * 37L % 53L);
        double distance = Math.abs(cycleTime - dipCenter);
        double signalDip = 0D;
        if (distance < 5D) {
            double proximity = 1D - distance / 5D;
            signalDip = proximity * proximity * (3D - 2D * proximity);
        }

        double signalStrength = 1D - appearance.flicker()
                * (0.08D * drift + 0.92D * signalDip);
        return (float) Math.max(0D, Math.min(1D,
                appearance.opacity() * fadeIn * fadeOut * signalStrength));
    }

    @Override
    public boolean shouldRenderOffScreen(AncientProjectorBlockEntity projector) {
        // Let Forge cull using the enlarged AABB on the BLOCK ENTITY.
        // Returning true here previously forced every nearby projector to
        // submit its entire animated scene even while completely off-screen.
        return false;
    }

    @Override
    public boolean shouldRender(AncientProjectorBlockEntity projector, Vec3 cameraPos) {
        // Extremely large archives can extend beyond the usual 64-block
        // block-entity viewing distance. Keep a modest distance allowance so
        // a visible projection is not cut off by its tiny physical emitter.
        int maxDistance = Math.max(64, projector.renderRadiusBlocks() + 16);
        return cameraPos.distanceToSqr(Vec3.atCenterOf(projector.getBlockPos()))
                < (double) maxDistance * maxDistance;
    }
}
