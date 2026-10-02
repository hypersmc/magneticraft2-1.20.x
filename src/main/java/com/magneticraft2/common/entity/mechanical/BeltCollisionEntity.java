package com.magneticraft2.common.entity.mechanical;

import com.magneticraft2.common.registry.registers.EntitiesRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;

/**
 * Invisible, non-persistent collision proxy for one short physical belt section.
 *
 * Vanilla automatically feeds collidable entity AABBs into Entity.collide(...), which
 * gives belts real movement collision without hidden world blocks or movement mixins.
 */
public class BeltCollisionEntity extends Entity implements IEntityAdditionalSpawnData {
    private AABB collisionBox;

    public BeltCollisionEntity(EntityType<? extends BeltCollisionEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setInvisible(true);
    }

    public BeltCollisionEntity(Level level, AABB collisionBox) {
        this(EntitiesRegistry.BELT_COLLISION.get(), level);
        setCollisionBox(collisionBox);
    }

    public void setCollisionBox(AABB box) {
        if (box == null) {
            return;
        }

        collisionBox = box;
        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerY = (box.minY + box.maxY) * 0.5D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;

        setPos(centerX, centerY, centerZ);
        setBoundingBox(box);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (collisionBox != null) {
            setBoundingBox(collisionBox);
        }
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        AABB box = collisionBox != null ? collisionBox : getBoundingBox();
        buffer.writeDouble(box.minX);
        buffer.writeDouble(box.minY);
        buffer.writeDouble(box.minZ);
        buffer.writeDouble(box.maxX);
        buffer.writeDouble(box.maxY);
        buffer.writeDouble(box.maxZ);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        setCollisionBox(new AABB(
                additionalData.readDouble(),
                additionalData.readDouble(),
                additionalData.readDouble(),
                additionalData.readDouble(),
                additionalData.readDouble(),
                additionalData.readDouble()
        ));
    }
}
