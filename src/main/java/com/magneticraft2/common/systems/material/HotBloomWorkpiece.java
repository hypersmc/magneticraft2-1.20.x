package com.magneticraft2.common.systems.material;

import com.magneticraft2.common.recipe.multiblock.MultiblockProcessingRecipe;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Server-authoritative forging heat for existing Iron Blooms, Wrought
 * Iron Ingots/Bars and small Steel stock. All retain their registered IDs.
 * The Kiln or Forge Hearth grants temporary heat only through identified,
 * fueled processing recipes with the appropriate existing HEAT budget;
 * the Forge Hammer and Rolling Mill check expiry at every supplied processing tick.
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
    private static final ResourceLocation REHEAT_WROUGHT_BAR_RECIPE =
            new ResourceLocation("magneticraft2", "reheat_wrought_iron_bar");
    private static final ResourceLocation HEARTH_INGOTS =
            new ResourceLocation("magneticraft2", "hearth_reheat_wrought_iron_ingots");
    private static final ResourceLocation HEARTH_BARS =
            new ResourceLocation("magneticraft2", "hearth_reheat_wrought_iron_bars");
    private static final ResourceLocation FORGE_HEARTH_MACHINE =
            new ResourceLocation("magneticraft2", "iron_forge_hearth");
    private static final ResourceLocation HEARTH_STEEL_INGOTS =
            new ResourceLocation("magneticraft2", "hearth_reheat_refined_steel_ingots");
    private static final ResourceLocation HEARTH_STEEL_BARS =
            new ResourceLocation("magneticraft2", "hearth_reheat_refined_steel_bars");

    /** Steel requires a higher Hearth HEAT threshold, not an unmodified Kiln. */
    public static boolean isSteelReheatRecipe(MultiblockProcessingRecipe recipe) {
        if (recipe == null || !(HEARTH_STEEL_INGOTS.equals(recipe.getId())
                || HEARTH_STEEL_BARS.equals(recipe.getId()))
                || !recipe.isForMachine(FORGE_HEARTH_MACHINE)
                || recipe.getInputCount() != 4 || recipe.getOutput().getCount() != 4)
            return false;
        return (HEARTH_STEEL_INGOTS.equals(recipe.getId())
                && recipe.getOutput().is(ItemRegistry.ITEM_REFINED_STEEL_INGOT.get()))
                || (HEARTH_STEEL_BARS.equals(recipe.getId())
                && recipe.getOutput().is(ItemRegistry.ITEM_REFINED_STEEL_BAR.get()));
    }

    public static boolean requiresSteelDie(MultiblockProcessingRecipe recipe) {
        return recipe != null && recipe.getOutput().is(ItemRegistry.ITEM_REFINED_STEEL_PLATE.get());
    }

    public static boolean requiresSteelRollers(MultiblockProcessingRecipe recipe) {
        return recipe != null && (recipe.getOutput().is(ItemRegistry.ITEM_REFINED_STEEL_BAR.get())
                || recipe.getOutput().is(ItemRegistry.ITEM_REFINED_STEEL_ROD.get()));
    }

    private HotBloomWorkpiece() {}

    public static boolean isReheatRecipe(MultiblockProcessingRecipe recipe) {
        // An arbitrary same-item kiln recipe must not grant free heat.
        return (REHEAT_RECIPE.equals(recipe.getId())
                    && recipe.getOutput().is(ItemRegistry.ITEM_IRON_BLOOM.get()))
                || (REHEAT_WROUGHT_INGOT_RECIPE.equals(recipe.getId())
                    && recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_INGOT.get()))
                || (REHEAT_WROUGHT_BAR_RECIPE.equals(recipe.getId())
                    && recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_BAR.get()))
                || (HEARTH_INGOTS.equals(recipe.getId())
                    && recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_INGOT.get())
                    && recipe.getInputCount() == 4 && recipe.getOutput().getCount() == 4)
                || (HEARTH_BARS.equals(recipe.getId())
                    && recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_BAR.get())
                    && recipe.getInputCount() == 4 && recipe.getOutput().getCount() == 4)
                || isSteelReheatRecipe(recipe);
    }

    public static boolean requiresHeat(MultiblockProcessingRecipe recipe) {
        // The existing Bloom consolidation remains hot-only. Iron 0B adds
        // plate forging; other shaped materials join only when implemented.
        return recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_INGOT.get())
                || recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_PLATE.get())
                || recipe.getOutput().is(ItemRegistry.ITEM_IRON_GEAR_BLANK_MEDIUM.get())
                || requiresSteelDie(recipe);
    }

    /**
     * Iron rolling uses the same finite hot-stock contract. Copper rolling
     * remains cold and must never acquire an artificial heat requirement.
     */
    public static boolean requiresRollingHeat(MultiblockProcessingRecipe recipe) {
        return recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_BAR.get())
                || recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_ROD.get())
                || requiresSteelRollers(recipe);
    }

    /**
     * Quality transfer and an expiring heat requirement are separate.
     * Cold-finished wrought sheets keep plate quality without requiring
     * an unavailable reheat-plate machine. Copper output remains unchanged.
     */
    public static boolean preservesRollingQuality(MultiblockProcessingRecipe recipe) {
        return requiresRollingHeat(recipe)
                || recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_SHEET.get());
    }

    public static boolean isHot(ItemStack stack, Level world) {
        if (stack.isEmpty() || (!stack.is(ItemRegistry.ITEM_IRON_BLOOM.get())
                && !stack.is(ItemRegistry.ITEM_WROUGHT_IRON_INGOT.get())
                && !stack.is(ItemRegistry.ITEM_WROUGHT_IRON_BAR.get())
                && !stack.is(ItemRegistry.ITEM_REFINED_STEEL_INGOT.get())
                && !stack.is(ItemRegistry.ITEM_REFINED_STEEL_BAR.get()))) return false;
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
