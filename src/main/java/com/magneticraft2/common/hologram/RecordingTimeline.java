package com.magneticraft2.common.hologram;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic, world-independent playback of recorded visual channels.
 *
 * <p>Nothing in this class touches a real mechanical network. A client can
 * reconstruct any frame from the recording's server-authoritative start tick,
 * including when joining midway through playback.
 */
public final class RecordingTimeline {
    private RecordingTimeline() {}

    public record Keyframe(int tick, float angleDegrees, boolean running,
                           boolean overloaded) {
        public Keyframe {
            if (tick < 0 || !Float.isFinite(angleDegrees)) {
                throw new IllegalArgumentException("Invalid hologram keyframe");
            }
            if (running && overloaded) {
                throw new IllegalArgumentException("A recorded drive cannot run while overloaded");
            }
        }
    }

    public record Sample(float angleDegrees, boolean running, boolean overloaded,
                         boolean finished) {}

    /**
     * Immutable recording of a single shaft. Additional named channels can
     * each have their own Track, with no change to the sampling algorithm.
     */
    public static final class Track {
        private final int durationTicks;
        private final List<Keyframe> frames;

        public Track(int durationTicks, List<Keyframe> keyframes) {
            if (durationTicks <= 0 || durationTicks > 20 * 60 * 10) {
                throw new IllegalArgumentException("Recording duration must be 1..12000 ticks");
            }
            if (keyframes == null || keyframes.isEmpty() || keyframes.size() > 512) {
                throw new IllegalArgumentException("Recording must have 1..512 keyframes");
            }
            List<Keyframe> sorted = new ArrayList<>(keyframes);
            sorted.sort(Comparator.comparingInt(Keyframe::tick));
            if (sorted.get(0).tick() != 0) {
                throw new IllegalArgumentException("First keyframe must start at tick zero");
            }
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).tick() > durationTicks ||
                        (i > 0 && sorted.get(i).tick() == sorted.get(i - 1).tick())) {
                    throw new IllegalArgumentException("Keyframes must be unique and within duration");
                }
            }
            this.durationTicks = durationTicks;
            this.frames = List.copyOf(sorted);
        }

        public int durationTicks() {
            return durationTicks;
        }

        public Sample sample(double elapsedTicks) {
            if (!Double.isFinite(elapsedTicks)) {
                throw new IllegalArgumentException("Playback time must be finite");
            }
            double t = Math.max(0.0D, Math.min(durationTicks, elapsedTicks));
            int lo = 0;
            int hi = frames.size() - 1;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (frames.get(mid).tick() <= t) lo = mid;
                else hi = mid - 1;
            }
            Keyframe current = frames.get(lo);
            float angle = current.angleDegrees();
            if (lo + 1 < frames.size()) {
                Keyframe next = frames.get(lo + 1);
                double alpha = (t - current.tick()) / (next.tick() - current.tick());
                angle = (float) (current.angleDegrees()
                        + (next.angleDegrees() - current.angleDegrees()) * alpha);
            }
            return new Sample(angle, current.running(), current.overloaded(),
                    elapsedTicks >= durationTicks);
        }
    }

    /**
     * A stepwise, resource-pack-authored scene property. Values 0..5 map to
     * PR #12's baked model variants. Each sample is stateless: late joiners,
     * seeking or frame skipping reconstruct the same model at the same tick.
     */
    public record DiscreteKeyframe(int tick, int value) {
        public DiscreteKeyframe {
            if (tick < 0 || value < 0 || value > 5) {
                throw new IllegalArgumentException("Invalid recorded model stage");
            }
        }
    }

    public static final class DiscreteTrack {
        private final int durationTicks;
        private final List<DiscreteKeyframe> frames;

        public DiscreteTrack(int durationTicks, List<DiscreteKeyframe> keyframes) {
            if (durationTicks < 1 || durationTicks > 12000) {
                throw new IllegalArgumentException("Invalid recording duration");
            }
            if (keyframes == null || keyframes.isEmpty() || keyframes.size() > 128) {
                throw new IllegalArgumentException("Expected 1..128 discrete keyframes");
            }
            List<DiscreteKeyframe> ordered = new ArrayList<>(keyframes);
            ordered.sort(Comparator.comparingInt(DiscreteKeyframe::tick));
            if (ordered.get(0).tick() != 0) {
                throw new IllegalArgumentException("First model stage must start at tick zero");
            }
            for (int i = 0; i < ordered.size(); i++) {
                if (ordered.get(i).tick() >= durationTicks
                        || (i > 0 && ordered.get(i).tick() == ordered.get(i - 1).tick())) {
                    throw new IllegalArgumentException(
                            "Discrete keyframes must be unique and before recording completion");
                }
            }
            this.durationTicks = durationTicks;
            this.frames = List.copyOf(ordered);
        }

        /** Immutable model-stage cues for checking pack/scene compatibility. */
        public List<DiscreteKeyframe> keyframes() {
            return frames;
        }

        public int sample(double elapsedTicks) {
            if (!Double.isFinite(elapsedTicks)) {
                throw new IllegalArgumentException("Recording time must be finite");
            }
            double tick = Math.max(0D, Math.min(durationTicks, elapsedTicks));
            int lo = 0;
            int hi = frames.size() - 1;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (frames.get(mid).tick() <= tick) lo = mid;
                else hi = mid - 1;
            }
            return frames.get(lo).value();
        }
    }

    /** Named animation inputs for the PR #12 scene graph. */
    public record Frame(Map<String, Float> angles, Set<String> states,
                        boolean finished) {
        public Frame {
            angles = Map.copyOf(angles);
            states = Set.copyOf(states);
        }
    }

    public static Frame sample(Map<String, Track> channels, double elapsedTicks) {
        if (channels == null || channels.isEmpty() || channels.size() > 32) {
            throw new IllegalArgumentException("Recording requires 1..32 channels");
        }
        Map<String, Float> angles = new HashMap<>();
        boolean running = false;
        boolean overloaded = false;
        boolean finished = true;
        for (Map.Entry<String, Track> entry : channels.entrySet()) {
            Sample sample = entry.getValue().sample(elapsedTicks);
            angles.put(entry.getKey(), sample.angleDegrees());
            running |= sample.running();
            overloaded |= sample.overloaded();
            finished &= sample.finished();
        }
        Set<String> states = overloaded ? Set.of("overloaded") :
                running ? Set.of("running") : Set.of("stopped");
        return new Frame(angles, states, finished);
    }
}
