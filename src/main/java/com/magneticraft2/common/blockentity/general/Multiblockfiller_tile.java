package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author JumpWatch on 01-07-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class Multiblockfiller_tile extends BlockEntity {
    public static double x = -3000;
    public static double y = -3000;
    public static double z = -3000;
    public static BlockState blockState = null;
    public static void setXYZ(double tx, double ty, double tz) {
        x = tx;
        y = ty;
        z = tz;
    }
    public static void setY(double ty) {
        y = ty;
    }
    public static void setX(double tx) {
        x = tx;
    }
    public static void setZ(double tz) {
        z = tz;
    }
    public static double getX() {
        return x;
    }
    public static double getY() {
        return y;
    }
    public static double getZ() {
        return z;
    }

    public Multiblockfiller_tile(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityRegistry.multiblockfillerBlockEntity.get(), pPos, pBlockState);
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
    }
}
