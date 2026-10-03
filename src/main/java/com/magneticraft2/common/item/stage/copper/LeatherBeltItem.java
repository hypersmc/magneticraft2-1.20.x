package com.magneticraft2.common.item.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.PulleyBlockEntity_wood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class LeatherBeltItem extends Item {
    public static final int MAX_BELT_SPAN = 8;

    private static final String START_POS = "Mgc2BeltStart";
    private static final String START_DIMENSION = "Mgc2BeltDimension";

    public LeatherBeltItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        if (!(level.getBlockEntity(clickedPos) instanceof PulleyBlockEntity_wood clickedPulley)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();

        if (player != null && player.isShiftKeyDown()) {
            disconnectPulley(clickedPulley, player);
            clearSelection(tag);
            return InteractionResult.SUCCESS;
        }

        String dimension = level.dimension().location().toString();
        if (!tag.contains(START_POS) || !dimension.equals(tag.getString(START_DIMENSION))) {
            tag.putLong(START_POS, clickedPos.asLong());
            tag.putString(START_DIMENSION, dimension);
            if (player != null) {
                player.displayClientMessage(Component.translatable(
                        "message.magneticraft2.belt_start",
                        clickedPos.getX(),
                        clickedPos.getY(),
                        clickedPos.getZ()), true);
            }
            return InteractionResult.SUCCESS;
        }

        BlockPos startPos = BlockPos.of(tag.getLong(START_POS));
        if (startPos.equals(clickedPos)) {
            clearSelection(tag);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.magneticraft2.belt_selection_cleared"), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(startPos) instanceof PulleyBlockEntity_wood startPulley)) {
            clearSelection(tag);
            return fail(player, "message.magneticraft2.belt_start_missing");
        }

        PlacementCheck placement = evaluateConnection(
                level,
                startPulley,
                clickedPulley
        );
        if (!placement.valid()) {
            return fail(player, placement.errorKey());
        }

        int requiredSegments = requiredSegments(
                startPos,
                clickedPos
        );

        if (player != null && !player.getAbilities().instabuild && stack.getCount() < requiredSegments) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft2.belt_not_enough",
                    requiredSegments), true);
            return InteractionResult.FAIL;
        }

        startPulley.linkBelt(clickedPos);
        clickedPulley.linkBelt(startPos);
        clearSelection(tag);

        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(requiredSegments);
        }

        if (player != null) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft2.belt_linked",
                    requiredSegments), true);
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    public static BlockPos getSelectedStart(ItemStack stack,
                                            Level level) {
        if (stack == null
                || stack.isEmpty()
                || !(stack.getItem() instanceof LeatherBeltItem)
                || level == null
                || stack.getTag() == null) {
            return null;
        }

        CompoundTag tag = stack.getTag();
        if (!tag.contains(START_POS)
                || !level.dimension().location().toString()
                .equals(tag.getString(START_DIMENSION))) {
            return null;
        }

        return BlockPos.of(tag.getLong(START_POS));
    }

    public static PlacementCheck evaluateConnection(
            Level level,
            PulleyBlockEntity_wood start,
            PulleyBlockEntity_wood end) {
        if (level == null || start == null || end == null) {
            return PlacementCheck.invalid(
                    "message.magneticraft2.belt_start_missing"
            );
        }

        if (start.getGearAxis() != end.getGearAxis()) {
            return PlacementCheck.invalid(
                    "message.magneticraft2.belt_parallel_required"
            );
        }

        if (!samePulleyPlane(
                start.getBlockPos(),
                end.getBlockPos(),
                start.getGearAxis()
        )) {
            return PlacementCheck.invalid(
                    "message.magneticraft2.belt_same_plane_required"
            );
        }

        double distance = Vec3.atCenterOf(start.getBlockPos())
                .distanceTo(Vec3.atCenterOf(end.getBlockPos()));
        if (distance > MAX_BELT_SPAN + 0.001D) {
            return PlacementCheck.invalid(
                    "message.magneticraft2.belt_too_long"
            );
        }

        if (distance < 1.5D) {
            return PlacementCheck.invalid(
                    "message.magneticraft2.belt_too_short"
            );
        }

        if ((start.getBeltPartner() != null
                && !start.isLinkedTo(end.getBlockPos()))
                || (end.getBeltPartner() != null
                && !end.isLinkedTo(start.getBlockPos()))) {
            return PlacementCheck.invalid(
                    "message.magneticraft2.belt_pulley_in_use"
            );
        }

        if (!isPathClear(
                level,
                start.getBlockPos(),
                end.getBlockPos()
        )) {
            return PlacementCheck.invalid(
                    "message.magneticraft2.belt_path_blocked"
            );
        }

        return PlacementCheck.valid();
    }

    public static int requiredSegments(BlockPos first,
                                       BlockPos second) {
        double distance = Vec3.atCenterOf(first)
                .distanceTo(Vec3.atCenterOf(second));
        return Math.max(1, (int) Math.ceil(distance));
    }

    private static boolean samePulleyPlane(BlockPos first,
                                           BlockPos second,
                                           Direction.Axis axis) {
        return switch (axis) {
            case X -> first.getX() == second.getX();
            case Y -> first.getY() == second.getY();
            case Z -> first.getZ() == second.getZ();
        };
    }

    private static boolean isPathClear(Level level,
                                       BlockPos first,
                                       BlockPos second) {
        Vec3 start = Vec3.atCenterOf(first);
        Vec3 end = Vec3.atCenterOf(second);
        double distance = start.distanceTo(end);
        int samples = Math.max(
                4,
                (int) Math.ceil(distance * 8.0D)
        );

        for (int i = 1; i < samples; i++) {
            double t = i / (double) samples;
            Vec3 sample = start.lerp(end, t);
            BlockPos samplePos = BlockPos.containing(sample);

            if (samplePos.equals(first)
                    || samplePos.equals(second)) {
                continue;
            }

            BlockState state = level.getBlockState(samplePos);
            if (!state.isAir() && !state.canBeReplaced()) {
                return false;
            }
        }

        return true;
    }

    public record PlacementCheck(@Nullable String errorKey) {
        public static PlacementCheck valid() {
            return new PlacementCheck(null);
        }

        public static PlacementCheck invalid(String errorKey) {
            return new PlacementCheck(errorKey);
        }

        public boolean valid() {
            return errorKey == null;
        }
    }

    private void disconnectPulley(PulleyBlockEntity_wood pulley, Player player) {
        BlockPos partnerPos = pulley.getBeltPartner();
        if (partnerPos == null) {
            player.displayClientMessage(Component.translatable("message.magneticraft2.belt_not_connected"), true);
            return;
        }

        int recoveredSegments = requiredSegments(
                pulley.getBlockPos(),
                partnerPos
        );
        pulley.disconnectBelt(true);

        if (!player.getAbilities().instabuild) {
            ItemStack recovered = new ItemStack(this, recoveredSegments);
            if (!player.getInventory().add(recovered)) {
                player.drop(recovered, false);
            }
        }

        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.belt_removed",
                recoveredSegments), true);
    }

    private InteractionResult fail(Player player, String translationKey) {
        if (player != null) {
            player.displayClientMessage(Component.translatable(translationKey), true);
        }
        return InteractionResult.FAIL;
    }

    private void clearSelection(CompoundTag tag) {
        tag.remove(START_POS);
        tag.remove(START_DIMENSION);
    }
}
