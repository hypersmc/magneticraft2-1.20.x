package com.magneticraft2.client.render.blocks;

import com.magneticraft2.common.blockentity.general.BlueprintMultiblockEntity;
import com.magneticraft2.common.utils.Magneticraft2ConfigClient;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import com.magneticraft2.common.utils.MultiBlockProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

/**
 * @author JumpWatch on 12-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BlueprintMultiblockRenderer implements BlockEntityRenderer<BlueprintMultiblockEntity> {
    public BlueprintMultiblockRenderer(BlockEntityRendererProvider.Context context) {}
    @Override
    public void render(BlueprintMultiblockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        // Get positions of the two posts
        BlockPos pos1 = pBlockEntity.getPos1BlockPos();
        BlockPos pos2 = pBlockEntity.getPos2BlockPos();
        // Define the model's resource location
        ModelData modelData = pBlockEntity.getModelData();
        String modelName = modelData.get(MultiBlockProperties.MODEL_NAME);

        if (!modelName.isEmpty()) {
            ResourceLocation modelLocation = new ResourceLocation("magneticraft2", modelName);

            // Retrieve the model from ModelManager
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLocation);
            if (model == null) {
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                    System.out.println("Custom model not found: " + modelLocation);
                }
                return;
            }

            // Prepare for rendering
            pPoseStack.pushPose();
            pPoseStack.translate(0, 0, 0); // Adjust position if needed

            // Get quads from the model and render them
            RandomSource random = RandomSource.create();
            List<BakedQuad> quads = model.getQuads((BlockState) null, (Direction) null, random);

            // Render each quad using the buffer source and RenderType
            var vertexConsumer = pBuffer.getBuffer(RenderType.solid());
            for (BakedQuad quad : quads) {
                vertexConsumer.putBulkData(pPoseStack.last(), quad, 1.0F, 1.0F, 1.0F, pPackedLight, pPackedOverlay);
            }

            pPoseStack.popPose();
        }
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
                float scaleFactor = 1.5f / maxDimension; // Scale down the model to fit within a 1-block size

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
                                if (modelName.contains("south")) {
                                    pPoseStack.translate(offsetX, yOffset + layerOffset, offsetZ + 0.5); // Translate to position blocks above the block entity
                                }else if (modelName.contains("west")) {
                                    pPoseStack.translate(offsetX - 0.5, yOffset + layerOffset, offsetZ); // Translate to position blocks above the block entity
                                }else if (modelName.contains("north")) {
                                    pPoseStack.translate(offsetX, yOffset + layerOffset, offsetZ - 0.5); // Translate to position blocks above the block entity
                                }else if (modelName.contains("east")) {
                                    pPoseStack.translate(offsetX + 0.5, yOffset + layerOffset, offsetZ); // Translate to position blocks above the block entity
                                }
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
}
