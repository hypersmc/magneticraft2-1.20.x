package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.stage.copper.ClutchBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Wooden in-line clutch for Gear V2.
 *
 * The clutch is normally engaged and behaves like a 1:1 shaft. Empty-hand
 * right-click toggles the manual latch. A redstone signal always pulls the
 * clutch open, allowing simple remote machine isolation without dismantling
 * the drivetrain.
 */
public class ClutchBlock_wood extends GearBlock {
    public static final BooleanProperty ENGAGED =
            BooleanProperty.create("engaged");

    private static final VoxelShape X_AXIS_SHAPE = Shapes.or(
            Block.box(0.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D),
            Block.box(4.5D, 4.0D, 4.0D, 11.5D, 12.0D, 12.0D)
    );
    private static final VoxelShape Y_AXIS_SHAPE = Shapes.or(
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D),
            Block.box(4.0D, 4.5D, 4.0D, 12.0D, 11.5D, 12.0D)
    );
    private static final VoxelShape Z_AXIS_SHAPE = Shapes.or(
            Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 16.0D),
            Block.box(4.0D, 4.0D, 4.5D, 12.0D, 12.0D, 11.5D)
    );

    public ClutchBlock_wood() {
        super(BlockBehaviour.Properties.of()
                .strength(3.5F)
                .noOcclusion());

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.EAST)
                        .setValue(ENGAGED, true)
        );
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos,
                                            BlockState state) {
        return new ClutchBlockEntity_wood(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placementState = defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(ENGAGED, true);

        return validateGearPlacement(context, placementState);
    }

    @Override
    public int getPlacementGearTeeth(BlockState state) {
        return 1;
    }

    @Override
    public boolean isShaftLikeForPlacement(BlockState state) {
        return true;
    }

    @Override
    public Direction.Axis getPlacementGearAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public VoxelShape getShape(BlockState state,
                               BlockGetter level,
                               BlockPos pos,
                               CollisionContext context) {
        return shapeFor(state.getValue(FACING).getAxis());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state,
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
    public InteractionResult use(BlockState state,
                                 Level level,
                                 BlockPos pos,
                                 Player player,
                                 InteractionHand hand,
                                 BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }

        if (level.getBlockEntity(pos)
                instanceof ClutchBlockEntity_wood clutch) {
            if (!level.isClientSide) {
                boolean manualEngaged =
                        clutch.toggleManualEngagement();

                player.displayClientMessage(
                        Component.translatable(
                                manualEngaged
                                        ? "message.magneticraft2.clutch_manual_engaged"
                                        : "message.magneticraft2.clutch_manual_disengaged"
                        ),
                        true
                );
            }

            return InteractionResult.sidedSuccess(
                    level.isClientSide
            );
        }

        return InteractionResult.PASS;
    }

    @Override
    public void neighborChanged(BlockState state,
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
                instanceof ClutchBlockEntity_wood clutch) {
            clutch.refreshEffectiveEngagement();
        }
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENGAGED);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        BlockEntityRegistry.CLUTCH_BE_WOOD.get(),
                        ClutchBlockEntity_wood::serverTick
                );
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity>
    BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType
                ? (BlockEntityTicker<A>) ticker
                : null;
    }
}
