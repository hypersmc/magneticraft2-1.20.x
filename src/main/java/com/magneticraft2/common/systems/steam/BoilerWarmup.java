package com.magneticraft2.common.systems.steam;

import com.magneticraft2.common.block.stage.steam.BoilerServiceFittingBlock;
import com.magneticraft2.common.block.stage.steel.SteelBoilerVesselBlock;
import com.magneticraft2.common.blockentity.stage.steam.BoilerFireboxBlockEntity;
import com.magneticraft2.common.blockentity.stage.steel.SteelBoilerVesselBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
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
 * One authoritative warm-up owner per vessel. Manual water containers enter
 * through the real inlet; fuel enters through the linked firebox. No Steam,
 * pressure, hot-fluid output, energy capability or automatic fuel queue.
 */
public final class BoilerWarmup implements IFluidHandler {
    private static final String NBT = "BoilerWarmup";
    private final SteelBoilerVesselBlockEntity owner;
    private BoilerWarmupState state = new BoilerWarmupState();
    @Nullable private Direction geometryFacing;
    private BlockPos inlet = BlockPos.ZERO, outletAbove = BlockPos.ZERO;
    private BlockPos[] stack = new BlockPos[0], requiredChunks = new BlockPos[0];
    private long lastTick = Long.MIN_VALUE;

    public BoilerWarmup(SteelBoilerVesselBlockEntity owner) { this.owner = owner; }

    @Nullable private ServerLevel server() {
        if (owner.isRemoved() || !owner.isFormed() || !(owner.getLevel() instanceof ServerLevel level)
                || !level.hasChunkAt(owner.getBlockPos())
                || level.getBlockEntity(owner.getBlockPos()) != owner) return null;
        return level;
    }
    private Direction facing() { return owner.getBlockState().getValue(SteelBoilerVesselBlock.FACING); }

    private void geometry() {
        Direction direction = facing();
        if (direction == geometryFacing) return;
        geometryFacing = direction;
        BlockPos p = owner.getBlockPos();
        inlet = BoilerServiceFittingBlock.attachmentPosition(p, BoilerServiceFittingBlock.Role.WATER, direction);
        stack = new BlockPos[BoilerFireboxPlan.STACK_HEIGHT];
        for (int i = 0; i < stack.length; i++) stack[i] = BoilerFireboxLayout.stack(p, direction, i);
        outletAbove = stack[stack.length - 1].above();
        // Complete vessel/extension envelope: cache its at-most-four X/Z chunks.
        BlockPos a = p.offset(BoilerFireboxLayout.offset(new BoilerFireboxPlan.Cell(-1, 0, -4), direction));
        BlockPos b = p.offset(BoilerFireboxLayout.offset(new BoilerFireboxPlan.Cell(2, 0, 2), direction));
        List<BlockPos> chunks = new ArrayList<>();
        for (int x = Math.min(a.getX(), b.getX()) >> 4; x <= (Math.max(a.getX(), b.getX()) >> 4); x++)
            for (int z = Math.min(a.getZ(), b.getZ()) >> 4; z <= (Math.max(a.getZ(), b.getZ()) >> 4); z++)
                chunks.add(new BlockPos(x << 4, 0, z << 4));
        requiredChunks = chunks.toArray(BlockPos[]::new);
    }

