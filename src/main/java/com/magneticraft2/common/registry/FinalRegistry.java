package com.magneticraft2.common.registry;

import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.registry.registers.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FinalRegistry {
    private static final Logger LOGGER = LogManager.getLogger("MGC2Registry");
//    private static final DeferredRegister<StructureFeature<?>> STRUCTURES = DeferredRegister.create(ForgeRegistries.STRUCTURE_FEATURES, MOD_ID);


    public static void register(){
        LOGGER.info("Started to register Blocks, items etc.");
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        CreativeTabRegistry.init(bus);
        BlockRegistry.init(bus);
        ItemRegistry.init(bus);
        BlockEntityRegistry.init(bus);
        EntitiesRegistry.init(bus);
//        STRUCTURES.register(bus);
//        RecipeRegistry.registerRecipes(bus);
        ContainerAndScreenRegistry.init(bus);
    }

    //Tags
    public static final TagKey<Item> clayitem = ItemTags.create(new ResourceLocation(MOD_ID, "clay_items"));

}
