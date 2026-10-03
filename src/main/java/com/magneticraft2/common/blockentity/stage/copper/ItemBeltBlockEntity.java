package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent ownership metadata for one generated physical Item Belt cell.
 *
 * Every generated cell knows the exact two Wooden Belt Rollers that own it. This makes
 * middle-belt breaking authoritative even immediately after a world/chunk load, without
 * depending on the transient ItemBeltConnectionManager runtime map.
 */
public class ItemBeltBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos startRoller;
    @Nullable
    private BlockPos endRoller;

    public ItemBeltBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ITEM_BELT_BE.get(), pos, state);
    }

    public void setRollers(BlockPos startRoller, BlockPos endRoller) {
        this.startRoller = startRoller == null ? null : startRoller.immutable();
        this.endRoller = endRoller == null ? null : endRoller.immutable();
        setChanged();
    }

    @Nullable
    public BlockPos getStartRoller() {
        return startRoller;
    }

    @Nullable
    public BlockPos getEndRoller() {
        return endRoller;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (startRoller != null) {
            tag.putLong("StartRoller", startRoller.asLong());
        }
        if (endRoller != null) {
            tag.putLong("EndRoller", endRoller.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        startRoller = tag.contains("StartRoller")
                ? BlockPos.of(tag.getLong("StartRoller"))
                : null;
        endRoller = tag.contains("EndRoller")
                ? BlockPos.of(tag.getLong("EndRoller"))
                : null;
    }
}
