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
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds Magneticraft's non-block belt geometry to normal vanilla entity movement.
 *
 * Entity.collide(...) calls collideBoundingBox(...) several times, including the normal
 * movement pass and vanilla step-up attempts. Redirect every one of those calls through
 * this wrapper so the exact same extra belt shapes participate in all movement solving.
 */
@Mixin(Entity.class)
public abstract class EntityBeltCollisionMixin {
    @Redirect(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 magneticraft2$collideWithPhysicalBelts(
            @Nullable Entity entity,
            Vec3 movement,
            AABB entityBounds,
            Level level,
            List<VoxelShape> vanillaEntityShapes) {

        AABB sweptBounds = entityBounds
                .expandTowards(movement)
                .inflate(0.001D);

        List<VoxelShape> beltShapes =
                BeltConnectionManager.getCollisionShapes(level, entity, sweptBounds);

        if (beltShapes.isEmpty()) {
            return Entity.collideBoundingBox(
                    entity,
                    movement,
                    entityBounds,
                    level,
                    vanillaEntityShapes
            );
        }

        List<VoxelShape> combined =
                new ArrayList<>(vanillaEntityShapes.size() + beltShapes.size());
        combined.addAll(vanillaEntityShapes);
        combined.addAll(beltShapes);

        return Entity.collideBoundingBox(
                entity,
                movement,
                entityBounds,
                level,
                combined
        );
    }
}
