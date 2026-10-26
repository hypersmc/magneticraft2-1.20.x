package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.RotaryOreClassifierBlock;
import com.magneticraft2.client.animation.AnimatedComponentRenderer;
import com.magneticraft2.client.animation.AnimationDefinition;
import com.magneticraft2.client.animation.AnimationFrame;
import com.magneticraft2.client.animation.AnimationResourceLoader;
import com.magneticraft2.common.blockentity.stage.bronze.RotaryOreClassifierBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalInputModuleBlockEntity;
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
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;

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

    // Renderer instances are shared by block type; state must belong to each
    // machine. Weak keys do not keep unloaded block entities alive.
    private final WeakHashMap<RotaryOreClassifierBlockEntity, RotaryClassifierOreMotion>
            oreStates = new WeakHashMap<>();

    private static final ResourceLocation ANIMATION =
            new ResourceLocation("magneticraft2", "rotary_ore_classifier");
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

    // These are the REAL port surfaces, not extra decorative inventories.
    // The output block's top is Y=10/16. Crushed ore has a 0.5 GROUND
    // transform with a +3px lift, but classified ore uses a 0.7 GROUND
    // transform with NO lift. Sharing the same origin buried all classified
    // pellets *below* the wooden module top, even when slot 1 was populated.
    private static final double INPUT_X = -0.5D;
    private static final double INPUT_Y = 1.97D;
    private static final double INPUT_Z = -2.5D;
    private static final double FINE_X = -0.5D;
    private static final double COARSE_X = 1.5D;
    // Fine: .82 BER scale * .7 GROUND; model tray at 1px -> Y~.639.
    private static final double FINE_OUTPUT_Y = 0.890D;
    // Coarse: .68 BER scale * .5 GROUND +3px lift -> Y~.641.
    private static final double OUTPUT_Y = 0.620D;
    private static final double OUTPUT_Z = 1.5D;
    private static final int MAX_DRUM_FRAGMENTS = 2;

    // Crushed ore's item models already have a GROUND-scale of 0.5.
    // The former 0.22/0.12 multiplier made chunks nearly invisible in
    // the 22px-diameter screen. Visual sizes do not alter recipe yields.
    private static final float INPUT_ITEM_SCALE = 0.68F;
    private static final float OUTPUT_ITEM_SCALE = 0.68F;
    // One classified item model already contains a full tray with several
    // small pellets; drawing a second one would overlap two solid trays.
    private static final float FINE_OUTPUT_ITEM_SCALE = 0.82F;
    private static final float FEED_ITEM_SCALE = 0.72F;
    private static final float DRUM_ITEM_SCALE = 0.82F;
    private static final float FINE_ITEM_SCALE = 0.38F;
    // Both the rotating model and contact simulation use the REAL Y=1.50 pivot.
    private static final double DRUM_AXIS_Y = RotaryClassifierOreMotion.AXIS_Y;

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
            oreStates.remove(classifier);
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
        // The drum's THREE discrete segments share one actual Gear V2 phase.
        // Each section keeps its original pivot and worn-mesh variant; the
        // stones/fines still use the existing specialized physics simulation.
        AnimationDefinition definition = AnimationResourceLoader.get(ANIMATION);
        AnimationDefinition drawn = null;
        if (definition != null
                && definition.component("drum_rear") != null
                && definition.component("drum_middle") != null
                && definition.component("drum_front") != null) {
            EnumSet<AnimationDefinition.State> states = EnumSet.of(
                    classifier.isProcessing()
                            ? AnimationDefinition.State.RUNNING
                            : AnimationDefinition.State.STOPPED);
            if (classifier.getPartWearStage() >= 2) {
                states.add(AnimationDefinition.State.WORN);
            }
            MechanicalInputModuleBlockEntity input = classifier.getMechanicalInput();
            if (input != null && input.isClientOverloaded()) {
                states.add(AnimationDefinition.State.OVERLOADED);
            }
            drawn = AnimatedComponentRenderer.render(ANIMATION,
                    new AnimationFrame(Map.of("drum", angle), states,
                            Map.of("classifier.wear_stage",
                                    Math.max(0, Math.min(5, classifier.getPartWearStage())))),
                    classifier.getBlockState(), pose, buffers, packedLight, packedOverlay);
        }
        if (drawn == null) {
            // Complete legacy model fallback when any JSON part/model fails.
            ResourceLocation[] models =
                    classifier.getPartWearStage() >= 2 ? WORN_DRUM : DRUM;
            for (int dz = -2; dz <= 0; dz++) {
                BakedModel model = Minecraft.getInstance().getModelManager()
                        .getModel(models[dz + 2]);
                pose.pushPose();
                pose.translate(0.5D, DRUM_AXIS_Y, dz + 0.5D);
                pose.mulPose(Axis.ZP.rotationDegrees(angle));
                pose.translate(-0.5D, -0.5D, -0.5D);
                drawBaked(model, pose, buffers, packedLight, packedOverlay);
                pose.popPose();
            }
        }

        ItemStack input = classifier.getInputStack();
        ItemStack fines = classifier.getOutputStack();
        ItemStack coarse = classifier.getByproductStack();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        if (!classifier.hasVisibleWorkpiece()) {
            oreStates.remove(classifier);
        }
        if (!input.isEmpty()) {
            // One model resolution per type per frame, not per fragment.
            BakedModel inputModel = itemRenderer.getModel(
                    input, classifier.getLevel(), null, 420);
            if (classifier.hasVisibleWorkpiece()) {
                float progress = classifier.getVisualProcessProgress(partialTicks);
                RotaryClassifierOreMotion motion = oreStates.computeIfAbsent(
                        classifier, ignored -> new RotaryClassifierOreMotion());
                motion.advance(classifier.getLevel().getGameTime(), progress,
                        angle, signedDriveDegreesPerTick(classifier),
                        classifier.isProcessing());
                drawScreeningOre(itemRenderer, input, inputModel, progress,
                        motion, partialTicks, pose, buffers, packedLight, packedOverlay);

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
            // Classified ore is much lower than crushed chunks in GROUND
            // space. Display its single built-in bed of fines on the actual
            // primary module instead of burying two overlapping bronze trays.
            drawItem(itemRenderer, fines, itemRenderer.getModel(
                            fines, classifier.getLevel(), null, 420),
                    pose, buffers, packedLight, packedOverlay,
                    FINE_X, FINE_OUTPUT_Y, OUTPUT_Z, FINE_OUTPUT_ITEM_SCALE, 0F);
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
     * Gear V2 drives BOTH the drum mesh and the material-contact simulation.
     * The authoritative rendered phase still comes from the existing smooth
     * Gear V2 interpolation; RPM gives deterministic motion per 20 Hz step.
     */
    private static float signedDriveDegreesPerTick(RotaryOreClassifierBlockEntity classifier) {
        if (!classifier.isProcessing()) {
            return 0F;
        }
        MechanicalInputModuleBlockEntity drive = classifier.getMechanicalInput();
        if (drive == null || drive.isClientOverloaded()
                || drive.getClientSpeed() <= 0.01F) {
            return 0F;
        }
        Direction facing = classifier.getFacing();
        float facingSign = facing == Direction.NORTH || facing == Direction.WEST ? -1F : 1F;
        return facingSign * drive.getClientSpeed() * (360F / 1200F)
                * drive.getDirectionMultiplier();
    }

    /**
     * Input slides through the real port into the drum. After that the rocks
     * move with the rotating octagon: wall contact + friction + gravity, not
     * a pair of sine waves or a progress-controlled X/Y path.
     *
     * The actual outputs are still shown ONLY from committed inventory slots.
     */
    private static void drawScreeningOre(ItemRenderer renderer, ItemStack input,
            BakedModel model, float progress, RotaryClassifierOreMotion motion,
            float partialTicks, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        progress = Mth.clamp(progress, 0F, 1F);

        if (progress < 0.20F) {
            float t = smoothstep(progress / 0.20F);
            double x = Mth.lerp(t, INPUT_X, 0.48D);
            double y = Mth.lerp(t * t, INPUT_Y, 0.86D);
            double z = Mth.lerp(t, INPUT_Z, -1.69D);
            drawItem(renderer, input, model, pose, buffers, light, overlay,
                    x, y, z, FEED_ITEM_SCALE, 0F);
            return;
        }

        for (int i = 0; i < MAX_DRUM_FRAGMENTS; i++) {
            RotaryClassifierOreMotion.Fragment rock = motion.fragment(i);
            if (!rock.active()) {
                continue;
            }
            float scale = DRUM_ITEM_SCALE - i * 0.055F;
            drawTumblingItem(renderer, input, model, pose, buffers, light, overlay,
                    0.5D + rock.x(partialTicks),
                    DRUM_AXIS_Y + rock.y(partialTicks),
                    rock.z(partialTicks), scale, (float) rock.roll(partialTicks));
        }

        // Fines originate at a lower-screen impact and drop into the REAL
        // internal pan. Only one visual chip can exist at a time.
        RotaryClassifierOreMotion.FineChip fine = motion.fine();
        if (fine.active()) {
            drawItem(renderer, input, model, pose, buffers, light, overlay,
                    0.5D + fine.x(), fine.y(partialTicks), fine.z(),
                    FINE_ITEM_SCALE, 0F);
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

    /**
     * Rotate each rock about its actual 3D model centre, not its GROUND item
     * origin. That prevents the old hovering/orbiting look while rolling.
     */
    private static void drawTumblingItem(ItemRenderer renderer, ItemStack stack,
            BakedModel model, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay, double x, double y, double z,
            float scale, float rollDegrees) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.ZP.rotationDegrees(rollDegrees));
        pose.translate(0D,
                -RotaryClassifierOreMotion.MODEL_CENTRE_AT_UNIT_SCALE * scale, 0D);
        pose.scale(scale, scale, scale);
        renderer.render(stack, ItemDisplayContext.GROUND, false,
                pose, buffers, light, overlay, model);
        pose.popPose();
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
