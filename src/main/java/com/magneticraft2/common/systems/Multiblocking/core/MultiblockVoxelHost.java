package com.magneticraft2.common.systems.Multiblocking.core;

import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Implemented by a V2 controller BE that holds local, packet-synced voxel
 * snapshots. Other machine controllers do not need to implement this.
 */
public interface MultiblockVoxelHost {
    void setMultiblockVoxelShapes(VoxelShape collision, VoxelShape selection);
}
