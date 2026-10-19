package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.blockentity.stage.bronze.BronzeStampMillBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Feed/crushing/discharge VISUALS for the Bronze Stamp Mill.
 *
 * Physical ports already forward to inventory slots 0/1/2, and the JSON
 * V2 machine already has three chute channels (-8, 8, 24 model pixels).
 * This renderer makes that existing material path visible. It never owns
 * items or changes recipes, inventory, processing time or Gear V2.
 *
 * The BE sends slot changes with its normal update packet. That means
 * manual input, hoppers and Mechanical Arms all use the same animation;
 * nothing is inferred from player-only click events.
 */
final class BronzeStampMillOreVisuals {
    // One short feed along the left ore chute from the real item-input
    // module at (-8,8,24) to the first hammer at (-8,24,-10).
    static final double FEED_TICKS = 24.0D;
    static final double DISCHARGE_TICKS = 20.0D;
    private static final double FEED_SPACING_TICKS = 4.0D;
    private static final int MAX_FEED_ITEMS = 3;
    private static final double LOW_HAMMER_THRESHOLD = 0.045D;

    private final Map<BronzeStampMillBlockEntity, VisualState> states =
            new WeakHashMap<>();

    private static final class MovingOre {
        private final ItemStack item;
        private final double started;

        private MovingOre(ItemStack stack, double tick) {
            this.item = stack.copy();
            this.item.setCount(1);
            this.started = tick;
        }
    }

    private static final class VisualState {
        private boolean initialized;
        private ItemStack oldInput = ItemStack.EMPTY;
        private ItemStack oldPrimary = ItemStack.EMPTY;
        private ItemStack oldByproduct = ItemStack.EMPTY;
        private ItemStack crushedPreview = ItemStack.EMPTY;
        private final List<MovingOre> feeding = new ArrayList<>(MAX_FEED_ITEMS);
        private MovingOre primaryDischarge;
        private MovingOre byproductDischarge;
        private final float[] oldLift = {Float.NaN, Float.NaN, Float.NaN};
        private final double[] lastDustTick = {-100D, -100D, -100D};
    }

    void render(BronzeStampMillBlockEntity mill, float partial,
                PoseStack pose, MultiBufferSource buffers,
                int light, int overlay) {
        if (mill.getLevel() == null) return;
        double now = mill.getLevel().getGameTime() + (double) partial;
        VisualState state = states.computeIfAbsent(mill, unused -> new VisualState());
        ItemStack input = mill.getInputStack();
        ItemStack primary = mill.getOutputStack();
        ItemStack byproduct = mill.getByproductStack();
        observeInventory(mill, state, input, primary, byproduct, now);
        state.feeding.removeIf(entry -> now - entry.started > FEED_TICKS);
        if (input.isEmpty() && !mill.isProcessing()) {
            state.feeding.clear();
        }

        // A freshly inserted stack is seen at the port and travels along
        // the ALREADY present left loading chute, rather than materializing
        // over the crushing tray during the very first rendered frame.
        for (MovingOre entry : state.feeding) {
            double elapsed = now - entry.started;
            if (elapsed < 0.0D || elapsed > FEED_TICKS) continue;
            double t = clamp(elapsed / FEED_TICKS);
            Vec3 point = inputFeedPoint(t);
            drawItem(mill, pose, buffers, light, overlay, entry.item,
                    point.x, point.y, point.z, 0.38F, 0F);
        }

        float progress = mill.getVisualProcessProgress(partial);
        // Do not conjure a second copy inside the tray while the first
        // visible ore piece is still travelling in from the real port.
        if (!input.isEmpty()
                && (state.feeding.isEmpty() || progress > 0.16F)) {
            renderCrushing(mill, partial, pose, buffers, light, overlay,
                    input, state.crushedPreview, progress);
        }

        renderOutput(mill, pose, buffers, light, overlay, primary,
                state.primaryDischarge, now, 24D);
        renderOutput(mill, pose, buffers, light, overlay, byproduct,
                state.byproductDischarge, now, 8D);

        emitImpactDust(mill, partial, state, now, input, progress);
    }

