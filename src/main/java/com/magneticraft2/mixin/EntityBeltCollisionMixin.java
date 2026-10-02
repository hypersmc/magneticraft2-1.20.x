package com.magneticraft2.mixin;

import com.magneticraft2.common.systems.GEAR.BeltConnectionManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds Magneticraft's non-block belt geometry to vanilla entity movement collision.
 *
 * The target method already combines block collision with an additional shape list;
 * we only extend that list and let vanilla resolve stepping, sliding and jumping.
 */
@Mixin(Entity.class)
public abstract class EntityBeltCollisionMixin {
    @ModifyVariable(
            method = "collideBoundingBox",
            at = @At("HEAD"),
            argsOnly = true,
            index = 4
    )
    private static List<VoxelShape> magneticraft2$appendBeltCollision(
            List<VoxelShape> originalShapes,
            @Nullable Entity entity,
            Vec3 movement,
            AABB entityBounds,
            Level level) {

        if (level == null || entityBounds == null || movement == null) {
            return originalShapes;
        }

        AABB sweptBounds = entityBounds
                .expandTowards(movement)
                .inflate(0.001D);

        List<VoxelShape> beltShapes =
                BeltConnectionManager.getCollisionShapes(level, entity, sweptBounds);

        if (beltShapes.isEmpty()) {
            return originalShapes;
        }

        List<VoxelShape> combined =
                new ArrayList<>(originalShapes.size() + beltShapes.size());
        combined.addAll(originalShapes);
        combined.addAll(beltShapes);
        return combined;
    }
}
