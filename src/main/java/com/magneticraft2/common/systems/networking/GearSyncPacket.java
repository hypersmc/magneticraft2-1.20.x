package com.magneticraft2.common.systems.networking;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class GearSyncPacket {
    private final BlockPos position;
    private final float speed;
    private final float torque;
    private final int directionMultiplier;
    private final BlockPos sourcePos;

    // Constructor
    public GearSyncPacket(BlockPos position, float speed, float torque, int directionMultiplier, BlockPos sourcePos) {
        this.position = position;
        this.speed = speed;
        this.torque = torque;
        this.directionMultiplier = directionMultiplier;
        this.sourcePos = sourcePos;
    }

    // Encode packet data to the buffer
    public static void encode(GearSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.position);
        buffer.writeFloat(packet.speed);
        buffer.writeFloat(packet.torque);
        buffer.writeInt(packet.directionMultiplier);
        buffer.writeBlockPos(packet.sourcePos != null ? packet.sourcePos : BlockPos.ZERO); // Null safety
    }

    // Decode packet data from the buffer
    public static GearSyncPacket decode(FriendlyByteBuf buffer) {
        BlockPos position = buffer.readBlockPos();
        float speed = buffer.readFloat();
        float torque = buffer.readFloat();
        int directionMultiplier = buffer.readInt();
        BlockPos sourcePos = buffer.readBlockPos();
        return new GearSyncPacket(position, speed, torque, directionMultiplier, sourcePos.equals(BlockPos.ZERO) ? null : sourcePos);
    }

    // Handle packet on the client side
    public static void handle(GearSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Client-side logic
            if (context.getDirection().getReceptionSide().isClient()) {
                // Retrieve the gear system's manager from your client-side environment
                GearNetworkManager manager = GearNetworkManager.getInstance();
                if (manager != null) {
                    GearNode gear = manager.getGear(packet.position);
                    if (gear != null) {
                        gear.updateClientData(packet.speed, packet.torque);
                        gear.setDirectionMultiplier(packet.directionMultiplier);
                        gear.setSourcePos(packet.sourcePos);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }

    // Getters for testing or other needs
    public BlockPos getPosition() {
        return position;
    }

    public float getSpeed() {
        return speed;
    }

    public float getTorque() {
        return torque;
    }

    public int getDirectionMultiplier() {
        return directionMultiplier;
    }

    public BlockPos getSourcePos() {
        return sourcePos;
    }
}