    private static void observeInventory(BronzeStampMillBlockEntity mill,
                                         VisualState visual, ItemStack input,
                                         ItemStack primary, ItemStack byproduct,
                                         double now) {
        // Expensive recipe lookup happens only when the synced input stack
        // changes, not once per rendered frame for every ore fragment.
        boolean changedInput = !sameVisualStack(visual.oldInput, input);
        if (changedInput || !visual.initialized)
            visual.crushedPreview = mill.getProcessingPrimaryPreview();
        if (!visual.initialized) {
            visual.initialized = true;
            // Even when an item is loaded before the first rendered frame,
            // show its path from the input, not a sudden central apparition.
            if (!input.isEmpty()) {
                visual.feeding.add(new MovingOre(input, now));
            }
        } else {
            int incoming = addedItems(visual.oldInput, input);
            if (incoming > 0) {
                if (differentType(visual.oldInput, input)) {
                    visual.feeding.clear();
                }
                for (int n = 0; n < Math.min(incoming, 2)
                        && visual.feeding.size() < MAX_FEED_ITEMS; n++) {
                    visual.feeding.add(
                            new MovingOre(input, now + n * FEED_SPACING_TICKS));
                }
            }
            if (addedItems(visual.oldPrimary, primary) > 0) {
                visual.primaryDischarge = new MovingOre(primary, now);
            }
            if (addedItems(visual.oldByproduct, byproduct) > 0) {
                visual.byproductDischarge = new MovingOre(byproduct, now);
            }
        }
        if (primary.isEmpty()) visual.primaryDischarge = null;
        if (byproduct.isEmpty()) visual.byproductDischarge = null;
        // Retain immutable NBT snapshots until an actual packet-visible
        // change. Previously there were three new ItemStack copies per
        // frame even when the machine was completely idle.
        if (changedInput) visual.oldInput = input.copy();
        if (!sameVisualStack(visual.oldPrimary,primary))
            visual.oldPrimary = primary.copy();
        if (!sameVisualStack(visual.oldByproduct,byproduct))
            visual.oldByproduct = byproduct.copy();
    }

    private static boolean sameVisualStack(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        return a.getCount()==b.getCount() &&
                ItemStack.isSameItemSameTags(a,b);
    }

    private static boolean differentType(ItemStack old, ItemStack current) {
        return !old.isEmpty() && !current.isEmpty()
                && !ItemStack.isSameItemSameTags(old, current);
    }

    private static int addedItems(ItemStack old, ItemStack current) {
        if (current.isEmpty()) return 0;
        if (old.isEmpty() || !ItemStack.isSameItemSameTags(old, current))
            return current.getCount();
        return Math.max(0, current.getCount() - old.getCount());
    }

    // Local points follow the physical ore chute that is already present
    // in V2. The feed is deliberately above its top surface, not drawn
    // through the closed supports or attached to an imaginary conveyor.
    static Vec3 inputFeedPoint(double t) {
        if (t < 0.18D) return path(
                -8,16.2,24, -8,19.8,17.8, t / 0.18D);
        if (t < 0.80D) return path(
                -8,19.8,17.8, -8,22.0,-3.0, (t-0.18D)/0.62D);
        return path(-8,22.0,-3.0, -8,24.1,-10.0, (t-0.80D)/0.20D);
    }

    // Both output chutes are mirror siblings of the actual input chute:
    // primary output at x=24, optional byproduct at x=8.
    static Vec3 outputPoint(double laneX, double t) {
        if (t < 0.20D) return path(
                laneX,24.1,-10, laneX,26.9,-4.0, t/0.20D);
        if (t < 0.75D) return path(
                laneX,26.9,-4.0, laneX,20.1,17.0, (t-0.20D)/0.55D);
        return path(laneX,20.1,17.0,
                laneX,16.4,24.0, (t-0.75D)/0.25D);
    }

    private static Vec3 path(double ax, double ay, double az,
                             double bx, double by, double bz, double p) {
        double t = clamp(p);
        return new Vec3(ax+(bx-ax)*t,ay+(by-ay)*t,az+(bz-az)*t);
    }

    private static double clamp(double t) {
        return Math.max(0.0D, Math.min(1.0D, t));
    }

    private static void renderCrushing(BronzeStampMillBlockEntity mill,
                                      float partial, PoseStack pose,
                                      MultiBufferSource buffers, int light,
                                      int overlay, ItemStack input,
                                      ItemStack preview, float progress) {
        if (progress < 0.34F) {
            // Whole ore beneath the first working shoe.
            double tremor = mill.isProcessing()
                    ? 0.05D * (1D - mill.getHammerLift(partial, 0)/0.32D)
                    : 0D;
            drawItem(mill,pose,buffers,light,overlay,input,
                    -8D,24.05D+tremor,-10D,0.51F,0F);
        } else if (progress < 0.68F) {
            // Two irregular broken chunks under the middle stamp.
            double fraction = clamp((progress-0.34F)/0.34F);
            double x = -8D + 16D * fraction;
            drawItem(mill,pose,buffers,light,overlay,input,
                    x-2D,24.1D,-11D,0.32F,32F);
            drawItem(mill,pose,buffers,light,overlay,input,
                    x+2.5D,24.1D,-8.5D,0.26F,-24F);
        } else {
            // Finely crushed material reaches the third stamp. The
            // preview is visual only: the real output is still created
            // exactly once by tickProcessing() when the recipe completes.
            double fraction = clamp((progress-0.68F)/0.32F);
            double x = 8D + 16D * fraction;
            ItemStack crushed = preview.isEmpty() ? input : preview;
            drawItem(mill,pose,buffers,light,overlay,crushed,
                    x-2.5D,24.1D,-10.8D,0.27F,-22F);
            drawItem(mill,pose,buffers,light,overlay,crushed,
                    x+2.6D,24.1D,-9.2D,0.22F,34F);
            drawItem(mill,pose,buffers,light,overlay,crushed,
                    x,24.1D,-12D,0.24F,8F);
        }
    }

