package com.magneticraft2.client.render.blocks.stage.stone;

import com.magneticraft2.common.blockentity.stage.stone.PrimitiveGrinderBMultiblockEntity;
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
    }
}
