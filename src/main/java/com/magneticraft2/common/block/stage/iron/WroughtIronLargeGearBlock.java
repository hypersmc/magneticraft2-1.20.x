package com.magneticraft2.common.block.stage.iron;

import com.magneticraft2.common.block.stage.copper.LargeGearBlock_wood;
import com.magneticraft2.common.blockentity.stage.copper.LargeGearBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.iron.WroughtIronLargeGearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Large, 16-tooth iron gear: existing placement, angle, mesh and network semantics. */
public class WroughtIronLargeGearBlock extends LargeGearBlock_wood {
    private static final VoxelShape IRON_SHAPE = Shapes.or(
            // Real iron axle is 2x2 pixels, unlike the 6x6 wooden shaft.
            net.minecraft.world.level.block.Block.box(7,0,7,9,16,9),
            // The rim extends outside its controller block like the wooden large gear.
            net.minecraft.world.level.block.Block.box(-7,6.85,-7,23,9.15,23)).optimize();

    public WroughtIronLargeGearBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F, 6.0F)
                .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    }

    @Override public VoxelShape downshape() { return IRON_SHAPE; }

    @Override protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WroughtIronLargeGearBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, BlockEntityRegistry.GEAR_LARGE_BE_IRON.get(),
                        LargeGearBlockEntity_wood::serverTick);
    }
}
