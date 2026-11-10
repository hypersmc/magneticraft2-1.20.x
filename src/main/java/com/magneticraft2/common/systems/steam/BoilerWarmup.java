package com.magneticraft2.common.systems.steam;

import com.magneticraft2.common.block.stage.steam.BoilerServiceFittingBlock;
import com.magneticraft2.common.block.stage.steel.SteelBoilerVesselBlock;
import com.magneticraft2.common.blockentity.stage.steam.BoilerFireboxBlockEntity;
import com.magneticraft2.common.blockentity.stage.steel.SteelBoilerVesselBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import com.magneticraft2.common.systems.PRESSURE.SteamNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * One authoritative vessel owner for liquid water, HEAT, finite fuel and the
 * small Steam buffer. Steam output reuses SteamNetworkManager; no second
 * pressure graph, legacy AIR pressure or electric power path is introduced.
 */
public final class BoilerWarmup implements IFluidHandler {
    private static final String NBT = "BoilerWarmup";
    private final SteelBoilerVesselBlockEntity owner;
    private BoilerWarmupState state = new BoilerWarmupState();
    private BoilerSteamState steam = new BoilerSteamState();
    @Nullable private Direction geometryFacing;
    private BlockPos inlet = BlockPos.ZERO, steamOutlet = BlockPos.ZERO,
            safetyMount = BlockPos.ZERO, steamPipe = BlockPos.ZERO, outletAbove = BlockPos.ZERO;
    private BlockPos[] stack = new BlockPos[0], requiredChunks = new BlockPos[0];
    private long lastTick = Long.MIN_VALUE;

    public BoilerWarmup(SteelBoilerVesselBlockEntity owner) { this.owner = owner; }

    @Nullable private ServerLevel server() {
        if (owner.isRemoved() || !owner.isFormed()
                || !(owner.getLevel() instanceof ServerLevel level)
                || !level.hasChunkAt(owner.getBlockPos())
                || level.getBlockEntity(owner.getBlockPos()) != owner) return null;
        return level;
    }
    private Direction facing() {
        return owner.getBlockState().getValue(SteelBoilerVesselBlock.FACING);
    }

    private void geometry() {
        Direction direction = facing();
        if (direction == geometryFacing) return;
        geometryFacing = direction;
        BlockPos p = owner.getBlockPos();
        inlet = BoilerServiceFittingBlock.attachmentPosition(
                p, BoilerServiceFittingBlock.Role.WATER, direction);
        steamOutlet = BoilerServiceFittingBlock.attachmentPosition(
                p, BoilerServiceFittingBlock.Role.STEAM, direction);
        safetyMount = BoilerServiceFittingBlock.attachmentPosition(
                p, BoilerServiceFittingBlock.Role.SAFETY, direction);
        steamPipe = BoilerServiceFittingBlock.steamPipePosition(steamOutlet, direction);
        stack = new BlockPos[BoilerFireboxPlan.STACK_HEIGHT];
        for (int i = 0; i < stack.length; i++)
            stack[i] = BoilerFireboxLayout.stack(p, direction, i);
        outletAbove = stack[stack.length - 1].above();

        BlockPos a = p.offset(BoilerFireboxLayout.offset(
                new BoilerFireboxPlan.Cell(-1, 0, -4), direction));
        BlockPos b = p.offset(BoilerFireboxLayout.offset(
                new BoilerFireboxPlan.Cell(2, 0, 2), direction));
        List<BlockPos> chunks = new ArrayList<>();
        for (int x = Math.min(a.getX(), b.getX()) >> 4;
                x <= (Math.max(a.getX(), b.getX()) >> 4); x++)
            for (int z = Math.min(a.getZ(), b.getZ()) >> 4;
                    z <= (Math.max(a.getZ(), b.getZ()) >> 4); z++)
                chunks.add(new BlockPos(x << 4, 0, z << 4));
        requiredChunks = chunks.toArray(BlockPos[]::new);
    }

    private String interlock(ServerLevel level) {
        geometry();
        for (BlockPos pos : requiredChunks) if (!level.hasChunkAt(pos)) return "unloaded";
        BoilerFireboxBlockEntity front = BoilerFireboxInstallation.linkedFirebox(level, owner);
        if (front == null) return "Form and link the firebox first";
        BlockState waterPort = level.getBlockState(inlet);
        if (!waterPort.is(BlockRegistry.BOILER_WATER_INLET.get())
                || waterPort.getValue(BoilerServiceFittingBlock.FACING) != facing())
            return "Install the water inlet";
        if (state.water() == 0) return "Fill the vessel with water before lighting fuel";
        for (BlockPos pos : stack) {
            BlockState section = level.getBlockState(pos);
            if (!section.is(BlockRegistry.BOILER_FLUE_CHIMNEY.get())
                    || section.getValue(BoilerServiceFittingBlock.FACING) != facing())
                return "Install all three chimney sections before lighting fuel";
        }
        if (!level.getBlockState(outletAbove).isAir()) return "Clear the chimney exhaust";
        return "";
    }

