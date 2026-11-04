package com.magneticraft2.common.systems.PRESSURE;

import com.magneticraft2.common.blockentity.stage.stone.BellowsMultiblockModuleEntity;
import com.magneticraft2.common.block.stage.steam.SteamPipeBlock;
import net.minecraft.server.level.ServerLevel;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Server-driven gas pipe transport. Pipes are passive ordinary blocks. Neither
 * pressure, gas nor heat is stored by the pipe geometry. No ticking BEs.
 * Callers MUST enforce their own gas production and output rate per server tick.
 */
public final class PressurizedPipeNetwork {
    public static final int MAX_AIR_PIPES = 128;
    public static final int MAX_STEAM_PIPES = 256;
    private static final double DROP_KPA_PER_PIPE = 0.20;
    private static final Direction[] DIRECTIONS = Direction.values();
    private PressurizedPipeNetwork() {}

    public record Receiver(BlockPos pos, Direction side, int length,
            IPressurizedGasHandler handler) {}
    public record Transfer(int amount, int nextReceiver) {}
    private record Step(BlockPos pos, int length) {}
    private record PortKey(BlockPos pos, Direction side) {}

    private static boolean isPipe(Level level, BlockPos pos, GasMedium medium) {
        if (!level.hasChunkAt(pos)) return false;
        return medium == GasMedium.AIR
                ? level.getBlockState(pos).is(BlockRegistry.BRONZE_AIR_DUCT.get())
                : level.getBlockState(pos).getBlock() instanceof SteamPipeBlock;
    }

    /** Directional capability only: no unsided fallback or cross-medium links. */
    @Nullable public static IPressurizedGasHandler port(BlockEntity entity,
            Direction side, GasMedium medium) {
        IPressurizedGasHandler handler = entity
                .getCapability(CapabilityPressurizedGas.GAS, side).orElse(null);
        if (handler != null)
            return handler.medium() == medium ? handler : null;
        if (medium != GasMedium.AIR) return null;
        if (entity instanceof BellowsMultiblockModuleEntity bellows
                && !bellows.isFormedModule()) return null;
        IPressureStorage old = entity.getCapability(
                CapabilityPressure.PRESSURE, side).orElse(null);
        return old == null ? null : new LegacyAirPort(old);
    }

    public static List<Receiver> receivers(Level level, BlockPos firstPipe,
            @Nullable BlockPos sourcePos, GasMedium medium) {
        if (level == null || !isPipe(level, firstPipe, medium)) return List.of();
        int cap = medium == GasMedium.AIR ? MAX_AIR_PIPES : MAX_STEAM_PIPES;
        ArrayDeque<Step> queue = new ArrayDeque<>();
        Set<BlockPos> discovered = new HashSet<>();
        Set<PortKey> endpoints = new HashSet<>();
        List<Receiver> result = new ArrayList<>();
        queue.addLast(new Step(firstPipe.immutable(), 1));
        discovered.add(firstPipe.immutable());
        while (!queue.isEmpty()) {
            Step current = queue.removeFirst();
            for (Direction dir : DIRECTIONS) {
                BlockPos adjacent = current.pos().relative(dir);
                if ((sourcePos != null && adjacent.equals(sourcePos))
                        || !level.hasChunkAt(adjacent)) continue;
                if (isPipe(level, adjacent, medium)) {
                    if (discovered.size() < cap && discovered.add(adjacent.immutable()))
                        queue.addLast(new Step(adjacent.immutable(), current.length() + 1));
                    continue;
                }
                Direction side = dir.getOpposite();
                if (!endpoints.add(new PortKey(adjacent.immutable(), side))) continue;
                BlockEntity entity = level.getBlockEntity(adjacent);
                if (entity == null) continue;
                IPressurizedGasHandler port = port(entity, side, medium);
                if (port != null && port.canReceive())
                    result.add(new Receiver(adjacent.immutable(), side,
                            current.length(), port));
            }
        }
        return result;
    }

    /**
     * Legacy integer charges cannot be compared in kPa. New typed endpoints
     * only flow down a positive pressure gradient after line resistance.
     * Source and destination implementations must provide deterministic
     * simulation and commit on the same authoritative server thread.
     */
    public static int transferDirect(IPressurizedGasHandler source,
            IPressurizedGasHandler destination, int pipeLength) {
        if (source == null || destination == null
                || source.medium() != destination.medium()
                || !source.canExtract() || !destination.canReceive()
                || pipeLength < 0) return 0;
        boolean legacy = source instanceof LegacyAirPort
                || destination instanceof LegacyAirPort;
        if (!legacy && source.gaugePressureKPa() <=
                destination.gaugePressureKPa() + pipeLength * DROP_KPA_PER_PIPE)
            return 0;
        int limit = Math.min(source.maxFlowPerTick(), destination.maxFlowPerTick());
        if (limit <= 0) return 0;
        int offered = source.extract(limit, true);
        if (offered <= 0) return 0;
        int accepted = destination.receive(offered, source.temperatureK(), true);
        if (accepted <= 0) return 0;
        // Source first avoids depositing gas that was never produced. Neither
        // handler may change during this synchronous call: see interface contract.
        int drained = source.extract(accepted, false);
        if (drained <= 0) return 0;
        int deposited = destination.receive(drained, source.temperatureK(), false);
        if (deposited != drained)
            throw new IllegalStateException("Gas port violated simulate/commit contract");
        return drained;
    }

    public static Transfer transfer(Level level, BlockPos firstPipe,
            BlockPos sourcePos, IPressurizedGasHandler source, int cursor) {
        if (level == null || level.isClientSide || source == null)
            return new Transfer(0, cursor);
        // Steam pipes are reservoirs, not teleport links between machine ports.
        // The server-owned steam ledger is the only place new STEAM may enter
        // this network. No such special routing is needed for legacy AIR.
        if (source.medium() == GasMedium.STEAM) {
            if (!(level instanceof ServerLevel server) || !source.canExtract())
                return new Transfer(0, cursor);
            if (source.gaugePressureKPa() <=
                    SteamNetworkManager.gaugePressureKPa(server, firstPipe))
                return new Transfer(0, cursor);
            int max = Math.min(source.maxFlowPerTick(), 64);
            int available = source.extract(max, true);
            int accepted = SteamNetworkManager.simulateAcceptSteam(
                    server, firstPipe, available, source.temperatureK());
            if (accepted <= 0) return new Transfer(0, cursor);
            int drained = source.extract(accepted, false);
            if (drained <= 0) return new Transfer(0, cursor);
            int stored = SteamNetworkManager.acceptSteam(
                    server, firstPipe, drained, source.temperatureK());
            if (stored != drained)
                throw new IllegalStateException("Steam admission changed during transfer");
            return new Transfer(stored, cursor);
        }
        List<Receiver> targets = receivers(level, firstPipe, sourcePos, source.medium());
        for (int i = 0; i < targets.size(); i++) {
            int index = Math.floorMod(cursor + i, targets.size());
            Receiver target = targets.get(index);
            int transferred = transferDirect(source, target.handler(), target.length());
            if (transferred > 0)
                return new Transfer(transferred, (index + 1) % targets.size());
        }
        return new Transfer(0, cursor);
    }
}
