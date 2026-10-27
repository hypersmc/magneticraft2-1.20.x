package com.magneticraft2.client.hologram;

import com.magneticraft2.common.hologram.AncientProjectorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Sparse particle cues for prerecorded holographic engineering demonstrations.
 *
 * <p>Events use the same scene-local, south-facing coordinate system as the
 * PR #12 animation renderer. Only the client creates particles; no blocks,
 * forces, mechanical damage, or network packets are involved.
 *
 * <p>Particle emissions are intentionally capped at 24 per projector/tick.
 * Long-running stress cues emit only on their authored interval and must never
 * spawn extra historical particles when the clock jumps.
 */
public final class HologramVisualEffects {
    private static final double MAX_DISTANCE_SQUARED = 32D * 32D;
    private static final int MAX_PARTICLES_PER_TICK = 24;
    private HologramVisualEffects() {}

    public static void emit(AncientProjectorBlockEntity projector,
                            HologramRecordingResourceLoader.Recording recording,
                            double previousFilmTick, double filmTick) {
        Level level = projector.getLevel();
        if (level == null || !level.isClientSide
                || Minecraft.getInstance().player == null
                || recording.visualEvents().isEmpty()
                || Minecraft.getInstance().player.distanceToSqr(
                    Vec3.atCenterOf(projector.getBlockPos())) > MAX_DISTANCE_SQUARED) {
            return;
        }

        int budget = MAX_PARTICLES_PER_TICK;
        for (HologramRecordingResourceLoader.VisualEvent event : recording.visualEvents()) {
            if (budget == 0) break;
            if (filmTick < event.tick() || previousFilmTick > event.lastTick()) {
                continue;
            }
            int lastPulse = (int) Math.floor(
                    (previousFilmTick - event.tick()) / event.intervalTicks());
            int currentPulse = (int) Math.floor(
                    (filmTick - event.tick()) / event.intervalTicks());
            if (currentPulse <= lastPulse || currentPulse < 0) continue;

            int pulseTick = event.tick() + currentPulse * event.intervalTicks();
            if (pulseTick > event.lastTick()) continue;
            int count = Math.min(event.count(), budget);
            if (count <= 0) continue;
            emitOne(level, projector, recording, event, pulseTick, count);
            budget -= count;
        }
    }

    private static void emitOne(Level level, AncientProjectorBlockEntity projector,
                                HologramRecordingResourceLoader.Recording recording,
                                HologramRecordingResourceLoader.VisualEvent event,
                                int pulseTick, int count) {
        Direction facing = projector.getBlockState().getValue(
                BlockStateProperties.HORIZONTAL_FACING);
        double radians = Math.toRadians(-facing.toYRot());
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);

        double localX = recording.offsetX() + event.x() - 0.5D;
        double localZ = recording.offsetZ() + event.z() - 0.5D;
        double x = projector.getBlockPos().getX() + 0.5D
                + cosine * localX + sine * localZ;
        double y = projector.getBlockPos().getY() + recording.offsetY() + event.y();
        double z = projector.getBlockPos().getZ() + 0.5D
                - sine * localX + cosine * localZ;

        // All clients sampling the same recorded pulse get the same positions.
        // The game may render particles at different frame rates, but the
        // geometry and timing of each emission are stable.
        long seed = projector.getBlockPos().asLong()
                ^ ((long) pulseTick * 0x9E3779B97F4A7C15L)
                ^ ((long) event.tick() * 0xC2B2AE3D27D4EB4FL);
        RandomSource random = RandomSource.create(seed);

        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f(event.red(), event.green(), event.blue()),
                "warning".equals(event.kind()) ? 1.0F : 0.65F);
        for (int i = 0; i < count; i++) {
            double angle = (i + 0.3D * random.nextDouble())
                    * (Math.PI * 2D / count);
            double radial = event.spread()
                    * (0.40D + 0.60D * random.nextDouble());
            double dx = Math.cos(angle) * radial;
            double dz = Math.sin(angle) * radial;
            double dy = (random.nextDouble() - 0.5D) * event.spread();

            double motion = "spark".equals(event.kind()) ? 0.045D : 0.012D;
            double vx = Math.cos(angle) * motion;
            double vz = Math.sin(angle) * motion;
            double vy = "reconstruct".equals(event.kind()) ? 0.065D
                    : "spark".equals(event.kind()) ? 0.02D : 0.007D;

            // Distinct shapes: short radial sparks, amber warning ring,
            // cool upward-rebuilding shimmer. No full-screen flash.
            if ("warning".equals(event.kind())) {
                dy *= 0.17D;
            } else if ("reconstruct".equals(event.kind())) {
                radial *= 0.4D;
                dx *= 0.4D;
                dz *= 0.4D;
            }
            level.addParticle(dust, x + dx, y + dy, z + dz, vx, vy, vz);
        }
    }
}
