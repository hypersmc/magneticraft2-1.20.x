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
 * Four physical buttons around the top of the base can each be cycled through:
 * NONE -> SOURCE (orange) -> DESTINATION (blue) -> NONE.
 *
 * The framed filter rack on the front face accepts a ghost filter item.
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

        ItemStack held =
                player.getItemInHand(hand);

        if (button != null && held.isEmpty()) {
            MechanicalTransferArmBlockEntity.SideRole role =
                    arm.cycleSide(button);

            player.displayClientMessage(
                    Component.literal(
                            arm.getRelativeSideName(button)
                                    + ": "
                                    + role.displayName()
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        if (button == null) {
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

                player.displayClientMessage(
                        Component.literal(
                                "Filter mode: "
                                        + (arm.isBlacklist()
                                        ? "BLACKLIST"
                                        : "WHITELIST")
                        ),
                        true
                );

                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @Nullable
    private Direction getButtonDirection(
            BlockState state,
            BlockPos pos,
            BlockHitResult hit) {
        double localX =
                hit.getLocation().x
                        - pos.getX();
        double localY =
                hit.getLocation().y
                        - pos.getY();
        double localZ =
                hit.getLocation().z
                        - pos.getZ();

        Direction facing =
                state.getValue(FACING);

        // Dedicated filter rack on the FRONT face. Clicking the framed center
        // is filter interaction, not the forward direction button.
        if (hit.getDirection() == facing
                && localY >= 1.7D / 16.0D
                && localY <= 5.6D / 16.0D) {
            double lateral =
                    facing.getAxis()
                            == Direction.Axis.Z
                            ? Math.abs(localX - 0.5D)
                            : Math.abs(localZ - 0.5D);

            if (lateral <= 0.24D) {
                return null;
            }
        }

        // Top face is the intended way to configure the four physical buttons.
        if (hit.getDirection() == Direction.UP) {
            double centeredX =
                    localX - 0.5D;
            double centeredZ =
                    localZ - 0.5D;

            if (Math.abs(centeredX)
                    > Math.abs(centeredZ)) {
                return centeredX > 0.0D
                        ? Direction.EAST
                        : Direction.WEST;
            }

            return centeredZ > 0.0D
                    ? Direction.SOUTH
                    : Direction.NORTH;
        }

        // Side-face clicks remain a convenience everywhere except the filter.
        if (hit.getDirection()
                .getAxis()
                .isHorizontal()) {
            return hit.getDirection();
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
