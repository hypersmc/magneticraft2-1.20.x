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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * An actual DRY classifier: material feeds from its real input port, tumbles
 * inside the shaft-driven mesh, drops fines through the screen onto the lower
 * pan and sends the coarse return toward the open end. Inventory transactions
 * still occur ONLY in the server-owned controller at batch completion.
 *
 * The static frame is provided by baked V2 models. Only the real screen drum
 * and a bounded number of display items are drawn by this BER; dust/grit and
 * quiet rattling are handled by the lightweight client ticker, never per frame.
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

    // True block-model surface heights: input 15/16 at Y=1, outputs 10/16
    // at Y=0. The extra ~0.03-0.04 is for the ItemDisplayContext.GROUND item.
    // These are module locations, NOT decorative chutes or fake inventories.
    private static final double INPUT_X = -0.5D;
    private static final double INPUT_Y = 1.97D;
    private static final double INPUT_Z = -2.5D;
    private static final double FINE_X = -0.5D;
    private static final double COARSE_X = 1.5D;
    private static final double OUTPUT_Y = 0.665D;
    private static final double OUTPUT_Z = 1.5D;
    private static final int MAX_DRUM_FRAGMENTS = 2;

    // Crushed ore's item models already have a GROUND-scale of 0.5.
    // The former 0.22/0.12 multiplier made chunks nearly invisible in
    // the 22px-diameter screen. Visual sizes do not alter recipe yields.
    private static final float INPUT_ITEM_SCALE = 0.68F;
    private static final float OUTPUT_ITEM_SCALE = 0.68F;
    private static final float FEED_ITEM_SCALE = 0.72F;
    private static final float DRUM_ITEM_SCALE = 0.82F;
    private static final float FINE_ITEM_SCALE = 0.38F;
    private static final float COARSE_ITEM_SCALE = 0.72F;
    // The drum is centered around Y=2.0 and the inner lower mesh is
    // ~Y=1.34. Keep visible chunks INSIDE at Y~1.50 instead of 1.04.
    private static final double SCREEN_ORE_REST_Y = 1.50D;

    public RotaryOreClassifierRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public void render(RotaryOreClassifierBlockEntity classifier,
            float partialTicks, PoseStack pose, MultiBufferSource buffers,
            int packedLight, int packedOverlay) {
        if (!classifier.isFormed() || classifier.getLevel() == null) {
            return;
        }

        Direction facing = classifier.getFacing();
        float yaw = switch (facing) {
            case WEST -> -90F;
            case NORTH -> 180F;
            case EAST -> 90F;
            default -> 0F;
        };
        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5D, 0D, -0.5D);

        // Gear V2 supplies the only real rotating phase: never spin from
        // world time. The shaft in the mechanical-input module is separate.
        float angle = classifier.getDrumRotationDegrees(partialTicks);
        ResourceLocation[] models =
                classifier.getPartWearStage() >= 2 ? WORN_DRUM : DRUM;
        for (int dz = -2; dz <= 0; dz++) {
            BakedModel model = Minecraft.getInstance().getModelManager()
                    .getModel(models[dz + 2]);
            pose.pushPose();
            pose.translate(0.5D, 1.5D, dz + 0.5D);
            pose.mulPose(Axis.ZP.rotationDegrees(angle));
            pose.translate(-0.5D, -0.5D, -0.5D);
            drawBaked(model, pose, buffers, packedLight, packedOverlay);
            pose.popPose();
        }

        ItemStack input = classifier.getInputStack();
        ItemStack fines = classifier.getOutputStack();
        ItemStack coarse = classifier.getByproductStack();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        if (!input.isEmpty()) {
            // One model resolution per type per frame, not per fragment.
            BakedModel inputModel = itemRenderer.getModel(
                    input, classifier.getLevel(), null, 420);
            if (classifier.hasVisibleWorkpiece()) {
                float progress = classifier.getVisualProcessProgress(partialTicks);
                drawScreeningOre(itemRenderer, input, inputModel, progress,
                        classifier.isProcessing() ? angle : 0F,
                        pose, buffers, packedLight, packedOverlay);

                // Input slot is consumed server-side only at completion.
                // If additional batches remain, leave the queued ore visibly
                // sitting ON the real input module throughout this batch.
                if (input.getCount() > classifier.getVisualBatchInputCount()) {
                    drawPortStack(itemRenderer, input, inputModel, pose, buffers,
                            packedLight, packedOverlay,
                            INPUT_X, INPUT_Y, INPUT_Z, INPUT_ITEM_SCALE);
                }
            } else {
                // No active batch (or no progress yet): the workpiece is on
                // the actual elevated input tray, rather than hovering
                // over a noninteractive model-only hopper.
                drawPortStack(itemRenderer, input, inputModel, pose, buffers,
                        packedLight, packedOverlay,
                        INPUT_X, INPUT_Y, INPUT_Z, INPUT_ITEM_SCALE);
            }
        }

        // Only completed and actually stored outputs appear on these ports.
        // A "future output" is never shown before the server commits it.
        if (!fines.isEmpty()) {
            drawPortStack(itemRenderer, fines, itemRenderer.getModel(
                            fines, classifier.getLevel(), null, 420),
                    pose, buffers, packedLight, packedOverlay,
                    FINE_X, OUTPUT_Y, OUTPUT_Z, OUTPUT_ITEM_SCALE);
        }
        if (!coarse.isEmpty()) {
            drawPortStack(itemRenderer, coarse, itemRenderer.getModel(
                            coarse, classifier.getLevel(), null, 420),
                    pose, buffers, packedLight, packedOverlay,
                    COARSE_X, OUTPUT_Y, OUTPUT_Z, OUTPUT_ITEM_SCALE);
        }
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
     * A visibly staged, deterministic ore path. The source is the real input
     * stack; fragment rendering never extracts, creates or reserves items.
     * If the batch is stalled, the server-synced progress freezes this path.
     *
     *  0–20% : feed from rear-left input through the small inlet throat
     * 20–82% : fragments roll along the inside BOTTOM of the screen
     * 36–92% : occasional tiny fines fall through the mesh onto the pan
     * 80–100%: coarse sample advances to the open discharge end
     */
    private static void drawScreeningOre(ItemRenderer renderer, ItemStack input,
            BakedModel model, float progress, float angle,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        progress = Mth.clamp(progress, 0F, 1F);

        if (progress < 0.20F) {
            float t = smoothstep(progress / 0.20F);
            // Travel diagonally from the genuine rear-left Input module
            // into the open rear mouth, NOT through the drive bearing.
            double x = Mth.lerp(t, INPUT_X, 0.48D);
            double y = Mth.lerp(t, INPUT_Y, SCREEN_ORE_REST_Y + 0.04D);
            double z = Mth.lerp(t, INPUT_Z, -1.69D);
            drawItem(renderer, input, model, pose, buffers, light, overlay,
                    x, y, z, FEED_ITEM_SCALE, 0F);
        } else {
            float advance = Mth.clamp((progress - 0.20F) / 0.76F, 0F, 1F);
            double radians = Math.toRadians(angle);
            for (int i = 0; i < MAX_DRUM_FRAGMENTS; i++) {
                float delay = i * 0.12F;
                if (advance < delay) {
                    continue;
                }
                float travel = smoothstep((advance - delay) / (1F - delay));
                // Stay within the drum's ~0.66-block inner radius, but
                // above the lower screening mesh, not underneath it.
                double x = 0.5D + 0.15D * Math.sin(radians * 0.45D + i * 2.3D);
                double y = SCREEN_ORE_REST_Y + 0.045D * Math.abs(
                        Math.cos(radians * 0.55D + i * 1.7D));
                double z = -1.71D + 2.28D * travel;
                drawItem(renderer, input, model, pose, buffers, light, overlay,
                        x, y, z, DRUM_ITEM_SCALE - i * 0.055F, angle * 0.13F + i * 55F);
            }
        }

        // A SMALL piece of the current ore falls from mesh to pan. Keep this
        // separate from committed output items, and omit it while stalled:
        // stopped machinery should not drop material or emit particles.
        if (progress >= 0.36F && progress < 0.92F && angle != 0F) {
            float cycle = (progress - 0.36F) * 7.0F;
            float fall = cycle - (float) Math.floor(cycle);
            double x = 0.39D;
            double y = SCREEN_ORE_REST_Y - 0.85D * smoothstep(fall);
            double z = -1.36D + (progress - 0.36F) * 2.3D;
            drawItem(renderer, input, model, pose, buffers, light, overlay,
                    x, y, z, FINE_ITEM_SCALE, angle * 0.10F);
        }

        if (progress >= 0.80F) {
            // Coarse return reaches the front mouth, but the actual output
            // tray stays empty until the controller completes this batch.
            float t = smoothstep((progress - 0.80F) / 0.20F);
            drawItem(renderer, input, model, pose, buffers, light, overlay,
                    0.65D + 0.09D * t, SCREEN_ORE_REST_Y + 0.02D,
                    0.34D + 0.48D * t, COARSE_ITEM_SCALE, angle * 0.11F);
        }
    }

    private static float smoothstep(float value) {
        float t = Mth.clamp(value, 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    /**
     * Completed inventory sits directly on the TRUE physical port's
     * visible top. Bigger stacks show two small pieces, not a hovering
     * pile or a separate model-only catch bin.
     */
    private static void drawPortStack(ItemRenderer renderer, ItemStack stack,
            BakedModel model, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay, double x, double y, double z, float scale) {
        drawItem(renderer, stack, model, pose, buffers, light, overlay,
                x - 0.14D, y, z + 0.10D, scale, 12F);
        if (stack.getCount() > 1) {
            drawItem(renderer, stack, model, pose, buffers, light, overlay,
                    x + 0.14D, y + 0.015D, z - 0.09D,
                    scale * 0.88F, -21F);
        }
    }

    private static void drawItem(ItemRenderer renderer, ItemStack stack,
            BakedModel model, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay, double x, double y, double z,
            float scale, float spinDegrees) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(spinDegrees));
        pose.scale(scale, scale, scale);
        renderer.render(stack, ItemDisplayContext.GROUND, false,
                pose, buffers, light, overlay, model);
        pose.popPose();
    }
}
