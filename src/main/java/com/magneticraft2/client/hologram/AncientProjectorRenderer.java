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
import net.minecraft.world.phys.AABB;
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
        double elapsed = projector.getLevel().getGameTime() - projector.startTime() + partialTick;
        if (elapsed < 0 || elapsed >= recording.durationTicks()) return;
        float opacity = projectionOpacity(elapsed, recording.durationTicks(),
                recording.appearance());
        if (opacity <= 0F) return;
        Direction facing = projector.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        pose.pushPose();
        try {
            pose.translate(.5D, 0D, .5D);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            pose.translate(-.5D, 0D, -.5D);
            pose.translate(recording.offsetX(), recording.offsetY(), recording.offsetZ());
            AnimatedComponentRenderer.renderScene(recording.animation(),
                    HologramAnimationBridge.frame(recording.channels(), elapsed),
                    Blocks.STONE.defaultBlockState(), pose,
                    new HologramBufferSource(buffers, opacity,
                            recording.appearance().red(),
                            recording.appearance().green(),
                            recording.appearance().blue()),
                    LightTexture.FULL_BRIGHT, overlay);
        } finally {
            pose.popPose();
        }
    }

    /**
     * Gentle materialization and dissolve, driven only by synchronized recording
     * time. The small deterministic shimmer does not alter the machine's motion.
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

        // Fully deterministic and intentionally subtle: no random events,
        // screen flashes, or updates to the physical machine simulation.
        double shimmer = 1D + appearance.flicker()
                * (0.60D * Math.sin(elapsed * 0.31D)
                    + 0.40D * Math.sin(elapsed * 1.12D));
        return (float) Math.max(0D, Math.min(1D,
                appearance.opacity() * fadeIn * fadeOut * shimmer));
    }

    @Override
    public boolean shouldRenderOffScreen(AncientProjectorBlockEntity projector) {
        return true;
    }

    public AABB getRenderBoundingBox(AncientProjectorBlockEntity projector) {
        return new AABB(projector.getBlockPos()).inflate(16);
    }

    @Override
    public boolean shouldRender(AncientProjectorBlockEntity projector, Vec3 cameraPos) {
        return cameraPos.distanceToSqr(Vec3.atCenterOf(projector.getBlockPos())) < 64 * 64;
    }
}
