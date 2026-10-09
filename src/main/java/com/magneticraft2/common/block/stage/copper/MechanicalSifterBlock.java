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
 * Controller for the 3x2x2 Gear V2-driven Mechanical Sifter multiblock.
 */
public class MechanicalSifterBlock
        extends BaseBlockMagneticraft2 {

    public static final BooleanProperty IS_FORMED =
            BooleanProperty.create("is_formed");

    // Controller-local SOUTH geometry, in model pixels. Keep this aligned
    // with mechanical_sifter_south.json and the separate tray/guide models.
    // The screen envelope follows its 10-degree tilt and includes the full
    // +/- 0.12-block crank stroke, without treating the whole machine as solid.
    // The individual copper mesh wires are intentionally not collision boxes.
    private static final VoxelShape FORMED_SOUTH =
            VoxelShapeUtils.combine(
                    // Floor runners and split front feet.
                    Block.box(-12, 0, 0, 15.5, 3, 4),
                    Block.box(-12, 0, 4, -8, 3, 31.5),
                    Block.box(0.5, 0, 15.5, 15.5, 3, 32),
                    Block.box(15.5, 0, 0, 24.6, 2.9, 4),
                    Block.box(24.6, 0, 0, 27.6, 2.9, 32),
                    Block.box(-11.5, 0.15, 29.7, 2, 2.85, 31.75),
                    Block.box(13.8, 0.15, 29.7, 24.7, 2.85, 31.75),

                    // Six timber uprights, including their copper-bearing collars.
                    Block.box(-11.5, 2.25, 2.5, -6.5, 25.5, 7.5),
                    Block.box(-11.5, 2.25, 11, -6.5, 25.5, 16),
                    Block.box(23.5, 16.25, 2.5, 28.5, 25.5, 7.5),
                    Block.box(23.5, 16.25, 11, 28.5, 25.5, 16),
                    Block.box(24, 2.55, 2.25, 28.4, 17.2, 7.5),
                    Block.box(23.5, 2.55, 11, 28.4, 17.2, 16),

                    // Upper wooden frame, front and rear hopper crossbars.
                    Block.box(-11, 22.5, 4, -7, 26, 24),
                    Block.box(24, 22.5, 4, 28, 26, 24),
                    Block.box(-7.25, 22.5, 4, 24.25, 26, 8),
                    Block.box(-7.25, 22.5, 20, 24.25, 26, 24),
                    Block.box(-9.5, 16.4, 14, 26.5, 18.2, 15.5),

                    // Hollow feed hopper (walls, not a solid blocking volume).
                    Block.box(-4, 19, -0.25, -0.25, 30, 16.25),
                    Block.box(16.25, 19, -0.25, 20, 30, 16.25),
                    Block.box(-0.25, 22, -4, 16.25, 30, -0.25),
                    Block.box(-0.25, 22, 16.25, 16.25, 27, 19.5),
                    Block.box(-4.5, 20, -0.5, -2.25, 29.5, 2.5),
                    Block.box(18.25, 20, -0.5, 20.5, 29.5, 2.5),
                    Block.box(3, 13.5, 11, 6, 16, 16),
                    Block.box(10, 13.5, 11, 13, 16, 16),

                    // Stationary sieve guide frame and parallel wooden slides.
                    Block.box(-7, 10.5, 10.5, -4, 14.5, 24),
                    Block.box(20, 10.5, 16.25, 23, 14.5, 24),
                    Block.box(-7, 7, 10.5, 15.5, 9.75, 14),
                    Block.box(0.5, 6.5, 24, 15.5, 9.5, 27.5),
                    Block.box(1.6, 8.8, 12, 2.45, 10, 26.25),
                    Block.box(13.55, 8.8, 12, 14.4, 10, 26.25),

                    // Two open output chutes (floor plus side walls).
                    Block.box(-14.5, 10.75, 18, -1.5, 12.5, 30),
                    Block.box(-15.5, 10.5, 18, -13, 15.5, 30),
                    Block.box(-3, 10.5, 18, -0.5, 15.5, 30),
                    Block.box(17.5, 10.75, 18, 30.5, 12.5, 30),
                    Block.box(16.5, 10.5, 18, 19, 15.5, 30),
                    Block.box(29, 10.5, 18, 31.5, 15.5, 30),

                    // Lower fines catch pan, chute and its grounded wooden saddles.
                    Block.box(3, 6, 15, 13, 7, 24),
                    Block.box(2.5, 5.8, 14.5, 4.2, 9, 24.2),
                    Block.box(11.8, 5.8, 14.5, 13.5, 9, 24.2),
                    Block.box(2.5, 5.8, 22.5, 13.5, 8, 24.5),
                    Block.box(7, 5, 22.5, 12, 7, 26.5),
                    Block.box(3.3, 4.85, 16.5, 12.7, 6.15, 17.9),
                    Block.box(3.3, 4.85, 20.5, 12.7, 6.15, 21.9),
                    Block.box(3.3, 2.75, 16.5, 4.65, 5.1, 17.9),
                    Block.box(3.3, 2.75, 20.5, 4.65, 5.1, 21.9),
                    Block.box(11.35, 2.75, 16.5, 12.7, 5.1, 17.9),
                    Block.box(11.35, 2.75, 20.5, 12.7, 5.1, 21.9),

                    // Swept envelope of tilted sieve (10 degrees, +/- 2 pixels of shake).
                    Block.box(0.95, 11.7, 9.36, 15.05, 15.45, 17.14),
                    Block.box(0.95, 9.51, 21.2, 15.05, 13.21, 28.74),
                    Block.box(1.15, 10.58, 12.2, 3.15, 14.42, 26.2),
                    Block.box(12.85, 10.58, 12.2, 14.85, 14.42, 26.2),
                    Block.box(3.08, 12.53, 12.48, 13, 13.63, 19.08),
                    Block.box(3.08, 12.13, 14.79, 13, 13.23, 21.39),
                    Block.box(3.08, 11.72, 17.11, 13, 12.82, 23.71),
                    Block.box(3.08, 11.31, 19.42, 13, 12.41, 26.02)
            );

    // Cache every facing: rotating this multi-piece shape on each getShape/
    // getCollisionShape call would be unnecessarily expensive.
    private static final VoxelShape FORMED_NORTH =
            VoxelShapeUtils.rotate(FORMED_SOUTH, Rotation.CLOCKWISE_180);
    private static final VoxelShape FORMED_EAST =
            VoxelShapeUtils.rotate(FORMED_SOUTH, Rotation.COUNTERCLOCKWISE_90);
    private static final VoxelShape FORMED_WEST =
            VoxelShapeUtils.rotate(FORMED_SOUTH, Rotation.CLOCKWISE_90);

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
            case NORTH -> FORMED_NORTH;
            case EAST -> FORMED_EAST;
            case WEST -> FORMED_WEST;
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
