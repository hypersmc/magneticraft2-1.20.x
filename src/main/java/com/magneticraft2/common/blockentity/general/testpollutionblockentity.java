package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.magneticraft2.common.world.pollution.PollutionData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author JumpWatch on 23-09-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class testpollutionblockentity extends BlockEntity {


    public static final Logger LOGGER = LogManager.getLogger("BaseBlockMagneticraft2");
    public testpollutionblockentity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityRegistry.testpollutionblock.get(), pPos, pBlockState);
    }

    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState estate, E e) {
        PollutionData pollutionData = PollutionData.get(level.getServer().overworld());
        if (!level.isClientSide()){
            testpollutionblockentity entity = (testpollutionblockentity) e.getLevel().getBlockEntity(pos);
            ChunkPos chunkPos = new ChunkPos(pos.getX(), pos.getZ());
            int currentPollutionLevel = pollutionData.getPollution(chunkPos);
            if (currentPollutionLevel < 110){
                int newPollutionLevel = currentPollutionLevel + 40;
                pollutionData.setPollution(chunkPos, newPollutionLevel);
                LOGGER.info(pollutionData.getPollution(chunkPos));
            }else {
                LOGGER.info("Pollution is now 110");
            }
        }
    }
}
