package com.magneticraft2.common.block.stage.bronze;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.stage.bronze.CharcoalRetortBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
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

/** Sealed 3x4x3 fuel-fired retort; all structure patterns are JSON-defined. */
public final class CharcoalRetortBlock extends BaseBlockMagneticraft2 {
    public static final BooleanProperty IS_FORMED = BooleanProperty.create("is_formed");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public CharcoalRetortBlock() {
        super(BlockBehaviour.Properties.of().strength(3.5F)
                .requiresCorrectToolForDrops().noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.SOUTH).setValue(IS_FORMED, false).setValue(LIT, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, IS_FORMED, LIT);
    }

    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
                .setValue(IS_FORMED, false).setValue(LIT, false);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof CharcoalRetortBlockEntity retort))
            return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!retort.isFormed()) retort.onRightClick();
        if (retort.isFormed()) retort.interactable(state, level, pos, player, hand, hit);
        else player.displayClientMessage(Component.translatable(
                "message.magneticraft2.charcoal_retort.incomplete"), true);
        return InteractionResult.CONSUME;
    }

    @Override protected void interactableNoGui(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CharcoalRetortBlockEntity retort)
            retort.interactable(state, level, pos, player, hand, hit);
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(IS_FORMED) || !state.getValue(LIT) || random.nextInt(5) != 0)
            return;
        BlockPos chimney = pos.above(3).relative(state.getValue(FACING).getOpposite());
        level.addParticle(ParticleTypes.SMOKE, chimney.getX() + .5D, chimney.getY() + 1.05D,
                chimney.getZ() + .5D, 0D, 0.035D, 0D);
    }

    @Override public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos,
            Player player, boolean harvest, FluidState fluid) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CharcoalRetortBlockEntity retort)
            retort.onDestroy(level);
        return super.onDestroyedByPlayer(state, level, pos, player, harvest, fluid);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        if (state.getValue(IS_FORMED)
                && level.getBlockEntity(pos) instanceof CharcoalRetortBlockEntity retort
                && retort.getLocalVoxelSelection() != null) return retort.getLocalVoxelSelection();
        return super.getShape(state, level, pos, context);
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        if (state.getValue(IS_FORMED)
                && level.getBlockEntity(pos) instanceof CharcoalRetortBlockEntity retort
                && retort.getLocalVoxelCollision() != null) return retort.getLocalVoxelCollision();
        return super.getCollisionShape(state, level, pos, context);
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CharcoalRetortBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,
                BlockEntityRegistry.CHARCOAL_RETORT_BE.get(), CharcoalRetortBlockEntity::serverTick);
    }
}
