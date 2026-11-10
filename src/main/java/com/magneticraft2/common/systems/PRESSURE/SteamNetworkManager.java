package com.magneticraft2.common.systems.PRESSURE;

import com.magneticraft2.common.block.stage.bronze.BronzeAirDuctBlock;
import com.magneticraft2.common.block.stage.steam.RupturedSteamPipeBlock;
import com.magneticraft2.common.block.stage.steam.SteamPipeBlock;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Initial server-only steam reservoir and rupture solver.
 *
 * Gas/thermal energy is persisted sparsely per steam-bearing pipe in dimension
 * SavedData (never individual block entities). Connected loaded pipes are
 * a shared bulk-pressure component. Equal shares make topology splits, merges
 * and chunk unloads conserve existing inventories without ghost network IDs.
 *
 * Only unsafe overpressure can break a pipe. A ruptured pipe is a single vent
 * per component: no passive wear, repeated random failures or runaway cascades.
 * No boilers are implemented here: future steam-producing machines call
 * acceptSteam after actually paying their HEAT/water/energy costs.
 */
public final class SteamNetworkManager {
    public static final int MAX_LOADED_PIPES = 256;
    private static final int SOLVE_INTERVAL_TICKS = 20;
    private static final int MAX_PORT_BATCH = 64;
    private static final Direction[] SIDES = Direction.values();
    private SteamNetworkManager() {}

    private record Component(List<BlockPos> positions, boolean overflow) {}
    private record Content(long units, double energy) {}

