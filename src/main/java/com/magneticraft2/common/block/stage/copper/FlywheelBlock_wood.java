package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.stage.copper.FlywheelBlockEntity_wood;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FlywheelBlock_wood extends GearBlock {
    private static final VoxelShape X_SHAPE = Shapes.or(
            Block.box(0.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D),
            Block.box(4.0D, 0.0D, 1.0D, 12.0D, 16.0D, 15.0D)
    );
    private static final VoxelShape Y_SHAPE = Shapes.or(
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D),
            Block.box(1.0D, 4.0D, 0.0D, 15.0D, 12.0D, 16.0D)
    );
    private static final VoxelShape Z_SHAPE = Shapes.or(
            Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 16.0D),
            Block.box(1.0D, 0.0D, 4.0D, 15.0D, 16.0D, 12.0D)
    );

    public FlywheelBlock_wood() {
        super(BlockBehaviour.Properties.of()
                .strength(4.0F)
                .noOcclusion());

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.EAST
                        )
        );
    }

    @Override
    protected BlockEntity createBlockEntity(
            BlockPos pos,
            BlockState state) {
        return new FlywheelBlockEntity_wood(
                pos,
                state
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context) {
        BlockState placementState =
                defaultBlockState()
                        .setValue(
                                FACING,
                                context.getClickedFace()
                        );

        return validateGearPlacement(
                context,
                placementState
        );
    }

    @Override
    public int getPlacementGearTeeth(
            BlockState state) {
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
        return switch (
                state.getValue(FACING).getAxis()
        ) {
            case X -> X_SHAPE;
            case Y -> Y_SHAPE;
            case Z -> Z_SHAPE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return getShape(
                state,
                level,
                pos,
                context
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
        if (!player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }

        if (level.getBlockEntity(pos)
                instanceof FlywheelBlockEntity_wood flywheel) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.magneticraft2.flywheel_status",
                                String.format(
                                        java.util.Locale.ROOT,
                                        "%.1f",
                                        flywheel.getStoredSpeed()
                                )
                        ),
                        true
                );
            }

            return InteractionResult
                    .sidedSuccess(
                            level.isClientSide
                    );
        }

        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
                                .FLYWHEEL_BE_WOOD
                                .get(),
                        FlywheelBlockEntity_wood
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
