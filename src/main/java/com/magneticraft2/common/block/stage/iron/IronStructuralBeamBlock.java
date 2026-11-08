package com.magneticraft2.common.block.stage.iron;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An axis-placed wrought I-beam for factory construction.
 * Visual and collision cross sections match on all three axes.
 * Ordinary static block: no ticker, power simulation or block entity.
 */
public final class IronStructuralBeamBlock extends RotatedPillarBlock {
    private static final VoxelShape Y = Shapes.or(
            Block.box(3, 0, 3, 13, 16, 5),
            Block.box(3, 0, 11, 13, 16, 13),
            Block.box(7, 0, 5, 9, 16, 11)).optimize();
    // The axis=x blockstate applies x=90, y=90 to the Y-axis model.
    // Its flanges are therefore horizontal (thin in Y), not thin in Z.
    // Keep the selection/collision boxes in the same orientation as the
    // rendered I-beam instead of rotating only the texture/model.
    private static final VoxelShape X = Shapes.or(
            Block.box(0, 3, 3, 16, 5, 13),
            Block.box(0, 11, 3, 16, 13, 13),
            Block.box(0, 5, 7, 16, 11, 9)).optimize();
    private static final VoxelShape Z = Shapes.or(
            Block.box(3, 3, 0, 13, 5, 16),
            Block.box(3, 11, 0, 13, 13, 16),
            Block.box(7, 5, 0, 9, 11, 16)).optimize();

    public IronStructuralBeamBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F, 6.0F)
                .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    }

    @Override public VoxelShape getShape(BlockState state,
            BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis axis = state.getValue(AXIS);
        return axis == Direction.Axis.X ? X : axis == Direction.Axis.Z ? Z : Y;
    }

    @Override public VoxelShape getCollisionShape(BlockState state,
            BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