    private static boolean isSteam(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos)
                && level.getBlockState(pos).getBlock() instanceof SteamPipeBlock;
    }

    private static Component component(ServerLevel level, BlockPos start) {
        if (!isSteam(level, start)) return new Component(List.of(), false);
        Set<BlockPos> discovered = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos root = start.immutable();
        discovered.add(root);
        queue.add(root);
        boolean overflow = false;
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : SIDES) {
                BlockPos adjacent = current.relative(direction);
                if (!isSteam(level, adjacent) || discovered.contains(adjacent)) continue;
                if (discovered.size() == MAX_LOADED_PIPES) {
                    overflow = true;
                    continue;
                }
                BlockPos immutable = adjacent.immutable();
                discovered.add(immutable);
                queue.addLast(immutable);
            }
        }
        List<BlockPos> sorted = new ArrayList<>(discovered);
        sorted.sort(Comparator.comparingLong(BlockPos::asLong));
        return new Component(sorted, overflow);
    }

    private static Content contents(SteamNetworkSavedData data, Component component) {
        long units = 0;
        double energy = 0;
        for (BlockPos pos : component.positions()) {
            SteamNetworkSavedData.Parcel parcel = data.parcel(pos);
            units += parcel.units();
            energy += parcel.energy();
        }
        return new Content(units, energy);
    }

    private static void spread(SteamNetworkSavedData data, Component component,
            long units, double energy) {
        int n = component.positions().size();
        if (n == 0) return;
        if (units < 0 || units > (long) n * 1000000
                || !Double.isFinite(energy) || energy < 0)
            throw new IllegalArgumentException("Invalid network contents");
        double temperature = units == 0 ? 0 : energy / units;
        for (int i = 0; i < n; i++) {
            int share = SteamNetworkMath.share(units, n, i);
            data.write(component.positions().get(i), share,
                    share == 0 ? 0 : share * temperature);
        }
    }

    private static double pressure(Content contents, Component component) {
        return SteamNetworkMath.pressureKPa(
                contents.units(), contents.energy(), component.positions().size());
    }

    /** No pipe wear, only strict structural overpressure. */
    private static void ruptureIfUnsafe(ServerLevel level, Component component,
            Content gas) {
        double actualPressure = pressure(gas, component);
        double safe = Magneticraft2ConfigCommon.GENERAL.SteamPipeRuptureKPa.get();
        // Avoid a false rupture at the rating due to double rounding.
        if (actualPressure <= safe + Math.max(0.000001, safe * 1e-9)
                || gas.units() <= 0) return;

        // An existing leak is already the network's sacrificial pressure
        // relief. Do not turn one mistake into dozens of broken pipes.
        for (BlockPos pos : component.positions())
            if (level.getBlockState(pos).getBlock() instanceof RupturedSteamPipeBlock)
                return;

        record Candidate(BlockPos pos, List<Direction> vents) {}
        List<Candidate> eligible = new ArrayList<>();
        for (BlockPos pos : component.positions()) {
            if (level.getBlockState(pos).getBlock() != BlockRegistry.STEAM_PIPE.get())
                continue;
            List<Direction> vents = new ArrayList<>();
            for (Direction direction : SIDES) {
                BlockPos exterior = pos.relative(direction);
                if (level.hasChunkAt(exterior) && !isSteam(level, exterior))
                    vents.add(direction);
            }
            if (!vents.isEmpty()) eligible.add(new Candidate(pos, vents));
        }
        if (eligible.isEmpty()) return;

        Candidate selected = eligible.get(level.getRandom().nextInt(eligible.size()));
        Direction face = selected.vents().get(
                level.getRandom().nextInt(selected.vents().size()));
        BlockState old = level.getBlockState(selected.pos());
        BlockState broken = BlockRegistry.RUPTURED_STEAM_PIPE.get()
                .defaultBlockState()
                .setValue(RupturedSteamPipeBlock.LEAK, face)
                .setValue(RupturedSteamPipeBlock.ACTIVE, true);
        for (Direction dir : SIDES)
            broken = broken.setValue(BronzeAirDuctBlock.property(dir),
                    old.getValue(BronzeAirDuctBlock.property(dir)));
        level.setBlock(selected.pos(), broken, Block.UPDATE_ALL);
    }

    private static void solve(ServerLevel level, SteamNetworkSavedData data,
            Component component) {
        if (component.overflow() || component.positions().isEmpty()) return;
        Content before = contents(data, component);
        if (before.units() <= 0) {
            setVentState(level, component, false);
            return;
        }
        // Equalize before fault evaluation. Already-stored shares are kept on
        // unloaded pipe positions, so newly split loaded islands cannot mint
        // or delete their disconnected share of the network inventory.
        spread(data, component, before.units(), before.energy());
        ruptureIfUnsafe(level, component, before);

        boolean hasVent = false;
        for (BlockPos pos : component.positions())
            if (level.getBlockState(pos).getBlock() instanceof RupturedSteamPipeBlock)
                hasVent = true;
        if (!hasVent) return;

        double kPa = pressure(before, component);
        int vented = SteamNetworkMath.leakUnits(before.units(), kPa);
        long remaining = before.units() - vented;
        double remainingEnergy = remaining == 0 ? 0
                : before.energy() * (remaining / (double) before.units());
        spread(data, component, remaining, remainingEnergy);
        setVentState(level, component, remaining > 0);
    }

    private static void setVentState(ServerLevel level, Component component,
            boolean active) {
        for (BlockPos pos : component.positions()) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof RupturedSteamPipeBlock
                    && state.getValue(RupturedSteamPipeBlock.ACTIVE) != active)
                level.setBlock(pos, state.setValue(
                        RupturedSteamPipeBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    /**
     * Pure capacity check for a source's simulated extraction. This must run
     * immediately before the actual transfer on the authoritative thread.
     */
    public static int simulateAcceptSteam(ServerLevel level, BlockPos pipe,
            int offered, double temperatureK) {
        if (offered <= 0 || !Double.isFinite(temperatureK)
                || temperatureK < 373.15 || temperatureK > 2000)
            return 0;
        Component component = component(level, pipe);
        if (component.overflow() || component.positions().isEmpty()) return 0;
        Content old = contents(SteamNetworkSavedData.get(level), component);
        long space = (long) component.positions().size() * 1000000 - old.units();
        return (int) Math.min(Math.min(Math.max(0, space), offered), MAX_PORT_BATCH);
    }

    /**
     * Admission check with a source-specific operating cutoff. This is not the
     * pipe rupture rating and does not vent anything; it simply refuses enough
     * source units to keep the projected shared component pressure at/below the
     * requested cutoff.
     */
    public static int simulateAcceptSteamBelowPressure(ServerLevel level, BlockPos pipe,
            int offered, double temperatureK, double maxGaugeKPa) {
        if (!Double.isFinite(maxGaugeKPa) || maxGaugeKPa <= 0) return 0;
        int capacityAccepted = simulateAcceptSteam(level, pipe, offered, temperatureK);
        if (capacityAccepted <= 0) return 0;
        Component component = component(level, pipe);
        if (component.overflow() || component.positions().isEmpty()) return 0;
        Content previous = contents(SteamNetworkSavedData.get(level), component);
        double epsilon = Math.max(0.000001, maxGaugeKPa * 1e-9);
        for (int accepted = capacityAccepted; accepted > 0; accepted--) {
            Content projected = new Content(previous.units() + accepted,
                    previous.energy() + accepted * temperatureK);
            if (pressure(projected, component) <= maxGaugeKPa + epsilon)
                return accepted;
        }
        return 0;
    }

    /**
     * Pressure-aware injection used by boiler outputs. The caller removes only
     * the returned amount from its own conserved buffer.
     */
    public static int acceptSteamBelowPressure(ServerLevel level, BlockPos pipe,
            int offered, double temperatureK, double maxGaugeKPa) {
        int accepted = simulateAcceptSteamBelowPressure(
                level, pipe, offered, temperatureK, maxGaugeKPa);
        if (accepted <= 0) return 0;
        Component component = component(level, pipe);
        SteamNetworkSavedData data = SteamNetworkSavedData.get(level);
        Content previous = contents(data, component);
        spread(data, component, previous.units() + accepted,
                previous.energy() + accepted * temperatureK);
        solve(level, data, component);
        return accepted;
    }

    /** Injects ONLY steam already produced by an authorized machine. */
    public static int acceptSteam(ServerLevel level, BlockPos pipe,
            int offered, double temperatureK) {
        int accepted = simulateAcceptSteam(level, pipe, offered, temperatureK);
        if (accepted <= 0) return 0;
        Component component = component(level, pipe);
        SteamNetworkSavedData data = SteamNetworkSavedData.get(level);
        Content previous = contents(data, component);
        // Source's own production budget MUST still be enforced by the caller.
        spread(data, component, previous.units() + accepted,
                previous.energy() + accepted * temperatureK);
        solve(level, data, component);
        return accepted;
    }

    /** A consuming machine drains exactly the inventory it receives. */
    public static int takeSteam(ServerLevel level, BlockPos pipe, int requested) {
        if (requested <= 0) return 0;
        Component component = component(level, pipe);
        if (component.overflow() || component.positions().isEmpty()) return 0;
        SteamNetworkSavedData data = SteamNetworkSavedData.get(level);
        Content before = contents(data, component);
        int taken = (int) Math.min(Math.min(requested, MAX_PORT_BATCH), before.units());
        if (taken == 0) return 0;
        long remaining = before.units() - taken;
        spread(data, component, remaining,
                remaining == 0 ? 0 : before.energy() * (remaining / (double) before.units()));
        setVentState(level, component, remaining > 0);
        return taken;
    }

    public static double gaugePressureKPa(ServerLevel level, BlockPos pipe) {
        Component component = component(level, pipe);
        if (component.overflow() || component.positions().isEmpty()) return 0;
        return pressure(contents(SteamNetworkSavedData.get(level), component), component);
    }

    /** Removing a real pipe vents only its existing share of gas. */
    public static void pipeRemoved(ServerLevel level, BlockPos removed) {
        SteamNetworkSavedData.get(level).write(removed, 0, 0);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)
                || level.getGameTime() % SOLVE_INTERVAL_TICKS != 0) return;
        SteamNetworkSavedData data = SteamNetworkSavedData.get(level);
        Set<Long> processed = new HashSet<>();
        for (long key : data.occupiedPositions()) {
            if (processed.contains(key)) continue;
            BlockPos pos = BlockPos.of(key);
            if (!level.hasChunkAt(pos)) continue; // preserved while unloaded
            if (!isSteam(level, pos)) {
                data.write(pos, 0, 0); // missing/externally replaced block
                continue;
            }
            Component component = component(level, pos);
            for (BlockPos connected : component.positions())
                processed.add(connected.asLong());
            if (!component.overflow()) solve(level, data, component);
        }
    }
}
