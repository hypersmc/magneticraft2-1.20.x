package com.magneticraft2.common.item.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.ConveyorRollerBlockEntity;
import com.magneticraft2.common.systems.GEAR.ItemBeltConnectionManager;
import com.magneticraft2.common.systems.GEAR.ItemBeltGeometry;
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

/**
 * Segment item used to create one continuous wide item belt between two Conveyor Rollers.
 *
 * The player selects only the two endpoints. The intermediate ItemBeltBlocks are generated
 * automatically so the belt remains one logical machine while still having real block physics.
 */
public class ItemBeltItem extends Item {
    public static final int MAX_ITEM_BELT_SPAN = ItemBeltConnectionManager.MAX_ITEM_BELT_SPAN;

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

        ItemBeltGeometry.Layout layout = ItemBeltGeometry.create(
                startPos,
                clickedPos,
                startRoller.getGearAxis(),
                MAX_ITEM_BELT_SPAN
        );

        if (layout == null) {
            return fail(player, "message.magneticraft2.item_belt_unsupported_geometry");
        }

        int requiredSegments = layout.requiredSegments();

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

        if ((start.getItemBeltPartner() != null && !start.isItemBeltLinkedTo(end.getBlockPos()))
                || (end.getItemBeltPartner() != null && !end.isItemBeltLinkedTo(start.getBlockPos()))) {
            return "message.magneticraft2.item_belt_roller_in_use";
        }

        int dx = end.getBlockPos().getX() - start.getBlockPos().getX();
        int dy = end.getBlockPos().getY() - start.getBlockPos().getY();
        int dz = end.getBlockPos().getZ() - start.getBlockPos().getZ();

        if ((axis == Direction.Axis.X && dx != 0)
                || (axis == Direction.Axis.Z && dz != 0)) {
            return "message.magneticraft2.item_belt_same_plane_required";
        }

        int horizontalSteps = Math.abs(axis == Direction.Axis.X ? dz : dx);
        int verticalSteps = Math.abs(dy);
        int gridSpan = Math.max(horizontalSteps, verticalSteps);

        if (gridSpan > MAX_ITEM_BELT_SPAN) {
            return "message.magneticraft2.item_belt_too_long";
        }

        if (gridSpan < 2) {
            return "message.magneticraft2.item_belt_too_short";
        }

        ItemBeltGeometry.Layout layout = ItemBeltGeometry.create(
                start.getBlockPos(),
                end.getBlockPos(),
                axis,
                MAX_ITEM_BELT_SPAN
        );

        if (layout == null) {
            return "message.magneticraft2.item_belt_unsupported_geometry";
        }

        if (!isPathClear(level, layout)) {
            return "message.magneticraft2.item_belt_path_blocked";
        }

        return null;
    }

    private boolean isPathClear(Level level, ItemBeltGeometry.Layout layout) {
        for (BlockPos beltPos : layout.beltBlocks()) {
            BlockState state = level.getBlockState(beltPos);
            if (!state.isAir() && !state.canBeReplaced()) {
                return false;
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

        // Saved endpoint links may exist briefly before the runtime connection map
        // has rebuilt after a chunk/world load. Rebuild it before unlinking so the
        // generated physical cells are always removed with the logical connection.
        ItemBeltConnectionManager.ensureRegistered(roller);

        ItemBeltGeometry.Layout layout = ItemBeltGeometry.create(
                roller.getBlockPos(),
                partnerPos,
                roller.getGearAxis(),
                MAX_ITEM_BELT_SPAN
        );

        int recoveredSegments = layout == null
                ? 1
                : layout.requiredSegments();

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
