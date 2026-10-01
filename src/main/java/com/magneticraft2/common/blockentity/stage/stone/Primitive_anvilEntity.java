package com.magneticraft2.common.blockentity.stage.stone;

import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class Primitive_anvilEntity extends BlockEntity {
    public Primitive_anvilEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityRegistry.Primitive_anvilEntity.get(), pPos, pBlockState);
    }


    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState estate, E e) {
        Primitive_anvilEntity entity = (Primitive_anvilEntity) e.getLevel().getBlockEntity(pos);
        if (!level.isClientSide()) {
        }
    }
}
