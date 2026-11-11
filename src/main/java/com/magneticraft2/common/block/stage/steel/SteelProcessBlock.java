package com.magneticraft2.common.block.stage.steel;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.stage.steel.SteelProcessBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

/** Three physical furnace controllers, shared verified thermal/process backend. */
public final class SteelProcessBlock extends BaseBlockMagneticraft2 {
    public static final BooleanProperty IS_FORMED = BooleanProperty.create("is_formed");
    private final ResourceLocation machine;

    public SteelProcessBlock(String id) {
        super(BlockBehaviour.Properties.of().strength(5F, 6F)
                .requiresCorrectToolForDrops().noOcclusion());
        machine = new ResourceLocation("magneticraft2", id);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH)
                .setValue(IS_FORMED, false));
    }
    public ResourceLocation machine() { return machine; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, IS_FORMED);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }
    @Override public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
                .setValue(IS_FORMED, false);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof SteelProcessBlockEntity machine))
            return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!machine.isFormed()) machine.onRightClick();
        if (machine.isFormed()) machine.interactable(state,level,pos,player,hand,hit);
        else player.displayClientMessage(Component.literal("Steel furnace incomplete; use the book's multiblock Visualize."),true);
        return InteractionResult.CONSUME;
    }
    @Override protected void interactableNoGui(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof SteelProcessBlockEntity furnace)
            furnace.interactable(state,level,pos,player,hand,hit);
    }
    @Override public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos,
            Player player, boolean harvest, FluidState fluid) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SteelProcessBlockEntity furnace)
            furnace.onDestroy(level);
        return super.onDestroyedByPlayer(state,level,pos,player,harvest,fluid);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
            CollisionContext context) {
        if (state.getValue(IS_FORMED) && world.getBlockEntity(pos) instanceof SteelProcessBlockEntity furnace
                && furnace.getLocalVoxelSelection() != null)
            return furnace.getLocalVoxelSelection();
        return super.getShape(state,world,pos,context);
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos,
            CollisionContext context) {
        if (state.getValue(IS_FORMED) && world.getBlockEntity(pos) instanceof SteelProcessBlockEntity furnace
                && furnace.getLocalVoxelCollision() != null)
            return furnace.getLocalVoxelCollision();
        return super.getCollisionShape(state,world,pos,context);
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteelProcessBlockEntity(pos,state);
    }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type,BlockEntityRegistry.STEEL_PROCESS_BE.get(),
                        SteelProcessBlockEntity::serverTick);
    }
}
