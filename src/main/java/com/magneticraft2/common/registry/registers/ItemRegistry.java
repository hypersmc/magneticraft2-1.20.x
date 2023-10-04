package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.item.general.Blueprintmarker;
import com.magneticraft2.common.item.general.pebble;
import com.magneticraft2.common.item.stage.stone.pots.ceramicPot;
import com.magneticraft2.common.item.stage.stone.pots.clayPot;
import com.magneticraft2.common.item.stage.stone.tools.FireStarter;
import com.magneticraft2.common.item.stage.stone.tools.StoneKnife;
import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.registry.ModFoods;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.magneticraft2.common.magneticraft2.MOD_ID;
import static com.magneticraft2.common.registry.registers.BlockRegistry.*;

/**
 * @author JumpWatch on 30-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
@Mod.EventBusSubscriber(modid = magneticraft2.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemRegistry {
    private static final Logger LOGGER = LogManager.getLogger("MGC2-ItemRegistry");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static void init(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
    public static final Item.Properties ITEM_PROPERTIES = new Item.Properties();
    public static final Item.Properties ORE_PROPERTIES = new Item.Properties();
    public static <B extends Block> RegistryObject<Item> fromBlock(RegistryObject<B> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), ITEM_PROPERTIES));
    }
    public static <B extends Block> RegistryObject<Item> fromBlockOre(RegistryObject<B> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), ORE_PROPERTIES));
    }
    //Items after this line
    public static final RegistryObject<Item> rice_seed = ITEMS.register("rice_seed", () -> new ItemNameBlockItem(rice_plant.get(), new Item.Properties()));
    public static final RegistryObject<Item> rice = ITEMS.register("rice", () -> new Item(new Item.Properties().food(ModFoods.rice)));
    public static final RegistryObject<clayPot> item_clay_pot = ITEMS.register("clay_pot", clayPot::new);
    public static final RegistryObject<ceramicPot> item_ceramic_pot = ITEMS.register("ceramic_pot", ceramicPot::new);
    public static final RegistryObject<FireStarter> item_fire_starter = ITEMS.register("firestarter", FireStarter::new);
    public static final RegistryObject<StoneKnife> item_stone_knife = ITEMS.register("stoneknife", () -> new StoneKnife(Tiers.WOOD, 0,0, ITEM_PROPERTIES));
    public static final RegistryObject<pebble> item_pebble = ITEMS.register("pebble", pebble::new);
    public static final RegistryObject<Blueprintmarker> item_blueprintmarker = ITEMS.register("blueprint", Blueprintmarker::new);

    //Blocks to items
    public static final RegistryObject<Item> item_pitkiln = fromBlock(PitKilnblock);
    public static final RegistryObject<Item> item_projector = fromBlock(protectortest);
    public static final RegistryObject<Item> item_blueprintmaker = fromBlock(blueprintmaker);
    public static final RegistryObject<Item> item_testmultiblockblock = fromBlock(testmultiblockblock);
    public static final RegistryObject<Item> item_testpowermoduleblock = fromBlock(testpowermoduleblock);
    public static final RegistryObject<Item> item_testpollutionblock = fromBlock(testpollutionblock);


    //ores
    public static final RegistryObject<Item> BAUXITE_ITEM = fromBlockOre(BAUXITE_ORE);
    public static final RegistryObject<Item> APATITE_ITEM = fromBlockOre(APATITE_ORE);
    public static final RegistryObject<Item> CHROMITE_ITEM = fromBlockOre(CHROMITE_ORE);
    public static final RegistryObject<Item> CHROMITE_DEEPSLATE_ITEM = fromBlockOre(CHROMITE_DEEPSLATE_ORE);
    public static final RegistryObject<Item> LIGNITE_ITEM = fromBlockOre(LIGNITE_ORE);
    public static final RegistryObject<Item> COBALTITE_ITEM = fromBlockOre(COBALTITE_ORE);
    public static final RegistryObject<Item> CHALCOCITE_ITEM = fromBlockOre(CHALCOCITE_ORE);
    public static final RegistryObject<Item> CRYOLITE_ITEM = fromBlockOre(CRYOLITE_ORE);
    public static final RegistryObject<Item> KIMBERLITE_ITEM = fromBlockOre(KIMBERLITE_ORE);
    public static final RegistryObject<Item> GRAPHITE_ITEM = fromBlockOre(GRAPHITE_ORE);
    public static final RegistryObject<Item> MAGNETITE_ITEM = fromBlockOre(MAGNETITE_ORE);
    public static final RegistryObject<Item> MAGNETITE_DEEPSLATE_ITEM = fromBlockOre(MAGNETITE_DEEPSLATE_ORE);
    public static final RegistryObject<Item> HEMATITE_ITEM = fromBlockOre(HEMATITE_ORE);
    public static final RegistryObject<Item> LIMONITE_ITEM = fromBlockOre(LIMONITE_ORE);
    public static final RegistryObject<Item> LIMONITE_DEEPSLATE_ITEM = fromBlockOre(LIMONITE_DEEPSLATE_ORE);
    public static final RegistryObject<Item> KAOLINITE_ITEM = fromBlockOre(KAOLINITE_ORE);
    public static final RegistryObject<Item> GALENA_ITEM = fromBlockOre(GALENA_ORE);
    public static final RegistryObject<Item> GALENA_DEEPSLATE_ITEM = fromBlockOre(GALENA_DEEPSLATE_ORE);
    public static final RegistryObject<Item> MANGANESE_ITEM = fromBlockOre(MANGANESE_ORE);
    public static final RegistryObject<Item> MANGANESE_DEEPSLATE_ITEM = fromBlockOre(MANGANESE_DEEPSLATE_ORE);
    public static final RegistryObject<Item> CINNABAR_ITEM = fromBlockOre(CINNABAR_ORE);
    public static final RegistryObject<Item> CINNABAR_DEEPSLATE_ITEM = fromBlockOre(CINNABAR_DEEPSLATE_ORE);
    public static final RegistryObject<Item> GARNIERITE_ITEM = fromBlockOre(GARNIERITE_ORE);
    public static final RegistryObject<Item> GARNIERITE_DEEPSLATE_ITEM = fromBlockOre(GARNIERITE_DEEPSLATE_ORE);
    public static final RegistryObject<Item> OSMIRIDIUM_ITEM = fromBlockOre(OSMIRIDIUM_ORE);
    public static final RegistryObject<Item> PLATINIUM_ITEM = fromBlockOre(PLATINIUM_ORE);
    public static final RegistryObject<Item> QUARTZ_ITEM = fromBlockOre(QUARTZ_ORE);
    public static final RegistryObject<Item> QUARTZ_DEEPSLATE_ITEM = fromBlockOre(QUARTZ_DEEPSLATE_ORE);
    public static final RegistryObject<Item> SALTPETER_ITEM = fromBlockOre(SALTPETER_ORE);
    public static final RegistryObject<Item> SILICIUM_ITEM = fromBlockOre(SILICIUM_ORE);
    public static final RegistryObject<Item> SILICIUM_DEEPSLATE_ITEM = fromBlockOre(SILICIUM_DEEPSLATE_ORE);
    public static final RegistryObject<Item> SILVER_ITEM = fromBlockOre(SILVER_ORE);
    public static final RegistryObject<Item> SULFUR_ITEM = fromBlockOre(SULFUR_ORE);
    public static final RegistryObject<Item> SULFUR_DEEPSLATE_ITEM = fromBlockOre(SULFUR_DEEPSLATE_ORE);
    public static final RegistryObject<Item> TANTALITE_ITEM = fromBlockOre(TANTALITE_ORE);
    public static final RegistryObject<Item> TANTALITE_DEEPSLATE_ITEM = fromBlockOre(TANTALITE_DEEPSLATE_ORE);
    public static final RegistryObject<Item> CASSITERITE_ITEM = fromBlockOre(CASSITERITE_ORE);
    public static final RegistryObject<Item> TITANITE_ITEM = fromBlockOre(TITANITE_ORE);
    public static final RegistryObject<Item> WOLFRAMITE_ITEM = fromBlockOre(WOLFRAMITE_ORE);
    public static final RegistryObject<Item> URANINITE_ITEM = fromBlockOre(URANINITE_ORE);
    public static final RegistryObject<Item> SPHALERITE_ITEM = fromBlockOre(SPHALERITE_ORE);
    public static final RegistryObject<Item> ANTHRACITE_ITEM = fromBlockOre(ANTHRACITE_ORE);
}
