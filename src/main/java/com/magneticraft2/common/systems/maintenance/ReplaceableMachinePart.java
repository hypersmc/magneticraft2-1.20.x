package com.magneticraft2.common.systems.maintenance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Server-owned life of a replaceable machine component.
 *
 * All legacy machines continue using the original, single-material
 * constructor. Only machines explicitly declaring an alternative can install
 * a different part. The active part is tracked by its registered item ID,
 * never by an unstable alternative-list index in saved data.
 */
public final class ReplaceableMachinePart {
    private static final String REMAINING = "RemainingCycles";
    private static final String INSTALLED = "InstalledPart";

    public record Alternative(Supplier<? extends Item> item, int maximumCycles) {
        public Alternative {
            Objects.requireNonNull(item, "alternative replacement");
            if (maximumCycles < 1) {
                throw new IllegalArgumentException("Maximum service cycles must be positive");
            }
        }
    }

    private final Supplier<? extends Item> replacement;
    private final int maximumCycles;
    private final List<Alternative> alternatives;
    private int installedVariant;
    private int remainingCycles;

    /** Backward-compatible single-part maintenance contract. */
    public ReplaceableMachinePart(Supplier<? extends Item> replacement,
                                  int maximumCycles) {
        this(replacement, maximumCycles, new Alternative[0]);
    }

    /** Optional material tiers, registered only on the machine that needs them. */
    public ReplaceableMachinePart(Supplier<? extends Item> replacement,
                                  int maximumCycles, Alternative... alternatives) {
        this.replacement = Objects.requireNonNull(replacement, "replacement");
        // Original maintenance contract clamps misconfigured legacy lifetimes.
        // Do not change that behavior for every existing machine.
        this.maximumCycles = Math.max(1, maximumCycles);
        this.alternatives = List.of(alternatives);
        this.installedVariant = 0;
        this.remainingCycles = maximumCycles;
    }

    private Item itemAt(int variant) {
        return variant == 0 ? replacement.get() : alternatives.get(variant - 1).item().get();
    }

    private int lifetimeAt(int variant) {
        return variant == 0 ? maximumCycles : alternatives.get(variant - 1).maximumCycles();
    }

    public int getRemainingCycles() {
        return remainingCycles;
    }

    /** Lifetime of the actual installed part, not always the bronze baseline. */
    public int getMaximumCycles() {
        return lifetimeAt(installedVariant);
    }
    /** Matches the actual installed registry item, not a disposable tier index. */
    public boolean isInstalled(Item item) {
        return item != null && itemAt(installedVariant) == item;
    }

    public boolean canOperate() {
        return remainingCycles > 0;
    }

    public boolean needsAttention() {
        return remainingCycles <= Math.max(1, getMaximumCycles() / 10);
    }

    /**
     * Six stable wear appearances: 0 pristine and 5 broken.
     * Derived from persisted life, without client-local counters.
     */
    public int getVisualWearStage() {
        if (remainingCycles <= 0) return 5;
        long current = 100L * remainingCycles;
        long maximum = getMaximumCycles();
        if (current >= 75L * maximum) return 0;
        if (current >= 50L * maximum) return 1;
        if (current >= 25L * maximum) return 2;
        if (current >= 10L * maximum) return 3;
        return 4;
    }

    /** Consumes service life only when the server completes a batch. */
    public boolean completeBatch() {
        if (remainingCycles == 0) return false;
        boolean wasWarning = needsAttention();
        remainingCycles--;
        return wasWarning != needsAttention() || remainingCycles == 0;
    }

    private int matchingVariant(ItemStack held) {
        if (held.is(replacement.get())) return 0;
        for (int i = 0; i < alternatives.size(); i++) {
            if (held.is(alternatives.get(i).item().get())) return i + 1;
        }
        return -1;
    }

    /**
     * Sneak-empty-hand inspects the installed material and remaining life.
     * Right-click with a supported part to service/upgrade (one consumed).
     * Swapping a fresh part for a different tier deliberately uses the new
     * part without refunding the old one; repeat-service with the same
     * already-fresh part consumes nothing.
     */
    public boolean handleInteraction(Player player, InteractionHand hand,
                                     Runnable sync) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (!player.isShiftKeyDown()) return false;
            showStatus(player);
            return true;
        }

        if (player.isShiftKeyDown()) return false;
        int requested = matchingVariant(held);
        if (requested < 0) return false;

        Component partName = new ItemStack(itemAt(requested)).getHoverName();
        if (requested == installedVariant && remainingCycles == getMaximumCycles()) {
            player.displayClientMessage(
                    Component.translatable("message.magneticraft2.machine_part_fresh",
                            partName), true);
            return true;
        }

        if (!player.getAbilities().instabuild) held.shrink(1);
        installedVariant = requested;
        remainingCycles = getMaximumCycles();
        sync.run();
        player.displayClientMessage(
                Component.translatable("message.magneticraft2.machine_part_installed",
                        partName), true);
        return true;
    }

    private void showStatus(Player player) {
        Component partName = new ItemStack(itemAt(installedVariant)).getHoverName();
        String key = canOperate()
                ? "message.magneticraft2.machine_part_status"
                : "message.magneticraft2.machine_part_broken";
        Component message = canOperate()
                ? Component.translatable(key, partName, remainingCycles, getMaximumCycles())
                : Component.translatable(key, partName);
        player.displayClientMessage(message, true);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(REMAINING, remainingCycles);
        // Preserve the exact legacy NBT shape for machines with no alternatives
        // and the Forge Hammer when the original Bronze die is installed.
        if (installedVariant != 0) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(itemAt(installedVariant));
            if (id != null) tag.putString(INSTALLED, id.toString());
        }
        return tag;
    }

    /**
     * Old worlds have no InstalledPart and therefore retain their original
     * bronze-part wear count exactly. Unknown/removed alternatives safely
     * fall back to the original material, clamping to its legitimate life.
     */
    public void load(CompoundTag tag) {
        installedVariant = 0;
        if (tag.contains(INSTALLED, Tag.TAG_STRING)) {
            ResourceLocation stored = ResourceLocation.tryParse(tag.getString(INSTALLED));
            if (stored != null) {
                for (int i = 0; i < alternatives.size(); i++) {
                    if (stored.equals(ForgeRegistries.ITEMS.getKey(itemAt(i + 1)))) {
                        installedVariant = i + 1;
                        break;
                    }
                }
            }
        }
        remainingCycles = tag.contains(REMAINING, Tag.TAG_INT)
                ? Math.max(0, Math.min(getMaximumCycles(), tag.getInt(REMAINING)))
                : getMaximumCycles();
    }
}
