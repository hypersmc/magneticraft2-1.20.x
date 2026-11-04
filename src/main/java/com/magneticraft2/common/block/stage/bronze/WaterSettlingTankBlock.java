package com.magneticraft2.common.block.stage.bronze;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.stage.bronze.WaterSettlingTankBlockEntity;
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

/**
 * Passive Bronze water treatment. The physical 5x3x5 construction and
 * fluid/item modules are defined in the four datapack multiblock blueprints.
 */
public final class WaterSettlingTankBlock extends BaseBlockMagneticraft2 {
    public static final BooleanProperty IS_FORMED = BooleanProperty.create("is_formed");

    public WaterSettlingTankBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.5F).requiresCorrectToolForDrops().noOcclusion());
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor world, BlockPos pos, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
                .setValue(IS_FORMED, false);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof WaterSettlingTankBlockEntity tank))
            return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!tank.isFormed()) tank.onRightClick();
        if (tank.isFormed()) tank.interactable(state, level, pos, player, hand, hit);
        else player.displayClientMessage(Component.translatable(
                "message.magneticraft2.water_settling_tank.incomplete"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void interactableNoGui(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof WaterSettlingTankBlockEntity tank)
            tank.interactable(state, level, pos, player, hand, hit);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos,
            Player player, boolean harvest, FluidState fluid) {
        if (!level.isClientSide &&
                level.getBlockEntity(pos) instanceof WaterSettlingTankBlockEntity tank)
            tank.onDestroy(level);
        return super.onDestroyedByPlayer(state, level, pos, player, harvest, fluid);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
            CollisionContext context) {
        if (state.getValue(IS_FORMED)
                && world.getBlockEntity(pos) instanceof WaterSettlingTankBlockEntity tank
                && tank.getLocalVoxelSelection() != null)
            return tank.getLocalVoxelSelection();
        return super.getShape(state, world, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos,
            CollisionContext context) {
        if (state.getValue(IS_FORMED)
                && world.getBlockEntity(pos) instanceof WaterSettlingTankBlockEntity tank
                && tank.getLocalVoxelCollision() != null)
            return tank.getLocalVoxelCollision();
        return super.getCollisionShape(state, world, pos, context);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterSettlingTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, BlockEntityRegistry.WATER_SETTLING_TANK_BE.get(),
                        WaterSettlingTankBlockEntity::serverTick);
    }
}