    /** The sole pausing case is an unloaded dependency, never a forced chunk load. */
    private String interlock(ServerLevel level) {
        geometry();
        for (BlockPos pos : requiredChunks) if (!level.hasChunkAt(pos)) return "unloaded";
        BoilerFireboxBlockEntity front = BoilerFireboxInstallation.linkedFirebox(level, owner);
        if (front == null) return "Form and link the firebox first";
        BlockState waterPort = level.getBlockState(inlet);
        if (!waterPort.is(BlockRegistry.BOILER_WATER_INLET.get())
                || waterPort.getValue(BoilerServiceFittingBlock.FACING) != facing()) return "Install the water inlet";
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

    /** Server ticker only; no offline catch-up and no per-frame geometry or graph scans. */
    public void tick() {
        if (!state.burning() && state.heat() == 0) return; // dormant vessels do no world lookups
        ServerLevel level = server();
        if (level == null || lastTick == level.getGameTime()) return;
        lastTick = level.getGameTime();
        if (!state.burning() && state.heat() == 0) return;
        BoilerWarmupParameters.Rules rules = BoilerWarmupParameters.rules();
        if (rules == null) {
            if (state.burning()) { state.extinguish(); owner.setChanged(); }
            return; // preserve water/heat if a pack reload has invalid settings
        }
        boolean wasBurning = state.burning();
        String reason = wasBurning ? interlock(level) : "";
        if (wasBurning && !reason.isEmpty() && !reason.equals("unloaded")) state.extinguish();
        boolean spentFuel = state.tick(reason.isEmpty(), rules.heatCapacity(), rules.passiveLoss());
        owner.setChanged();
        if (wasBurning != state.burning()) owner.sync();
        // Real fuel, valid flue and loaded server only: never decorative idle smoke.
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
            status(player); return;
        }
        BoilerWarmupParameters.Rules rules = BoilerWarmupParameters.rules();
        if (rules == null) { tell(player, "Boiler warm-up rules are missing or invalid"); return; }
        String reason = interlock(level);
        if (!reason.isEmpty()) { tell(player, reason.equals("unloaded") ? "Load the complete boiler and firebox first" : reason); return; }
        if (state.burning()) { tell(player, "Fuel is already burning; no extra item consumed"); status(player); return; }
        if (state.heat() >= rules.heatCapacity()) { tell(player, "Warm-up heat buffer is full; no fuel consumed"); return; }
        int ticks = held.getCraftingRemainingItem().isEmpty()
                ? (held.is(ItemRegistry.ITEM_METALLURGICAL_COKE.get()) ? 3200
                    : ForgeHooks.getBurnTime(held, RecipeType.SMELTING)) : 0;
        if (!state.ignite(ticks, rules.heatPerBurnTick())) {
            tell(player, "Use solid furnace fuel; container fuels are not supported"); return;
        }
        if (!player.getAbilities().instabuild) held.shrink(1);
        owner.setChanged(); owner.sync(); status(player);
    }

