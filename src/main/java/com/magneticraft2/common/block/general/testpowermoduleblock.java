package com.magneticraft2.common.block.general;

import com.magneticraft2.common.blockentity.general.testpowermodule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 21-09-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class testpowermoduleblock extends BaseBlockMagneticraft2{
    public testpowermoduleblock() {
        super(BlockBehaviour.Properties.of().noOcclusion().requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new testpowermodule(pPos,pState);
    }
}
