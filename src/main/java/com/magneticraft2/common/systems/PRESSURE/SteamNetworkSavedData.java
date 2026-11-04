package com.magneticraft2.common.systems.PRESSURE;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sparse, dimension-owned steam inventory. Each occupied pipe has a share
 * of the network's mass and thermal energy; empty pipes have NO saved entry.
 * Keeping shares at positions makes splits, merges and chunk unloading
 * conserve mass without saving topology identifiers or making pipe BEs.
 */
public final class SteamNetworkSavedData extends SavedData {
    private static final String ID = "magneticraft2_steam_network";
    private static final int FORMAT = 1;
    private final Map<Long, Parcel> cells = new HashMap<>();
    public record Parcel(int units, double energy) {}

    public static SteamNetworkSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                SteamNetworkSavedData::load, SteamNetworkSavedData::new, ID);
    }

    public Parcel parcel(BlockPos pos) {
        return cells.getOrDefault(pos.asLong(), new Parcel(0, 0.0));
    }
    public Set<Long> occupiedPositions() {
        return Set.copyOf(cells.keySet());
    }
    public void write(BlockPos pos, int units, double energy) {
        if (units < 0 || !Double.isFinite(energy) || energy < 0
                || (units > 0 && (energy < units * 373.15 - 0.000001
                    || energy > units * 2000.0 + 0.000001)))
            throw new IllegalArgumentException("Invalid steam parcel");
        long key = pos.asLong();
        Parcel old = cells.get(key);
        if (units == 0) {
            if (old != null) { cells.remove(key); setDirty(); }
        } else {
            Parcel next = new Parcel(units, energy);
            if (!next.equals(old)) { cells.put(key, next); setDirty(); }
        }
    }

    public static SteamNetworkSavedData load(CompoundTag tag) {
        SteamNetworkSavedData result = new SteamNetworkSavedData();
        if (tag.getInt("Version") != FORMAT) return result;
        ListTag list = tag.getList("Cells", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            int units = c.getInt("Units");
            double energy = c.getDouble("Energy");
            if (units <= 0 || units > 1000000 || !Double.isFinite(energy)
                    || energy < units * 373.15 - 0.000001 || energy > units * 2000.0 + 0.000001)
                continue;
            result.cells.put(c.getLong("Position"), new Parcel(units, energy));
        }
        return result;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("Version", FORMAT);
        ListTag list = new ListTag();
        for (var entry : cells.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("Position", entry.getKey());
            c.putInt("Units", entry.getValue().units());
            c.putDouble("Energy", entry.getValue().energy());
            list.add(c);
        }
        tag.put("Cells", list);
        return tag;
    }
}
