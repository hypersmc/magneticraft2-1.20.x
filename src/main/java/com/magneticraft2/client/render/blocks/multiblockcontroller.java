package com.magneticraft2.client.render.blocks;

import com.magneticraft2.client.model.MultiBlockModel;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

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
    public void render(testmultiblock testmultiblock, float v, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay) {
        // Define the model's resource location
        ModelData modelData = testmultiblock.getModelData();
        String modelName = modelData.get(MultiBlockProperties.MODEL_NAME);

        if (!modelName.isEmpty()) {
            ResourceLocation modelLocation = new ResourceLocation("magneticraft2", "multiblock/primitive_furnace_formed");

            // Retrieve the model from ModelManager
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLocation);
            if (model == null) {
                System.out.println("Custom model not found: " + modelLocation);
                return;
            }

            // Prepare for rendering
            poseStack.pushPose();
            poseStack.translate(0, 0, 0); // Adjust position if needed

            // Get quads from the model and render them
            RandomSource random = RandomSource.create();
            List<BakedQuad> quads = model.getQuads((BlockState) null, (Direction) null, random);

            // Render each quad using the buffer source and RenderType
            var vertexConsumer = multiBufferSource.getBuffer(RenderType.solid());
            for (BakedQuad quad : quads) {
                vertexConsumer.putBulkData(poseStack.last(), quad, 1.0F, 1.0F, 1.0F, light, overlay);
            }

            poseStack.popPose();
        }
    }
    private void renderQuads(PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, BakedModel model, Direction side) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.translucent());
        RandomSource random = RandomSource.create();
        for (BakedQuad quad : model.getQuads(null, side, random)) {
            vertexConsumer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, light, overlay);
        }
    }

}
