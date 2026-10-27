package com.magneticraft2.client.hologram;

import com.magneticraft2.client.animation.AnimationDefinition;
import com.magneticraft2.client.animation.AnimationFrame;
import com.magneticraft2.common.hologram.RecordingTimeline;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Converts a predetermined hologram timeline to the exact visual frame type
 * already consumed by PR #12's AnimatedComponentRenderer.
 *
 * <p>This adapter does not create a machine, mechanical network or block entity.
 * Its inputs are recorded values, not live machine measurements.
 */
public final class HologramAnimationBridge {
    private HologramAnimationBridge() {}

    public static AnimationFrame frame(Map<String, RecordingTimeline.Track> tracks,
                                       double elapsedTicks) {
        return frame(tracks, Map.of(), elapsedTicks);
    }

    public static AnimationFrame frame(
            Map<String, RecordingTimeline.Track> tracks,
            Map<String, RecordingTimeline.DiscreteTrack> visualChannels,
            double elapsedTicks) {
        RecordingTimeline.Frame sampled = RecordingTimeline.sample(tracks, elapsedTicks);
        Set<AnimationDefinition.State> states =
                EnumSet.noneOf(AnimationDefinition.State.class);
        if (sampled.states().contains("overloaded")) {
            states.add(AnimationDefinition.State.OVERLOADED);
        } else if (sampled.states().contains("running")) {
            states.add(AnimationDefinition.State.RUNNING);
        } else {
            states.add(AnimationDefinition.State.STOPPED);
        }
        Map<String, Integer> modelStages = new HashMap<>();
        for (Map.Entry<String, RecordingTimeline.DiscreteTrack> entry
                : visualChannels.entrySet()) {
            modelStages.put(entry.getKey(), entry.getValue().sample(elapsedTicks));
        }
        return new AnimationFrame(sampled.angles(), states, modelStages);
    }
}
