package com.magneticraft2.client.hologram;

import com.magneticraft2.client.animation.AnimatedComponentRenderer;
import com.magneticraft2.common.hologram.AncientProjectorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * First in-world visual proof: render an existing PR #12 scene using recorded
 * channels. The existing scene's material is not yet hologram-tinted.
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
        Direction facing = projector.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        pose.pushPose();
        try {
            pose.translate(.5D, 0D, .5D);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            pose.translate(-.5D, 0D, -.5D);
            pose.translate(recording.offsetX(), recording.offsetY(), recording.offsetZ());
            AnimatedComponentRenderer.renderScene(recording.animation(),
                    HologramAnimationBridge.frame(recording.channels(), elapsed),
                    Blocks.STONE.defaultBlockState(), pose, buffers, light, overlay);
        } finally {
            pose.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(AncientProjectorBlockEntity projector) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(AncientProjectorBlockEntity projector) {
        return new AABB(projector.getBlockPos()).inflate(16);
    }

    @Override
    public boolean shouldRender(AncientProjectorBlockEntity projector, Vec3 cameraPos) {
        return cameraPos.distanceToSqr(Vec3.atCenterOf(projector.getBlockPos())) < 64 * 64;
    }
}
