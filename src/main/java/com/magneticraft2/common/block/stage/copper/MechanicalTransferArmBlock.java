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
 * Four buttons on the front control console are arranged as a clean 2x2
 * F/B/L/R grid and can each be cycled through:
 * NONE -> SOURCE (orange) -> DESTINATION (blue) -> NONE. FACING is the
 * physical panel side and the side naturally facing the player after placement.
 *
 * The dedicated filter bay below the direction grid accepts a ghost filter item.
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

        // The player stands on the FACING side and looks into the block.
        // In that view, screen-right is FACING.counterClockWise().
        Direction right =
                facing.getCounterClockWise();

        double lateral =
                dx * right.getStepX()
                        + dz * right.getStepZ();

        return Math.abs(lateral)
                <= 4.50D / 16.0D
                && localY >= 7.65D / 16.0D
                && localY <= 10.30D / 16.0D;
    }

    @Nullable
    private Direction getButtonDirection(
            BlockState state,
            BlockPos pos,
            BlockHitResult hit) {
        Direction facing =
                state.getValue(FACING);

        // FACING is the one exposed control-panel side.
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

        // The player stands on the FACING side and looks into the block.
        // In that view, screen-right is FACING.counterClockWise().
        Direction right =
                facing.getCounterClockWise();

        double lateral =
                dx * right.getStepX()
                        + dz * right.getStepZ();

        // Dedicated filter bay below the organized 2x2 direction grid.
        if (Math.abs(lateral)
                <= 4.50D / 16.0D
                && localY >= 7.65D / 16.0D
                && localY <= 10.30D / 16.0D) {
            return null;
        }

        double leftCenter =
                -2.80D / 16.0D;
        double rightCenter =
                2.80D / 16.0D;
        double halfWidth =
                1.65D / 16.0D;

        // Top row, as seen by the player: F | B.
        if (localY >= 13.00D / 16.0D
                && localY <= 14.90D / 16.0D) {
            if (Math.abs(
                    lateral - leftCenter
            ) <= halfWidth) {
                return facing;
            }

            if (Math.abs(
                    lateral - rightCenter
            ) <= halfWidth) {
                return facing.getOpposite();
            }
        }

        // Bottom row: L | R.
        if (localY >= 10.90D / 16.0D
                && localY <= 12.85D / 16.0D) {
            if (Math.abs(
                    lateral - leftCenter
            ) <= halfWidth) {
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
