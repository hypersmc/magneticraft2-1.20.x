package com.magneticraft2.client.animation;

import java.util.Map;
import java.util.Set;

/**
 * Immutable snapshot of client-synchronized animation inputs.
 *
 * Mechanical angles (degrees), real machine states, and discrete properties
 * are supplied by Java. Resource-pack JSON can map them to visuals, but
 * cannot alter the machine's server-side processing or Gear V2 simulation.
 */
public record AnimationFrame(Map<String, Float> moduleAngles,
                             Set<AnimationDefinition.State> states,
                             Map<String, Integer> integerInputs) {
    /** Preserve old two-argument callers, including the Bronze Lathe. */
    public AnimationFrame(Map<String, Float> moduleAngles,
                          Set<AnimationDefinition.State> states) {
        this(moduleAngles, states, Map.of());
    }

    public AnimationFrame {
        moduleAngles = Map.copyOf(moduleAngles);
        states = Set.copyOf(states);
        integerInputs = Map.copyOf(integerInputs);
        if (states.contains(AnimationDefinition.State.RUNNING)
                && states.contains(AnimationDefinition.State.STOPPED)) {
            throw new IllegalArgumentException(
                    "A machine cannot be running and stopped simultaneously");
        }
        for (float angle : moduleAngles.values()) {
            if (!Float.isFinite(angle)) {
                throw new IllegalArgumentException("Animation input angles must be finite");
            }
        }
    }

    public Float angle(String module) {
        return moduleAngles.get(module);
    }

    public Integer integer(String source) {
        return integerInputs.get(source);
    }
}
