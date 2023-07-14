package com.magneticraft2.common.systems.Multiblocking.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public interface IMultiblockModule {
    boolean isValid(Level world, BlockPos pos);
    void onActivate(Level world, BlockPos pos);
    void onDeactivate(Level world, BlockPos pos);
    String getModuleKey();
    BlockPos getModuleOffset();
    IMultiblockModule createModule(Level world, BlockPos pos);
}