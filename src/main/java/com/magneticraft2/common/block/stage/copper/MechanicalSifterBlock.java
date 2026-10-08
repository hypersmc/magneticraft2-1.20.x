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

    // Canonical SOUTH collision mirrors the open classifier frame and
    // moving screen volume rather than using one opaque 3x2x2 cuboid.
    private static final VoxelShape FORMED_SOUTH =
            VoxelShapeUtils.combine(
                    // Heavy base frame.
                    Block.box(-12, 0, 0, 28, 3, 4),
                    Block.box(-12, 0, 28, 28, 3, 32),
                    Block.box(-12, 0, 4, -8, 3, 28),
                    Block.box(24, 0, 4, 28, 3, 28),

                    // Four structural posts.
                    Block.box(-11, 3, 3, -7, 27, 7),
                    Block.box(24, 3, 3, 28, 27, 7),
                    Block.box(-11, 3, 25, -7, 27, 29),
                    Block.box(24, 3, 25, 28, 27, 29),

                    // Upper frame + hopper.
                    Block.box(-11, 24, 4, 28, 27, 28),
                    Block.box(-5, 21, 2, 21, 30, 18),

                    // Moving screen carriage and support rails.
                    Block.box(-7, 8, 11, 23, 16, 28),

                    // Lower fines catch tray.
                    Block.box(-3, 6, 13, 19, 10, 28),

                    // Coarse and fine discharge sections.
                    Block.box(-11, 5, 24, 6, 12, 34),
                    Block.box(4, 4, 21, 20, 11, 30),

                    // Right-side drive tower.
                    Block.box(23, 6, 10, 30, 22, 24)
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
        return RenderShape.MODEL;
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
    public VoxelShape getInteractionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos) {
        return state.getValue(IS_FORMED)
                ? getFormedShape(state)
                : super.getInteractionShape(
                        state,
                        level,
                        pos
                );
    }

    @Override
    public VoxelShape getVisualShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return state.getValue(IS_FORMED)
                ? getFormedShape(state)
                : super.getVisualShape(
                        state,
                        level,
                        pos,
                        context
                );
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
            case NORTH -> VoxelShapeUtils.rotate(
                    FORMED_SOUTH,
                    Rotation.CLOCKWISE_180
            );
            case EAST -> VoxelShapeUtils.rotate(
                    FORMED_SOUTH,
                    Rotation.COUNTERCLOCKWISE_90
            );
            case WEST -> VoxelShapeUtils.rotate(
                    FORMED_SOUTH,
                    Rotation.CLOCKWISE_90
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
