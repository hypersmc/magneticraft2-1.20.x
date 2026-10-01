package com.magneticraft2.client.render.blocks.stage.stone;

import com.magneticraft2.common.block.stage.stone.PrimitiveStorageCellarLayout;
import com.magneticraft2.common.blockentity.stage.stone.PrimitiveStorageCellarMultiblockEntity;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockHitHelper;
import com.magneticraft2.common.utils.MultiBlockProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
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

        for (PrimitiveStorageCellarLayout.Slot slot : PrimitiveStorageCellarLayout.slots()) {
            ItemStack stack = blockEntity.itemHandler.getStackInSlot(slot.index());
            if (stack.isEmpty()) {
                continue;
            }

            long seed = getVisualSeed(slot.index(), stack);
            if (stack.getItem() instanceof BlockItem) {
                renderBlockPile(blockEntity, slot, stack, seed, formedFacing, poseStack, buffer, packedLight, packedOverlay);
            } else {
                renderLooseItems(blockEntity, slot, stack, seed, formedFacing, poseStack, buffer, packedLight, packedOverlay);
            }
        }
    }

    private void renderBlockPile(PrimitiveStorageCellarMultiblockEntity blockEntity,
                                 PrimitiveStorageCellarLayout.Slot slot,
                                 ItemStack stack,
                                 long seed,
                                 Direction formedFacing,
                                 PoseStack poseStack,
                                 MultiBufferSource buffer,
                                 int packedLight,
                                 int packedOverlay) {
        int visibleBlocks = stack.getCount() <= 8 ? 1
                : stack.getCount() <= 24 ? 2
                : stack.getCount() <= 40 ? 3
                : 4;

        double[][] pile = {
                {-0.11D, 0.00D,  0.00D},
                { 0.11D, 0.00D,  0.02D},
                {-0.09D, 0.18D,  0.01D},
                { 0.11D, 0.18D, -0.01D}
        };

        Vec3 visibilityOffset = getLowerCornerVisibilityOffset(slot);

        for (int i = 0; i < visibleBlocks; i++) {
            double jitterX = signedUnit(seed + i * 17L) * 0.025D;
            double jitterZ = signedUnit(seed + i * 31L) * 0.025D;
            float yaw = (float) (signedUnit(seed + i * 43L) * 7.0D);

            renderShelfItem(
                    blockEntity,
                    slot,
                    stack,
                    formedFacing,
                    visibilityOffset.x + pile[i][0] + jitterX,
                    visibilityOffset.y + pile[i][1],
                    visibilityOffset.z + pile[i][2] + jitterZ,
                    yaw,
                    0.0F,
                    0.0F,
                    0.43F,
                    ItemDisplayContext.FIXED,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    slot.index() * 10 + i
            );
        }
    }

    private void renderLooseItems(PrimitiveStorageCellarMultiblockEntity blockEntity,
                                  PrimitiveStorageCellarLayout.Slot slot,
                                  ItemStack stack,
                                  long seed,
                                  Direction formedFacing,
                                  PoseStack poseStack,
                                  MultiBufferSource buffer,
                                  int packedLight,
                                  int packedOverlay) {
        int visibleItems = stack.getCount() <= 15 ? 1
                : stack.getCount() <= 31 ? 2
                : stack.getCount() <= 47 ? 3
                : 4;

        Vec3 visibilityOffset = getLowerCornerVisibilityOffset(slot);

        // Loose items stay grounded on the shelf and build upward as a small pile.
        // The offsets are deliberately compact so the pile still clearly belongs
        // to one logical storage slot.
        double[][] pile = {
                {-0.040D, 0.000D,  0.000D},
                { 0.035D, 0.022D,  0.010D},
                {-0.022D, 0.044D, -0.008D},
                { 0.026D, 0.066D,  0.004D}
        };

        for (int i = 0; i < visibleItems; i++) {
            double jitterX = signedUnit(seed + i * 23L) * 0.018D;
            double jitterZ = signedUnit(seed + i * 37L) * 0.018D;
            float yaw = (float) (signedUnit(seed + i * 53L) * 18.0D);

            renderShelfItem(
                    blockEntity,
                    slot,
                    stack,
                    formedFacing,
                    visibilityOffset.x + pile[i][0] + jitterX,
                    visibilityOffset.y + pile[i][1],
                    visibilityOffset.z + pile[i][2] + jitterZ,
                    yaw,
                    90.0F,
                    0.0F,
                    0.48F,
                    ItemDisplayContext.GROUND,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    slot.index() * 10 + i
            );
        }
    }

    private void renderShelfItem(PrimitiveStorageCellarMultiblockEntity blockEntity,
                                 PrimitiveStorageCellarLayout.Slot slot,
                                 ItemStack stack,
                                 Direction formedFacing,
                                 double offsetX,
                                 double offsetY,
                                 double offsetZ,
                                 float yaw,
                                 float tilt,
                                 float roll,
                                 float scale,
                                 ItemDisplayContext displayContext,
                                 PoseStack poseStack,
                                 MultiBufferSource buffer,
                                 int packedLight,
                                 int packedOverlay,
                                 int renderSeed) {
        Vec3 canonical = slot.renderPosition().add(offsetX, offsetY, offsetZ);
        Vec3 renderPos = MultiblockHitHelper.fromCanonicalWest(canonical, formedFacing);

        poseStack.pushPose();
        poseStack.translate(renderPos.x, renderPos.y, renderPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(getFacingYaw(formedFacing) + yaw));
        if (tilt != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(tilt));
        }
        if (roll != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        }
        poseStack.scale(scale, scale, scale);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                displayContext,
                packedLight,
                packedOverlay,
                poseStack,
                buffer,
                blockEntity.getLevel(),
                renderSeed
        );

        poseStack.popPose();
    }

    private Vec3 getLowerCornerVisibilityOffset(PrimitiveStorageCellarLayout.Slot slot) {
        if (slot.shelf() != PrimitiveStorageCellarLayout.Shelf.LOWER || !isCornerSlot(slot)) {
            return Vec3.ZERO;
        }

        // Keep the lower displays on the shelf itself. Move the end slots
        // horizontally toward the visible corners/opening instead of lifting them.
        int indexInLevel = slot.index() % 16;

        return switch (slot.wall()) {
            case NORTH -> new Vec3(
                    indexInLevel == 0 ? -0.12D : 0.12D,
                    0.0D,
                    0.16D
            );
            case SOUTH -> new Vec3(
                    indexInLevel == 6 ? -0.12D : 0.12D,
                    0.0D,
                    -0.16D
            );
            case WEST -> new Vec3(
                    0.16D,
                    0.0D,
                    indexInLevel == 12 ? -0.10D : 0.10D
            );
        };
    }

    private boolean isCornerSlot(PrimitiveStorageCellarLayout.Slot slot) {
        int indexInLevel = slot.index() % 16;

        return switch (slot.wall()) {
            case NORTH -> indexInLevel == 0 || indexInLevel == 5;
            case SOUTH -> indexInLevel == 6 || indexInLevel == 11;
            case WEST -> indexInLevel == 12 || indexInLevel == 15;
        };
    }

    private long getVisualSeed(int slot, ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        long itemHash = itemId == null ? 0L : itemId.toString().hashCode();
        return slot * 73428767L ^ itemHash * 912931L;
    }

    private double signedUnit(long seed) {
        long mixed = seed;
        mixed ^= (mixed >>> 33);
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= (mixed >>> 33);
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= (mixed >>> 33);

        return ((mixed & 0xFFFFL) / 32767.5D) - 1.0D;
    }

    private float getFacingYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case EAST -> 180.0F;
            case SOUTH -> 270.0F;
            default -> 0.0F;
        };
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
