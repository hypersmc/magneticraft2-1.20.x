package com.magneticraft2.common.blockentity.stage.bronze;

import com.magneticraft2.common.registry.registers.ItemRegistry;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeHooks;

/**
 * Server-owned Foundry heat, fuel and bronze accounting.
 *
 * The crucible stores ingot-equivalents and an input ItemStack template
 * (including quality NBT), NOT Forge fluid/millibuckets. Its phase is derived
 * from the current heat: cooling solidifies the contents in place, and
 * reheating makes them pour-ready again. There is no drain API until the
 * Casting Basin/ladle can commit transfers atomically.
 */
public final class BronzeFoundryThermalCore {
    private static final int FUEL_QUEUE_LIMIT = 16;
    private final BronzeFoundryBlockEntity foundry;
    private ItemStack fuel = ItemStack.EMPTY;
    private ItemStack charge = ItemStack.EMPTY;
    private ItemStack crucibleTemplate = ItemStack.EMPTY;
    private int crucibleUnits;
    private int burnRemaining;
    private int burnTotal;

    public BronzeFoundryThermalCore(BronzeFoundryBlockEntity foundry) {
        this.foundry = foundry;
    }

    public ItemStack fuelDisplay() { return fuel; }
    public ItemStack chargeDisplay() { return charge; }
    public int crucibleUnits() { return crucibleUnits; }
    public int burnRemaining() { return burnRemaining; }

    public int crucibleCapacity() {
        return Magneticraft2ConfigCommon.GENERAL.FoundryCrucibleIngotCapacity.get();
    }

    public boolean isHot() {
        return foundry.getHeatStorage()
                >= Magneticraft2ConfigCommon.GENERAL.FoundryMeltThreshold.get();
    }

    public boolean isPourReady(int ingotUnits) {
        return foundry.isFormed() && isHot()
                && ingotUnits > 0 && crucibleUnits >= ingotUnits;
    }

    private boolean canMelt() {
        return !charge.isEmpty() && crucibleUnits < crucibleCapacity()
                && (crucibleUnits == 0
                || ItemStack.isSameItemSameTags(crucibleTemplate, charge));
    }

    /** All inventory mutations and HEAT updates occur on the logical server. */
    public void tick(Level world) {
        int capacity = foundry.heatHandler.getMaxHeatStored();
        int heat = Math.max(0, Math.min(capacity, foundry.heatHandler.getHeatStored()));
        int threshold = Magneticraft2ConfigCommon.GENERAL.FoundryMeltThreshold.get();
        int heatCost = Magneticraft2ConfigCommon.GENERAL.FoundryHeatCostPerIngot.get();
        boolean changed = false;
        boolean notifyImmediately = false;

        // Never consume a second fuel while idle, full, or misconfigured.
        if (burnRemaining <= 0 && heat < capacity && threshold <= capacity
                && canMelt() && !fuel.isEmpty()) {
            int duration = fuelTime(fuel);
            if (duration > 0) {
                ItemStack next = fuel.copy();
                next.shrink(1);
                fuel = next.isEmpty() ? ItemStack.EMPTY : next;
                burnRemaining = duration;
                burnTotal = duration;
                changed = true;
                notifyImmediately = true;
            }
        }

        if (burnRemaining > 0) {
            burnRemaining--;
            heat = (int) Math.min((long) capacity, (long) heat
                    + Magneticraft2ConfigCommon.GENERAL.FoundryHeatPerFuelTick.get());
            changed = true;
        }

        heat = Math.max(0, heat
                - Magneticraft2ConfigCommon.GENERAL.FoundryPassiveHeatLoss.get());
        if (heat != foundry.heatHandler.getHeatStored()) {
            foundry.heatHandler.setHeat(heat);
            changed = true;
        }

        // Heat threshold and per-ingot heat consumption replace a magic
        // "recipe timer". The solid charge shrinks exactly when its equivalent
        // appears in the crucible, preserving all original quality metadata.
        if (heat >= threshold && heat >= heatCost && canMelt()) {
            if (crucibleUnits == 0) {
                crucibleTemplate = charge.copy();
                crucibleTemplate.setCount(1);
            }
            ItemStack next = charge.copy();
            next.shrink(1);
            charge = next.isEmpty() ? ItemStack.EMPTY : next;
            crucibleUnits++;
            foundry.heatHandler.setHeat(heat - heatCost);
            changed = true;
            notifyImmediately = true;
        }

        if (changed) {
            foundry.setChanged();
            if (notifyImmediately || world.getGameTime() % 20L == 0L) {
                foundry.sync();
            }
        }
    }

    private static int fuelTime(ItemStack stack) {
        // The early Foundry has no return container. Exclude lava buckets
        // rather than silently deleting a bucket or generating free fuel.
        if (stack.isEmpty() || !stack.getCraftingRemainingItem().isEmpty()) return 0;
        return Math.max(0, ForgeHooks.getBurnTime(stack, RecipeType.SMELTING));
    }

