package com.magneticraft2.common.systems.steam;

import com.magneticraft2.common.block.stage.steam.BoilerServiceFittingBlock;
import com.magneticraft2.common.block.stage.steam.BoilerSafetyMountBlock;
import com.magneticraft2.common.block.stage.steel.SteelBoilerVesselBlock;
import com.magneticraft2.common.blockentity.stage.steam.BoilerFireboxBlockEntity;
import com.magneticraft2.common.blockentity.stage.steel.SteelBoilerVesselBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import com.magneticraft2.common.systems.PRESSURE.SteamNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Containers;
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
    private static final int FUEL_QUEUE_LIMIT = 16;
    private final SteelBoilerVesselBlockEntity owner;
    private BoilerWarmupState state = new BoilerWarmupState();
    private BoilerSteamState steam = new BoilerSteamState();
    @Nullable private Direction geometryFacing;
    private BlockPos inlet = BlockPos.ZERO, steamOutlet = BlockPos.ZERO,
            safetyMount = BlockPos.ZERO, steamPipe = BlockPos.ZERO, outletAbove = BlockPos.ZERO;
    private BlockPos[] stack = new BlockPos[0], requiredChunks = new BlockPos[0];
    private long lastTick = Long.MIN_VALUE;
    private boolean reliefOpen;
    private long reliefVented;
    private final List<ItemStack> fuelQueue = new ArrayList<>();

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
        for (BlockPos pos : requiredChunks) if (!level.hasChunkAt(pos)) return false;
        if (BoilerFireboxInstallation.linkedFirebox(level, owner) == null) return false;
        if (!level.hasChunkAt(steamOutlet) || !level.hasChunkAt(safetyMount)) return false;
        BlockState waterPort = level.getBlockState(inlet);
        BlockState output = level.getBlockState(steamOutlet);
        BlockState safety = level.getBlockState(safetyMount);
        if (!waterPort.is(BlockRegistry.BOILER_WATER_INLET.get())
                || waterPort.getValue(BoilerServiceFittingBlock.FACING) != facing()
                || !output.is(BlockRegistry.BOILER_STEAM_OUTLET.get())
                || output.getValue(BoilerServiceFittingBlock.FACING) != facing()
                || !safety.is(BlockRegistry.BOILER_SAFETY_MOUNT.get())
                || safety.getValue(BoilerServiceFittingBlock.FACING) != facing()) return false;
        for (BlockPos pos : stack) {
            BlockState section = level.getBlockState(pos);
            if (!section.is(BlockRegistry.BOILER_FLUE_CHIMNEY.get())
                    || section.getValue(BoilerServiceFittingBlock.FACING) != facing()) return false;
        }
        return level.getBlockState(outletAbove).isAir();
    }

    private boolean pipeConnected(ServerLevel level) {
        geometry();
        return level.hasChunkAt(steamPipe)
                && level.getBlockState(steamPipe).is(BlockRegistry.STEAM_PIPE.get());
    }

    private double boilerPressureKPa(BoilerWarmupParameters.Rules rules) {
        if (!rules.vesselPressureEnabled()) return 0.0;
        return BoilerPressureMath.pressureKPa(steam.stored(),
                rules.steamTemperatureK(), rules.vesselPipeVolumeEquivalents());
    }

    private int transferSteamToNetwork(ServerLevel level,
            BoilerWarmupParameters.Rules rules) {
        if (steam.stored() <= 0 || !pipeConnected(level)) return 0;
        int offered = Math.min(steam.stored(), rules.maxOutputUnitsPerTick());

        if (!rules.vesselPressureEnabled())
            return SteamNetworkManager.acceptSteamBelowPressure(level, steamPipe,
                    offered, rules.steamTemperatureK(), rules.outputCutoffKPa());

        double networkPressure = SteamNetworkManager.gaugePressureKPa(level, steamPipe);
        double boilerPressure = boilerPressureKPa(rules);
        if (boilerPressure <= networkPressure + 0.000001) return 0;

        for (int amount = offered; amount > 0; amount--) {
            double afterBoiler = BoilerPressureMath.pressureKPa(
                    steam.stored() - amount, rules.steamTemperatureK(),
                    rules.vesselPipeVolumeEquivalents());
            if (afterBoiler <= 0.0) continue;
            int accepted = SteamNetworkManager.simulateAcceptSteamBelowPressure(
                    level, steamPipe, amount, rules.steamTemperatureK(), afterBoiler);
            if (accepted == amount)
                return SteamNetworkManager.acceptSteamBelowPressure(
                        level, steamPipe, amount, rules.steamTemperatureK(), afterBoiler);
        }
        return 0;
    }

    private boolean updateSafety(ServerLevel level, BoilerWarmupParameters.Rules rules) {
        geometry();
        if (!level.hasChunkAt(safetyMount)) return false;
        BlockState safety = level.getBlockState(safetyMount);
        if (!(safety.getBlock() instanceof BoilerSafetyMountBlock)
                || safety.getValue(BoilerServiceFittingBlock.FACING) != facing()) return false;

        boolean vesselDomain = rules.vesselPressureEnabled();
        double pressure = vesselDomain
                ? boilerPressureKPa(rules)
                : (pipeConnected(level)
                    ? SteamNetworkManager.gaugePressureKPa(level, steamPipe) : 0.0);
        int step = BoilerSafetyLogic.gaugeStep(pressure, rules.gaugeMaxKPa());

        if (rules.reliefEnabled() && (vesselDomain || pipeConnected(level))) {
            reliefOpen = BoilerSafetyLogic.nextReliefOpen(reliefOpen, pressure,
                    rules.reliefOpenKPa(), rules.reliefCloseKPa());
            if (reliefOpen) {
                int vented = vesselDomain
                        ? steam.vent(rules.maxReliefUnitsPerTick())
                        : SteamNetworkManager.ventSteam(
                            level, steamPipe, rules.maxReliefUnitsPerTick());
                if (vented > 0) {
                    reliefVented += vented;
                    pressure = vesselDomain
                            ? boilerPressureKPa(rules)
                            : SteamNetworkManager.gaugePressureKPa(level, steamPipe);
                    step = BoilerSafetyLogic.gaugeStep(pressure, rules.gaugeMaxKPa());
                    double x = safetyMount.getX() + 0.5;
                    double y = safetyMount.getY() + 1.02;
                    double z = safetyMount.getZ() + 0.5;
                    level.sendParticles(ParticleTypes.CLOUD, x, y, z,
                            5, 0.14, 0.16, 0.14, 0.025);
                    if (level.getGameTime() % 10L == 0L)
                        level.playSound(null, safetyMount, SoundEvents.FIRE_EXTINGUISH,
                                SoundSource.BLOCKS, 0.45F, 1.55F);
                }
            }
        } else {
            reliefOpen = false;
        }

        if (safety.getValue(BoilerSafetyMountBlock.PRESSURE_STEP) != step
                || safety.getValue(BoilerSafetyMountBlock.RELIEVING) != reliefOpen) {
            level.setBlock(safetyMount, safety
                    .setValue(BoilerSafetyMountBlock.PRESSURE_STEP, step)
                    .setValue(BoilerSafetyMountBlock.RELIEVING, reliefOpen),
                    Block.UPDATE_CLIENTS);
            return true;
        }
        return false;
    }

    private static int fuelTicks(ItemStack stack) {
        if (stack.isEmpty() || !stack.getCraftingRemainingItem().isEmpty()) return 0;
        if (stack.is(ItemRegistry.ITEM_METALLURGICAL_COKE.get())) return 3200;
        return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    private int queuedFuelItems() {
        int count = 0;
        for (ItemStack stack : fuelQueue) count += stack.getCount();
        return count;
    }

    private int enqueueFuel(ItemStack offered) {
        int room = FUEL_QUEUE_LIMIT - queuedFuelItems();
        if (room <= 0 || fuelTicks(offered) <= 0) return 0;
        int accepted = Math.min(room, offered.getCount());
        if (accepted <= 0) return 0;

        ItemStack queued = offered.copy();
        queued.setCount(accepted);
        if (!fuelQueue.isEmpty()) {
            ItemStack last = fuelQueue.get(fuelQueue.size() - 1);
            if (ItemStack.isSameItemSameTags(last, queued)
                    && last.getCount() + accepted <= last.getMaxStackSize()) {
                last.grow(accepted);
                return accepted;
            }
        }
        fuelQueue.add(queued);
        return accepted;
    }

    private boolean startNextQueuedFuel(BoilerWarmupParameters.Rules rules) {
        if (state.burning() || fuelQueue.isEmpty()
                || state.heat() >= rules.heatCapacity()) return false;
        ItemStack next = fuelQueue.get(0);
        int ticks = fuelTicks(next);
        if (ticks <= 0 || !state.ignite(ticks, rules.heatPerBurnTick())) return false;
        next.shrink(1);
        if (next.isEmpty()) fuelQueue.remove(0);
        return true;
    }

    private int dropQueuedFuel(ServerLevel level) {
        int returned = 0;
        for (ItemStack stack : fuelQueue) {
            if (stack.isEmpty()) continue;
            returned += stack.getCount();
            Containers.dropItemStack(level,
                    owner.getBlockPos().getX() + 0.5,
                    owner.getBlockPos().getY() + 0.5,
                    owner.getBlockPos().getZ() + 0.5,
                    stack.copy());
        }
        fuelQueue.clear();
        return returned;
    }

    /** Server ticker only; no offline catch-up and no unbounded graph scans here. */
    public void tick() {
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
        String reason = (wasBurning || !fuelQueue.isEmpty()) ? interlock(level) : "";
        if (wasBurning && !reason.isEmpty() && !reason.equals("unloaded")) {
            state.extinguish();
            changed = true;
        }
        if (!state.burning() && !fuelQueue.isEmpty() && reason.isEmpty()
                && startNextQueuedFuel(rules)) changed = true;

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
                int accepted = transferSteamToNetwork(level, rules);
                if (accepted > 0) {
                    if (steam.extract(accepted) != accepted)
                        throw new IllegalStateException("Steam export ledger mismatch");
                    changed = true;
                }
            }
        }

        if (rules.reliefEnabled() || pipeConnected(level))
            changed |= updateSafety(level, rules);

        if (changed) owner.setChanged();
        if (wasBurning != state.burning()) owner.sync();

        if (spentFuel) {
            if (level.getGameTime() % 20L == 0L)
                level.sendParticles(ParticleTypes.SMOKE, outletAbove.getX() + 0.5,
                        outletAbove.getY() + 0.05, outletAbove.getZ() + 0.5,
                        1, 0.05, 0.05, 0.05, 0.01);
            if (level.getGameTime() % 4L == 0L) {
                BoilerFireboxBlockEntity front = BoilerFireboxInstallation.linkedFirebox(level, owner);
                if (front != null) {
                    Direction outward = front.assemblyFacing();
                    double x = front.getBlockPos().getX() + 0.5 + outward.getStepX() * 0.58;
                    double y = front.getBlockPos().getY() + 0.68;
                    double z = front.getBlockPos().getZ() + 0.5 + outward.getStepZ() * 0.58;
                    level.sendParticles(ParticleTypes.FLAME, x, y, z,
                            2, 0.16, 0.12, 0.16, 0.004);
                    if (level.getGameTime() % 12L == 0L)
                        level.sendParticles(ParticleTypes.SMALL_FLAME, x, y + 0.04, z,
                                1, 0.08, 0.08, 0.08, 0.002);
                }
            }
        }
    }

    public void useFirebox(BoilerFireboxBlockEntity front, Player player, InteractionHand hand) {
        ServerLevel level = server();
        if (level == null || hand != InteractionHand.MAIN_HAND) return;
        if (!player.getAbilities().mayBuild || !level.mayInteract(player, front.getBlockPos())
                || BoilerFireboxInstallation.linkedFirebox(level, owner) != front) return;

        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (player.isShiftKeyDown() && state.burning()) {
                state.extinguish();
                owner.setChanged();
                owner.sync();
            }
            status(player);
            return;
        }

        BoilerWarmupParameters.Rules rules = BoilerWarmupParameters.rules();
        if (rules == null) {
            tell(player, "Boiler rules are missing or invalid");
            return;
        }
        if (fuelTicks(held) <= 0) {
            tell(player, "Use solid furnace fuel; container fuels are not supported");
            return;
        }

        int accepted = enqueueFuel(held);
        if (accepted <= 0) {
            tell(player, "Firebox fuel queue is full ("
                    + queuedFuelItems() + "/" + FUEL_QUEUE_LIMIT + ")");
            return;
        }
        if (!player.getAbilities().instabuild) held.shrink(accepted);

        String reason = interlock(level);
        if (!state.burning() && reason.isEmpty() && startNextQueuedFuel(rules))
            owner.sync();
        owner.setChanged();

        if (!reason.isEmpty() && !reason.equals("unloaded"))
            tell(player, "Loaded " + accepted + " fuel item(s); ignition waiting: " + reason);
        else
            tell(player, "Loaded " + accepted + " fuel item(s)");
        status(player);
    }

    @Nullable public static IFluidHandler waterHandlerForInlet(
            Level world, BlockPos inletPos, BlockState inletState, BlockPos connectedPipe) {
        if (!inletState.is(BlockRegistry.BOILER_WATER_INLET.get())
                || !inletState.hasProperty(BoilerServiceFittingBlock.FACING)) return null;
        Direction facing = inletState.getValue(BoilerServiceFittingBlock.FACING);
        if (!BoilerServiceFittingBlock.waterPipePosition(inletPos, facing).equals(connectedPipe))
            return null;
        BlockPos delta = BoilerServiceFittingBlock.attachmentPosition(BlockPos.ZERO,
                BoilerServiceFittingBlock.Role.WATER, facing);
        BlockPos vesselPos = inletPos.subtract(delta);
        if (!world.hasChunkAt(vesselPos)
                || !(world.getBlockEntity(vesselPos) instanceof SteelBoilerVesselBlockEntity vessel)
                || !vessel.isFormed()
                || vessel.getBlockState().getValue(SteelBoilerVesselBlock.FACING) != facing)
            return null;
        return vessel.warmup();
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
        String boilerPressure = rules.vesselPressureEnabled()
                ? (Math.round(boilerPressureKPa(rules) * 10.0) / 10.0) + " kPa"
                : "legacy";
        tell(player, limits + "; fuel " + state.burnTicks() + " ticks + "
                + queuedFuelItems() + "/" + FUEL_QUEUE_LIMIT + " queued; Steam "
                + steam.stored() + "/" + rules.steamBufferCapacity()
                + " units; boiler " + boilerPressure + "; network " + network
                + (reliefOpen ? "; RELIEF OPEN" : "; relief closed"));
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
        if (!(owner.getLevel() instanceof ServerLevel level)) return;
        BoilerWarmupState.Snapshot before = state.spill();
        int steamSpilled = steam.spill();
        int queuedReturned = dropQueuedFuel(level);
        if (before.water() != 0 || before.heat() != 0
                || before.burnTicks() != 0 || steamSpilled != 0 || queuedReturned != 0) {
            LogManager.getLogger("Magneticraft2Boiler").info(
                    "Boiler dismantled at {}: spilled {} mB, dissipated {} HEAT, discarded {} fuel ticks, vented {} Steam units, returned {} queued fuel items",
                    owner.getBlockPos(), before.water(), before.heat(),
                    before.burnTicks(), steamSpilled, queuedReturned);
            owner.setChanged();
        }
    }

    public void stopFuel() {
        if (!(owner.getLevel() instanceof ServerLevel level)) return;
        boolean changed = false;
        if (state.burning()) {
            state.extinguish();
            changed = true;
        }
        if (!fuelQueue.isEmpty()) {
            dropQueuedFuel(level);
            changed = true;
        }
        if (changed) owner.setChanged();
    }

    public void save(CompoundTag root) {
        BoilerWarmupState.Snapshot s = state.snapshot();
        BoilerSteamState.Snapshot gas = steam.snapshot();
        CompoundTag n = new CompoundTag();
        n.putInt("Version", 5);
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
        n.putLong("SteamVented", gas.vented());
        n.putLong("SteamSpilled", gas.spilled());
        n.putBoolean("ReliefOpen", reliefOpen);
        n.putLong("ReliefVented", reliefVented);
        ListTag queued = new ListTag();
        for (ItemStack stack : fuelQueue) {
            CompoundTag item = new CompoundTag();
            stack.save(item);
            queued.add(item);
        }
        n.put("FuelQueue", queued);
        root.put(NBT, n);
    }

    public void load(CompoundTag root) {
        state = new BoilerWarmupState();
        steam = new BoilerSteamState();
        reliefOpen = false; reliefVented = 0;
        geometryFacing = null;
        lastTick = Long.MIN_VALUE;
        if (!root.contains(NBT, Tag.TAG_COMPOUND)) return;
        CompoundTag n = root.getCompound(NBT);
        try {
            int version = n.getInt("Version");
            if (version != 1 && version != 2 && version != 3 && version != 4 && version != 5)
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
            if (version >= 2)
                steam.restore(version >= 4
                        ? new BoilerSteamState.Snapshot(
                            n.getInt("SteamStored"), n.getLong("SteamProduced"),
                            n.getLong("SteamExported"), n.getLong("SteamVented"),
                            n.getLong("SteamSpilled"))
                        : new BoilerSteamState.Snapshot(
                            n.getInt("SteamStored"), n.getLong("SteamProduced"),
                            n.getLong("SteamExported"), n.getLong("SteamSpilled")));
            if (version >= 3) {
                reliefOpen = n.getBoolean("ReliefOpen");
                reliefVented = Math.max(0L, n.getLong("ReliefVented"));
            }
            fuelQueue.clear();
            if (version >= 5 && n.contains("FuelQueue", Tag.TAG_LIST)) {
                ListTag queued = n.getList("FuelQueue", Tag.TAG_COMPOUND);
                int total = 0;
                for (int i = 0; i < queued.size(); i++) {
                    ItemStack stack = ItemStack.of(queued.getCompound(i));
                    if (stack.isEmpty() || fuelTicks(stack) <= 0)
                        throw new IllegalArgumentException("Invalid queued fuel item");
                    total += stack.getCount();
                    if (total > FUEL_QUEUE_LIMIT)
                        throw new IllegalArgumentException("Fuel queue exceeds capacity");
                    fuelQueue.add(stack);
                }
            }
        } catch (IllegalArgumentException invalid) {
            state = new BoilerWarmupState();
            steam = new BoilerSteamState();
            fuelQueue.clear();
            LogManager.getLogger("Magneticraft2Boiler").error(
                    "Rejected invalid boiler save at {}: {}",
                    owner.getBlockPos(), invalid.getMessage());
        }
    }
}
