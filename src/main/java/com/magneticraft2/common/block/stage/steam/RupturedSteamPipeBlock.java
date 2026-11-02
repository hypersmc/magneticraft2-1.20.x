package com.magneticraft2.common.block.stage.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Persisted server-ruptured segment. No block entity, no wear ticking.
 * The network solver owns the leak's actual mass/energy loss. Client-side
 * particles and hiss are visible only while the network marks it active.
 */
public final class RupturedSteamPipeBlock extends SteamPipeBlock {
    public static final DirectionProperty LEAK = DirectionProperty.create("leak");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public RupturedSteamPipeBlock() {
        super();
        registerDefaultState(defaultBlockState()
                .setValue(LEAK, Direction.UP)
                .setValue(ACTIVE, false));
    }

    @Override protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEAK, ACTIVE);
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos,
            RandomSource random) {
        if (!state.getValue(ACTIVE)) return;
        Direction face = state.getValue(LEAK);
        if (random.nextInt(3) == 0) {
            double x = pos.getX() + 0.5 + 0.47 * face.getStepX();
            double y = pos.getY() + 0.5 + 0.47 * face.getStepY();
            double z = pos.getZ() + 0.5 + 0.47 * face.getStepZ();
            level.addParticle(ParticleTypes.CLOUD, x, y, z,
                    face.getStepX() * 0.05, 0.02 + face.getStepY() * 0.04,
                    face.getStepZ() * 0.05);
        }
        if (random.nextInt(24) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS, 0.12F, 1.35F, false);
        }
    }
}
