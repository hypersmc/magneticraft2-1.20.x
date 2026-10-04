package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.stage.copper.CrankBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Rotation-to-reciprocation crank.
 *
 * FACING is the shaft direction/axis. ROD_DIRECTION is the linear output side.
 * Sneaking while placing flips the selected rod direction, which lets the same
 * block drive bellows or other reciprocating machines from either side.
 */
public class CrankBlock_wood extends GearBlock {
    public static final DirectionProperty ROD_DIRECTION =
            DirectionProperty.create("rod_direction");

    private static final VoxelShape SHAPE =
            Block.box(
                    1.0D, 0.0D, 1.0D,
                    15.0D, 16.0D, 15.0D
            );

    public CrankBlock_wood() {
        super(BlockBehaviour.Properties.of()
                .strength(3.5F)
                .noOcclusion());

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.EAST)
                        .setValue(
                                ROD_DIRECTION,
                                Direction.UP
                        )
        );
    }

    @Override
    protected BlockEntity createBlockEntity(
            BlockPos pos,
            BlockState state) {
        return new CrankBlockEntity_wood(
                pos,
                state
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context) {
        Direction shaftDirection =
                context.getClickedFace();

        Direction rodDirection =
                chooseRodDirection(
                        context,
                        shaftDirection
                );

        BlockState placementState =
                defaultBlockState()
                        .setValue(
                                FACING,
                                shaftDirection
                        )
                        .setValue(
                                ROD_DIRECTION,
                                rodDirection
                        );

        return validateGearPlacement(
                context,
                placementState
        );
    }

    private Direction chooseRodDirection(
            BlockPlaceContext context,
            Direction shaftDirection) {
        Player player = context.getPlayer();

        Direction rodDirection;

        if (player != null
                && player.getDirection().getAxis()
                != shaftDirection.getAxis()) {
            rodDirection = player.getDirection();
        } else if (shaftDirection.getAxis()
                == Direction.Axis.Y) {
            rodDirection = Direction.NORTH;
        } else {
            rodDirection = Direction.UP;
        }

        if (player != null
                && player.isShiftKeyDown()) {
            rodDirection =
                    rodDirection.getOpposite();
        }

        if (rodDirection.getAxis()
                == shaftDirection.getAxis()) {
            rodDirection =
                    shaftDirection.getAxis()
                            == Direction.Axis.Y
                            ? Direction.NORTH
                            : Direction.UP;
        }

        return rodDirection;
    }

    @Override
    public int getPlacementGearTeeth(BlockState state) {
        return 1;
    }

    @Override
    public boolean isShaftLikeForPlacement(
            BlockState state) {
        return true;
    }

    @Override
    public Direction.Axis getPlacementGearAxis(
            BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                FACING,
                ROD_DIRECTION
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T>
    getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        BlockEntityRegistry
                                .CRANK_BE_WOOD
                                .get(),
                        CrankBlockEntity_wood
                                ::serverTick
                );
    }

    @Nullable
    protected static <E extends BlockEntity,
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
