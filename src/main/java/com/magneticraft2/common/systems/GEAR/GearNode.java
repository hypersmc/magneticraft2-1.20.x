package com.magneticraft2.common.systems.GEAR;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * @version 1.0.0
 */
public class GearNode {
    private final BlockPos position;
    private float speed = 0.0F;
    private float torque = 0.0F;
    private float maxTorque = 8.0F;
    private boolean overloaded = false;
    private float meshPhaseDegrees = 0.0F;
    private float rotationDegrees = 0.0F;
    private float clientSpeed = 0.0F;
    private float clientTorque = 0.0F;
    private float clientMaxTorque = 8.0F;
    private boolean clientOverloaded = false;
    private float clientMeshPhaseDegrees = 0.0F;
    private float clientRotationDegrees = 0.0F;
    private int directionMultiplier = 1;
    private BlockPos sourcePos;
    private boolean source = false;
    private boolean shaftLike = false;
    private int teeth = 8;
    private Direction.Axis axis = Direction.Axis.Y;

    public GearNode(BlockPos position) {
        this.position = position;
    }

    public BlockPos getPosition() {
        return position;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = Math.max(0.0F, speed);
    }

    public float getTorque() {
        return torque;
    }

    public void setTorque(float torque) {
        this.torque = Math.max(0.0F, torque);
    }

    public float getMaxTorque() {
        return maxTorque;
    }

    public void setMaxTorque(float maxTorque) {
        this.maxTorque = Math.max(0.0F, maxTorque);
    }

    public boolean isOverloaded() {
        return overloaded;
    }

    public void setOverloaded(boolean overloaded) {
        this.overloaded = overloaded;
    }

    public float getMeshPhaseDegrees() {
        return meshPhaseDegrees;
    }

    public void setMeshPhaseDegrees(float meshPhaseDegrees) {
        this.meshPhaseDegrees = normalizeDegrees(meshPhaseDegrees);
    }

    public float getClientMeshPhaseDegrees() {
        return clientMeshPhaseDegrees;
    }

    public void setClientMeshPhaseDegrees(float clientMeshPhaseDegrees) {
        this.clientMeshPhaseDegrees = normalizeDegrees(clientMeshPhaseDegrees);
    }

    public float getRotationDegrees() {
        return rotationDegrees;
    }

    public void setRotationDegrees(float rotationDegrees) {
        this.rotationDegrees = normalizeDegrees(rotationDegrees);
    }

    public float getClientRotationDegrees() {
        return clientRotationDegrees;
    }

    public void setClientRotationDegrees(float clientRotationDegrees) {
        this.clientRotationDegrees = normalizeDegrees(clientRotationDegrees);
    }

    public void advanceRotation(float deltaTicks) {
        if (deltaTicks <= 0.0F || speed <= 0.0F || overloaded) {
            return;
        }

        float degreesPerTick = speed * 360.0F / 1200.0F;
        setRotationDegrees(rotationDegrees + degreesPerTick * deltaTicks * directionMultiplier);
    }

    public int getDirectionMultiplier() {
        return directionMultiplier;
    }

    public void setDirectionMultiplier(int multiplier) {
        this.directionMultiplier = multiplier < 0 ? -1 : 1;
    }

    public BlockPos getSourcePos() {
        return sourcePos;
    }

    public void setSourcePos(BlockPos sourcePos) {
        this.sourcePos = sourcePos;
    }

    public boolean isSource() {
        return source;
    }

    public void setSource(boolean source) {
        this.source = source;
    }

    public boolean isShaftLike() {
        return shaftLike;
    }

    public void setShaftLike(boolean shaftLike) {
        this.shaftLike = shaftLike;
    }

    public int getTeeth() {
        return teeth;
    }

    public void setTeeth(int teeth) {
        this.teeth = Math.max(1, teeth);
    }

    public Direction.Axis getAxis() {
        return axis;
    }

    public void setAxis(Direction.Axis axis) {
        this.axis = axis == null ? Direction.Axis.Y : axis;
    }

    public float getClientSpeed() {
        return clientSpeed;
    }

    public float getClientTorque() {
        return clientTorque;
    }

    public float getClientMaxTorque() {
        return clientMaxTorque;
    }

    public boolean isClientOverloaded() {
        return clientOverloaded;
    }

    public void updateClientData(float speed, float torque, float maxTorque, boolean overloaded, float meshPhaseDegrees, float rotationDegrees) {
        this.clientSpeed = Math.max(0.0F, speed);
        this.clientTorque = Math.max(0.0F, torque);
        this.clientMaxTorque = Math.max(0.0F, maxTorque);
        this.clientOverloaded = overloaded;
        this.clientMeshPhaseDegrees = normalizeDegrees(meshPhaseDegrees);
        this.clientRotationDegrees = normalizeDegrees(rotationDegrees);
    }

