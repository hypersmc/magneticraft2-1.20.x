package com.magneticraft2.client.animation;

import java.util.Map;
import java.util.Set;

/**
 * One client render sample of already-synchronized machine state.
 *
 * A machine binds real Gear V2 phases (degrees) by module name. Animation
 * definitions cannot create mechanical power, RPM, or server game state.
 * RUNNING and STOPPED describe machine work; the mechanical input may still
 * turn when the machine is stopped. OVERLOADED and WORN are additional flags.
 */
public record AnimationFrame(Map<String, Float> moduleAngles,
                             Set<AnimationDefinition.State> states) {
    public AnimationFrame {
        moduleAngles = Map.copyOf(moduleAngles);
        states = Set.copyOf(states);
        if (states.contains(AnimationDefinition.State.RUNNING)
                && states.contains(AnimationDefinition.State.STOPPED)) {
            throw new IllegalArgumentException("A machine cannot be running and stopped simultaneously");
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
}
