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
    private static final ResourceLocation ROTOR_MODEL =
            new ResourceLocation("magneticraft2", "multiblock/primitive_grinder_rotor");
    private static final ResourceLocation AXLE_MODEL =
            new ResourceLocation("magneticraft2", "multiblock/primitive_grinder_axle");

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

        renderModel(
                new ResourceLocation("magneticraft2", modelName),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        float inputRotation = getInputRotation(blockEntity, partialTick);

        // The horizontal input axle follows the exact Gear V2 shaft rotation.
        renderRotatingModel(
                AXLE_MODEL,
                Axis.XP,
                inputRotation,
                0.0D,
                0.5D,
                0.5D,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        // Internal right-angle gearing is represented as a 1:1 direction change.
        // The upper grinding assembly therefore rotates around Y in the opposite direction.
        renderRotatingModel(
                ROTOR_MODEL,
                Axis.YP,
                -inputRotation,
                0.5D,
                0.0D,
                0.5D,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderInputWorkpiece(
                blockEntity,
                partialTick,
                poseStack,
                buffer,
                packedLight
        );

        renderOutputPile(
                blockEntity,
                poseStack,
                buffer,
                packedLight
        );
    }

    private float getInputRotation(PrimitiveGrinderBMultiblockEntity blockEntity, float partialTick) {
        return blockEntity.getMechanicalVisualRotationDegrees(partialTick);
    }

    private void renderInputWorkpiece(PrimitiveGrinderBMultiblockEntity blockEntity,
                                      float partialTick,
                                      PoseStack poseStack,
                                      MultiBufferSource buffer,
                                      int packedLight) {
        ItemStack input = blockEntity.getInputStack();
        if (input.isEmpty()) {
            return;
        }

        // The workpiece belongs in the actual grinding area, not on top of the machine.
        // As the recipe progresses it settles deeper into the grinding ring and becomes
        // slightly smaller, giving useful GUI-less feedback without pretending the item
        // has already been consumed.
        float progress = blockEntity.getVisualCrushProgress(partialTick);
        double y = 1.285D - (0.19D * progress);
        float scale = 0.30F - (0.07F * progress);
        float yaw = -8.0F + (6.0F * progress);

        ItemStack displayStack = input.copy();
        displayStack.setCount(1);

        renderStoredItem(
                blockEntity,
                displayStack,
                0.5D,
                y,
                0.5D,
                yaw,
                scale,
                101,
                poseStack,
                buffer,
                packedLight
        );
    }

    private void renderOutputPile(PrimitiveGrinderBMultiblockEntity blockEntity,
                                  PoseStack poseStack,
                                  MultiBufferSource buffer,
                                  int packedLight) {
        ItemStack output = blockEntity.getOutputStack();
        if (output.isEmpty()) {
            return;
        }

        // The front-center gap in the current stone lip acts as a provisional catch point.
        // Keep this entirely visual for now so the eventual dedicated tray/bin design can be
        // changed without touching recipe or inventory behavior.
        int count = output.getCount();
        int copies = count >= 16 ? 3 : (count >= 4 ? 2 : 1);

        double[] xOffsets = {0.0D, -0.055D, 0.055D};
        double[] yOffsets = {0.0D, 0.018D, 0.028D};
        double[] zOffsets = {0.0D, 0.035D, 0.025D};
        float[] yawOffsets = {10.0F, -14.0F, 23.0F};

        ItemStack displayStack = output.copy();
        displayStack.setCount(1);

        for (int i = 0; i < copies; i++) {
            renderStoredItem(
                    blockEntity,
                    displayStack,
                    0.5D + xOffsets[i],
                    0.925D + yOffsets[i],
                    0.105D + zOffsets[i],
                    yawOffsets[i],
                    0.27F,
                    202 + i,
                    poseStack,
                    buffer,
                    packedLight
            );
        }
    }

    private void renderRotatingModel(ResourceLocation modelLocation,
                                     Axis axis,
                                     float rotationDegrees,
                                     double pivotX,
                                     double pivotY,
                                     double pivotZ,
                                     PoseStack poseStack,
                                     MultiBufferSource buffer,
                                     int packedLight,
                                     int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(pivotX, pivotY, pivotZ);
        poseStack.mulPose(axis.rotationDegrees(rotationDegrees));
        poseStack.translate(-pivotX, -pivotY, -pivotZ);

        renderModel(modelLocation, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderModel(ResourceLocation modelLocation,
                             PoseStack poseStack,
                             MultiBufferSource buffer,
                             int packedLight,
                             int packedOverlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLocation);
        if (model == null) {
            return;
        }

        RandomSource random = RandomSource.create(0L);
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
