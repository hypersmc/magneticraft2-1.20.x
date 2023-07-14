package com.magneticraft2.common.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * @author JumpWatch on 01-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public interface ISync
{
    Level getThisWorld();

    BlockPos getThisPosition();

    CompoundTag sync();
}
