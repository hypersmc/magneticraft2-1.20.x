package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.MechanicalBellowsBlockEntity;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class MechanicalBellowsBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING =
            net.minecraft.world.level.block.DirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE =
            BooleanProperty.create("active");

    private static final VoxelShape X_SHAPE = Shapes.or(
            Block.box(0.0D, 2.0D, 2.0D, 16.0D, 14.0D, 14.0D),
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 2.0D, 14.0D)
    );
    private static final VoxelShape Y_SHAPE = Shapes.or(
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D),
            Block.box(2.0D, 2.0D, 0.0D, 14.0D, 14.0D, 16.0D)
    );
    private static final VoxelShape Z_SHAPE = Shapes.or(
            Block.box(2.0D, 2.0D, 0.0D, 14.0D, 14.0D, 16.0D),
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 2.0D, 14.0D)
    );

    public MechanicalBellowsBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.5F)
                .noOcclusion());

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(ACTIVE, false)
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getClickedFace()
                )
                .setValue(ACTIVE, false);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return switch (state.getValue(FACING).getAxis()) {
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
        return getShape(state, level, pos, context);
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
                instanceof MechanicalBellowsBlockEntity bellows)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.magneticraft2.mechanical_bellows_status",
                            bellows.getStoredAir(),
                            bellows.getMaxStoredAir(),
                            bellows.hasValidCrank()
                                    ? Component.translatable(
                                            "message.magneticraft2.mechanical_bellows_connected"
                                    )
                                    : Component.translatable(
                                            "message.magneticraft2.mechanical_bellows_disconnected"
                                    )
                    ),
                    true
            );
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
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
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state) {
        return new MechanicalBellowsBlockEntity(
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
                                .MECHANICAL_BELLOWS_BE
                                .get(),
                        MechanicalBellowsBlockEntity
                                ::serverTick
                );
    }
}
