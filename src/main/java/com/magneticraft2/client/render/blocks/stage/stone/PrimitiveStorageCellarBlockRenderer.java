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
        int visibleItems = stack.getCount() >= 40 ? 3 : stack.getCount() >= 16 ? 2 : 1;
        Vec3 visibilityOffset = getLowerCornerVisibilityOffset(slot);
        LeanSupport support = findAdjacentBlockSupport(blockEntity, slot, seed);

        for (int i = 0; i < visibleItems; i++) {
            double offsetX = visibilityOffset.x + signedUnit(seed + i * 23L) * 0.055D;
            double offsetZ = visibilityOffset.z + signedUnit(seed + i * 37L) * 0.045D;
            double offsetY = visibilityOffset.y + i * 0.014D;

            float yaw = (float) (signedUnit(seed + i * 53L) * 16.0D);
            float tilt = 88.0F;
            float roll = (float) (signedUnit(seed + i * 71L) * 6.0D);

            if (support != null) {
                // Move the loose item toward the neighboring block pile and visibly
                // prop it against that pile instead of giving it a generic random pose.
                offsetX += support.direction().x * 0.13D;
                offsetZ += support.direction().z * 0.13D;
                offsetY += 0.035D;
                tilt = 58.0F;
                roll = support.sideSign() * 22.0F;
                yaw += support.yawOffset();
            } else if ((seed + i) % 3L == 0L) {
                // A minority of unsupported items sit at a shallow angle, but most
                // lie flat on the shelf.
                tilt = 78.0F;
            }

            renderShelfItem(
                    blockEntity,
                    slot,
                    stack,
                    formedFacing,
                    offsetX,
                    offsetY,
                    offsetZ,
                    yaw,
                    tilt,
                    roll,
                    0.34F,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    slot.index() * 10 + i
            );
        }
    }

    private LeanSupport findAdjacentBlockSupport(PrimitiveStorageCellarMultiblockEntity blockEntity,
                                                 PrimitiveStorageCellarLayout.Slot slot,
                                                 long seed) {
        PrimitiveStorageCellarLayout.Slot previous = getAdjacentSlot(slot, -1);
        PrimitiveStorageCellarLayout.Slot next = getAdjacentSlot(slot, 1);

        boolean previousIsBlock = previous != null
                && blockEntity.itemHandler.getStackInSlot(previous.index()).getItem() instanceof BlockItem;
        boolean nextIsBlock = next != null
                && blockEntity.itemHandler.getStackInSlot(next.index()).getItem() instanceof BlockItem;

        if (!previousIsBlock && !nextIsBlock) {
            return null;
        }

        PrimitiveStorageCellarLayout.Slot support;
        int sideSign;
        if (previousIsBlock && nextIsBlock) {
            boolean usePrevious = (seed & 1L) == 0L;
            support = usePrevious ? previous : next;
            sideSign = usePrevious ? -1 : 1;
        } else {
            support = previousIsBlock ? previous : next;
            sideSign = previousIsBlock ? -1 : 1;
        }

        Vec3 from = slot.renderPosition();
        Vec3 to = support.renderPosition();
        Vec3 direction = new Vec3(to.x - from.x, 0.0D, to.z - from.z).normalize();

        float yawOffset = slot.wall() == PrimitiveStorageCellarLayout.Wall.WEST
                ? sideSign * 18.0F
                : sideSign * 12.0F;

        return new LeanSupport(direction, sideSign, yawOffset);
    }

    private PrimitiveStorageCellarLayout.Slot getAdjacentSlot(PrimitiveStorageCellarLayout.Slot slot, int direction) {
        int candidateIndex = slot.index() + direction;
        if (candidateIndex < 0 || candidateIndex >= PrimitiveStorageCellarLayout.SLOT_COUNT) {
            return null;
        }

        PrimitiveStorageCellarLayout.Slot candidate = PrimitiveStorageCellarLayout.slots().get(candidateIndex);
        if (candidate.wall() != slot.wall() || candidate.shelf() != slot.shelf()) {
            return null;
        }

        return candidate;
    }

    private record LeanSupport(Vec3 direction, int sideSign, float yawOffset) {
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
                ItemDisplayContext.FIXED,
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

        // Lower corner displays disappear behind the shelf lip/pillars at a normal
        // standing camera height. Lift them clearly above the lip, pull them into
        // the room, and nudge them slightly away from the outer corner.
        Vec3 base = switch (slot.wall()) {
            case NORTH -> new Vec3(0.0D, 0.22D, 0.20D);
            case SOUTH -> new Vec3(0.0D, 0.22D, -0.20D);
            case WEST -> new Vec3(0.20D, 0.22D, 0.0D);
        };

        int indexInLevel = slot.index() % 16;
        double alongWall = switch (slot.wall()) {
            case NORTH -> indexInLevel == 0 ? 0.11D : indexInLevel == 5 ? -0.11D : 0.0D;
            case SOUTH -> indexInLevel == 6 ? 0.11D : indexInLevel == 11 ? -0.11D : 0.0D;
            case WEST -> indexInLevel == 12 ? 0.11D : indexInLevel == 15 ? -0.11D : 0.0D;
        };

        return switch (slot.wall()) {
            case NORTH, SOUTH -> base.add(alongWall, 0.0D, 0.0D);
            case WEST -> base.add(0.0D, 0.0D, alongWall);
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
