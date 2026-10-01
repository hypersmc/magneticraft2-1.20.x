package com.magneticraft2.client.systems.networking;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.systems.networking.GearSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client-only gear packet handler.
 */
public final class ClientGearSyncPacketHandler {
    private ClientGearSyncPacketHandler() {
    }

    public static void handle(GearSyncPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || !level.hasChunkAt(packet.getPosition())) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(packet.getPosition());
        if (blockEntity instanceof GearBlockEntity gearBlockEntity) {
            gearBlockEntity.syncGearState(
                    packet.getSpeed(),
                    packet.getTorque(),
                    packet.getMaxTorque(),
                    packet.isOverloaded(),
                    packet.getMeshPhaseDegrees(),
                    packet.getRotationDegrees(),
                    packet.getDirectionMultiplier(),
                    packet.getSourcePos()
            );
        }
    }
}