    private boolean steamHardwareReady(ServerLevel level) {
        geometry();
        if (!level.hasChunkAt(steamOutlet) || !level.hasChunkAt(safetyMount)) return false;
        BlockState output = level.getBlockState(steamOutlet);
        BlockState safety = level.getBlockState(safetyMount);
        return output.is(BlockRegistry.BOILER_STEAM_OUTLET.get())
                && output.getValue(BoilerServiceFittingBlock.FACING) == facing()
                && safety.is(BlockRegistry.BOILER_SAFETY_MOUNT.get())
                && safety.getValue(BoilerServiceFittingBlock.FACING) == facing();
    }

    private boolean pipeConnected(ServerLevel level) {
        geometry();
        return level.hasChunkAt(steamPipe)
                && level.getBlockState(steamPipe).is(BlockRegistry.STEAM_PIPE.get());
    }

    /** Server ticker only; no offline catch-up and no unbounded graph scans here. */
    public void tick() {
        if (!state.burning() && state.heat() == 0 && steam.stored() == 0) return;
        ServerLevel level = server();
        if (level == null || lastTick == level.getGameTime()) return;
        lastTick = level.getGameTime();

        BoilerWarmupParameters.Rules rules = BoilerWarmupParameters.rules();
        if (rules == null) {
            if (state.burning()) { state.extinguish(); owner.setChanged(); }
            return;
        }

        boolean changed = false;
        boolean wasBurning = state.burning();
        String reason = wasBurning ? interlock(level) : "";
        if (wasBurning && !reason.isEmpty() && !reason.equals("unloaded")) {
            state.extinguish();
            changed = true;
        }
        boolean spentFuel = state.tick(reason.isEmpty(),
                rules.heatCapacity(), rules.passiveLoss());
        changed |= spentFuel || state.heat() > 0;

        if (rules.steamEnabled() && steamHardwareReady(level)) {
            for (int i = 0; i < rules.maxPhaseUnitsPerTick()
                    && steam.space(rules.steamBufferCapacity()) > 0; i++) {
                if (!state.convertToSteam(rules.steamWaterPerUnit(),
                        rules.steamHeatPerUnit(), rules.boilThresholdHeat())) break;
                if (steam.produce(1, rules.steamBufferCapacity()) != 1)
                    throw new IllegalStateException("Steam buffer rejected pre-authorized unit");
                changed = true;
            }

            if (steam.stored() > 0 && pipeConnected(level)) {
                int offered = Math.min(steam.stored(), rules.maxOutputUnitsPerTick());
                int accepted = SteamNetworkManager.acceptSteamBelowPressure(level, steamPipe,
                        offered, rules.steamTemperatureK(), rules.outputCutoffKPa());
                if (accepted > 0) {
                    if (steam.extract(accepted) != accepted)
                        throw new IllegalStateException("Steam export ledger mismatch");
                    changed = true;
                }
            }
        }

        if (changed) owner.setChanged();
        if (wasBurning != state.burning()) owner.sync();

        if (spentFuel && level.getGameTime() % 20L == 0L)
            level.sendParticles(ParticleTypes.SMOKE, outletAbove.getX() + 0.5,
                    outletAbove.getY() + 0.05, outletAbove.getZ() + 0.5,
                    1, 0.05, 0.05, 0.05, 0.01);
    }

    public void useFirebox(BoilerFireboxBlockEntity front, Player player, InteractionHand hand) {
        ServerLevel level = server();
        if (level == null || hand != InteractionHand.MAIN_HAND) return;
        if (!player.getAbilities().mayBuild || !level.mayInteract(player, front.getBlockPos())
                || BoilerFireboxInstallation.linkedFirebox(level, owner) != front) return;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (player.isShiftKeyDown() && state.burning()) {
                state.extinguish(); owner.setChanged(); owner.sync();
            }
            status(player);
            return;
        }

        BoilerWarmupParameters.Rules rules = BoilerWarmupParameters.rules();
        if (rules == null) { tell(player, "Boiler rules are missing or invalid"); return; }
        String reason = interlock(level);
        if (!reason.isEmpty()) {
            tell(player, reason.equals("unloaded")
                    ? "Load the complete boiler and firebox first" : reason);
            return;
        }
        if (state.burning()) {
            tell(player, "Fuel is already burning; no extra item consumed");
            status(player);
            return;
        }
        if (state.heat() >= rules.heatCapacity()) {
            tell(player, "Boiler HEAT buffer is full; no fuel consumed");
            return;
        }

