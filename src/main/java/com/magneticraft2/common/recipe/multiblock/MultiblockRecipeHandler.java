package com.magneticraft2.common.recipe.multiblock;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Central lookup entry point for JSON multiblock recipes.
 *
 * RecipeManager#getRecipeFor cannot distinguish two multiblock machines that
 * happen to accept the same ingredient because all generic recipes share one
 * RecipeType. This handler performs the machine-id filter first and is therefore
 * the API controllers should use.
 */
public final class MultiblockRecipeHandler {

    private MultiblockRecipeHandler() {
    }

    @Nullable
    public static MultiblockProcessingRecipe findRecipe(
            @Nullable Level level,
            ResourceLocation machine,
            ItemStack input) {
        if (level == null
                || input.isEmpty()) {
            return null;
        }

        for (MultiblockProcessingRecipe recipe :
                level.getRecipeManager()
                        .getAllRecipesFor(
                                MultiblockProcessingRecipe
                                        .Type.INSTANCE
                        )) {
            if (recipe.isForMachine(machine)
                    && recipe.matchesInput(input)) {
                return recipe;
            }
        }

        return null;
    }

    public static boolean acceptsInput(
            @Nullable Level level,
            ResourceLocation machine,
            ItemStack input) {
        if (level == null
                || input.isEmpty()) {
            return false;
        }

        for (MultiblockProcessingRecipe recipe :
                level.getRecipeManager()
                        .getAllRecipesFor(
                                MultiblockProcessingRecipe
                                        .Type.INSTANCE
                        )) {
            if (recipe.isForMachine(machine)
                    && recipe.acceptsInputType(input)) {
                return true;
            }
        }

        return false;
    }
}
