package com.magneticraft2.common.block.stage.iron;

import com.magneticraft2.common.block.stage.copper.ShaftBlock_wood;
import com.magneticraft2.common.blockentity.stage.copper.ShaftBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.iron.WroughtIronShaftBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Existing Gear V2 axial 1:1 shaft connection, forged iron material. */
public final class WroughtIronShaftBlock extends ShaftBlock_wood {
    // Match the existing 2x2 through-axle on the Iron Medium Gear.
    // Never reuse the 6x6 wooden shaft outline for this metal model.
    private static final VoxelShape SHAPE_Y = Block.box(7, 0, 7, 9, 16, 9);
    private static final VoxelShape SHAPE_X = Block.box(0, 7, 7, 16, 9, 9);
    private static final VoxelShape SHAPE_Z = Block.box(7, 7, 0, 9, 9, 16);

    public WroughtIronShaftBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F, 6.0F)
                .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        Direction.Axis axis = state.getValue(FACING).getAxis();
        return axis == Direction.Axis.X ? SHAPE_X
                : axis == Direction.Axis.Z ? SHAPE_Z : SHAPE_Y;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WroughtIronShaftBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
            BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null :
                createTickerHelper(type, BlockEntityRegistry.SHAFT_BE_IRON.get(),
                        ShaftBlockEntity_wood::serverTick);
    }
}