    public void useFirebox(Player player, ItemStack held) {
        if (!held.isEmpty()) {
            boolean ok = fuelTime(held) > 0
                    && fuel.getCount() < FUEL_QUEUE_LIMIT
                    && (fuel.isEmpty() || ItemStack.isSameItemSameTags(fuel, held));
            if (ok) {
                if (fuel.isEmpty()) {
                    fuel = held.copy();
                    fuel.setCount(1);
                } else {
                    fuel.grow(1);
                }
                if (!player.getAbilities().instabuild) held.shrink(1);
                foundry.sync();
            }
            message(player, ok ? "fuel_added" : "fuel_rejected");
            return;
        }
        if (player.isShiftKeyDown() && !fuel.isEmpty()) {
            ItemStack out = fuel;
            fuel = ItemStack.EMPTY;
            deliver(player, out);
            foundry.sync();
            return;
        }
        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.bronze_foundry.firebox_status",
                fuel.getCount(), burnRemaining, foundry.getHeatStorage(),
                foundry.getMaxHeatStorage()), true);
    }

    public void useCrucible(Player player, ItemStack held) {
        if (!held.isEmpty()) {
            boolean ok = held.is(ItemRegistry.ITEM_BRONZE_INGOT.get())
                    && charge.getCount() < crucibleCapacity()
                    && (charge.isEmpty() || ItemStack.isSameItemSameTags(charge, held))
                    && (crucibleUnits == 0 || ItemStack.isSameItemSameTags(crucibleTemplate, held));
            if (ok) {
                if (charge.isEmpty()) {
                    charge = held.copy();
                    charge.setCount(1);
                } else {
                    charge.grow(1);
                }
                if (!player.getAbilities().instabuild) held.shrink(1);
                foundry.sync();
            }
            message(player, ok ? "charge_added" : "charge_rejected");
            return;
        }
        if (player.isShiftKeyDown() && !charge.isEmpty()) {
            ItemStack out = charge;
            charge = ItemStack.EMPTY;
            deliver(player, out);
            foundry.sync();
            return;
        }
        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.bronze_foundry.crucible_status",
                charge.getCount(), crucibleUnits, crucibleCapacity(),
                isHot() ? "hot" : "cold"), true);
    }

    public void useTap(Player player) {
        // Intentionally read-only until an actual ladle/basin accepts a batch.
        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.bronze_foundry.tap_status",
                crucibleUnits, crucibleCapacity(), isHot() ? "hot" : "cold"), true);
    }

    private static void message(Player player, String suffix) {
        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.bronze_foundry." + suffix), true);
    }

    private static void deliver(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false);
    }

    /**
     * One-off prototype salvage: quality-tagged crucible material returns as
     * ingot-equivalents until solidification and slag have proper outputs.
     * State clears BEFORE spawning drops to prevent double return on dismantle.
     */
    public void salvage(Level world) {
        if (world.isClientSide) return;
        ItemStack toDropFuel = fuel, toDropCharge = charge;
        ItemStack toDropCrucible = crucibleTemplate.isEmpty()
                ? ItemStack.EMPTY : crucibleTemplate.copy();
        if (!toDropCrucible.isEmpty()) toDropCrucible.setCount(crucibleUnits);
        fuel = ItemStack.EMPTY;
        charge = ItemStack.EMPTY;
        crucibleTemplate = ItemStack.EMPTY;
        crucibleUnits = 0;
        burnRemaining = 0;
        burnTotal = 0;
        if (!toDropFuel.isEmpty()) Block.popResource(world, foundry.getBlockPos(), toDropFuel);
        if (!toDropCharge.isEmpty()) Block.popResource(world, foundry.getBlockPos(), toDropCharge);
        if (!toDropCrucible.isEmpty()) Block.popResource(world, foundry.getBlockPos(), toDropCrucible);
        foundry.setChanged();
    }

    public void load(CompoundTag nbt) {
        fuel = ItemStack.of(nbt.getCompound("Fuel"));
        charge = ItemStack.of(nbt.getCompound("Charge"));
        crucibleTemplate = ItemStack.of(nbt.getCompound("CrucibleTemplate"));
        crucibleUnits = Math.max(0, Math.min(64, nbt.getInt("CrucibleUnits")));
        if (crucibleTemplate.isEmpty()) crucibleUnits = 0;
        if (crucibleUnits == 0) crucibleTemplate = ItemStack.EMPTY;
        burnTotal = Math.max(0, nbt.getInt("BurnTotal"));
        burnRemaining = Math.max(0, Math.min(burnTotal, nbt.getInt("BurnRemaining")));
    }

    public void save(CompoundTag nbt) {
        if (!fuel.isEmpty()) nbt.put("Fuel", fuel.save(new CompoundTag()));
        if (!charge.isEmpty()) nbt.put("Charge", charge.save(new CompoundTag()));
        if (!crucibleTemplate.isEmpty()) nbt.put("CrucibleTemplate",
                crucibleTemplate.save(new CompoundTag()));
        nbt.putInt("CrucibleUnits", crucibleUnits);
        nbt.putInt("BurnRemaining", burnRemaining);
        nbt.putInt("BurnTotal", burnTotal);
    }
}
