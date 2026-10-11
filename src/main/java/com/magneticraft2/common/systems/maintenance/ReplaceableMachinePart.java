package com.magneticraft2.common.systems.maintenance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * A single physical, replaceable part on a machine.
 *
 * Wear advances only when a recipe batch successfully completes. Maintenance
 * data belongs to the block entity and is persisted separately from its
 * normal input/output slots so automation cannot extract the installed part.
 *
 * One instance per component supports multiple distinct parts per machine in
 * later ages. Nothing here assumes a particular material quality system.
 */
public final class ReplaceableMachinePart {
    private static final String REMAINING = "RemainingCycles";

    private final Supplier<? extends Item> replacement;
    private final int maximumCycles;
    private int remainingCycles;

    public ReplaceableMachinePart(Supplier<? extends Item> replacement,
                                  int maximumCycles) {
        this.replacement = replacement;
        this.maximumCycles = Math.max(1, maximumCycles);
        this.remainingCycles = this.maximumCycles;
    }

    public int getRemainingCycles() {
        return remainingCycles;
    }

    public int getMaximumCycles() {
        return maximumCycles;
    }

    public boolean canOperate() {
        return remainingCycles > 0;
    }

    public boolean needsAttention() {
        return remainingCycles <= Math.max(1, maximumCycles / 10);
    }

    /**
     * Six stable visual appearances, 0 = pristine and 5 = broken.
     * Derived from persisted life so there is no extra client state to sync.
     * Long arithmetic prevents overflow with large configurable lifetimes.
     */
    public int getVisualWearStage() {
        if (remainingCycles <= 0) {
            return 5;
        }
        long current = 100L * remainingCycles;
        long maximum = maximumCycles;
        if (current >= 75L * maximum) {
            return 0;
        }
        if (current >= 50L * maximum) {
            return 1;
        }
        if (current >= 25L * maximum) {
            return 2;
        }
        if (current >= 10L * maximum) {
            return 3;
        }
        return 4;
    }

    /**
     * @return true if the part crossed the warning threshold or broke
     */
    public boolean completeBatch() {
        if (remainingCycles == 0) {
            return false;
        }

        boolean wasWarning = needsAttention();
        remainingCycles--;
        return wasWarning != needsAttention() || remainingCycles == 0;
    }

    /**
     * Sneak-right-click with an empty hand to inspect a part; right-click
     * normally while holding its replacement item to service the machine.
     * Nothing is extracted from processing item input/output slots.
     */
    public boolean handleInteraction(Player player, InteractionHand hand,
                                     Runnable sync) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (!player.isShiftKeyDown()) {
                return false;
            }
            showStatus(player);
            return true;
        }

        // Sneaking with held items bypasses ordinary block interaction.
        if (player.isShiftKeyDown() || !held.is(replacement.get())) {
            return false;
        }

        Component partName = new ItemStack(replacement.get()).getHoverName();
        if (remainingCycles == maximumCycles) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.magneticraft2.machine_part_fresh", partName),
                    true);
            return true;
        }

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        remainingCycles = maximumCycles;
        sync.run();
        player.displayClientMessage(
                Component.translatable(
                        "message.magneticraft2.machine_part_installed", partName),
                true);
        return true;
    }

    private void showStatus(Player player) {
        Component partName = new ItemStack(replacement.get()).getHoverName();
        String key = canOperate()
                ? "message.magneticraft2.machine_part_status"
                : "message.magneticraft2.machine_part_broken";
        Component message = canOperate()
                ? Component.translatable(key, partName, remainingCycles, maximumCycles)
                : Component.translatable(key, partName);
        player.displayClientMessage(message, true);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(REMAINING, remainingCycles);
        return tag;
    }

    /**
     * Legacy machines (missing Maintenance tag) start with their original
     * installed parts intact rather than requiring an immediate repair.
     */
    public void load(CompoundTag tag) {
        remainingCycles = tag.contains(REMAINING, Tag.TAG_INT)
                ? Math.max(0, Math.min(maximumCycles, tag.getInt(REMAINING)))
                : maximumCycles;
    }
}
