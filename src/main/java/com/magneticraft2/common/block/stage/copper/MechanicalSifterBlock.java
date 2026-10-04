package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalSifterBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.utils.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Controller for the 3x2x2 crank-driven Mechanical Sifter multiblock.
 */
public class MechanicalSifterBlock
        extends BaseBlockMagneticraft2 {

    public static final BooleanProperty IS_FORMED =
            BooleanProperty.create("is_formed");

    private static final VoxelShape FORMED_SOUTH =
            Block.box(
                    -16.0D, 0.0D, 0.0D,
                    32.0D, 32.0D, 32.0D
            );

    public MechanicalSifterBlock() {
        super(
                BlockBehaviour.Properties.of()
                        .strength(3.5F)
                        .noOcclusion()
                        .isSuffocating(
                                (state, level, pos) ->
                                        !state.getValue(IS_FORMED)
                        )
                        .isViewBlocking(
                                (state, level, pos) ->
                                        !state.getValue(IS_FORMED)
                        )
        );

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.SOUTH)
                        .setValue(IS_FORMED, false)
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection()
                )
                .setValue(IS_FORMED, false);
    }

    @Override
    public BlockState rotate(
            BlockState state,
            LevelAccessor level,
            BlockPos pos,
            Rotation rotation) {
        return state
                .setValue(
                        FACING,
                        rotation.rotate(
                                state.getValue(FACING)
                        )
                )
                .setValue(IS_FORMED, false);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, IS_FORMED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(IS_FORMED)
                ? RenderShape.INVISIBLE
                : RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (!(blockEntity
                instanceof MechanicalSifterBlockEntity sifter)) {
            return super.use(
                    state,
                    level,
                    pos,
                    player,
                    hand,
                    hit
            );
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!sifter.isFormed()) {
            sifter.onRightClick();
        } else {
            sifter.interactable(
                    state,
                    level,
                    pos,
                    player,
                    hand,
                    hit
            );
        }

        return InteractionResult.CONSUME;
    }

    @Override
    protected void interactableNoGui(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (level.getBlockEntity(pos)
                instanceof MechanicalSifterBlockEntity sifter) {
            sifter.interactable(
                    state,
                    level,
                    pos,
                    player,
                    hand,
                    hit
            );
        }
    }

    @Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            boolean willHarvest,
            FluidState fluid) {
        if (!level.isClientSide
                && level.getBlockEntity(pos)
                instanceof BaseBlockEntityMagneticraft2 controller) {
            controller.onDestroy(level);
        }

        return super.onDestroyedByPlayer(
                state,
                level,
                pos,
                player,
                willHarvest,
                fluid
        );
    }

    @Override
    public VoxelShape getVisualShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return getFormedShape(state);
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return state.getValue(IS_FORMED)
                ? getFormedShape(state)
                : super.getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return state.getValue(IS_FORMED)
                ? getFormedShape(state)
                : super.getCollisionShape(
                        state,
                        level,
                        pos,
                        context
                );
    }

    private VoxelShape getFormedShape(BlockState state) {
        Direction facing = state.getValue(FACING);

        return switch (facing) {
            case SOUTH -> FORMED_SOUTH;
            case NORTH -> VoxelShapeUtils.rotateHorizontal(
                    FORMED_SOUTH,
                    Direction.SOUTH
            );
            case EAST -> VoxelShapeUtils.rotateHorizontal(
                    FORMED_SOUTH,
                    Direction.WEST
            );
            case WEST -> VoxelShapeUtils.rotateHorizontal(
                    FORMED_SOUTH,
                    Direction.EAST
            );
            default -> FORMED_SOUTH;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state) {
        return new MechanicalSifterBlockEntity(
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
                                .MECHANICAL_SIFTER_BE
                                .get(),
                        MechanicalSifterBlockEntity
                                ::serverTick
                );
    }
}