    private static void renderOutput(BronzeStampMillBlockEntity mill,
                                     PoseStack pose, MultiBufferSource buffers,
                                     int light, int overlay, ItemStack stack,
                                     MovingOre animation, double now, double laneX) {
        if (stack.isEmpty()) return;
        if (animation != null) {
            double age = now - animation.started;
            if (age >= 0.0D && age < DISCHARGE_TICKS) {
                Vec3 v = outputPoint(laneX, age/DISCHARGE_TICKS);
                drawItem(mill,pose,buffers,light,overlay,animation.item,
                        v.x,v.y,v.z,0.33F,0F);
                return;
            }
        }
        // A stored product waits at its real output port rather than
        // popping into the middle of the crushing machinery.
        drawItem(mill,pose,buffers,light,overlay,stack,
                laneX,16.4D,24D,0.36F,0F);
    }

    private static void drawItem(BronzeStampMillBlockEntity mill,
                                 PoseStack pose, MultiBufferSource buffers,
                                 int light, int overlay, ItemStack original,
                                 double x, double y, double z,
                                 float scale, float yaw) {
        if (original.isEmpty()) return;
        // ItemRenderer renders a single model; unlike ItemEntityRenderer,
        // it does not fan out copies based on the stack count. Avoid
        // allocating ItemStack copies 2-5 times each rendered frame.
        ItemStack stack = original;
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        pose.pushPose();
        pose.translate(x/16D,y/16D,z/16D);
        if (yaw != 0F) pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.scale(scale,scale,scale);
        BakedModel model = items.getModel(stack,mill.getLevel(),null,721);
        items.render(stack,ItemDisplayContext.GROUND,false,
                pose,buffers,light,overlay,model);
        pose.popPose();
    }

    private static void emitImpactDust(BronzeStampMillBlockEntity mill,
                                       float partial, VisualState state,
                                       double now, ItemStack input, float progress) {
        if (mill.getLevel() == null) return;
        if (!mill.isProcessing() || input.isEmpty()) {
            for (int i=0;i<3;i++) state.oldLift[i] = Float.NaN;
            return;
        }

        int activeLane = progress < 0.34F ? 0 : progress < 0.68F ? 1 : 2;
        for (int i=0;i<3;i++) {
            float lift = mill.getHammerLift(partial,i);
            float previous = state.oldLift[i];
            state.oldLift[i] = lift;
            if (i != activeLane || Float.isNaN(previous)
                    || previous <= LOW_HAMMER_THRESHOLD
                    || lift > LOW_HAMMER_THRESHOLD
                    || now-state.lastDustTick[i] < 3.0D) continue;
            state.lastDustTick[i] = now;
            spawnDust(mill, new int[]{-8,8,24}[i], 24.2D,-10D,input);
        }
    }

    private static void spawnDust(BronzeStampMillBlockEntity mill,
                                  double x, double y, double z, ItemStack ore) {
        Level level = mill.getLevel();
        if (level == null || !level.isClientSide) return;
        Block sourceBlock = Block.byItem(ore.getItem());
        BlockState stone = (sourceBlock == Blocks.AIR
                ? Blocks.STONE : sourceBlock).defaultBlockState();
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK,stone);
        BlockPos origin = mill.getBlockPos();
        Direction face = mill.getFacing();
        double angle = Math.toRadians(switch (face) {
            case WEST -> -90D;
            case NORTH -> 180D;
            case EAST -> 90D;
            default -> 0D;
        });
        double dx = x/16D - 0.5D;
        double dz = z/16D - 0.5D;
        double wx = origin.getX()+0.5D+Math.cos(angle)*dx+Math.sin(angle)*dz;
        double wz = origin.getZ()+0.5D-Math.sin(angle)*dx+Math.cos(angle)*dz;
        double wy = origin.getY()+y/16D;
        RandomSource random = level.random;
        for(int i=0;i<2;i++) {
            level.addParticle(debris,
                    wx+(random.nextDouble()-0.5D)*0.12D,
                    wy+0.035D,
                    wz+(random.nextDouble()-0.5D)*0.12D,
                    (random.nextDouble()-0.5D)*0.035D,
                    0.045D+random.nextDouble()*0.035D,
                    (random.nextDouble()-0.5D)*0.035D);
        }
    }
}
