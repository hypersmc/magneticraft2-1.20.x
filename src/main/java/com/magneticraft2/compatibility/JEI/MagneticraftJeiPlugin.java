package com.magneticraft2.compatibility.JEI;

import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.recipe.multiblock.MultiblockProcessingRecipe;
import com.magneticraft2.common.recipe.stage.copper.MechanicalOreWasherRecipe;
import com.magneticraft2.common.recipe.stage.copper.MechanicalSifterRecipe;
import com.magneticraft2.common.recipe.stage.stone.primitive_furnace_multiblockrecipe;
import com.magneticraft2.common.recipe.stage.stone.primitive_grinder_multiblockrecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class MagneticraftJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID =
            new ResourceLocation(
                    magneticraft2.MOD_ID,
                    "jei_plugin"
            );

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(
            IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MultiblockProcessingJeiCategory(
                        registration.getJeiHelpers()
                                .getGuiHelper()
                )
        );
    }

    @Override
    public void registerRecipes(
            IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            magneticraft2.LOGGER.warn(
                    "JEI requested Magneticraft2 recipes without an active level"
            );
            return;
        }

        List<MultiblockJeiRecipe> recipes =
                new ArrayList<>();

        level.getRecipeManager()
                .getAllRecipesFor(
                        MultiblockProcessingRecipe.Type.INSTANCE
                )
                .stream()
                .map(MultiblockJeiRecipe::from)
                .forEach(recipes::add);

        level.getRecipeManager()
                .getAllRecipesFor(
                        primitive_furnace_multiblockrecipe.Type.INSTANCE
                )
                .stream()
                .map(recipe ->
                        MultiblockJeiRecipe.from(
                                recipe,
                                level.registryAccess()
                        )
                )
                .forEach(recipes::add);

        level.getRecipeManager()
                .getAllRecipesFor(
                        primitive_grinder_multiblockrecipe.Type.INSTANCE
                )
                .stream()
                .map(recipe ->
                        MultiblockJeiRecipe.from(
                                recipe,
                                level.registryAccess()
                        )
                )
                .forEach(recipes::add);

        // Transitional support for datapacks that still use the old
        // Copper-machine serializers. New recipes should use
        // magneticraft2:multiblock_processing.
        level.getRecipeManager()
                .getAllRecipesFor(
                        MechanicalOreWasherRecipe.Type.INSTANCE
                )
                .stream()
                .map(MultiblockJeiRecipe::from)
                .forEach(recipes::add);

        level.getRecipeManager()
                .getAllRecipesFor(
                        MechanicalSifterRecipe.Type.INSTANCE
                )
                .stream()
                .map(MultiblockJeiRecipe::from)
                .forEach(recipes::add);

        registration.addRecipes(
                MultiblockProcessingJeiCategory.TYPE,
                recipes
        );

        magneticraft2.LOGGER.info(
                "Registered {} Magneticraft2 multiblock recipes with JEI",
                recipes.size()
        );
    }

    @Override
    public void registerRecipeCatalysts(
            IRecipeCatalystRegistration registration) {
        registerCatalyst(
                registration,
                MultiblockJeiRecipe.PRIMITIVE_FURNACE
        );
        registerCatalyst(
                registration,
                MultiblockJeiRecipe.PRIMITIVE_GRINDER
        );
        registerCatalyst(
                registration,
                MultiblockJeiRecipe.MECHANICAL_ORE_WASHER
        );
        registerCatalyst(
                registration,
                MultiblockJeiRecipe.MECHANICAL_SIFTER
        );
    }

    private static void registerCatalyst(
            IRecipeCatalystRegistration registration,
            ResourceLocation itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(
                itemId
        );

        if (item == null || item == Items.AIR) {
            magneticraft2.LOGGER.warn(
                    "Could not register JEI multiblock catalyst {}",
                    itemId
            );
            return;
        }

        registration.addRecipeCatalyst(
                new ItemStack(item),
                MultiblockProcessingJeiCategory.TYPE
        );
    }
}
