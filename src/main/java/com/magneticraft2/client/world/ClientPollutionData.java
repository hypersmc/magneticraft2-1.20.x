package com.magneticraft2.client.world;
import com.magneticraft2.common.systems.networking.PollutionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
/**
 * @author JumpWatch on 25-09-2023
 * @Project mgc2-1.20
 * v1.0.0
 */


public class ClientPollutionData {
    public static final Logger LOGGER = LogManager.getLogger("MGC2ClientPollutionData");
    // A concurrent map to store pollution data received from the server.
    private static final ConcurrentHashMap<ChunkPos, Integer> pollutionData = new ConcurrentHashMap<>();

    // Handle the received pollution packet and update the pollution data.
    public static void handlePollutionPacket(PollutionPacket packet) {
        ChunkPos pos = packet.pos();
        int pollutionLevel = packet.pollutionLevel();
        pollutionData.put(pos, pollutionLevel);
    }

    // Get the pollution level for a specific chunk.
    public static int getPollutionLevel(ChunkPos pos) {
        return pollutionData.getOrDefault(pos, 0);
    }

    // Get the pollution level for a specific block position.
    public static int getPollutionLevel(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        return getPollutionLevel(chunkPos);
    }

    // Clear the pollution data, for example, when the player disconnects from the server.
    public static void clear() {
        pollutionData.clear();
    }
}
