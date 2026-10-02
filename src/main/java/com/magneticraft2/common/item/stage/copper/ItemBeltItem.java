package com.magneticraft2.common.item.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.ConveyorRollerBlockEntity;
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

/**
 * Segment item used to create one continuous wide item belt between two Conveyor Rollers.
 */
public class ItemBeltItem extends Item {
    public static final int MAX_ITEM_BELT_SPAN = 16;

    private static final String START_POS = "Mgc2ItemBeltStart";
    private static final String START_DIMENSION = "Mgc2ItemBeltDimension";

    public ItemBeltItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();

        if (!(level.getBlockEntity(clickedPos) instanceof ConveyorRollerBlockEntity clickedRoller)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();

        if (player != null && player.isShiftKeyDown()) {
            disconnectRoller(clickedRoller, player);
            clearSelection(tag);
            return InteractionResult.SUCCESS;
        }

        String dimension = level.dimension().location().toString();
        if (!tag.contains(START_POS) || !dimension.equals(tag.getString(START_DIMENSION))) {
            tag.putLong(START_POS, clickedPos.asLong());
            tag.putString(START_DIMENSION, dimension);
            if (player != null) {
                player.displayClientMessage(Component.translatable(
                        "message.magneticraft2.item_belt_start",
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
                player.displayClientMessage(Component.translatable(
                        "message.magneticraft2.item_belt_selection_cleared"), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(startPos) instanceof ConveyorRollerBlockEntity startRoller)) {
            clearSelection(tag);
            return fail(player, "message.magneticraft2.item_belt_start_missing");
        }

        String validationError = validateConnection(level, startRoller, clickedRoller);
        if (validationError != null) {
            return fail(player, validationError);
        }

        double distance = Vec3.atCenterOf(startPos).distanceTo(Vec3.atCenterOf(clickedPos));
        int requiredSegments = Math.max(1, (int) Math.ceil(distance));

        if (player != null
                && !player.getAbilities().instabuild
                && stack.getCount() < requiredSegments) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft2.item_belt_not_enough",
                    requiredSegments), true);
            return InteractionResult.FAIL;
        }

        startRoller.linkItemBelt(clickedPos);
        clickedRoller.linkItemBelt(startPos);
        clearSelection(tag);

        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(requiredSegments);
        }

        if (player != null) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft2.item_belt_linked",
                    requiredSegments), true);
        }

        return InteractionResult.SUCCESS;
    }

    private String validateConnection(Level level,
                                      ConveyorRollerBlockEntity start,
                                      ConveyorRollerBlockEntity end) {
        Direction.Axis axis = start.getGearAxis();

        if (axis == Direction.Axis.Y || axis != end.getGearAxis()) {
            return "message.magneticraft2.item_belt_parallel_required";
        }

        if (!sameRollerPlane(start.getBlockPos(), end.getBlockPos(), axis)) {
            return "message.magneticraft2.item_belt_same_plane_required";
        }

        Vec3 startCenter = Vec3.atCenterOf(start.getBlockPos());
        Vec3 endCenter = Vec3.atCenterOf(end.getBlockPos());
        double distance = startCenter.distanceTo(endCenter);

        if (distance > MAX_ITEM_BELT_SPAN + 0.001D) {
            return "message.magneticraft2.item_belt_too_long";
        }

        if (distance < 1.5D) {
            return "message.magneticraft2.item_belt_too_short";
        }

        double horizontalDistance = Math.sqrt(
                Math.pow(endCenter.x - startCenter.x, 2.0D)
                        + Math.pow(endCenter.z - startCenter.z, 2.0D)
        );
        double verticalDistance = Math.abs(endCenter.y - startCenter.y);

        if (horizontalDistance < 1.0D || verticalDistance > horizontalDistance + 0.001D) {
            return "message.magneticraft2.item_belt_slope_too_steep";
        }

        if ((start.getItemBeltPartner() != null && !start.isItemBeltLinkedTo(end.getBlockPos()))
                || (end.getItemBeltPartner() != null && !end.isItemBeltLinkedTo(start.getBlockPos()))) {
            return "message.magneticraft2.item_belt_roller_in_use";
        }

        if (!isPathClear(level, start.getBlockPos(), end.getBlockPos(), axis)) {
            return "message.magneticraft2.item_belt_path_blocked";
        }

        return null;
    }

    private boolean sameRollerPlane(BlockPos first, BlockPos second, Direction.Axis axis) {
        return switch (axis) {
            case X -> first.getX() == second.getX();
            case Z -> first.getZ() == second.getZ();
            case Y -> false;
        };
    }

    private boolean isPathClear(Level level,
                                BlockPos first,
                                BlockPos second,
                                Direction.Axis axis) {
        Vec3 start = Vec3.atCenterOf(first);
        Vec3 end = Vec3.atCenterOf(second);
        double distance = start.distanceTo(end);
        int samples = Math.max(4, (int) Math.ceil(distance * 6.0D));

        Vec3 width = axis == Direction.Axis.X
                ? new Vec3(0.34D, 0.0D, 0.0D)
                : new Vec3(0.0D, 0.0D, 0.34D);

        for (int i = 1; i < samples; i++) {
            double t = i / (double) samples;
            Vec3 center = start.lerp(end, t);

            for (Vec3 sample : new Vec3[]{
                    center,
                    center.add(width),
                    center.subtract(width)
            }) {
                BlockPos samplePos = BlockPos.containing(sample);
                if (samplePos.equals(first) || samplePos.equals(second)) {
                    continue;
                }

                BlockState state = level.getBlockState(samplePos);
                if (!state.isAir() && !state.canBeReplaced()) {
                    return false;
                }
            }
        }

        return true;
    }

    private void disconnectRoller(ConveyorRollerBlockEntity roller, Player player) {
        BlockPos partnerPos = roller.getItemBeltPartner();
        if (partnerPos == null) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft2.item_belt_not_connected"), true);
            return;
        }

        double distance = Vec3.atCenterOf(roller.getBlockPos())
                .distanceTo(Vec3.atCenterOf(partnerPos));
        int recoveredSegments = Math.max(1, (int) Math.ceil(distance));

        roller.disconnectItemBelt(true);

        if (!player.getAbilities().instabuild) {
            ItemStack recovered = new ItemStack(this, recoveredSegments);
            if (!player.getInventory().add(recovered)) {
                player.drop(recovered, false);
            }
        }

        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.item_belt_removed",
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
