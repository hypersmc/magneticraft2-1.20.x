package com.magneticraft2.common.block.stage.bronze;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeFoundryBlockEntity;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** 3x2x3 Foundry shell. The formation blueprint remains datapack JSON. */
public final class BronzeFoundryBlock extends BaseBlockMagneticraft2 {
    public static final BooleanProperty IS_FORMED = BooleanProperty.create("is_formed");
    // Controller at south/front, bottom center; leave central crucible and front access open.
    private static final VoxelShape FORMED_SOUTH = VoxelShapeUtils.combine(
            Block.box(-16, 0, -32, 32, 16, 16),
            Block.box(-16, 16, -32, 32, 32, -16),
            Block.box(-16, 16, -16, 0, 32, 16),
            Block.box(16, 16, -16, 32, 32, 16));
    private static final VoxelShape FORMED_NORTH =
            VoxelShapeUtils.rotate(FORMED_SOUTH, Rotation.CLOCKWISE_180);
    private static final VoxelShape FORMED_EAST =
            VoxelShapeUtils.rotate(FORMED_SOUTH, Rotation.COUNTERCLOCKWISE_90);
    private static final VoxelShape FORMED_WEST =
            VoxelShapeUtils.rotate(FORMED_SOUTH, Rotation.CLOCKWISE_90);

    public BronzeFoundryBlock() {
        super(BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops()
                .noOcclusion()
                .isSuffocating((state, world, pos) -> !state.getValue(IS_FORMED))
                .isViewBlocking((state, world, pos) -> !state.getValue(IS_FORMED)));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.SOUTH).setValue(IS_FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, IS_FORMED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection())
                .setValue(IS_FORMED, false);
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor world, BlockPos pos, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
                .setValue(IS_FORMED, false);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(world.getBlockEntity(pos) instanceof BronzeFoundryBlockEntity foundry)) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (!foundry.isFormed()) foundry.onRightClick();
        else foundry.interactable(state, world, pos, player, hand, hit);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void interactableNoGui(BlockState state, Level world, BlockPos pos,
                                      Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof BronzeFoundryBlockEntity foundry) {
            foundry.interactable(state, world, pos, player, hand, hit);
        }
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos,
                                       Player player, boolean harvest, FluidState fluid) {
        if (!world.isClientSide
                && world.getBlockEntity(pos) instanceof BaseBlockEntityMagneticraft2 foundry) {
            foundry.onDestroy(world);
        }
        return super.onDestroyedByPlayer(state, world, pos, player, harvest, fluid);
    }

    private VoxelShape formedShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case NORTH -> FORMED_NORTH;
            case EAST -> FORMED_EAST;
            case WEST -> FORMED_WEST;
            default -> FORMED_SOUTH;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return state.getValue(IS_FORMED) ? formedShape(state) : super.getShape(state, world, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world,
                                        BlockPos pos, CollisionContext context) {
        return state.getValue(IS_FORMED) ? formedShape(state) : super.getCollisionShape(state, world, pos, context);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return state.getValue(IS_FORMED) ? formedShape(state) : super.getInteractionShape(state, world, pos);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world,
                                     BlockPos pos, CollisionContext context) {
        return state.getValue(IS_FORMED) ? formedShape(state) : super.getVisualShape(state, world, pos, context);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BronzeFoundryBlockEntity(pos, state);
    }
}
