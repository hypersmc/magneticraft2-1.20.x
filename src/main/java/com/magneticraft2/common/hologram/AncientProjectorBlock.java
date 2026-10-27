package com.magneticraft2.common.hologram;

import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class AncientProjectorBlock extends BaseEntityBlock {
    // Compact physical artifact: nine pixels across, under four pixels high.
    private static final VoxelShape SHAPE = box(3.5D, 0D, 3.5D, 12.5D, 4D, 12.5D);

    public AncientProjectorBlock() {
        super(BlockBehaviour.Properties.of().strength(1.5F).noOcclusion()
                .lightLevel(state -> 8));
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
                               CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world,
                                        BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * All projector removals return the original artifact through one path.
     * The block entity is available until BaseEntityBlock.onRemove runs.
     * Neither breaking nor the timed completion should issue loot separately.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState nextState, boolean moving) {
        if (!state.is(nextState.getBlock()) && !level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AncientProjectorBlockEntity projector) {
                ItemStack artifact = projector.takeArtifact();
                if (!artifact.isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level,
                            pos.getX() + 0.5D, pos.getY() + 0.3D,
                            pos.getZ() + 0.5D, artifact));
                }
            }
        }
        super.onRemove(state, level, pos, nextState, moving);
    }

    /** The artifact is returned by onRemove; vanilla loot must not duplicate it. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AncientProjectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,
                BlockEntityRegistry.ANCIENT_PROJECTOR_BE.get(), AncientProjectorBlockEntity::tick);
    }
}
