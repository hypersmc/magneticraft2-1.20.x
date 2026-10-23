package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.RotaryOreClassifierBlock;
import com.magneticraft2.common.blockentity.stage.bronze.RotaryOreClassifierBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Gear V2-driven, three-segment large rotating trommel screen. The compact
 * timber/bronze frame and internal screening pan are baked V2 parts, not BER geometry.
 *
 * Only the enlarged drum and inventory previews are rendered here.
 * Screen wear is shown on the screen, never as a whole-machine tint.
 */
public final class RotaryOreClassifierRenderer
        implements BlockEntityRenderer<RotaryOreClassifierBlockEntity> {
    private static final ResourceLocation[] DRUM = {
            new ResourceLocation("magneticraft2", "multiblock/rotary_ore_classifier_drum_rear"),
            new ResourceLocation("magneticraft2", "multiblock/rotary_ore_classifier_drum_middle"),
            new ResourceLocation("magneticraft2", "multiblock/rotary_ore_classifier_drum_front")
    };
    private static final ResourceLocation[] WORN_DRUM = {
            new ResourceLocation("magneticraft2", "multiblock/rotary_ore_classifier_drum_rear_worn"),
            new ResourceLocation("magneticraft2", "multiblock/rotary_ore_classifier_drum_middle_worn"),
            new ResourceLocation("magneticraft2", "multiblock/rotary_ore_classifier_drum_front_worn")
    };

    public RotaryOreClassifierRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public int getViewDistance() { return 64; }

    @Override
    public void render(RotaryOreClassifierBlockEntity classifier,
            float partialTicks, PoseStack pose, MultiBufferSource buffers,
            int packedLight, int packedOverlay) {
        if (!classifier.isFormed() || classifier.getLevel() == null) return;

        Direction facing = classifier.getBlockState().getValue(RotaryOreClassifierBlock.FACING);
        float yaw = switch (facing) {
            case WEST -> -90F;
            case NORTH -> 180F;
            case EAST -> 90F;
            default -> 0F;
        };
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-.5D, 0D, -.5D);

        // The elevated rear Gear V2 module is at (0,1,-3), on the SAME
        // longitudinal Z axis. Its normal rotor remains independently drawn
        // by the module BER, so no fake shaft is inserted through its cell.
        float angle = classifier.getDrumRotationDegrees(partialTicks);
        // Three large, open octagonal mesh sections. Each has its own cast
        // rim, with open feed/discharge ends and uninterrupted 16px UV tiles.
        // Only the drum moves: the frame and internal collection pan are V2 baked.
        ResourceLocation[] drum = classifier.getPartWearStage() >= 2 ? WORN_DRUM : DRUM;
        for (int dz = -2; dz <= 0; dz++) {
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(drum[dz + 2]);
            pose.pushPose();
            pose.translate(.5D, 1.5D, dz + .5D);
            pose.mulPose(Axis.ZP.rotationDegrees(angle));
            pose.translate(-.5D, -.5D, -.5D);
            drawBaked(model, pose, buffers, packedLight, packedOverlay);
            pose.popPose();
        }

        // Client-only ore visuals. The real item/recipe state is owned
        // exclusively by the server; no invisible phantom inventories.
        ItemStack input = classifier.getInputStack();
        ItemStack primary = classifier.getOutputStack();
        ItemStack coarse = classifier.getByproductStack();
        if (!input.isEmpty()) {
            if (classifier.isProcessing()) {
                drawScreeningOre(classifier, input, angle, partialTicks,
                        pose, buffers, packedLight, packedOverlay);
            } else {
                // Waiting material is displayed on the REAL elevated item input port.
                drawItem(classifier, input, pose, buffers,
                        packedLight, packedOverlay, -.5D, 2.05D, -2.5D, .35F);
            }
        }
        if (!primary.isEmpty()) drawItem(classifier, primary, pose, buffers,
                packedLight, packedOverlay, -.5D, .76D, 1.5D, .34F);
        if (!coarse.isEmpty()) drawItem(classifier, coarse, pose, buffers,
                packedLight, packedOverlay, 1.5D, .76D, 1.5D, .34F);
        pose.popPose();
    }

    private static void drawBaked(BakedModel model, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        dispatcher.getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.cutout()),
                BlockRegistry.ROTARY_ORE_CLASSIFIER.get().defaultBlockState(),
                model, 1F, 1F, 1F, light, overlay);
    }

    /**
     * Two visible pieces tumble across the BOTTOM of the open screen as
     * classification advances from the rear inlet to the front discharge.
     * No item entity, texture/geometry bake, particle or inventory write
     * happens here. Stopped machines never execute the moving preview.
     */
    private static void drawScreeningOre(RotaryOreClassifierBlockEntity owner,
            ItemStack stack, float degrees, float partialTicks,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = renderer.getModel(stack, owner.getLevel(), null, 420);
        float progress = owner.getVisualProcessProgress(partialTicks);
        double radians = Math.toRadians(degrees);
        for (int n = 0; n < 2; n++) {
            double z = -1.85D + n * 0.4D + 2.15D * progress;
            double x = .5D + .22D * Math.sin(radians + n * 2.2D);
            double y = 1.10D + .045D * Math.cos(radians + n * 2.2D);
            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.YP.rotationDegrees((float) (n * 34.0D + degrees * .07D)));
            pose.scale(.24F, .24F, .24F);
            renderer.render(stack, ItemDisplayContext.GROUND, false,
                    pose, buffers, light, overlay, model);
            pose.popPose();
        }
    }

    private static void drawItem(RotaryOreClassifierBlockEntity owner,
            ItemStack stack, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay, double x, double y, double z, float scale) {
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(scale, scale, scale);
        BakedModel model = renderer.getModel(stack, owner.getLevel(), null, 420);
        renderer.render(stack, ItemDisplayContext.GROUND, false,
                pose, buffers, light, overlay, model);
        pose.popPose();
    }
}
