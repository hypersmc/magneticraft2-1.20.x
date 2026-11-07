package com.magneticraft2.common.systems.material;

import com.magneticraft2.common.recipe.multiblock.MultiblockProcessingRecipe;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Server-authoritative forging heat for existing Iron Blooms and Wrought
 * Iron Ingots. Both keep their original registry IDs. The Refractory Kiln
 * grants temporary heat only through identified, fueled processing recipes;
 * the Forge Hammer checks expiry at every supplied processing tick.
 * No fake hot item IDs, client clocks or extra item entities.
 *
 * Historical class name retained for the accepted Bronze Bloom route.
 */
public final class HotBloomWorkpiece {
    public static final String HOT_UNTIL_TAG = "magneticraft2:hot_until";
    public static final int HOT_WINDOW_TICKS = 2400; // provisional: 120 s
    private static final ResourceLocation REHEAT_RECIPE =
            new ResourceLocation("magneticraft2", "reheat_iron_bloom");
    private static final ResourceLocation REHEAT_WROUGHT_INGOT_RECIPE =
            new ResourceLocation("magneticraft2", "reheat_wrought_iron_ingot");

    private HotBloomWorkpiece() {}

    public static boolean isReheatRecipe(MultiblockProcessingRecipe recipe) {
        // An arbitrary same-item kiln recipe must not grant free heat.
        return (REHEAT_RECIPE.equals(recipe.getId())
                    && recipe.getOutput().is(ItemRegistry.ITEM_IRON_BLOOM.get()))
                || (REHEAT_WROUGHT_INGOT_RECIPE.equals(recipe.getId())
                    && recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_INGOT.get()));
    }

    public static boolean requiresHeat(MultiblockProcessingRecipe recipe) {
        // The existing Bloom consolidation remains hot-only. Iron 0B adds
        // plate forging; other shaped materials join only when implemented.
        return recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_INGOT.get())
                || recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_PLATE.get());
    }

    public static boolean isHot(ItemStack stack, Level world) {
        if (stack.isEmpty() || (!stack.is(ItemRegistry.ITEM_IRON_BLOOM.get())
                && !stack.is(ItemRegistry.ITEM_WROUGHT_IRON_INGOT.get()))) return false;
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

    /** Keep material quality NBT, but remove temporary heat from finished metal. */
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
