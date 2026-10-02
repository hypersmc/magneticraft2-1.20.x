package com.magneticraft2.client.render.blocks.stage.stone;

import com.magneticraft2.common.blockentity.stage.stone.PrimitiveGrinderBMultiblockEntity;
import com.magneticraft2.common.utils.MultiBlockProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

public class PrimitiveGrinderBlockEntityRenderer implements BlockEntityRenderer<PrimitiveGrinderBMultiblockEntity> {
    public PrimitiveGrinderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PrimitiveGrinderBMultiblockEntity blockEntity,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight,
                       int packedOverlay) {
        if (!blockEntity.isFormed()) {
            return;
        }

        ModelData modelData = blockEntity.getModelData();
        String modelName = modelData.get(MultiBlockProperties.MODEL_NAME);
        if (modelName == null || modelName.isEmpty()) {
            return;
        }

        ResourceLocation modelLocation = new ResourceLocation("magneticraft2", modelName);
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLocation);
        if (model == null) {
            return;
        }

        RandomSource random = RandomSource.create();
        List<BakedQuad> quads = model.getQuads((BlockState) null, (Direction) null, random);
        var vertexConsumer = buffer.getBuffer(RenderType.solid());

        poseStack.pushPose();
        for (BakedQuad quad : quads) {
            vertexConsumer.putBulkData(
                    poseStack.last(),
                    quad,
                    1.0F,
                    1.0F,
                    1.0F,
                    packedLight,
                    packedOverlay
            );
        }
        poseStack.popPose();

        renderStoredItem(
                blockEntity,
                blockEntity.getInputStack(),
                0.5D,
                1.365D,
                0.5D,
                -12.0F,
                0.34F,
                101,
                poseStack,
                buffer,
                packedLight
        );

        renderStoredItem(
                blockEntity,
                blockEntity.getOutputStack(),
                0.5D,
                0.925D,
                0.16D,
                14.0F,
                0.32F,
                202,
                poseStack,
                buffer,
                packedLight
        );
    }

    private void renderStoredItem(PrimitiveGrinderBMultiblockEntity blockEntity,
                                  ItemStack stack,
                                  double x,
                                  double y,
                                  double z,
                                  float yaw,
                                  float scale,
                                  int seed,
                                  PoseStack poseStack,
                                  MultiBufferSource buffer,
                                  int packedLight) {
        if (stack.isEmpty()) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel itemModel = itemRenderer.getModel(stack, blockEntity.getLevel(), null, seed);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        if (!itemModel.isGui3d()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }

        poseStack.scale(scale, scale, scale);
        itemRenderer.render(
                stack,
                ItemDisplayContext.NONE,
                false,
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                itemModel
        );
        poseStack.popPose();
    }
}
