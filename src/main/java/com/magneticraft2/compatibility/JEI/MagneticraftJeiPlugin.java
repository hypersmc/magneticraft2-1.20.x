package com.magneticraft2.compatibility.JEI;

import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.blockentity.stage.bronze.RefractoryKilnBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.registry.registers.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import com.magneticraft2.common.recipe.multiblock.MultiblockProcessingRecipe;
import com.magneticraft2.common.recipe.multiblock.MechanicalAssemblyRecipe;
import com.magneticraft2.common.recipe.multiblock.BloomeryRecipe;
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
                ),
                new KilnLiquidFuelJeiCategory(
                        registration.getJeiHelpers().getGuiHelper()
                )
        );
    }

    @Override
    public void registerRecipes(
            IRecipeRegistration registration) {
        // Distinct JEI fuel usage, NOT an extra processing recipe: a heat
        // buffer can burn many 50 mB batches per item. Keep the input
        // amount and burn duration sourced from the actual Kiln constants.
        registration.addRecipes(
                KilnLiquidFuelJeiCategory.TYPE,
                List.of(new KilnLiquidFuelJeiCategory.FuelRecipe(
                        KilnLiquidFuelJeiCategory.WOOD_TAR_ID,
                        new FluidStack(FluidRegistry.WOOD_TAR.get(),
                                RefractoryKilnBlockEntity.WOOD_TAR_IGNITION_MB),
                        RefractoryKilnBlockEntity.WOOD_TAR_BURN_TICKS
                ))
        );

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
                .getAllRecipesFor(MechanicalAssemblyRecipe.Type.INSTANCE)
                .stream()
                .map(MultiblockJeiRecipe::from)
                .forEach(recipes::add);

        level.getRecipeManager()
                .getAllRecipesFor(BloomeryRecipe.Type.INSTANCE)
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
        registerCatalyst(registration, MultiblockJeiRecipe.BRONZE_STAMP_MILL);
        registerCatalyst(registration, MultiblockJeiRecipe.BRONZE_MECHANICAL_LATHE);
        registerCatalyst(registration, MultiblockJeiRecipe.BRONZE_FORGE_HAMMER);
        registerCatalyst(registration, MultiblockJeiRecipe.BRONZE_ROLLING_MILL);
        registerCatalyst(registration, MultiblockJeiRecipe.BRONZE_MECHANICAL_PRESS);
        registerCatalyst(registration, MultiblockJeiRecipe.IRON_STOCK_SHEAR);
        registerCatalyst(registration, MultiblockJeiRecipe.BRONZE_PRECISION_BORING_MACHINE);
        registerCatalyst(registration, MultiblockJeiRecipe.BRONZE_MECHANICAL_SAWMILL);
        registerCatalyst(registration, MultiblockJeiRecipe.BRONZE_MECHANICAL_ASSEMBLY_STATION);
        registerCatalyst(registration, MultiblockJeiRecipe.BRONZE_FOUNDRY_SAND_MIXER);
        registerCatalyst(registration, MultiblockJeiRecipe.ROTARY_ORE_CLASSIFIER);
        registerCatalyst(registration, MultiblockJeiRecipe.WATER_SETTLING_TANK);
        registerCatalyst(registration, MultiblockJeiRecipe.CHARCOAL_RETORT);
        registerCatalyst(registration, MultiblockJeiRecipe.REFRACTORY_KILN);
        registration.addRecipeCatalyst(
                new ItemStack(BlockRegistry.REFRACTORY_KILN.get()),
                KilnLiquidFuelJeiCategory.TYPE
        );
        registerCatalyst(registration, MultiblockJeiRecipe.ORE_ROASTING_KILN);
        registerCatalyst(registration, MultiblockJeiRecipe.BLOOMERY_FURNACE);
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
