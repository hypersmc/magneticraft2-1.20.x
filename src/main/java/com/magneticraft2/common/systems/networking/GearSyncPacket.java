package com.magneticraft2.common.systems.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * @version 1.0.0
 * Common-safe gear sync packet. Keep client-only Minecraft classes out of this file.
 */
public class GearSyncPacket {
    private final BlockPos position;
    private final float speed;
    private final float torque;
    private final float maxTorque;
    private final boolean overloaded;
    private final float meshPhaseDegrees;
    private final float rotationDegrees;
    private final int directionMultiplier;
    private final BlockPos sourcePos;

    public GearSyncPacket(BlockPos position, float speed, float torque, float maxTorque, boolean overloaded, float meshPhaseDegrees, float rotationDegrees, int directionMultiplier, BlockPos sourcePos) {
        this.position = position;
        this.speed = speed;
        this.torque = torque;
        this.maxTorque = maxTorque;
        this.overloaded = overloaded;
        this.meshPhaseDegrees = meshPhaseDegrees;
        this.rotationDegrees = rotationDegrees;
        this.directionMultiplier = directionMultiplier;
        this.sourcePos = sourcePos;
    }

    public static void encode(GearSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.position);
        buffer.writeFloat(packet.speed);
        buffer.writeFloat(packet.torque);
        buffer.writeFloat(packet.maxTorque);
        buffer.writeBoolean(packet.overloaded);
        buffer.writeFloat(packet.meshPhaseDegrees);
        buffer.writeFloat(packet.rotationDegrees);
        buffer.writeInt(packet.directionMultiplier);
        buffer.writeBlockPos(packet.sourcePos != null ? packet.sourcePos : BlockPos.ZERO);
    }

    public static GearSyncPacket decode(FriendlyByteBuf buffer) {
        BlockPos position = buffer.readBlockPos();
        float speed = buffer.readFloat();
        float torque = buffer.readFloat();
        float maxTorque = buffer.readFloat();
        boolean overloaded = buffer.readBoolean();
        float meshPhaseDegrees = buffer.readFloat();
        float rotationDegrees = buffer.readFloat();
        int directionMultiplier = buffer.readInt();
        BlockPos sourcePos = buffer.readBlockPos();
        return new GearSyncPacket(position, speed, torque, maxTorque, overloaded, meshPhaseDegrees, rotationDegrees, directionMultiplier, sourcePos.equals(BlockPos.ZERO) ? null : sourcePos);
    }

    public static void handle(GearSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isClient()) {
                return;
            }

            try {
                Class<?> handlerClass = Class.forName("com.magneticraft2.client.systems.networking.ClientGearSyncPacketHandler");
                handlerClass.getMethod("handle", GearSyncPacket.class).invoke(null, packet);
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException("Failed to handle client gear sync packet", exception);
            }
        });
        context.setPacketHandled(true);
    }

    public BlockPos getPosition() {
        return position;
    }

    public float getSpeed() {
        return speed;
    }

    public float getTorque() {
        return torque;
    }

    public float getMaxTorque() {
        return maxTorque;
    }

    public boolean isOverloaded() {
        return overloaded;
    }

    public float getMeshPhaseDegrees() {
        return meshPhaseDegrees;
    }

    public float getRotationDegrees() {
        return rotationDegrees;
    }

    public int getDirectionMultiplier() {
        return directionMultiplier;
    }

    public BlockPos getSourcePos() {
        return sourcePos;
    }
}
