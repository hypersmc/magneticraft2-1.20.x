package com.magneticraft2.common.world.pollution;

import com.magneticraft2.common.systems.networking.PollutionPacket;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 23-09-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PollutionHandler {
    private static final int SPREAD_THRESHOLD = 100; // Example threshold value
    public static final Logger LOGGER = LogManager.getLogger("MGC2PollutionHandler");
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event){
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) event.level;
            PollutionData pollutionData = PollutionData.get(serverLevel);

            // Iterate over all loaded chunks
            for (ChunkHolder pos2 : serverLevel.getChunkSource().chunkMap.getChunks()) {
                ChunkPos pos = pos2.getPos();
                int pollutionLevel = pollutionData.getPollution(pos);
                if (pollutionLevel > SPREAD_THRESHOLD) {
                    spreadPollution(pos, pollutionLevel, pollutionData, serverLevel);
                }
            }

        }
    }
    private static void spreadPollution(ChunkPos pos, int pollutionLevel, PollutionData pollutionData, ServerLevel serverLevel) {
        int spreadAmount = calculateSpreadAmount(pollutionLevel); // Define how you want to calculate the amount of pollution to spread

        // List of neighboring chunk positions
        List<ChunkPos> neighbors = Arrays.asList(
                new ChunkPos(pos.x + 1, pos.z),
                new ChunkPos(pos.x - 1, pos.z),
                new ChunkPos(pos.x, pos.z + 1),
                new ChunkPos(pos.x, pos.z - 1)
        );

        for (ChunkPos neighbor : neighbors) {
            if (serverLevel.hasChunk(neighbor.x, neighbor.z)) { // Check if the neighboring chunk is loaded
                int neighborPollution = pollutionData.getPollution(neighbor);
                pollutionData.setPollution(neighbor, neighborPollution + spreadAmount);
            }
        }

        // Reduce the pollution in the current chunk by the total amount spread to neighbors
        pollutionData.setPollution(pos, pollutionLevel - spreadAmount * neighbors.size());
    }
    private static int calculateSpreadAmount(int pollutionLevel) {
        final float SPREAD_PERCENTAGE = 0.1f; // 10% of the pollution level will spread
        return (int) (pollutionLevel * SPREAD_PERCENTAGE);
    }
}
