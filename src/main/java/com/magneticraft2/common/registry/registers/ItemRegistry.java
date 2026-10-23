package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.blockitems.beltblockitem;
import com.magneticraft2.common.item.creativeitems.MBdebugger;
import com.magneticraft2.common.item.creativeitems.multiblock_filler_item;
import com.magneticraft2.common.item.creativeitems.pitkiln_item;
import com.magneticraft2.common.item.general.Blueprintmarker;
import com.magneticraft2.common.item.general.pebble;
import com.magneticraft2.common.item.stage.copper.ItemBeltItem;
import com.magneticraft2.common.item.stage.copper.LeatherBeltItem;
import com.magneticraft2.common.item.stage.bronze.BronzePouringLadleItem;
import com.magneticraft2.common.item.stage.stone.pots.ceramicPot;
import com.magneticraft2.common.item.stage.stone.pots.clayPot;
import com.magneticraft2.common.item.stage.stone.slagitem;
import com.magneticraft2.common.item.stage.stone.tools.FireStarter;
import com.magneticraft2.common.item.stage.stone.tools.StoneHammer;
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
* @version 1.0.0
 */
@Mod.EventBusSubscriber(modid = magneticraft2.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemRegistry {
    private static final Logger LOGGER = LogManager.getLogger("MGC2-ItemRegistry");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
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
    //Should sort this for each stage

    public static final RegistryObject<Blueprintmarker> item_blueprintmarker = ITEMS.register("blueprint", Blueprintmarker::new);
    public static final RegistryObject<BlockItem> item_beltblock = ITEMS.register("beltblock", () -> new beltblockitem(beltblock.get(), new Item.Properties()));
    public static final RegistryObject<multiblock_filler_item> item_multiblock_filler = ITEMS.register("multiblock_filler_item", () -> new multiblock_filler_item(multiblockfiller.get(), new Item.Properties()));
    public static final RegistryObject<pitkiln_item> item_pitkiln = ITEMS.register("pitkilnblock", () -> new pitkiln_item(PitKilnblock.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<MBdebugger> item_mbdebugger = ITEMS.register("mbdebugger", MBdebugger::new);
    //Blocks to items
//    public static final RegistryObject<Item> item_pitkiln = fromBlock(PitKilnblock); no reason to have a pitkiln block as its never a block that should be set.
    public static final RegistryObject<Item> item_projector = fromBlock(protectortest);
    public static final RegistryObject<Item> item_testpowermoduleblock = fromBlock(testpowermoduleblock);
    public static final RegistryObject<Item> item_testpollutionblock = fromBlock(testpollutionblock);
    public static final RegistryObject<Item> item_blueprintmakermultiblock = fromBlock(blueprintmultiblock);
    public static final RegistryObject<Item> item_primitivefurnacemultiblock = fromBlock(primitivefurnacemultiblock);
    public static final RegistryObject<Item> item_primitivestoragecellarmultiblock = fromBlock(primitivestoragecellarmultiblock);
    public static final RegistryObject<Item> item_primitivefurnacemultiblock_nogui = fromBlock(primitivefurnace_multiblock_nogui);
    public static final RegistryObject<Item> item_bellowsmultiblockmodule = fromBlock(bellowsmultiblockmodule);



    //Stone
    public static final RegistryObject<Item> rice_seed = ITEMS.register("rice_seed", () -> new ItemNameBlockItem(rice_plant.get(), new Item.Properties()));
    public static final RegistryObject<Item> rice = ITEMS.register("rice", () -> new Item(new Item.Properties().food(ModFoods.rice)));
    public static final RegistryObject<clayPot> item_clay_pot = ITEMS.register("clay_pot", clayPot::new);
    public static final RegistryObject<ceramicPot> item_ceramic_pot = ITEMS.register("ceramic_pot", ceramicPot::new);
    public static final RegistryObject<FireStarter> item_fire_starter = ITEMS.register("firestarter", FireStarter::new);
    public static final RegistryObject<StoneHammer> item_stone_hammer = ITEMS.register("primitive_hammer", StoneHammer::new);
    public static final RegistryObject<slagitem> item_slag = ITEMS.register("slagitem", slagitem::new);
    public static final RegistryObject<StoneKnife> item_stone_knife = ITEMS.register("stoneknife", () -> new StoneKnife(Tiers.WOOD, 0,0, ITEM_PROPERTIES));
    public static final RegistryObject<pebble> item_pebble = ITEMS.register("pebble", pebble::new);
    public static final RegistryObject<Item> ITEM_PRIMITIVE_ANVIL = fromBlock(Primitive_anvillBlock);
    //Copper
    public static final RegistryObject<Item> ITEM_COPPER_PLATE = ITEMS.register("copper_plate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_CRUSHED_CHALCOCITE = ITEMS.register("crushed_chalcocite", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_CRUSHED_CASSITERITE = ITEMS.register("crushed_cassiterite", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_WASHED_CASSITERITE = ITEMS.register("washed_cassiterite", () -> new Item(new Item.Properties()));
    // Copper-age expansion: processed material forms (quality grades follow later).
    public static final RegistryObject<Item> ITEM_WASHED_CHALCOCITE = ITEMS.register("washed_chalcocite", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_COPPER_CONCENTRATE = ITEMS.register("copper_concentrate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_SIFTER_MESH = ITEMS.register("sifter_mesh", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ITEM_CRUSHED_QUARTZ = ITEMS.register("crushed_quartz", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_WASHED_QUARTZ = ITEMS.register("washed_quartz", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_TIN_CONCENTRATE = ITEMS.register("tin_concentrate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_TIN_INGOT = ITEMS.register("tin_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_BRONZE_CHARGE = ITEMS.register("bronze_charge", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_BRONZE_INGOT = ITEMS.register("bronze_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_WOODEN_GEAR_PATTERN =
            ITEMS.register("wooden_gear_pattern", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ITEM_WOODEN_BEARING_PATTERN =
            ITEMS.register("wooden_bearing_pattern", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ITEM_BRONZE_PATTERN_MAKER = fromBlock(BRONZE_PATTERN_MAKER);
    public static final RegistryObject<Item> ITEM_BRONZE_MOLD_TABLE = fromBlock(BRONZE_MOLD_TABLE);
    public static final RegistryObject<Item> ITEM_BRONZE_MECHANICAL_LATHE = fromBlock(BRONZE_MECHANICAL_LATHE);
    public static final RegistryObject<Item> ITEM_BRONZE_STAMP_MILL = fromBlock(BRONZE_STAMP_MILL);
    public static final RegistryObject<Item> ITEM_ROTARY_ORE_CLASSIFIER = fromBlock(ROTARY_ORE_CLASSIFIER);
    public static final RegistryObject<Item> ITEM_WATER_SETTLING_TANK = fromBlock(WATER_SETTLING_TANK);
    public static final RegistryObject<Item> ITEM_SETTLED_SEDIMENT =
            ITEMS.register("settled_sediment", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_CLASSIFIER_SCREEN =
            ITEMS.register("bronze_classifier_screen", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ITEM_CLASSIFIED_CHALCOCITE =
            ITEMS.register("classified_chalcocite", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_CLASSIFIED_CASSITERITE =
            ITEMS.register("classified_cassiterite", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_CLASSIFIED_QUARTZ =
            ITEMS.register("classified_quartz", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_BRONZE_STAMP_SHOE =
            ITEMS.register("bronze_stamp_shoe", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ITEM_BRONZE_FOUNDRY = fromBlock(BRONZE_FOUNDRY);
    public static final RegistryObject<Item> ITEM_BRONZE_CASTING_BASIN = fromBlock(BRONZE_CASTING_BASIN);
    public static final RegistryObject<BronzePouringLadleItem> ITEM_BRONZE_POURING_LADLE =
            ITEMS.register("bronze_pouring_ladle", BronzePouringLadleItem::new);
    public static final RegistryObject<Item> ITEM_LATHE_CUTTER =
            ITEMS.register("bronze_lathe_cutter", () -> new Item(new Item.Properties().durability(64)));
    public static final RegistryObject<Item> ITEM_PRECISION_BRONZE_GEAR =
            ITEMS.register("precision_bronze_gear", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_PRECISION_BRONZE_BEARING =
            ITEMS.register("precision_bronze_bearing", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_ROUGH_BRONZE_GEAR =
            ITEMS.register("rough_bronze_gear", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_ROUGH_BRONZE_BEARING =
            ITEMS.register("rough_bronze_bearing", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_GEAR_SAND_MOLD =
            ITEMS.register("gear_sand_mold", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ITEM_BEARING_SAND_MOLD =
            ITEMS.register("bearing_sand_mold", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ITEM_PRIMITIVE_GRINDER_BASE = fromBlock(primitive_grinder_bmultiblock);
    public static final RegistryObject<Item> ITEM_PRIMITIVE_GRINDER_TOP = fromBlock(primitive_grindertop);
    public static final RegistryObject<Item> ITEM_GEAR_MEDIUM_WOOD = fromBlock(GEAR_MEDIUM_WOOD);
    public static final RegistryObject<Item> ITEM_GEAR_LARGE_WOOD = fromBlock(GEAR_LARGE_WOOD);
    public static final RegistryObject<Item> ITEM_GEAR_LARGE_WITH_HANDLE_WOOD = fromBlock(GEAR_LARGE_WITH_HANDLE_WOOD);
    public static final RegistryObject<Item> ITEM_SHAFT_WOOD = fromBlock(SHAFT_WOOD);
    public static final RegistryObject<Item> ITEM_CLUTCH_WOOD = fromBlock(CLUTCH_WOOD);
    public static final RegistryObject<Item> ITEM_OVERLOAD_DISCONNECT_WOOD = fromBlock(OVERLOAD_DISCONNECT_WOOD);
    public static final RegistryObject<Item> ITEM_MECHANICAL_BRAKE_WOOD = fromBlock(MECHANICAL_BRAKE_WOOD);
    public static final RegistryObject<Item> ITEM_CRANK_WOOD = fromBlock(CRANK_WOOD);
    public static final RegistryObject<Item> ITEM_MECHANICAL_BELLOWS = fromBlock(MECHANICAL_BELLOWS);
    public static final RegistryObject<Item> ITEM_FLYWHEEL_WOOD = fromBlock(FLYWHEEL_WOOD);
    public static final RegistryObject<Item> ITEM_MECHANICAL_ORE_WASHER = fromBlock(MECHANICAL_ORE_WASHER);
    public static final RegistryObject<Item> ITEM_MECHANICAL_SIFTER = fromBlock(MECHANICAL_SIFTER);
    public static final RegistryObject<Item> ITEM_MECHANICAL_INPUT_MODULE = fromBlock(MECHANICAL_INPUT_MODULE);
    public static final RegistryObject<Item> ITEM_MULTIBLOCK_ITEM_INPUT = fromBlock(MULTIBLOCK_ITEM_INPUT);
    public static final RegistryObject<Item> ITEM_MULTIBLOCK_ITEM_OUTPUT = fromBlock(MULTIBLOCK_ITEM_OUTPUT);
    public static final RegistryObject<Item> ITEM_MULTIBLOCK_ITEM_PRIMARY_OUTPUT = fromBlock(MULTIBLOCK_ITEM_PRIMARY_OUTPUT);
    public static final RegistryObject<Item> ITEM_MULTIBLOCK_ITEM_BYPRODUCT_OUTPUT = fromBlock(MULTIBLOCK_ITEM_BYPRODUCT_OUTPUT);
    public static final RegistryObject<Item> ITEM_MULTIBLOCK_FLUID_INPUT = fromBlock(MULTIBLOCK_FLUID_INPUT);
    public static final RegistryObject<Item> ITEM_MULTIBLOCK_FLUID_OUTPUT = fromBlock(MULTIBLOCK_FLUID_OUTPUT);
    public static final RegistryObject<Item> ITEM_WATER_PIPE = fromBlock(WATER_PIPE);
    public static final RegistryObject<Item> ITEM_MECHANICAL_WATER_PUMP = fromBlock(MECHANICAL_WATER_PUMP);
    public static final RegistryObject<Item> ITEM_MECHANICAL_TRANSFER_ARM = fromBlock(MECHANICAL_TRANSFER_ARM);
    public static final RegistryObject<Item> ITEM_COPPER_FLUID_TANK = fromBlock(COPPER_FLUID_TANK);
    public static final RegistryObject<Item> ITEM_PULLEY_SMALL_WOOD = fromBlock(PULLEY_SMALL_WOOD);
    public static final RegistryObject<Item> ITEM_PULLEY_LARGE_WOOD = fromBlock(PULLEY_LARGE_WOOD);
    public static final RegistryObject<Item> ITEM_CONVEYOR_ROLLER = fromBlock(CONVEYOR_ROLLER);
    public static final RegistryObject<Item> ITEM_GEARBOX_WOOD = fromBlock(GEARBOX_WOOD);
    public static final RegistryObject<Item> ITEM_CUSTOM_GEARBOX_WOOD = fromBlock(CUSTOM_GEARBOX_WOOD);
    public static final RegistryObject<LeatherBeltItem> ITEM_LEATHER_BELT = ITEMS.register("leather_belt", LeatherBeltItem::new);
    public static final RegistryObject<ItemBeltItem> ITEM_ITEM_BELT = ITEMS.register("item_belt", ItemBeltItem::new);
    public static final RegistryObject<Item> ITEM_WATER_WHEEL_SMALL = fromBlock(WATER_WHEEL_SMALL);
    public static final RegistryObject<Item> ITEM_WATER_WHEEL_LARGE = fromBlock(WATER_WHEEL_LARGE);





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
