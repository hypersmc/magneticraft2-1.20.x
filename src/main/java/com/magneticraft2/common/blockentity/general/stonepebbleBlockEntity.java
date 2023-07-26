package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.common.block.general.stonepebble;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.EntitiesRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * @author JumpWatch on 18-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class stonepebbleBlockEntity extends BlockEntity {
    private static stonepebbleBlockEntity self;
    public stonepebbleBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.stonepebbleBlockEntity.get(), pos, state);
        self = this;
    }
    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState estate, E e) {
        if (!level.isClientSide()){
            int type = estate.getValue(stonepebble.TYPE);
            if (type == 0){
                Random random = new Random();
                int randomnum = random.nextInt(4) + 1;
                BlockState currentState = level.getBlockState(pos);
                BlockState newState = currentState.setValue(stonepebble.TYPE, randomnum);
                level.setBlock(pos, newState, 3);
            }
        }
    }

    @Override
    public void load(CompoundTag pTag) {
        this.getBlockState().setValue(stonepebble.TYPE, pTag.getInt("type"));
        super.load(pTag);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        int state = this.getBlockState().getValue(stonepebble.TYPE);
        pTag.putInt("type", state);
        super.saveAdditional(pTag);
    }
}
