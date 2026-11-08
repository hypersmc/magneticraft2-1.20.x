package com.magneticraft2.common.block.stage.iron;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Open riveted wrought-iron factory frame with matching twelve-beam collision. */
public final class RivetedIronFrameBlock extends Block {
    private static final VoxelShape FRAME = Shapes.or(
            Block.box(1, 1, 1, 4, 15, 4),
            Block.box(12, 1, 1, 15, 15, 4),
            Block.box(1, 1, 12, 4, 15, 15),
            Block.box(12, 1, 12, 15, 15, 15),
            Block.box(4, 1, 1, 12, 4, 4),
            Block.box(4, 1, 12, 12, 4, 15),
            Block.box(1, 1, 4, 4, 4, 12),
            Block.box(12, 1, 4, 15, 4, 12),
            Block.box(4, 12, 1, 12, 15, 4),
            Block.box(4, 12, 12, 12, 15, 15),
            Block.box(1, 12, 4, 4, 15, 12),
            Block.box(12, 12, 4, 15, 15, 12)).optimize();

    public RivetedIronFrameBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F, 6.0F)
                .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return FRAME;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                        BlockPos pos, CollisionContext context) {
        return FRAME;
    }
}
