package com.magneticraft2.client.render.blocks;

import com.magneticraft2.common.blockentity.general.blueprintmakerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author JumpWatch on 20-08-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BlueprintMakerBlockEntityRenderer implements BlockEntityRenderer<blueprintmakerBlockEntity> {
    private static final Logger LOGGER = LogManager.getLogger("blueprintmaker");
    public BlueprintMakerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}
    @Override
    public void render(blueprintmakerBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        int destX = pBlockEntity.getBlockPos().getX();
        int destY = pBlockEntity.getBlockPos().getY();
        int destZ = pBlockEntity.getBlockPos().getZ();
        if (pBlockEntity.getPos2long() != 0 && pBlockEntity.getPos1long() != 0) {
            BlockPos pos1xyz = pBlockEntity.getPos1BlockPos();
            BlockPos pos2xyz = pBlockEntity.getPos2BlockPos();
            int pos1X = pos1xyz.getX();
            int pos1Y = pos1xyz.getY();
            int pos1Z = pos1xyz.getZ();

            int pos2X = pos2xyz.getX();
            int pos2Y = pos2xyz.getY();
            int pos2Z = pos2xyz.getZ();

            // Calculate the range of positions between pos1 and pos2
            int minX = Math.min(pos1X, pos2X);
            int minY = Math.min(pos1Y, pos2Y);
            int minZ = Math.min(pos1Z, pos2Z);
            int maxX = Math.max(pos1X, pos2X);
            int maxY = Math.max(pos1Y, pos2Y);
            int maxZ = Math.max(pos1Z, pos2Z);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockState blockState = pBlockEntity.getLevel().getBlockState(new BlockPos(x, y, z));
                        if (blockState != null && !blockState.isAir()) {
                            // Calculate animation progress based on a sine wave for smoother easing
                            float animationProgress = (float) Math.sin(pBlockEntity.getLevel().getGameTime() / 40.0) * 0.5f + 0.5f;
                            boolean shouldRender = true;
                            // Calculate animation progress based on a sine wave for smoother easing
                            if (shouldRender) {
                                animationProgress = (float) Math.sin(pBlockEntity.getLevel().getGameTime() / 40.0) * 0.5f + 0.5f;
                            } else {
                                animationProgress = 1.0f;
                            }
                            if (animationProgress >= 0.998f) {
                                pBlockEntity.setJustPlaced(false);
                                shouldRender = false; // Set the flag to prevent further rendering
                            }
                            // Interpolate between source position and destination
                            double interpX = x + (destX - x) * animationProgress;
                            double interpY = y + (destY - y) * animationProgress;
                            double interpZ = z + (destZ - z) * animationProgress;

                            // Translate the render position
                            pPoseStack.pushPose();
                            pPoseStack.translate(interpX - destX, interpY - destY, interpZ - destZ); // Translate to the interpolated position

                            // Render the block
                            if (shouldRender) {
                                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                                        blockState,
                                        pPoseStack,
                                        pBuffer,
                                        pPackedLight,
                                        pPackedOverlay
                                );
                            }



                            // Pop the matrix stack
                            pPoseStack.popPose();
                        }
                    }
                }
            }
        }
    }


    @Override
    public boolean shouldRenderOffScreen(blueprintmakerBlockEntity pBlockEntity) {
        return true;
    }

    @Override
    public boolean shouldRender(blueprintmakerBlockEntity pBlockEntity, Vec3 pCameraPos) {
        return true;
    }
}
