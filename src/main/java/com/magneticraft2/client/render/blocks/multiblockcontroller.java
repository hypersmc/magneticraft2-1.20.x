package com.magneticraft2.client.render.blocks;

import com.magneticraft2.common.blockentity.general.testmultiblock;
import com.magneticraft2.common.utils.MultiBlockProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.model.data.ModelData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Random;

import static mekanism.client.ClientTickHandler.minecraft;

/**
 * @author JumpWatch on 02-07-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class multiblockcontroller implements BlockEntityRenderer<testmultiblock> {
    private static final Logger LOGGER = LogManager.getLogger("multiblockcontroller");
    private final RandomSource random = RandomSource.create();
    private final Minecraft minecraft = Minecraft.getInstance();

    public multiblockcontroller(BlockEntityRendererProvider.Context context) {}
    @Override
    public void render(testmultiblock testmultiblock, float v, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int i1) {
        ModelData modelData = testmultiblock.getModelData();
        String modelName = modelData.get(MultiBlockProperties.MODEL_NAME);
//        if (modelName != null) {
//            ResourceLocation modelResource = new ResourceLocation("magneticraft2", "models/multiblock/primitive_furnace_formed");
//            BakedModel model = minecraft.getModelManager().getModel(modelResource);
//
//                    poseStack.pushPose();
//            poseStack.translate(0.5, 0, 0.5); // Adjust position if necessary
//
//            // Choose the correct render layer, typically RenderType.solid() for most block models
//            VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.solid());
//
//            // Render all quads. You might need to handle different sides if necessary.
//            RandomSource random = RandomSource.create();
//            for (Direction direction : Direction.values()) {
//                model.getQuads(null, direction, random).forEach(quad -> {
//                    vertexConsumer.putBulkData(poseStack.last(), quad, 1.0F, 1.0F, 1.0F, i, i1);
//                });
//            }
//
//            // Also render any non-culled faces (like translucent parts)
//            model.getQuads(null, null, random).forEach(quad -> {
//                vertexConsumer.putBulkData(poseStack.last(), quad, 1.0F, 1.0F, 1.0F, i, i1);
//            });
//
//            poseStack.popPose();
//        }


    }
    private void renderQuads(PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, BakedModel model, Direction side) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.translucent());
        RandomSource random = RandomSource.create();
        for (BakedQuad quad : model.getQuads(null, side, random)) {
            vertexConsumer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, light, overlay);
        }
    }

}
