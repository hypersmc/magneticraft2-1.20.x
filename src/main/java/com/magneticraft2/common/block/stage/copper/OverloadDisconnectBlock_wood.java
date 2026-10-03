package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.stage.copper.OverloadDisconnectBlockEntity_wood;
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
 * Automatic overload isolator for Gear V2.
 *
 * Unlike the normal clutch, this block is not a player-controlled drivetrain
 * switch. It stays closed until a downstream load would overload the source,
 * then opens before that load can stall the input side. After its configured
 * delay it retries automatically.
 *
 * The retry delay is configured directly in-world:
 *   empty-hand right click       -> next delay
 *   sneak + empty-hand right click -> previous delay
 */
public class OverloadDisconnectBlock_wood extends GearBlock {
    public static final BooleanProperty ENGAGED =
            BooleanProperty.create("engaged");
    public static final BooleanProperty ROTATING =
            BooleanProperty.create("rotating");

    private static final VoxelShape X_AXIS_SHAPE = Shapes.or(
            Block.box(0.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D),
            Block.box(4.5D, 3.5D, 3.5D, 11.5D, 13.5D, 12.5D)
    );
    private static final VoxelShape Y_AXIS_SHAPE = Shapes.or(
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D),
            Block.box(3.5D, 4.5D, 3.5D, 12.5D, 11.5D, 13.5D)
    );
    private static final VoxelShape Z_AXIS_SHAPE = Shapes.or(
            Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 16.0D),
            Block.box(3.5D, 3.5D, 4.5D, 12.5D, 13.5D, 11.5D)
    );

    public OverloadDisconnectBlock_wood() {
        super(BlockBehaviour.Properties.of()
                .strength(3.5F)
                .noOcclusion());

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.EAST)
                        .setValue(ENGAGED, true)
                        .setValue(ROTATING, false)
        );
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos,
                                            BlockState state) {
        return new OverloadDisconnectBlockEntity_wood(
                pos,
                state
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context) {
        BlockState placementState = defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(ENGAGED, true)
                .setValue(ROTATING, false);

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
    public VoxelShape getShape(BlockState state,
                               BlockGetter level,
                               BlockPos pos,
                               CollisionContext context) {
        return shapeFor(
                state.getValue(FACING).getAxis()
        );
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return shapeFor(
                state.getValue(FACING).getAxis()
        );
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

        if (!(level.getBlockEntity(pos)
                instanceof OverloadDisconnectBlockEntity_wood disconnect)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            int seconds = disconnect.cycleResetDelay(
                    player.isShiftKeyDown() ? -1 : 1
            );

            player.displayClientMessage(
                    Component.translatable(
                            "message.magneticraft2.overload_disconnect_reset_time",
                            seconds
                    ),
                    true
            );
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENGAGED, ROTATING);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T>
    getTicker(Level level,
              BlockState state,
              BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        BlockEntityRegistry
                                .OVERLOAD_DISCONNECT_BE_WOOD
                                .get(),
                        OverloadDisconnectBlockEntity_wood
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
