package com.magneticraft2.client.animation;

import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Set;

/**
 * Read-only client snapshot of real, synchronized machine presentation state.
 *
 * Angles drive existing mechanical scenes; scalar channels are normalized
 * [0..1] real processing progress. Item channels contain the actual client
 * ItemStack shown in a machine port, not a fabricated decorative item.
 * Callers keep the ItemStacks read-only while the frame is rendered.
 *
 * This record is client-side presentation only: JSON cannot change inventory,
 * recipe duration, server-authoritative progress, load, or machine state.
 */
public record AnimationFrame(Map<String, Float> moduleAngles,
                             Set<AnimationDefinition.State> states,
                             Map<String, Integer> integerInputs,
                             Map<String, Float> scalarInputs,
                             Map<String, ItemStack> itemInputs) {
    /** Existing legacy mechanical renderers need no edits. */
    public AnimationFrame(Map<String, Float> moduleAngles,
                          Set<AnimationDefinition.State> states) {
        this(moduleAngles, states, Map.of(), Map.of(), Map.of());
    }

    /** Preserve the original wear/integer visual input constructor. */
    public AnimationFrame(Map<String, Float> moduleAngles,
                          Set<AnimationDefinition.State> states,
                          Map<String, Integer> integerInputs) {
        this(moduleAngles, states, integerInputs, Map.of(), Map.of());
    }

    public AnimationFrame {
        moduleAngles = Map.copyOf(moduleAngles);
        states = Set.copyOf(states);
        integerInputs = Map.copyOf(integerInputs);
        scalarInputs = Map.copyOf(scalarInputs);
        itemInputs = Map.copyOf(itemInputs);

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
        for (float normalized : scalarInputs.values()) {
            if (!Float.isFinite(normalized) || normalized < 0F || normalized > 1F) {
                throw new IllegalArgumentException(
                        "Animation progress must be finite and normalized to [0,1]");
            }
        }
    }

    public Float angle(String module) {
        return moduleAngles.get(module);
    }

    public Integer integer(String source) {
        return integerInputs.get(source);
    }

    public Float scalar(String source) {
        return scalarInputs.get(source);
    }

    /** Null = unavailable channel; ItemStack.EMPTY = valid but empty slot. */
    public ItemStack item(String source) {
        return itemInputs.get(source);
    }
}