        int ticks = held.getCraftingRemainingItem().isEmpty()
                ? (held.is(ItemRegistry.ITEM_METALLURGICAL_COKE.get()) ? 3200
                    : ForgeHooks.getBurnTime(held, RecipeType.SMELTING)) : 0;
        if (!state.ignite(ticks, rules.heatPerBurnTick())) {
            tell(player, "Use solid furnace fuel; container fuels are not supported");
            return;
        }
        if (!player.getAbilities().instabuild) held.shrink(1);
        owner.setChanged();
        owner.sync();
        status(player);
    }

    public static InteractionResult useInlet(Level world, BlockPos pos, BlockState port,
            Player player, InteractionHand hand) {
        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (!(world instanceof ServerLevel level) || !player.getAbilities().mayBuild
                || !world.mayInteract(player, pos)) return InteractionResult.FAIL;
        Direction facing = port.getValue(BoilerServiceFittingBlock.FACING);
        BlockPos delta = BoilerServiceFittingBlock.attachmentPosition(BlockPos.ZERO,
                BoilerServiceFittingBlock.Role.WATER, facing);
        BlockPos vesselPos = pos.subtract(delta);
        if (!world.hasChunkAt(vesselPos)
                || !(world.getBlockEntity(vesselPos) instanceof SteelBoilerVesselBlockEntity vessel)
                || !vessel.isFormed()
                || vessel.getBlockState().getValue(SteelBoilerVesselBlock.FACING) != facing)
            return InteractionResult.FAIL;
        if (!world.mayInteract(player, vesselPos)) return InteractionResult.FAIL;

        BoilerWarmup warmup = vessel.warmup();
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && !FluidUtil.interactWithFluidHandler(player, hand, warmup))
            tell(player, "Use a water container or empty container; the vessel may be full or empty");
        else warmup.status(player);
        return InteractionResult.CONSUME;
    }

    public void status(Player player) {
        BoilerWarmupParameters.Rules rules = BoilerWarmupParameters.rules();
        String limits = rules == null ? "rules disabled"
                : state.water() + "/" + rules.waterCapacity() + " mB water; "
                + state.heat() + "/" + rules.heatCapacity() + " HEAT";
        if (rules == null || !rules.steamEnabled()) {
            tell(player, limits + "; fuel " + state.burnTicks() + " ticks; Steam disabled");
            return;
        }

        ServerLevel level = server();
        String network = "disconnected";
        if (level != null && pipeConnected(level))
            network = (Math.round(SteamNetworkManager.gaugePressureKPa(
                    level, steamPipe) * 10.0) / 10.0) + " kPa";
        tell(player, limits + "; fuel " + state.burnTicks() + " ticks; Steam "
                + steam.stored() + "/" + rules.steamBufferCapacity()
                + " units; network " + network);
    }

    private static void tell(Player player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }

    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int tank) {
        return tank == 0 && state.water() > 0
                ? new FluidStack(Fluids.WATER, state.water()) : FluidStack.EMPTY;
    }
    @Override public int getTankCapacity(int tank) {
        BoilerWarmupParameters.Rules rules = BoilerWarmupParameters.rules();
        return tank == 0 ? Math.max(state.water(),
                rules == null ? 0 : rules.waterCapacity()) : 0;
    }
    @Override public boolean isFluidValid(int tank, FluidStack fluid) {
        return tank == 0 && !fluid.isEmpty()
                && fluid.getFluid() == Fluids.WATER && !fluid.hasTag();
    }
    @Override public int fill(FluidStack resource, FluidAction action) {
        BoilerWarmupParameters.Rules rules = BoilerWarmupParameters.rules();
        if (server() == null || rules == null || !isFluidValid(0, resource)) return 0;
        int accepted = state.fill(resource.getAmount(), rules.waterCapacity(), action.simulate());
        if (accepted > 0 && action.execute()) { owner.setChanged(); owner.sync(); }
        return accepted;
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        return isFluidValid(0, resource) ? drain(resource.getAmount(), action)
                : FluidStack.EMPTY;
    }
    @Override public FluidStack drain(int requested, FluidAction action) {
        if (server() == null) return FluidStack.EMPTY;
        int amount = state.drain(requested, action.simulate());
        if (amount > 0 && action.execute()) { owner.setChanged(); owner.sync(); }
        return amount > 0 ? new FluidStack(Fluids.WATER, amount) : FluidStack.EMPTY;
    }

    public void discardContents() {
        if (!(owner.getLevel() instanceof ServerLevel)) return;
        BoilerWarmupState.Snapshot before = state.spill();
        int steamSpilled = steam.spill();
        if (before.water() != 0 || before.heat() != 0
                || before.burnTicks() != 0 || steamSpilled != 0) {
            LogManager.getLogger("Magneticraft2Boiler").info(
                    "Boiler dismantled at {}: spilled {} mB, dissipated {} HEAT, discarded {} fuel ticks, vented {} Steam units",
                    owner.getBlockPos(), before.water(), before.heat(),
                    before.burnTicks(), steamSpilled);
            owner.setChanged();
        }
    }

    public void stopFuel() {
        if (!(owner.getLevel() instanceof ServerLevel)) return;
        if (state.burning()) { state.extinguish(); owner.setChanged(); }
    }

    public void save(CompoundTag root) {
        BoilerWarmupState.Snapshot s = state.snapshot();
        BoilerSteamState.Snapshot gas = steam.snapshot();
        CompoundTag n = new CompoundTag();
        n.putInt("Version", 2);
        n.putInt("Water", s.water()); n.putInt("Heat", s.heat());
        n.putInt("BurnTicks", s.burnTicks());
        n.putInt("HeatPerBurnTick", s.heatPerBurnTick());
        n.putLong("WaterIn", s.waterIn()); n.putLong("WaterOut", s.waterOut());
        n.putLong("WaterSpilled", s.waterSpilled());
        n.putLong("WaterToSteam", s.waterToSteam());
        n.putLong("FuelEnergy", s.fuelEnergy());
        n.putLong("GeneratedHeat", s.generatedHeat());
        n.putLong("HeatLost", s.heatLost());
        n.putLong("HeatInDrainedWater", s.heatInDrainedWater());
        n.putLong("HeatSpilled", s.heatSpilled());
        n.putLong("FuelDiscarded", s.fuelDiscarded());
        n.putLong("HeatToSteam", s.heatToSteam());
        n.putInt("SteamStored", gas.stored());
        n.putLong("SteamProduced", gas.produced());
        n.putLong("SteamExported", gas.exported());
        n.putLong("SteamSpilled", gas.spilled());
        root.put(NBT, n);
    }

    public void load(CompoundTag root) {
        state = new BoilerWarmupState();
        steam = new BoilerSteamState();
        geometryFacing = null;
        lastTick = Long.MIN_VALUE;
        if (!root.contains(NBT, Tag.TAG_COMPOUND)) return;
        CompoundTag n = root.getCompound(NBT);
        try {
            int version = n.getInt("Version");
            if (version != 1 && version != 2)
                throw new IllegalArgumentException("Unsupported warm-up version");
            BoilerWarmupState.Snapshot water = version == 1
                    ? new BoilerWarmupState.Snapshot(n.getInt("Water"), n.getInt("Heat"),
                        n.getInt("BurnTicks"), n.getInt("HeatPerBurnTick"),
                        n.getLong("WaterIn"), n.getLong("WaterOut"),
                        n.getLong("WaterSpilled"), n.getLong("FuelEnergy"),
                        n.getLong("GeneratedHeat"), n.getLong("HeatLost"),
                        n.getLong("HeatInDrainedWater"), n.getLong("HeatSpilled"),
                        n.getLong("FuelDiscarded"))
                    : new BoilerWarmupState.Snapshot(n.getInt("Water"), n.getInt("Heat"),
                        n.getInt("BurnTicks"), n.getInt("HeatPerBurnTick"),
                        n.getLong("WaterIn"), n.getLong("WaterOut"),
                        n.getLong("WaterSpilled"), n.getLong("FuelEnergy"),
                        n.getLong("GeneratedHeat"), n.getLong("HeatLost"),
                        n.getLong("HeatInDrainedWater"), n.getLong("HeatSpilled"),
                        n.getLong("FuelDiscarded"), n.getLong("WaterToSteam"),
                        n.getLong("HeatToSteam"));
            state.restore(water);
            if (version == 2)
                steam.restore(new BoilerSteamState.Snapshot(
                        n.getInt("SteamStored"), n.getLong("SteamProduced"),
                        n.getLong("SteamExported"), n.getLong("SteamSpilled")));
        } catch (IllegalArgumentException invalid) {
            state = new BoilerWarmupState();
            steam = new BoilerSteamState();
            LogManager.getLogger("Magneticraft2Boiler").error(
                    "Rejected invalid boiler save at {}: {}",
                    owner.getBlockPos(), invalid.getMessage());
        }
    }
}