    public void stop() {
        this.speed = 0.0F;
        this.torque = 0.0F;
        this.overloaded = false;
        this.sourcePos = null;
        // Keep directionMultiplier and meshPhaseDegrees intact.
        // They are visual/meshing alignment state, not motion state.
        // Resetting them when RPM reaches zero makes gears snap back out of tooth alignment.
    }

    public void decayMotion(float decayFactor, float stopEpsilon) {
        this.overloaded = false;
        if (this.speed <= stopEpsilon && this.torque <= stopEpsilon) {
            stop();
            return;
        }

        this.speed *= decayFactor;
        this.torque *= decayFactor;

        if (this.speed <= stopEpsilon) {
            this.speed = 0.0F;
        }
        if (this.torque <= stopEpsilon) {
            this.torque = 0.0F;
        }
        if (this.speed == 0.0F && this.torque == 0.0F) {
            this.sourcePos = null;
        }
    }

    public void copyServerStateFrom(GearNode other) {
        if (other == null) {
            stop();
            return;
        }
        this.speed = other.speed;
        this.torque = other.torque;
        this.maxTorque = other.maxTorque;
        this.overloaded = other.overloaded;
        this.meshPhaseDegrees = other.meshPhaseDegrees;
        this.rotationDegrees = other.rotationDegrees;
        this.directionMultiplier = other.directionMultiplier;
        this.sourcePos = other.sourcePos;
        this.shaftLike = other.shaftLike;
    }

    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Speed", speed);
        tag.putFloat("Torque", torque);
        tag.putFloat("MaxTorque", maxTorque);
        tag.putBoolean("Overloaded", overloaded);
        tag.putFloat("MeshPhaseDegrees", meshPhaseDegrees);
        tag.putFloat("RotationDegrees", rotationDegrees);
        tag.putInt("DirectionMultiplier", directionMultiplier);
        tag.putBoolean("Source", source);
        tag.putBoolean("ShaftLike", shaftLike);
        tag.putInt("Teeth", teeth);
        tag.putString("Axis", axis.getName());
        if (sourcePos != null) {
            tag.putLong("SourcePos", sourcePos.asLong());
        }
        return tag;
    }

    public void loadFromNBT(CompoundTag tag) {
        this.speed = Math.max(0.0F, tag.getFloat("Speed"));
        this.torque = Math.max(0.0F, tag.getFloat("Torque"));
        if (tag.contains("MaxTorque")) {
            this.maxTorque = Math.max(0.0F, tag.getFloat("MaxTorque"));
        }
        this.overloaded = tag.getBoolean("Overloaded");
        if (tag.contains("MeshPhaseDegrees")) {
            this.meshPhaseDegrees = normalizeDegrees(tag.getFloat("MeshPhaseDegrees"));
        }
        if (tag.contains("RotationDegrees")) {
            this.rotationDegrees = normalizeDegrees(tag.getFloat("RotationDegrees"));
        }
        this.directionMultiplier = tag.getInt("DirectionMultiplier") < 0 ? -1 : 1;
        this.source = tag.getBoolean("Source");
        this.shaftLike = tag.getBoolean("ShaftLike");
        if (tag.contains("Teeth")) {
            this.teeth = Math.max(1, tag.getInt("Teeth"));
        }
        if (tag.contains("Axis")) {
            this.axis = Direction.Axis.byName(tag.getString("Axis"));
            if (this.axis == null) {
                this.axis = Direction.Axis.Y;
            }
        }
        if (tag.contains("SourcePos")) {
            this.sourcePos = BlockPos.of(tag.getLong("SourcePos"));
        } else {
            this.sourcePos = null;
        }
    }

    private float normalizeDegrees(float degrees) {
        float normalized = degrees % 360.0F;
        if (normalized < 0.0F) {
            normalized += 360.0F;
        }
        return normalized;
    }

    @Override
    public String toString() {
        return "GearNode{" +
                "position=" + position +
                ", speed=" + speed +
                ", torque=" + torque +
                ", maxTorque=" + maxTorque +
                ", overloaded=" + overloaded +
                ", meshPhaseDegrees=" + meshPhaseDegrees +
                ", rotationDegrees=" + rotationDegrees +
                ", clientSpeed=" + clientSpeed +
                ", clientTorque=" + clientTorque +
                ", clientMaxTorque=" + clientMaxTorque +
                ", clientOverloaded=" + clientOverloaded +
                ", clientMeshPhaseDegrees=" + clientMeshPhaseDegrees +
                ", clientRotationDegrees=" + clientRotationDegrees +
                ", directionMultiplier=" + directionMultiplier +
                ", sourcePos=" + sourcePos +
                ", source=" + source +
                ", shaftLike=" + shaftLike +
                ", teeth=" + teeth +
                ", axis=" + axis +
                '}';
    }
}
