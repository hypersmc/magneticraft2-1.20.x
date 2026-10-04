package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalBrakeBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Wooden band/shoe brake.
 *
 * Unlike the clutch this never breaks the topology. Applying the brake adds a
 * real mechanical load to the shaft. Redstone forces the brake on; empty-hand
 * right click toggles the manual lever.
 */
public class MechanicalBrakeBlock_wood extends GearBlock {
    public static final BooleanProperty BRAKING =
            BooleanProperty.create("braking");

    private static final VoxelShape X_AXIS_SHAPE = Shapes.or(
            Block.box(0.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D),
            Block.box(2.0D, 1.0D, 1.0D, 14.0D, 15.0D, 15.0D)
    );
    private static final VoxelShape Y_AXIS_SHAPE = Shapes.or(
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D),
            Block.box(1.0D, 2.0D, 1.0D, 15.0D, 14.0D, 15.0D)
    );
    private static final VoxelShape Z_AXIS_SHAPE = Shapes.or(
            Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 16.0D),
            Block.box(1.0D, 1.0D, 2.0D, 15.0D, 15.0D, 14.0D)
    );

    public MechanicalBrakeBlock_wood() {
        super(BlockBehaviour.Properties.of()
                .strength(3.5F)
                .noOcclusion());

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.EAST)
                        .setValue(BRAKING, false)
        );
    }

    @Override
    protected BlockEntity createBlockEntity(
            BlockPos pos,
            BlockState state) {
        return new MechanicalBrakeBlockEntity_wood(
                pos,
                state
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context) {
        BlockState placementState = defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(
                        BRAKING,
                        context.getLevel().hasNeighborSignal(
                                context.getClickedPos()
                                        .relative(context.getClickedFace())
                        )
                );

        return validateGearPlacement(
                context,
                placementState
        );
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
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!(level.getBlockEntity(pos)
                instanceof MechanicalBrakeBlockEntity_wood brake)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            boolean manual = brake.toggleManualBrake();

            player.displayClientMessage(
                    Component.translatable(
                            manual
                                    ? "message.magneticraft2.brake_applied"
                                    : "message.magneticraft2.brake_released"
                    ),
                    true
            );
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        super.neighborChanged(
                state,
                level,
                pos,
                neighborBlock,
                neighborPos,
                movedByPiston
        );

        if (!level.isClientSide
                && level.getBlockEntity(pos)
                instanceof MechanicalBrakeBlockEntity_wood brake) {
            brake.refreshBrakeState();
        }
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return shapeFor(state.getValue(FACING).getAxis());
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return shapeFor(state.getValue(FACING).getAxis());
    }

    private VoxelShape shapeFor(Direction.Axis axis) {
        return switch (axis) {
            case X -> X_AXIS_SHAPE;
            case Y -> Y_AXIS_SHAPE;
            case Z -> Z_AXIS_SHAPE;
        };
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BRAKING);
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (!level.isClientSide
                && !state.is(newState.getBlock())) {
            GearNetworkManager.getInstance()
                    .removeMechanicalLoad(level, pos);
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
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
                                .MECHANICAL_BRAKE_BE_WOOD
                                .get(),
                        MechanicalBrakeBlockEntity_wood
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
