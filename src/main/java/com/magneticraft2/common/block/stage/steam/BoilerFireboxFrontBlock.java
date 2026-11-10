package com.magneticraft2.common.block.stage.steam;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.stage.steam.BoilerFireboxBlockEntity;
import com.magneticraft2.common.item.stage.steam.BoilerServiceFittingItem;
import com.magneticraft2.common.systems.steam.BoilerAssemblyRecovery;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
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

/** Non-ticking front/controller for the separate refractory firebox extension. */
public final class BoilerFireboxFrontBlock extends BaseBlockMagneticraft2 {
    public static final BooleanProperty IS_FORMED = BooleanProperty.create("is_formed");

    public BoilerFireboxFrontBlock() {
        super(BlockBehaviour.Properties.of().strength(5F, 8F).sound(SoundType.METAL)
                .requiresCorrectToolForDrops().noOcclusion().dynamicShape());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(IS_FORMED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, IS_FORMED);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.getValue(IS_FORMED) ? state : state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
        return rotate(state, rotation);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof BoilerServiceFittingItem) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof BoilerFireboxBlockEntity firebox) firebox.use(player);
        return InteractionResult.CONSUME;
    }
    @Override protected void interactableNoGui(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof BoilerFireboxBlockEntity firebox) firebox.use(player);
    }
    @Override public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos,
            Player player, boolean harvest, FluidState fluid) {
        if (state.getValue(IS_FORMED)) {
            BoilerAssemblyRecovery.playerBreak(level, pos, player);
            // The assembly coordinator returns the front itself. Suppress vanilla's second drop.
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, harvest, fluid);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (state.getBlock() != replacement.getBlock() && state.getValue(IS_FORMED))
            BoilerAssemblyRecovery.unexpectedRemoval(level, pos);
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (state.getValue(IS_FORMED) && world.getBlockEntity(pos) instanceof BoilerFireboxBlockEntity firebox
                && firebox.localSelection() != null) return firebox.localSelection();
        return super.getShape(state, world, pos, context);
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (state.getValue(IS_FORMED) && world.getBlockEntity(pos) instanceof BoilerFireboxBlockEntity firebox
                && firebox.localCollision() != null) return firebox.localCollision();
        return super.getCollisionShape(state, world, pos, context);
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BoilerFireboxBlockEntity(pos, state); }
}
