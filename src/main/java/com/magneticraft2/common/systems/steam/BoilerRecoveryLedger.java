package com.magneticraft2.common.systems.steam;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Pending entries are claimed BEFORE modifying the world or spawning an item.
 * Reentrant block callbacks therefore cannot claim the same material twice.
 * Persistence and chunk readiness are supplied by BoilerAssemblyRecovery.
 */
public final class BoilerRecoveryLedger<T> {
    private final Map<Long, T> remaining = new LinkedHashMap<>();

    public BoilerRecoveryLedger(Map<Long, T> entries) {
        remaining.putAll(entries);
    }
    public Optional<T> claim(long position) {
        return Optional.ofNullable(remaining.remove(position));
    }
    public void updateIfPresent(long position, UnaryOperator<T> operation) {
        remaining.computeIfPresent(position, (key, value) -> operation.apply(value));
    }
    public Map<Long, T> snapshot() { return Map.copyOf(remaining); }
    public boolean isEmpty() { return remaining.isEmpty(); }
    public int size() { return remaining.size(); }
}
