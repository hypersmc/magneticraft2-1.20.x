package com.magneticraft2.client.render.blocks.stage.stone;

import com.magneticraft2.common.block.stage.stone.PrimitiveFurnaceMultiblock_nogui;
import com.magneticraft2.common.blockentity.stage.stone.PrimitiveFurnaceMultiblockEntity_nogui;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockHitHelper;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import com.magneticraft2.common.utils.MultiBlockProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 14-11-2024
 * @Project mgc2-1.20
* @version 1.0.0
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

        if (pBlockEntity.isFormed()) {
            renderInputItem(pBlockEntity, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
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

        // Going to have more if cases now since this is GUIless

        if (pBlockEntity.isFormed()) {
            renderOutputItems(pBlockEntity, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
        }

        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get() && pBlockEntity.isFormed()) {
            renderInteractionZones(pBlockEntity, pPoseStack, pBuffer, pPackedLight);
        }
    }

    private void renderInputItem(PrimitiveFurnaceMultiblockEntity_nogui blockEntity,
                                 PoseStack poseStack,
                                 MultiBufferSource buffer,
                                 int packedLight,
                                 int packedOverlay) {
        ItemStack input = blockEntity.getItemInSlot(0);
        if (input.isEmpty()) {
            return;
        }

        Direction formedFacing = PrimitiveFurnaceMultiblock_nogui.getFormedFacing(
                blockEntity,
                blockEntity.getBlockState()
        );

        PrimitiveFurnaceMultiblock_nogui.FurnaceZoneBox inputZone =
                getZoneBox(PrimitiveFurnaceMultiblock_nogui.FurnaceZone.SMELTABLE_INPUT);
        if (inputZone == null) {
            return;
        }

        Vec3 center = inputZone.bounds().getCenter();

        // Keep the input low enough that the coal model, rendered immediately
        // after this, visually surrounds and partially covers the item.
        Vec3 renderPos = MultiblockHitHelper.fromCanonicalWest(
                new Vec3(center.x, 0.11D, center.z),
                formedFacing
        );

        renderInventoryItem(
                blockEntity,
                input,
                renderPos,
                formedFacing,
                4.0F,
                0.38F,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                100
        );
    }

    private void renderOutputItems(PrimitiveFurnaceMultiblockEntity_nogui blockEntity,
                                   PoseStack poseStack,
                                   MultiBufferSource buffer,
                                   int packedLight,
                                   int packedOverlay) {
        Direction formedFacing = PrimitiveFurnaceMultiblock_nogui.getFormedFacing(
                blockEntity,
                blockEntity.getBlockState()
        );

        renderOutputItem(
                blockEntity,
                2,
                PrimitiveFurnaceMultiblock_nogui.FurnaceZone.PRIMARY_OUTPUT,
                formedFacing,
                -10.0F,
                200,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderOutputItem(
                blockEntity,
                3,
                PrimitiveFurnaceMultiblock_nogui.FurnaceZone.SECONDARY_OUTPUT,
                formedFacing,
                11.0F,
                300,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
    }

    private void renderOutputItem(PrimitiveFurnaceMultiblockEntity_nogui blockEntity,
                                  int slot,
                                  PrimitiveFurnaceMultiblock_nogui.FurnaceZone zone,
                                  Direction formedFacing,
                                  float extraYaw,
                                  int seed,
                                  PoseStack poseStack,
                                  MultiBufferSource buffer,
                                  int packedLight,
                                  int packedOverlay) {
        ItemStack stack = blockEntity.getItemInSlot(slot);
        if (stack.isEmpty()) {
            return;
        }

        PrimitiveFurnaceMultiblock_nogui.FurnaceZoneBox zoneBox = getZoneBox(zone);
        if (zoneBox == null) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel itemModel = itemRenderer.getModel(
                stack,
                blockEntity.getLevel(),
                null,
                seed
        );

        Vec3 center = zoneBox.bounds().getCenter();

        if (itemModel.isGui3d()) {
            Vec3 renderPos = MultiblockHitHelper.fromCanonicalWest(
                    new Vec3(center.x, 0.20D, center.z),
                    formedFacing
            );

            renderBlockOutput(
                    blockEntity,
                    stack,
                    renderPos,
                    formedFacing,
                    extraYaw,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    seed
            );
        } else {
            Vec3 renderPos = MultiblockHitHelper.fromCanonicalWest(
                    new Vec3(center.x, 0.145D, center.z),
                    formedFacing
            );

            renderFlatOutput(
                    blockEntity,
                    stack,
                    itemModel,
                    renderPos,
                    formedFacing,
                    extraYaw,
                    poseStack,
                    buffer,
                    packedLight,
                    seed
            );
        }
    }

    private void renderBlockOutput(PrimitiveFurnaceMultiblockEntity_nogui blockEntity,
                                   ItemStack stack,
                                   Vec3 renderPos,
                                   Direction formedFacing,
                                   float extraYaw,
                                   PoseStack poseStack,
                                   MultiBufferSource buffer,
                                   int packedLight,
                                   int packedOverlay,
                                   int seed) {
        poseStack.pushPose();
        poseStack.translate(renderPos.x, renderPos.y, renderPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(facingYaw(formedFacing) + extraYaw));
        poseStack.scale(0.52F, 0.52F, 0.52F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                buffer,
                blockEntity.getLevel(),
                seed
        );

        poseStack.popPose();
    }

    private void renderFlatOutput(PrimitiveFurnaceMultiblockEntity_nogui blockEntity,
                                  ItemStack stack,
                                  BakedModel itemModel,
                                  Vec3 renderPos,
                                  Direction formedFacing,
                                  float extraYaw,
                                  PoseStack poseStack,
                                  MultiBufferSource buffer,
                                  int packedLight,
                                  int seed) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        poseStack.pushPose();
        poseStack.translate(renderPos.x, renderPos.y, renderPos.z);

        // Match the Storage Cellar's flat-item path: generated/2D item models
        // live in the XY plane, so explicitly lay them onto the furnace shelf.
        // NONE avoids the additional GROUND transform/scale.
        poseStack.mulPose(Axis.YP.rotationDegrees(facingYaw(formedFacing) + extraYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.40F, 0.40F, 0.40F);

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

    private void renderInventoryItem(PrimitiveFurnaceMultiblockEntity_nogui blockEntity,
                                     ItemStack stack,
                                     Vec3 renderPos,
                                     Direction formedFacing,
                                     float extraYaw,
                                     float scale,
                                     PoseStack poseStack,
                                     MultiBufferSource buffer,
                                     int packedLight,
                                     int packedOverlay,
                                     int seed) {
        poseStack.pushPose();
        poseStack.translate(renderPos.x, renderPos.y, renderPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(facingYaw(formedFacing) + extraYaw));
        poseStack.scale(scale, scale, scale);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                packedLight,
                packedOverlay,
                poseStack,
                buffer,
                blockEntity.getLevel(),
                seed
        );

        poseStack.popPose();
    }

    private PrimitiveFurnaceMultiblock_nogui.FurnaceZoneBox getZoneBox(
            PrimitiveFurnaceMultiblock_nogui.FurnaceZone zone) {
        for (PrimitiveFurnaceMultiblock_nogui.FurnaceZoneBox zoneBox
                : PrimitiveFurnaceMultiblock_nogui.getInteractionZoneBoxes()) {
            if (zoneBox.zone() == zone) {
                return zoneBox;
            }
        }
        return null;
    }

    private float facingYaw(Direction formedFacing) {
        return switch (formedFacing) {
            case WEST -> 0.0F;
            case NORTH -> 90.0F;
            case EAST -> 180.0F;
            case SOUTH -> -90.0F;
            default -> 0.0F;
        };
    }

    private void renderInteractionZones(PrimitiveFurnaceMultiblockEntity_nogui blockEntity,
                                        PoseStack poseStack,
                                        MultiBufferSource buffer,
                                        int packedLight) {
        Direction formedFacing = PrimitiveFurnaceMultiblock_nogui.getFormedFacing(
                blockEntity,
                blockEntity.getBlockState()
        );

        Map<PrimitiveFurnaceMultiblock_nogui.FurnaceZone, AABB> labelBounds =
                new EnumMap<>(PrimitiveFurnaceMultiblock_nogui.FurnaceZone.class);

        for (PrimitiveFurnaceMultiblock_nogui.FurnaceZoneBox zoneBox
                : PrimitiveFurnaceMultiblock_nogui.getInteractionZoneBoxes()) {
            AABB renderBox = transformCanonicalBox(zoneBox.bounds(), formedFacing).inflate(0.003D);
            float[] color = zoneColor(zoneBox.zone());

            LevelRenderer.renderLineBox(
                    poseStack,
                    buffer.getBuffer(RenderType.LINES),
                    renderBox,
                    color[0],
                    color[1],
                    color[2],
                    0.95F
            );

            labelBounds.merge(zoneBox.zone(), renderBox, AABB::minmax);
        }

        for (Map.Entry<PrimitiveFurnaceMultiblock_nogui.FurnaceZone, AABB> entry : labelBounds.entrySet()) {
            renderZoneLabel(entry.getKey(), entry.getValue().getCenter(), poseStack, buffer, packedLight);
        }
    }

    private AABB transformCanonicalBox(AABB canonicalBox, Direction formedFacing) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        double[] xs = {canonicalBox.minX, canonicalBox.maxX};
        double[] ys = {canonicalBox.minY, canonicalBox.maxY};
        double[] zs = {canonicalBox.minZ, canonicalBox.maxZ};

        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    Vec3 transformed = MultiblockHitHelper.fromCanonicalWest(
                            new Vec3(x, y, z),
                            formedFacing
                    );

                    minX = Math.min(minX, transformed.x);
                    minY = Math.min(minY, transformed.y);
                    minZ = Math.min(minZ, transformed.z);
                    maxX = Math.max(maxX, transformed.x);
                    maxY = Math.max(maxY, transformed.y);
                    maxZ = Math.max(maxZ, transformed.z);
                }
            }
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void renderZoneLabel(PrimitiveFurnaceMultiblock_nogui.FurnaceZone zone,
                                 Vec3 renderPos,
                                 PoseStack poseStack,
                                 MultiBufferSource buffer,
                                 int packedLight) {
        String label = zone.getDebugLabel();
        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        poseStack.translate(renderPos.x, renderPos.y + 0.04D, renderPos.z);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.011F, -0.011F, 0.011F);

        font.drawInBatch(
                label,
                -font.width(label) / 2.0F,
                0.0F,
                zoneTextColor(zone),
                false,
                poseStack.last().pose(),
                buffer,
                Font.DisplayMode.SEE_THROUGH,
                0x70000000,
                packedLight
        );

        poseStack.popPose();
    }

    private float[] zoneColor(PrimitiveFurnaceMultiblock_nogui.FurnaceZone zone) {
        return switch (zone) {
            case SMELTABLE_INPUT -> new float[]{0.25F, 0.60F, 1.00F};
            case FUEL_INPUT -> new float[]{1.00F, 0.55F, 0.15F};
            case PRIMARY_OUTPUT -> new float[]{0.30F, 1.00F, 0.30F};
            case SECONDARY_OUTPUT -> new float[]{1.00F, 0.30F, 1.00F};
            case NONE -> new float[]{1.00F, 1.00F, 1.00F};
        };
    }

    private int zoneTextColor(PrimitiveFurnaceMultiblock_nogui.FurnaceZone zone) {
        return switch (zone) {
            case SMELTABLE_INPUT -> 0xFF66AAFF;
            case FUEL_INPUT -> 0xFFFF9933;
            case PRIMARY_OUTPUT -> 0xFF66FF66;
            case SECONDARY_OUTPUT -> 0xFFFF66FF;
            case NONE -> 0xFFFFFFFF;
        };
    }
}
