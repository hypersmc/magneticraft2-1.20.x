package com.magneticraft2.common.systems.GEAR;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import static com.magneticraft2.common.magneticraft2.LOGGER;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class GearNode {
    private final BlockPos position;
    private float speed = 0; // Current speed of the gear
    private float torque = 0; // Current torque of the gear
    private float clientSpeed = 0; // Synced speed for the client
    private float clientTorque = 0; // Synced torque for the client
    private int directionMultiplier = 1; // +1 or -1 based on gear placement
    private BlockPos sourcePos; // Tracks the source of power

    public GearNode(BlockPos position) {
        this.position = position;
    }

    // --- Getters and Setters ---
    public BlockPos getPosition() {
        return position;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
//        LOGGER.info("Gear at {}: Speed set to {}", position, speed);
        this.speed = speed;
    }

    public float getTorque() {
        return torque;
    }

    public void setTorque(float torque) {
        this.torque = torque;
    }

    public int getDirectionMultiplier() {
        return directionMultiplier;
    }

    public void setDirectionMultiplier(int multiplier) {
        this.directionMultiplier = multiplier;
    }

    public BlockPos getSourcePos() {
        return sourcePos;
    }

    public void setSourcePos(BlockPos sourcePos) {
        this.sourcePos = sourcePos;
    }

    public float getClientSpeed() {
        return clientSpeed;
    }

    public float getClientTorque() {
        return clientTorque;
    }

    public void updateClientData(float speed, float torque) {
        this.clientSpeed = speed;
        this.clientTorque = torque;
    }

    // --- NBT Handling ---
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putFloat("Speed", speed);
        tag.putFloat("Torque", torque);
        tag.putInt("DirectionMultiplier", directionMultiplier);
        if (sourcePos != null) {
            tag.putLong("SourcePos", sourcePos.asLong());
        }
        return tag;
    }

    public void loadFromNBT(CompoundTag tag) {
        this.speed = tag.getFloat("Speed");
        this.torque = tag.getFloat("Torque");
        this.directionMultiplier = tag.getInt("DirectionMultiplier");
        if (tag.contains("SourcePos")) {
            this.sourcePos = BlockPos.of(tag.getLong("SourcePos"));
        }
    }

    @Override
    public String toString() {
        return "GearNode{" +
                "position=" + position +
                ", speed=" + speed +
                ", torque=" + torque +
                ", clientSpeed=" + clientSpeed +
                ", clientTorque=" + clientTorque +
                ", directionMultiplier=" + directionMultiplier +
                ", sourcePos=" + sourcePos +
                '}';
    }
}
