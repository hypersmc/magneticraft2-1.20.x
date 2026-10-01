package com.magneticraft2.client.render.blocks.stage.stone;

import com.magneticraft2.common.block.stage.stone.PrimitiveStorageCellarLayout;
import com.magneticraft2.common.blockentity.stage.stone.PrimitiveStorageCellarMultiblockEntity;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockHitHelper;
import com.magneticraft2.common.utils.MultiBlockProperties;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
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
            BakedModel itemModel = Minecraft.getInstance().getItemRenderer()
                    .getModel(stack, blockEntity.getLevel(), null, slot.index());

            // Do not classify by BlockItem: several flat items (redstone dust,
            // seeds, etc.) place blocks and therefore extend BlockItem. The baked
            // model tells us what we actually care about visually.
            if (itemModel.isGui3d()) {
                renderBlockPile(blockEntity, slot, stack, seed, formedFacing, poseStack, buffer, packedLight, packedOverlay);
            } else {
                renderLooseItems(blockEntity, slot, stack, seed, formedFacing, poseStack, buffer, packedLight, packedOverlay);
            }

            renderSlotDebugLabel(blockEntity, slot, formedFacing, poseStack, buffer, packedLight);
        }
    }

    private void renderSlotDebugLabel(PrimitiveStorageCellarMultiblockEntity blockEntity,
                                      PrimitiveStorageCellarLayout.Slot slot,
                                      Direction formedFacing,
                                      PoseStack poseStack,
                                      MultiBufferSource buffer,
                                      int packedLight) {
        if (!Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
            return;
        }

        Vec3 offset = getLowerCornerVisibilityOffset(slot).add(getPillarAvoidanceOffset(slot));
        Vec3 canonical = slot.renderPosition().add(offset);
        Vec3 renderPos = MultiblockHitHelper.fromCanonicalWest(canonical, formedFacing);

        String label = Integer.toString(slot.index());
        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        poseStack.translate(renderPos.x, renderPos.y + 0.18D, renderPos.z);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.0125F, -0.0125F, 0.0125F);

        float textX = -font.width(label) / 2.0F;
        font.drawInBatch(
                label,
                textX,
                0.0F,
                0xFFFFFF55,
                false,
                poseStack.last().pose(),
                buffer,
                Font.DisplayMode.SEE_THROUGH,
                0x60000000,
                packedLight
        );

        poseStack.popPose();
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
        Vec3 pillarOffset = getPillarAvoidanceOffset(slot);

        for (int i = 0; i < visibleBlocks; i++) {
            double jitterX = signedUnit(seed + i * 17L) * 0.025D;
            double jitterZ = signedUnit(seed + i * 31L) * 0.025D;
            float yaw = (float) (signedUnit(seed + i * 43L) * 7.0D);

            renderShelfItem(
                    blockEntity,
                    slot,
                    stack,
                    formedFacing,
                    visibilityOffset.x + pillarOffset.x + pile[i][0] + jitterX,
                    visibilityOffset.y + pillarOffset.y + pile[i][1],
                    visibilityOffset.z + pillarOffset.z + pile[i][2] + jitterZ,
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
        int visibleItems = stack.getCount() <= 21 ? 1
                : stack.getCount() <= 42 ? 2
                : stack.getCount() <= 63 ? 3
                : 4;

        Vec3 visibilityOffset = getLowerCornerVisibilityOffset(slot);
        Vec3 pillarOffset = getPillarAvoidanceOffset(slot);
        Vec3 naturalOffset = visibilityOffset.lengthSqr() > 0.0D
                ? Vec3.ZERO
                : getNaturalLooseItemOffset(slot, seed);

        double[][] pile = switch (visibleItems) {
            case 1 -> new double[][]{
                    { 0.000D, 0.000D,  0.000D, -8.0D}
            };
            case 2 -> new double[][]{
                    {-0.018D, 0.000D,  0.008D, -18.0D},
                    { 0.016D, 0.014D, -0.006D,  14.0D}
            };
            case 3 -> new double[][]{
                    {-0.020D, 0.000D,  0.010D, -20.0D},
                    { 0.018D, 0.012D, -0.006D,  10.0D},
                    { 0.000D, 0.026D,  0.000D,  24.0D}
            };
            default -> new double[][]{
                    {-0.022D, 0.000D,  0.010D, -22.0D},
                    { 0.018D, 0.010D, -0.008D,  12.0D},
                    {-0.006D, 0.024D, -0.004D, -6.0D},
                    { 0.012D, 0.038D,  0.006D,  26.0D}
            };
        };

        float slotYaw = getLooseItemSlotYaw(slot, seed);
        float looseScale = slot.wall() == PrimitiveStorageCellarLayout.Wall.WEST ? 0.36F : 0.40F;

        for (int i = 0; i < visibleItems; i++) {
            double tinyX = signedUnit(seed + i * 23L) * 0.003D;
            double tinyZ = signedUnit(seed + i * 37L) * 0.003D;
            float yaw = (float) (slotYaw + pile[i][3] + signedUnit(seed + i * 53L) * 3.0D);

            renderLooseShelfItemRaw(
                    blockEntity,
                    slot,
                    stack,
                    formedFacing,
                    visibilityOffset.x + pillarOffset.x + naturalOffset.x + pile[i][0] + tinyX,
                    visibilityOffset.y + pillarOffset.y + naturalOffset.y + pile[i][1],
                    visibilityOffset.z + pillarOffset.z + naturalOffset.z + pile[i][2] + tinyZ,
                    yaw,
                    looseScale,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    slot.index() * 10 + i
            );
        }
    }

    private Vec3 getNaturalLooseItemOffset(PrimitiveStorageCellarLayout.Slot slot, long seed) {
        if (slot.index() == 16 || slot.index() == 22
                || slot.index() == 32 || slot.index() == 38) {
            return Vec3.ZERO;
        }

        if (slot.wall() == PrimitiveStorageCellarLayout.Wall.WEST) {
            int centerIndex = (slot.index() % 16) - 12;
            double[] depth = {0.035D, -0.015D, 0.020D, -0.030D};
            double[] along = {-0.025D, 0.018D, -0.012D, 0.026D};
            int index = Math.max(0, Math.min(3, centerIndex));
            return new Vec3(
                    depth[index],
                    0.0D,
                    along[index] + signedUnit(seed + 101L) * 0.012D
            );
        }

        return switch (slot.wall()) {
            case NORTH -> new Vec3(
                    signedUnit(seed + 101L) * 0.018D,
                    0.0D,
                    -0.026D
            );
            case SOUTH -> new Vec3(
                    signedUnit(seed + 101L) * 0.018D,
                    0.0D,
                    0.026D
            );
            case WEST -> Vec3.ZERO;
        };
    }

    private float getLooseItemSlotYaw(PrimitiveStorageCellarLayout.Slot slot, long seed) {
        if (slot.wall() == PrimitiveStorageCellarLayout.Wall.WEST) {
            int centerIndex = Math.max(0, Math.min(3, (slot.index() % 16) - 12));
            float[] yaw = {-16.0F, 9.0F, -7.0F, 17.0F};
            return yaw[centerIndex];
        }

        return (float) (signedUnit(seed + 151L) * 9.0D);
    }

    private void renderLooseShelfItemRaw(PrimitiveStorageCellarMultiblockEntity blockEntity,
                                         PrimitiveStorageCellarLayout.Slot slot,
                                         ItemStack stack,
                                         Direction formedFacing,
                                         double offsetX,
                                         double offsetY,
                                         double offsetZ,
                                         float yaw,
                                         float scale,
                                         PoseStack poseStack,
                                         MultiBufferSource buffer,
                                         int packedLight,
                                         int packedOverlay,
                                         int renderSeed) {
        Vec3 canonical = slot.renderPosition().add(offsetX, offsetY, offsetZ);
        Vec3 renderPos = MultiblockHitHelper.fromCanonicalWest(canonical, formedFacing);

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, blockEntity.getLevel(), null, renderSeed);

        poseStack.pushPose();
        poseStack.translate(renderPos.x, renderPos.y, renderPos.z);

        // Redstone and other flat generated items are XY-plane models.
        // Lay that plane onto the horizontal shelf with X=90, then yaw it
        // around the shelf. NONE avoids GROUND's extra 0.5 model scale.
        poseStack.mulPose(Axis.YP.rotationDegrees(getFacingYaw(formedFacing) + yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(scale, scale, scale);

        itemRenderer.render(
                stack,
                ItemDisplayContext.NONE,
                false,
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                model
        );

        poseStack.popPose();
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

        // PoseStack post-multiplies rotations, so the call order is opposite to
        // the order experienced by the rendered vertices. Yaw must be composed
        // first here so the item is actually laid onto the shelf before its
        // horizontal yaw is applied.
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

    private Vec3 getPillarAvoidanceOffset(PrimitiveStorageCellarLayout.Slot slot) {
        int indexInLevel = slot.index() % 16;

        // Exact four confirmed in-game. Their base X is -1.80; move only
        // sideways toward the left timber post, keeping their current Z/depth.
        // Confirmed mirrored corner pairs. Keep X untouched so they cannot
        // overlap the neighboring long-shelf slots; move only along Z toward
        // the open side of their respective timber posts.
        if (slot.index() == 16 || slot.index() == 32) {
            return new Vec3(0.0D, 0.0D, 0.12D);
        }
        if (slot.index() == 22 || slot.index() == 38) {
            return new Vec3(0.0D, 0.0D, -0.12D);
        }

        // Preserve the working lower WEST shelf endpoint clearance.
        if (slot.wall() == PrimitiveStorageCellarLayout.Wall.WEST
                && slot.shelf() == PrimitiveStorageCellarLayout.Shelf.LOWER) {
            if (indexInLevel == 12) {
                return new Vec3(0.0D, 0.0D, 0.10D);
            }
            if (indexInLevel == 15) {
                return new Vec3(0.0D, 0.0D, -0.10D);
            }
        }

        return Vec3.ZERO;
    }

    private Vec3 getLowerCornerVisibilityOffset(PrimitiveStorageCellarLayout.Slot slot) {
        if (slot.shelf() != PrimitiveStorageCellarLayout.Shelf.LOWER) {
            return Vec3.ZERO;
        }

        int indexInLevel = slot.index() % 16;

        // Keep the two inner corner slots in their hand-tuned visible positions.
        if (slot.wall() == PrimitiveStorageCellarLayout.Wall.NORTH && indexInLevel == 0) {
            Vec3 current = slot.renderPosition();
            Vec3 target = new Vec3(-1.47D, current.y, -0.05D);
            return target.subtract(current);
        }

        if (slot.wall() == PrimitiveStorageCellarLayout.Wall.SOUTH && indexInLevel == 6) {
            Vec3 current = slot.renderPosition();
            Vec3 target = new Vec3(-1.47D, current.y, 1.05D);
            return target.subtract(current);
        }

        // The rest of the lower shelf is pulled toward the room/opening. This
        // keeps the items over the shelf surface while making them easier to see
        // and giving the player a clearer piece of shelf to click beneath them.
        return switch (slot.wall()) {
            case NORTH -> new Vec3(0.0D, 0.0D, 0.14D);
            case SOUTH -> new Vec3(0.0D, 0.0D, -0.14D);
            case WEST -> new Vec3(0.14D, 0.0D, 0.0D);
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
