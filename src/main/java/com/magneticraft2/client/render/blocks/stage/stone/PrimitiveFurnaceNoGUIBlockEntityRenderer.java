package com.magneticraft2.client.render.blocks.stage.stone;

import com.magneticraft2.common.blockentity.stage.stone.PrimitiveFurnaceMultiblockEntity_nogui;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

/**
 * @author JumpWatch on 14-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class PrimitiveFurnaceNoGUIBlockEntityRenderer implements BlockEntityRenderer<PrimitiveFurnaceMultiblockEntity_nogui> {
    public PrimitiveFurnaceNoGUIBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}
    @Override
    public void render(PrimitiveFurnaceMultiblockEntity_nogui pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        ModelData modelData = pBlockEntity.getModelData();
        String modelName = modelData.get(MultiBlockProperties.MODEL_NAME);
        if (!modelName.isEmpty()) {
            ResourceLocation modelLocation = new ResourceLocation("magneticraft2", modelName);

            // Retrieve the model from ModelManager
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLocation);
            if (model == null) {
                System.out.println("Custom model not found: " + modelLocation);
                return;
            }

            // Prepare for rendering
            pPoseStack.pushPose();
            pPoseStack.translate(0, 1, 0); // Adjust position if needed

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

        if (pBlockEntity.getItemInSlot(1).getItem() == Items.COAL) {

            if (pBlockEntity.isCooking()) {

                ResourceLocation coalamber = new ResourceLocation("magneticraft2", "multiblock/coalamber");
                BakedModel model1 = Minecraft.getInstance().getModelManager().getModel(coalamber);
                if (model1 == null) {
                    if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                        System.out.println("Custom model not found: " + model1);
                    }
                    return;
                }
                pPoseStack.pushPose();
                pPoseStack.translate(0, 0.1, 0);
                // Get quads from the model and render them
                RandomSource random = RandomSource.create();
                List<BakedQuad> quads = model1.getQuads((BlockState) null, (Direction) null, random);

                // Render each quad using the buffer source and RenderType
                var vertexConsumer = pBuffer.getBuffer(RenderType.solid());
                for (BakedQuad quad : quads) {
                    vertexConsumer.putBulkData(pPoseStack.last(), quad, 1.0F, 1.0F, 1.0F, pPackedLight, pPackedOverlay);
                }

                pPoseStack.popPose();
            } else {

                ResourceLocation coalidle = new ResourceLocation("magneticraft2", "multiblock/coalidle");
                BakedModel model = Minecraft.getInstance().getModelManager().getModel(coalidle);
                if (model == null) {
                    if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                        System.out.println("Custom model not found: " + model);
                    }
                    return;
                }
                pPoseStack.pushPose();
                pPoseStack.translate(0, 0.1, 0);
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
        }

    }
}
