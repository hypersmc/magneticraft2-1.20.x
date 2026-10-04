package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalTransferArmBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Copper Age one-item mechanical inserter.
 *
 * Four buttons on the single front control console can each be cycled through:
 * NONE -> SOURCE (orange) -> DESTINATION (blue) -> NONE. Keeping every control
 * on one face means the arm remains configurable when full blocks touch its
 * left/right/back sides.
 *
 * The framed filter rack below the D-pad accepts a ghost filter item.
 * Empty-hand filter click toggles whitelist/blacklist; shift + empty-hand
 * filter click clears it.
 */
public class MechanicalTransferArmBlock extends GearBlock {

    public MechanicalTransferArmBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.5F)
                .noOcclusion());

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context) {
        BlockState state =
                defaultBlockState()
                        .setValue(
                                FACING,
                                context.getHorizontalDirection()
                                        .getOpposite()
                        );

        return validateGearPlacement(
                context,
                state
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public int getPlacementGearTeeth(
            BlockState state) {
        return 8;
    }

    @Override
    public Direction.Axis getPlacementGearAxis(
            BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean isShaftLikeForPlacement(
            BlockState state) {
        return false;
    }

    @Override
    protected BlockEntity createBlockEntity(
            BlockPos pos,
            BlockState state) {
        return new MechanicalTransferArmBlockEntity(
                pos,
                state
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity>
    BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        BlockEntityRegistry
                                .MECHANICAL_TRANSFER_ARM_BE
                                .get(),
                        MechanicalTransferArmBlockEntity
                                ::serverTick
                );
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (!level.isClientSide
                && !state.is(newState.getBlock())
                && level.getBlockEntity(pos)
                instanceof MechanicalTransferArmBlockEntity arm) {
            ItemStack carried =
                    arm.takeCarriedStackForDrop();

            if (!carried.isEmpty()) {
                level.addFreshEntity(
                        new ItemEntity(
                                level,
                                pos.getX() + 0.5D,
                                pos.getY() + 0.75D,
                                pos.getZ() + 0.5D,
                                carried
                        )
                );
            }
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
        );
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos)
                instanceof MechanicalTransferArmBlockEntity arm)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Direction button =
                getButtonDirection(
                        state,
                        pos,
                        hit
                );
        boolean filterHit =
                isFilterHit(
                        state,
                        pos,
                        hit
                );

        ItemStack held =
                player.getItemInHand(hand);

        if (button != null && held.isEmpty()) {
            MechanicalTransferArmBlockEntity.SideRole role =
                    arm.cycleSide(button);

            BlockPos targetPos =
                    pos.relative(button);

            Component targetName =
                    level.getBlockState(targetPos)
                            .getBlock()
                            .getName();

            player.displayClientMessage(
                    Component.literal(
                            arm.getRelativeSideName(button)
                                    + ": "
                                    + role.displayName()
                                    + " -> "
                    ).append(targetName),
                    true
            );

            return InteractionResult.CONSUME;
        }

        if (filterHit) {
            if (!held.isEmpty()) {
                ItemStack filter =
                        held.copy();
                filter.setCount(1);
                arm.setFilter(filter);

                player.displayClientMessage(
                        Component.literal(
                                "Filter: "
                                        + (arm.isBlacklist()
                                        ? "BLACKLIST "
                                        : "WHITELIST ")
                        ).append(
                                filter.getHoverName()
                        ),
                        true
                );

                return InteractionResult.CONSUME;
            }

            if (player.isShiftKeyDown()) {
                arm.clearFilter();
                player.displayClientMessage(
                        Component.literal(
                                "Transfer Arm filter cleared"
                        ),
                        true
                );
                return InteractionResult.CONSUME;
            }

            if (!arm.getFilterStack().isEmpty()) {
                arm.toggleBlacklist();

                ItemStack filter =
                        arm.getFilterStack();

                player.displayClientMessage(
                        Component.literal(
                                "Filter: "
                                        + (arm.isBlacklist()
                                        ? "BLACKLIST "
                                        : "WHITELIST ")
                        ).append(
                                filter.getHoverName()
                        ),
                        true
                );

                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    private boolean isFilterHit(
            BlockState state,
            BlockPos pos,
            BlockHitResult hit) {
        Direction facing =
                state.getValue(FACING);

        if (hit.getDirection() != facing) {
            return false;
        }

        double dx =
                hit.getLocation().x
                        - pos.getX()
                        - 0.5D;
        double dz =
                hit.getLocation().z
                        - pos.getZ()
                        - 0.5D;
        double localY =
                hit.getLocation().y
                        - pos.getY();

        Direction right =
                facing.getClockWise();

        double lateral =
                dx * right.getStepX()
                        + dz * right.getStepZ();

        return Math.abs(lateral)
                <= 3.10D / 16.0D
                && localY >= 1.45D / 16.0D
                && localY <= 3.25D / 16.0D;
    }

    @Nullable
    private Direction getButtonDirection(
            BlockState state,
            BlockPos pos,
            BlockHitResult hit) {
        Direction facing =
                state.getValue(FACING);

        // All configuration lives on the one exposed front console. This is
        // intentional: in real machine lines the arm is often sandwiched
        // directly between two full inventories/modules, making side buttons
        // impossible to click.
        if (hit.getDirection() != facing) {
            return null;
        }

        double dx =
                hit.getLocation().x
                        - pos.getX()
                        - 0.5D;
        double dz =
                hit.getLocation().z
                        - pos.getZ()
                        - 0.5D;
        double localY =
                hit.getLocation().y
                        - pos.getY();

        Direction right =
                facing.getClockWise();

        double lateral =
                dx * right.getStepX()
                        + dz * right.getStepZ();

        // Filter rack: x ~= center, y = 1.55..3.25 pixels.
        if (Math.abs(lateral)
                <= 3.10D / 16.0D
                && localY >= 1.45D / 16.0D
                && localY <= 3.25D / 16.0D) {
            return null;
        }

        // Forward button.
        if (Math.abs(lateral)
                <= 1.25D / 16.0D
                && localY >= 5.95D / 16.0D
                && localY <= 7.45D / 16.0D) {
            return facing;
        }

        // Back button.
        if (Math.abs(lateral)
                <= 1.25D / 16.0D
                && localY >= 3.35D / 16.0D
                && localY <= 4.80D / 16.0D) {
            return facing.getOpposite();
        }

        // Left / right row.
        if (localY >= 4.65D / 16.0D
                && localY <= 6.10D / 16.0D) {
            double leftCenter =
                    -2.45D / 16.0D;
            double rightCenter =
                    2.45D / 16.0D;
            double halfWidth =
                    1.20D / 16.0D;

            if (Math.abs(
                    lateral - leftCenter
            ) <= halfWidth) {
                // From the player's view of the front face, screen-left is
                // clockwise around the block's outward-facing direction.
                return facing.getClockWise();
            }

            if (Math.abs(
                    lateral - rightCenter
            ) <= halfWidth) {
                return facing.getCounterClockWise();
            }
        }

        return null;
    }

    @Nullable
    private static <E extends BlockEntity,
                    A extends BlockEntity>
    BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType
                ? (BlockEntityTicker<A>) ticker
                : null;
    }
}
