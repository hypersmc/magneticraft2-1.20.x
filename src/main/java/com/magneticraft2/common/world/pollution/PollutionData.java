package com.magneticraft2.common.world.pollution;

import com.magneticraft2.common.systems.networking.PollutionPacket;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static com.magneticraft2.common.systems.mgc2Network.CHANNEL;

/**
 * @author JumpWatch on 23-09-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class PollutionData extends SavedData {
    private Map<ChunkPos, Integer> pollutionMap = new HashMap<>();
    public static final Logger LOGGER = LogManager.getLogger("MGC2PollutionData");
    public static final String DATA_NAME = "MGC2_PollutionData";
    public static PollutionData get(Level world){
        if (!(world instanceof ServerLevel)){
            throw new RuntimeException("Attempted to get the data from a client world. This is wrong.");
        }
        ServerLevel overworld = world.getServer().getLevel(Level.OVERWORLD);
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(PollutionData::new, PollutionData::new, DATA_NAME);
    }
    public PollutionData(){}
    public PollutionData(CompoundTag nbt) {
        pollutionMap.clear();
        ListTag list = nbt.getList("pollutionMap", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ChunkPos pos = new ChunkPos(tag.getInt("x"), tag.getInt("z"));
            int level = tag.getInt("level");
            pollutionMap.put(pos, level);

        }
    }
    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        ListTag list = new ListTag();
        for (Map.Entry<ChunkPos, Integer> entry : pollutionMap.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", entry.getKey().x);
            tag.putInt("z", entry.getKey().z);
            tag.putInt("level", entry.getValue());
            list.add(tag);
        }
        pCompoundTag.put("pollutionMap", list);
        return pCompoundTag;
    }
    public int getPollution(ChunkPos pos) {
//        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get())
//            LOGGER.info("Getting pollution for pos: " + pos + " Currently is: " + pollutionMap.getOrDefault(pos, 0));
        return pollutionMap.getOrDefault(pos, 0);
    }

    public void setPollution(ChunkPos pos, int level) {
        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get())
            LOGGER.info("Trying to set pollution for pos: " + pos + " to level: " + level);
        pollutionMap.put(pos, level);
        CHANNEL.send(PacketDistributor.ALL.noArg(), new PollutionPacket(pos, level));
        setDirty(); // Mark as dirty to ensure it gets saved
    }


}
