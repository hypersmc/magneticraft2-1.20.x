package com.magneticraft2.client.hologram;

import com.magneticraft2.common.hologram.AncientProjectorBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Sparse local audio and visual event scheduling for prerecorded films.
 *
 * <p>Sounds are scheduled from the same server-start-time-derived timeline as
 * the geometry. No new entities, server packets or per-frame sound instances.
 * A newly observed projector starts at NOW and never replays cues from the
 * past (late join, chunk load or returning to range).
 *
 * <p>Invoked from the client-only BE ticker, never from its renderer: frame
 * rate, frustum culling and graphics settings cannot duplicate cue events.
 */
public final class HologramSoundPlayback {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_CATCHUP_TICKS = 10;
    private static final double MAX_HEAR_DISTANCE_SQUARED = 24D * 24D;
    private static final Map<AncientProjectorBlockEntity, Cursor> CURSORS =
            new WeakHashMap<>();
    private static final Set<ResourceLocation> MISSING_SOUNDS = new HashSet<>();

    private static final class Cursor {
        private final ResourceLocation recordingId;
        private final HologramRecordingResourceLoader.Recording recording;
        private final long start;
        private final int duration;
        private long lastWorldTick;
        private double lastFilmTick;

        private Cursor(AncientProjectorBlockEntity projector,
                       HologramRecordingResourceLoader.Recording visual,
                       long worldTick, double filmTick) {
            recordingId = projector.recording();
            recording = visual;
            start = projector.startTime();
            duration = projector.durationTicks();
            lastWorldTick = worldTick;
            lastFilmTick = filmTick;
        }

        private boolean matches(AncientProjectorBlockEntity projector,
                                HologramRecordingResourceLoader.Recording visual) {
            return start == projector.startTime()
                    && duration == projector.durationTicks()
                    && recordingId.equals(projector.recording())
                    && recording == visual;
        }
    }

    private HologramSoundPlayback() {}

    public static void tick(AncientProjectorBlockEntity projector) {
        Level level = projector.getLevel();
        if (level == null || !level.isClientSide || projector.startTime() < 0L
                || projector.durationTicks() <= 0) {
            CURSORS.remove(projector);
            return;
        }

        long elapsed = level.getGameTime() - projector.startTime();
        if (elapsed < 0L || elapsed >= projector.durationTicks()) {
            CURSORS.remove(projector);
            return;
        }
        HologramRecordingResourceLoader.Recording visual =
                HologramRecordingResourceLoader.get(projector.recording());
        if (visual == null
                || !HologramRecordingValidator.isUsable(projector.recording(), visual)
                || (visual.audioCues().isEmpty() && visual.visualEvents().isEmpty())) {
            CURSORS.remove(projector);
            return;
        }

        double filmTick = elapsed * (double) visual.durationTicks()
                / projector.durationTicks();
        Cursor cursor = CURSORS.get(projector);
        // Initial observation, replay, resource reload, changed definition
        // or a clock rewinding: set the current position and remain silent.
        if (cursor == null || !cursor.matches(projector, visual)
                || elapsed < cursor.lastWorldTick
                || filmTick < cursor.lastFilmTick) {
            CURSORS.put(projector, new Cursor(projector, visual,
                    elapsed, filmTick));
            return;
        }

        long delta = elapsed - cursor.lastWorldTick;
        double previousFilmTick = cursor.lastFilmTick;
        cursor.lastWorldTick = elapsed;
        cursor.lastFilmTick = filmTick;
        // Significant time skips should never make several old warnings and
        // mechanical impacts fire at once when a chunk or client catches up.
        if (delta <= 0L || delta > MAX_CATCHUP_TICKS
                || filmTick - previousFilmTick > MAX_CATCHUP_TICKS) return;

        // Sparse particle effects have their own visual range (32 blocks);
        // audio has the shorter 24-block range. Neither is linked to the
        // camera frustum or how often the BER draws the actual models.
        HologramVisualEffects.emit(projector, visual, previousFilmTick, filmTick);

        if (Minecraft.getInstance().player == null) return;

        for (HologramRecordingResourceLoader.AudioCue cue : visual.audioCues()) {
            if (cue.tick() > previousFilmTick && cue.tick() <= filmTick) {
                // Default sound originates at the small physical artifact,
                // but an author may bind a cue to the point where a recorded
                // brake, gear or bearing actually fails. The same transform
                // powers particle effects, for all horizontal facings.
                Vec3 source = cue.position() == null
                        ? Vec3.atCenterOf(projector.getBlockPos())
                        : HologramSceneCoordinates.toWorld(projector, visual,
                                cue.position());
                if (Minecraft.getInstance().player.distanceToSqr(source)
                        > MAX_HEAR_DISTANCE_SQUARED) continue;
                SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(cue.sound());
                if (sound == null) {
                    if (MISSING_SOUNDS.add(cue.sound())) {
                        LOGGER.warn("Unknown hologram audio event {} in recording {}",
                                cue.sound(), projector.recording());
                    }
                    continue;
                }
                level.playLocalSound(
                        source.x, source.y, source.z,
                        sound, SoundSource.BLOCKS,
                        cue.volume(), cue.pitch(), false);
                // Even when a datapack compresses playback dramatically,
                // never burst several recorded impacts in one client tick.
                break;
            }
        }
    }
}
