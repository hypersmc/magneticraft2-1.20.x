package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lightweight upper half of the Mechanical Transfer Arm.
 *
 * The controller, inventory state and Gear V2 node remain in the lower block.
 * This block reserves the second vertical block so the machine is genuinely
 * 1x2x1 instead of rendering through whatever happens to be placed above it.
 */
public class MechanicalTransferArmUpperBlock extends DirectionalBlock {

    public MechanicalTransferArmUpperBlock() {
        super(
                BlockBehaviour.Properties.of()
                        .strength(3.5F)
                        .noOcclusion()
        );

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos) {
        return level.getBlockState(
                        pos.below()
                )
                .is(
                        BlockRegistry
                                .MECHANICAL_TRANSFER_ARM
                                .get()
                );
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        // The articulated arm moves through this reserved block volume. Keep
        // placement occupancy without pretending the whole upper cube is solid.
        return Shapes.empty();
    }

    @Override
    public void playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player) {
        if (!level.isClientSide
                && player.isCreative()
                && level.getBlockState(
                        pos.below()
                )
                .is(
                        BlockRegistry
                                .MECHANICAL_TRANSFER_ARM
                                .get()
                )) {
            level.destroyBlock(
                    pos.below(),
                    false,
                    player
            );
        }

        super.playerWillDestroy(
                level,
                pos,
                state,
                player
        );
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (!level.isClientSide
                && !state.is(
                newState.getBlock()
        )
                && level.getBlockState(
                        pos.below()
                )
                .is(
                        BlockRegistry
                                .MECHANICAL_TRANSFER_ARM
                                .get()
                )) {
            level.destroyBlock(
                    pos.below(),
                    true
            );
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
        );
    }
}
