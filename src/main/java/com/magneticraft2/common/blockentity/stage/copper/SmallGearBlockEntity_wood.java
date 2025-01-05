package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class SmallGearBlockEntity_wood extends GearBlockEntity {
    private static final Set<BlockPos> updatedGears = new HashSet<>();

    public SmallGearBlockEntity_wood(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GEAR_SMALL_BE_WOOD.get(), pos, state);
    }

    @Override
    public void setPowered(boolean powered) {

    }
}
