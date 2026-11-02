package com.magneticraft2.common.block.stage.steam;

import com.magneticraft2.common.block.stage.bronze.BronzeAirDuctBlock;
import com.magneticraft2.common.systems.PRESSURE.CapabilityPressurizedGas;
import com.magneticraft2.common.systems.PRESSURE.GasMedium;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Passive steam-only pipe. Does not connect to bronze AIR ducts or liquid
 * pipes, store fluid, tick, or allocate shapes for each collision check.
 */
public final class SteamPipeBlock extends Block {
    public SteamPipeBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F).noOcclusion());
        BlockState state = stateDefinition.any();
        for (Direction d : Direction.values())
            state = state.setValue(BronzeAirDuctBlock.property(d), false);
        registerDefaultState(state);
    }
    private static boolean connects(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockPos neighbour = pos.relative(dir);
        if (!level.hasChunkAt(neighbour)) return false;
        BlockState state = level.getBlockState(neighbour);
        if (state.getBlock() instanceof SteamPipeBlock) return true;
        BlockEntity entity = level.getBlockEntity(neighbour);
        if (entity == null) return false;
        var gas = entity.getCapability(
                CapabilityPressurizedGas.GAS, dir.getOpposite()).orElse(null);
        return gas != null && gas.medium() == GasMedium.STEAM
                && (gas.canReceive() || gas.canExtract());
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values())
            state = state.setValue(BronzeAirDuctBlock.property(d),
                    connects(context.getLevel(), context.getClickedPos(), d));
        return state;
    }
    @Override public BlockState updateShape(BlockState state, Direction direction,
            BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return state.setValue(BronzeAirDuctBlock.property(direction),
                connects(level, pos, direction));
    }
    @Override protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BronzeAirDuctBlock.NORTH, BronzeAirDuctBlock.EAST,
                BronzeAirDuctBlock.SOUTH, BronzeAirDuctBlock.WEST,
                BronzeAirDuctBlock.UP, BronzeAirDuctBlock.DOWN);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return BronzeAirDuctBlock.shapeForConnections(state);
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
