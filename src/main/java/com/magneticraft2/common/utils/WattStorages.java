package com.magneticraft2.common.utils;

import com.magneticraft2.common.systems.WATT.WattStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
/**
 * @author JumpWatch on 01-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class WattStorages extends WattStorage implements INBTSerializable<CompoundTag> {
    public WattStorages(int capacity, int maxTransfer) {
        super(capacity, maxTransfer);
    }
    protected void onWattChanged() {
    }

    public void setWatt(int watt) {
        this.watt = watt;
        onWattChanged();
    }
    public void addWatt(int watt) {
        this.watt += watt;
        if (this.watt > getMaxWattStored()) {
            this.watt = getWattStored();
        }
        onWattChanged();
    }
    public void consumeWatt(int watt) {
        this.watt -= watt;
        if (this.watt < 0) {
            this.watt = 0;
        }
        onWattChanged();
    }
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("heat", getWattStored());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        setWatt(nbt.getInt("watt"));
    }
}
