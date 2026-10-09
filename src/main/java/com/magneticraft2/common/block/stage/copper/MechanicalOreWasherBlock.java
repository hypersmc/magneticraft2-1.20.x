package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalOreWasherBlockEntity;
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
 * Controller for the 3x2x3 Mechanical Ore Washer multiblock.
 *
 * Unformed it is only the controller crate. Right-click after building the
 * JSON-defined structure to form the full trommel machine.
 */
public class MechanicalOreWasherBlock
        extends BaseBlockMagneticraft2 {

    public static final BooleanProperty IS_FORMED =
            BooleanProperty.create("is_formed");

    // Canonical SOUTH collision follows the actual formed machine instead
    // of claiming the whole 3x2x3 bounding box is solid.
    private static final VoxelShape FORMED_SOUTH =
            VoxelShapeUtils.combine(
                    // Timber base / lower frame from the authoritative SOUTH model.
                    Block.box(-10, 0, -13, -7, 3, 29),
                    Block.box(23, 0, -13, 26, 3, 29),
                    Block.box(-12, 2, -11, 28, 5, -8),
                    Block.box(-12, 2, 7, 28, 5, 10),
                    Block.box(-12, 2, 26, 28, 5, 29),
                    Block.box(-7, 5, -5, 23, 7, 23),

                    // Updated uprights / top braces.
                    Block.box(-8, 9, -7, -5, 27, -4),
                    Block.box(21, 9, -7, 24, 27, -4),
                    Block.box(-8, 9, 22, -5, 27, 25),
                    Block.box(21, 9, 22, 24, 27, 25),
                    Block.box(-8, 24, -7, 24, 27, -4),
                    Block.box(-8, 24, 22, 24, 27, 25),

                    // Trommel / central working envelope.
                    Block.box(2, 9, -4, 14, 20, 22),

                    // Updated stepped input chute.
                    Block.box(2, 8, -14, 14, 10, -10),
                    Block.box(2, 10, -10, 14, 12, -6),
                    Block.box(2, 12, -6, 14, 14, -2),
                    Block.box(1, 9, -14, 15, 15, -1),

                    // Updated output chute moved farther SOUTH.
                    Block.box(2, 9, 21, 14, 11, 26),
                    Block.box(2, 7, 26, 14, 10, 31),
                    Block.box(1, 8, 21, 15, 13, 31),

                    // Side fluid housings.
                    Block.box(-9, 5, -9, 0, 13, 27),
                    Block.box(16, 5, -9, 25, 13, 27)
            );

    public MechanicalOreWasherBlock() {
        super(
                BlockBehaviour.Properties.of()
                        .strength(4.0F)
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
        // The formed static body is a normal baked block model. Keeping it in
        // chunk rendering is dramatically cheaper than resubmitting the entire
        // multiblock through a BER every frame.
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
                instanceof MechanicalOreWasherBlockEntity washer)) {
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

        if (!washer.isFormed()) {
            washer.onRightClick();
        } else {
            washer.interactable(
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
                instanceof MechanicalOreWasherBlockEntity washer) {
            washer.interactable(
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
        return new MechanicalOreWasherBlockEntity(
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
                                .MECHANICAL_ORE_WASHER_BE
                                .get(),
                        MechanicalOreWasherBlockEntity
                                ::serverTick
                );
    }
}
