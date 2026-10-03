package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.stage.copper.PulleyBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Wooden pulley endpoint for Gear V2 belt transmission.
 *
 * Pulleys behave like shaft nodes locally and may additionally form one remote
 * open-belt connection to another parallel pulley.
 */
public class PulleyBlock_wood extends GearBlock {
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    private final int pulleyTeeth;

    public PulleyBlock_wood(int pulleyTeeth) {
        super(BlockBehaviour.Properties.of().strength(2.5F).noOcclusion());
        this.pulleyTeeth = Math.max(1, pulleyTeeth);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    public int getPulleyTeeth() {
        return pulleyTeeth;
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PulleyBlockEntity_wood(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placementState = defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(POWERED, false);
        return validateGearPlacement(context, placementState);
    }

    @Override
    public int getPlacementGearTeeth(BlockState state) {
        return pulleyTeeth;
    }

    @Override
    public boolean isShaftLikeForPlacement(BlockState state) {
        return true;
    }

    @Override
    public Direction.Axis getPlacementGearAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof PulleyBlockEntity_wood pulley) {
            pulley.disconnectBelt(true);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state,
                                                                  BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntityRegistry.PULLEY_BE_WOOD.get(), PulleyBlockEntity_wood::tick);
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> serverType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker) {
        return expectedType == serverType ? (BlockEntityTicker<A>) ticker : null;
    }
}