    /** Called only by the actually clicked water-inlet block, not any arbitrary filler. */
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
                || !vessel.isFormed() || vessel.getBlockState().getValue(SteelBoilerVesselBlock.FACING) != facing)
            return InteractionResult.FAIL;
        if (!world.mayInteract(player, vesselPos)) return InteractionResult.FAIL;
        BoilerWarmup warmup = vessel.warmup();
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && !FluidUtil.interactWithFluidHandler(player, hand, warmup))
            tell(player, "Use a water container or empty container; the vessel may be full or empty");
        else warmup.status(player);
        return InteractionResult.CONSUME; // failed water/lava buckets must not place a world fluid
    }

    public void status(Player player) {
        BoilerWarmupParameters.Rules rules = BoilerWarmupParameters.rules();
        String limits = rules == null ? "rules disabled" : state.water() + "/" + rules.waterCapacity()
                + " mB water; " + state.heat() + "/" + rules.heatCapacity() + " HEAT";
        tell(player, limits + "; fuel " + state.burnTicks() + " ticks; Steam/pressure disabled");
    }
    private static void tell(Player player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }

    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int tank) {
        return tank == 0 && state.water() > 0 ? new FluidStack(Fluids.WATER, state.water()) : FluidStack.EMPTY;
    }
    @Override public int getTankCapacity(int tank) {
        BoilerWarmupParameters.Rules rules = BoilerWarmupParameters.rules();
        return tank == 0 ? Math.max(state.water(), rules == null ? 0 : rules.waterCapacity()) : 0;
    }
    @Override public boolean isFluidValid(int tank, FluidStack fluid) {
        return tank == 0 && !fluid.isEmpty() && fluid.getFluid() == Fluids.WATER && !fluid.hasTag();
    }
    @Override public int fill(FluidStack resource, FluidAction action) {
        BoilerWarmupParameters.Rules rules = BoilerWarmupParameters.rules();
        if (server() == null || rules == null || !isFluidValid(0, resource)) return 0;
        int accepted = state.fill(resource.getAmount(), rules.waterCapacity(), action.simulate());
        if (accepted > 0 && action.execute()) { owner.setChanged(); owner.sync(); }
        return accepted;
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        return isFluidValid(0, resource) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
    }
    @Override public FluidStack drain(int requested, FluidAction action) {
        if (server() == null) return FluidStack.EMPTY;
        int amount = state.drain(requested, action.simulate());
        if (amount > 0 && action.execute()) { owner.setChanged(); owner.sync(); }
        return amount > 0 ? new FluidStack(Fluids.WATER, amount) : FluidStack.EMPTY;
    }

    /** Dismantling returns structure items, never free buckets or burning fuel. */
    public void discardContents() {
        if (!(owner.getLevel() instanceof ServerLevel)) return;
        BoilerWarmupState.Snapshot before = state.spill();
        if (before.water() != 0 || before.heat() != 0 || before.burnTicks() != 0) {
            LogManager.getLogger("Magneticraft2Boiler").info(
                    "Boiler dismantled at {}: spilled {} mB, dissipated {} HEAT, discarded {} fuel ticks",
                    owner.getBlockPos(), before.water(), before.heat(), before.burnTicks());
            owner.setChanged();
        }
    }
    public void stopFuel() {
        if (!(owner.getLevel() instanceof ServerLevel)) return;
        if (state.burning()) { state.extinguish(); owner.setChanged(); }
    }

    public void save(CompoundTag root) {
        BoilerWarmupState.Snapshot s = state.snapshot();
        CompoundTag n = new CompoundTag(); n.putInt("Version", 1);
        n.putInt("Water", s.water()); n.putInt("Heat", s.heat());
        n.putInt("BurnTicks", s.burnTicks()); n.putInt("HeatPerBurnTick", s.heatPerBurnTick());
        n.putLong("WaterIn", s.waterIn()); n.putLong("WaterOut", s.waterOut()); n.putLong("WaterSpilled", s.waterSpilled());
        n.putLong("FuelEnergy", s.fuelEnergy()); n.putLong("GeneratedHeat", s.generatedHeat());
        n.putLong("HeatLost", s.heatLost()); n.putLong("HeatInDrainedWater", s.heatInDrainedWater());
        n.putLong("HeatSpilled", s.heatSpilled()); n.putLong("FuelDiscarded", s.fuelDiscarded());
        root.put(NBT, n);
    }
    public void load(CompoundTag root) {
        state = new BoilerWarmupState(); geometryFacing = null; lastTick = Long.MIN_VALUE;
        if (!root.contains(NBT, Tag.TAG_COMPOUND)) return; // old vessels start cold, empty and unlit
        CompoundTag n = root.getCompound(NBT);
        try {
            if (n.getInt("Version") != 1) throw new IllegalArgumentException("Unsupported warm-up version");
            state.restore(new BoilerWarmupState.Snapshot(n.getInt("Water"), n.getInt("Heat"),
                    n.getInt("BurnTicks"), n.getInt("HeatPerBurnTick"), n.getLong("WaterIn"),
                    n.getLong("WaterOut"), n.getLong("WaterSpilled"), n.getLong("FuelEnergy"),
                    n.getLong("GeneratedHeat"), n.getLong("HeatLost"), n.getLong("HeatInDrainedWater"),
                    n.getLong("HeatSpilled"), n.getLong("FuelDiscarded")));
        } catch (IllegalArgumentException invalid) {
            LogManager.getLogger("Magneticraft2Boiler").error(
                    "Rejected invalid boiler warm-up save at {}: {}", owner.getBlockPos(), invalid.getMessage());
        }
    }
}
