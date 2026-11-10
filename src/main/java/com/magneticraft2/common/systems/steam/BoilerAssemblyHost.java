package com.magneticraft2.common.systems.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/** Optional lifecycle contract used only by V2 fireboxes and their linked vessel. */
public interface BoilerAssemblyHost {
    record Link(BlockPos position, UUID identity) { }

    UUID boilerAssemblyId();
    BlockPos boilerAssemblyPosition();
    boolean boilerAssemblyTracked();
    boolean boilerAssemblyFormed();
    Map<BlockPos, BlockState> boilerOriginalBlocks();
    @Nullable Link boilerChild();

    /** Clear/consume the existing MultiblockController ledger, not a duplicate one. */
    Map<BlockPos, BlockState> claimBoilerOriginalBlocks();
}
