package com.magneticraft2.client.render.blocks.stage.stone;

import com.magneticraft2.common.block.stage.stone.PrimitiveStorageCellarLayout;
import com.magneticraft2.common.blockentity.stage.stone.PrimitiveStorageCellarMultiblockEntity;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockHitHelper;
import com.magneticraft2.common.utils.MultiBlockProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

public class PrimitiveStorageCellarBlockRenderer implements BlockEntityRenderer<PrimitiveStorageCellarMultiblockEntity> {
    public PrimitiveStorageCellarBlockRenderer(BlockEntityRendererProvider.Context context) {}
    @Override
    public void render(PrimitiveStorageCellarMultiblockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
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
            if (modelName.contains("west")) {
                pPoseStack.translate(-1, 0, 0); // Adjust position if needed
            }


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

        renderStoredItems(pBlockEntity, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
    }

    private void renderStoredItems(PrimitiveStorageCellarMultiblockEntity blockEntity,
                                   PoseStack poseStack,
                                   MultiBufferSource buffer,
                                   int packedLight,
                                   int packedOverlay) {
        if (!blockEntity.isFormed() || blockEntity.itemHandler == null) {
            return;
        }

        Direction formedFacing = getFormedFacing(blockEntity.getMBblueprintname());
        var itemRenderer = Minecraft.getInstance().getItemRenderer();

        for (PrimitiveStorageCellarLayout.Slot slot : PrimitiveStorageCellarLayout.slots()) {
            ItemStack stack = blockEntity.itemHandler.getStackInSlot(slot.index());
            if (stack.isEmpty()) {
                continue;
            }

            Vec3 renderPos = MultiblockHitHelper.fromCanonicalWest(slot.renderPosition(), formedFacing);

            poseStack.pushPose();
            poseStack.translate(renderPos.x, renderPos.y, renderPos.z);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(0.35F, 0.35F, 0.35F);

            itemRenderer.renderStatic(
                    stack,
                    ItemDisplayContext.FIXED,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    buffer,
                    blockEntity.getLevel(),
                    slot.index()
            );

            poseStack.popPose();
        }
    }

    private Direction getFormedFacing(String blueprintName) {
        if (blueprintName != null) {
            if (blueprintName.endsWith("_east")) {
                return Direction.EAST;
            }
            if (blueprintName.endsWith("_north")) {
                return Direction.NORTH;
            }
            if (blueprintName.endsWith("_south")) {
                return Direction.SOUTH;
            }
        }
        return Direction.WEST;
    }
}
