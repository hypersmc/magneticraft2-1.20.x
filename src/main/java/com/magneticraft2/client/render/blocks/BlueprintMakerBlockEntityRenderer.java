package com.magneticraft2.client.render.blocks;

import com.magneticraft2.common.blockentity.general.blueprintmakerBlockEntity;
import com.magneticraft2.common.utils.Magneticraft2ConfigClient;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

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
        // Get positions of the two posts
        BlockPos pos1 = pBlockEntity.getPos1BlockPos();
        BlockPos pos2 = pBlockEntity.getPos2BlockPos();
        if (pBlockEntity.getPos2long() != 0 && pBlockEntity.getPos1long() != 0) {
            if (Magneticraft2ConfigClient.GENERAL.blueprintmakerrenderer.get()) {
                // Calculate the center position between pos1 and pos2
                double centerX = (pos1.getX() + pos2.getX()) / 2.0;
                double centerY = (pos1.getY() + pos2.getY()) / 2.0;
                double centerZ = (pos1.getZ() + pos2.getZ()) / 2.0;

                // Calculate the bounding box between the two posts
                BlockPos minPos = new BlockPos(
                        Math.min(pos1.getX(), pos2.getX()),
                        Math.min(pos1.getY(), pos2.getY()),
                        Math.min(pos1.getZ(), pos2.getZ())
                );

                BlockPos maxPos = new BlockPos(
                        Math.max(pos1.getX(), pos2.getX()),
                        Math.max(pos1.getY(), pos2.getY()),
                        Math.max(pos1.getZ(), pos2.getZ())
                );
                // Calculate the dimensions of the model formed between pos1 and pos2
                int modelWidth = maxPos.getX() - minPos.getX() + 1;
                int modelHeight = maxPos.getY() - minPos.getY() + 1;
                int modelDepth = maxPos.getZ() - minPos.getZ() + 1;
                // Calculate the scaling factor based on the maximum dimension within a 1-block size
                int maxDimension = Math.max(modelWidth, Math.max(modelHeight, modelDepth));
                float scaleFactor = 1.0f / maxDimension; // Scale down the model to fit within a 1-block size

                // Offset to place blocks on top of the block entity
                double yOffset = 1.0;

                // Iterate through the bounding box
                for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
                    for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
                        for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                            BlockPos currentPos = new BlockPos(x, y, z);

                            // Check if the block is not air
                            BlockState blockState = pBlockEntity.getLevel().getBlockState(currentPos);
                            if (blockState != null && !blockState.isAir()) {
                                // Calculate the offset to position blocks based on the center point between pos1 and pos2
                                double offsetX = (x - centerX) * scaleFactor + 0.5 * (1 - scaleFactor); // Add offset to center the scaled model within the block
                                double offsetZ = (z - centerZ) * scaleFactor + 0.5 * (1 - scaleFactor); // Add offset to center the scaled model within the block
                                double layerOffset = (y - minPos.getY()) * scaleFactor; // Increase Y position for each layer

                                // Render a scaled-down representation of the block on top of the block entity
                                pPoseStack.pushPose();
                                pPoseStack.translate(offsetX, yOffset + layerOffset, offsetZ); // Translate to position blocks above the block entity
                                pPoseStack.scale(scaleFactor, scaleFactor, scaleFactor); // Scale down the block

                                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                                        blockState,
                                        pPoseStack,
                                        pBuffer,
                                        pPackedLight,
                                        pPackedOverlay
                                );
                                pPoseStack.popPose();
                            }
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
