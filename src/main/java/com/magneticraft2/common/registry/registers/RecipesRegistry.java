package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.recipe.stage.stone.primitive_furnace_multiblockrecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 14-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class RecipesRegistry {
    private static final Logger LOGGER = LogManager.getLogger("MGC2-RecipesRegistry");
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);
    public static void init(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }
    //Recipes after this line
    public static final RegistryObject<RecipeSerializer<primitive_furnace_multiblockrecipe>> primitive_furnace_multiblockrecipe = RECIPE_SERIALIZERS.register("primitive_furnace_multiblock", () -> com.magneticraft2.common.recipe.stage.stone.primitive_furnace_multiblockrecipe.Serializer.INSTANCE);

}
