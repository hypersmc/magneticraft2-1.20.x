package com.magneticraft2.common.systems.material;

import com.magneticraft2.common.recipe.multiblock.MultiblockProcessingRecipe;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * One persistent, server-authoritative forging-heat value on the existing
 * iron_bloom item. No new "hot bloom" item ID, client tick, inventory entity
 * or magical always-hot intermediate. Game time survives chunk/server reload.
 *
 * Only the fuel-fired Refractory Kiln can grant forging heat through the
 * dedicated datapack recipe. The Forge Hammer must consume a still-hot
 * bloom under actual mechanical load; expired blooms can be re-fired.
 */
public final class HotBloomWorkpiece {
    public static final String HOT_UNTIL_TAG = "magneticraft2:hot_until";
    public static final int HOT_WINDOW_TICKS = 2400; // provisional: 120 s
    private static final ResourceLocation REHEAT_RECIPE =
            new ResourceLocation("magneticraft2", "reheat_iron_bloom");

    private HotBloomWorkpiece() {}

    public static boolean isReheatRecipe(MultiblockProcessingRecipe recipe) {
        return REHEAT_RECIPE.equals(recipe.getId())
                && recipe.getOutput().is(ItemRegistry.ITEM_IRON_BLOOM.get());
    }

    public static boolean requiresHeat(MultiblockProcessingRecipe recipe) {
        return recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_INGOT.get());
    }

    public static boolean isHot(ItemStack stack, Level world) {
        if (stack.isEmpty() || !stack.is(ItemRegistry.ITEM_IRON_BLOOM.get())) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(HOT_UNTIL_TAG, Tag.TAG_LONG)
                && tag.getLong(HOT_UNTIL_TAG) > world.getGameTime();
    }

    public static long remainingTicks(ItemStack stack, Level world) {
        if (!isHot(stack, world)) return 0L;
        return stack.getTag().getLong(HOT_UNTIL_TAG) - world.getGameTime();
    }

    /** Same material and quality NBT, but with a new, expiring heat state. */
    public static ItemStack heatInKiln(ItemStack input, ItemStack recipeOutput, Level world) {
        ItemStack result = recipeOutput.copy();
        if (input.hasTag()) result.setTag(input.getTag().copy());
        result.getOrCreateTag().putLong(HOT_UNTIL_TAG,
                world.getGameTime() + HOT_WINDOW_TICKS);
        return result;
    }

    /** Do not carry the temporary forging heat into finished wrought metal. */
    public static ItemStack consolidate(ItemStack hotBloom, ItemStack recipeOutput) {
        ItemStack result = recipeOutput.copy();
        if (hotBloom.hasTag()) {
            CompoundTag metalData = hotBloom.getTag().copy();
            metalData.remove(HOT_UNTIL_TAG);
            if (!metalData.isEmpty()) result.setTag(metalData);
        }
        return result;
    }
}
