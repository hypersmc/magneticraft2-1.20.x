package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.block.general.*;
import com.magneticraft2.common.block.general.ores.deepslate.*;
import com.magneticraft2.common.block.general.ores.normal.*;
import com.magneticraft2.common.block.stage.copper.LargeGearBlock_wood;
import com.magneticraft2.common.block.stage.copper.LargeGearWithHandleBlock_wood;
import com.magneticraft2.common.block.stage.copper.MediumGearBlock_wood;
import com.magneticraft2.common.block.stage.stone.BellowsMultiblockModule;
import com.magneticraft2.common.block.stage.stone.PitKilnBlock;
import com.magneticraft2.common.block.stage.stone.PrimitiveFurnaceMultiblock;
import com.magneticraft2.common.block.stage.stone.PrimitiveFurnaceMultiblock_nogui;
import com.magneticraft2.common.item.general.foods.RicePlantBlock;
import com.magneticraft2.common.magneticraft2;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 30-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
@Mod.EventBusSubscriber(modid = magneticraft2.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BlockRegistry {
    private static final Logger LOGGER = LogManager.getLogger("MGC2-BlockRegistry");
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static void init(IEventBus eventBus){
        BLOCKS.register(eventBus);
    }
    private static <T extends Block> RegistryObject<T> registerBlockWithoutBlockItem(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }
    //Blocks after this line
    //Should sort this for each stage
    public static final RegistryObject<PitKilnBlock> PitKilnblock = BLOCKS.register("pitkilnblock", PitKilnBlock::new);
    public static final RegistryObject<Block> rice_plant = registerBlockWithoutBlockItem("rice_plant", () -> new RicePlantBlock(BlockBehaviour.Properties.of().noOcclusion()));
    public static final RegistryObject<stick> stickblock = BLOCKS.register("stick", stick::new);
    public static final RegistryObject<stonepebble> stonepebble = BLOCKS.register("stonepebble", stonepebble::new);
    public static final RegistryObject<projectortest> protectortest = BLOCKS.register("projector", projectortest::new);
    public static final RegistryObject<testpowermoduleblock> testpowermoduleblock = BLOCKS.register("testpowermodule", testpowermoduleblock::new);
    public static final RegistryObject<testpollutionblock> testpollutionblock = BLOCKS.register("testpollutionblock", testpollutionblock::new);
    public static final RegistryObject<BeltBlock> beltblock = BLOCKS.register("beltblock", BeltBlock::new);
    public static final RegistryObject<Multiblockfiller> multiblockfiller = BLOCKS.register("multiblock_filler", Multiblockfiller::new);
    public static final RegistryObject<BlueprintMultiblock> blueprintmultiblock = BLOCKS.register("blueprint_multiblock", BlueprintMultiblock::new);
    public static final RegistryObject<PrimitiveFurnaceMultiblock> primitivefurnacemultiblock = BLOCKS.register("primitivefurnace_multiblock", PrimitiveFurnaceMultiblock::new);
    public static final RegistryObject<PrimitiveFurnaceMultiblock_nogui> primitivefurnace_multiblock_nogui = BLOCKS.register("primitivefurnace_multiblock_nogui", PrimitiveFurnaceMultiblock_nogui::new);
    public static final RegistryObject<BellowsMultiblockModule> bellowsmultiblockmodule = BLOCKS.register("bellows_multiblock_module", BellowsMultiblockModule::new);

    //Stone


    //Copper

    public static final RegistryObject<MediumGearBlock_wood> GEAR_MEDIUM_WOOD = BLOCKS.register("gear_medium_wood", MediumGearBlock_wood::new);
    public static final RegistryObject<LargeGearBlock_wood> GEAR_LARGE_WOOD = BLOCKS.register("gear_large_wood", LargeGearBlock_wood::new);
    public static final RegistryObject<LargeGearWithHandleBlock_wood> GEAR_LARGE_WITH_HANDLE_WOOD = BLOCKS.register("gear_large_wood_with_handle", LargeGearWithHandleBlock_wood::new);






    //ores
    public static final RegistryObject<Bauxite> BAUXITE_ORE = BLOCKS.register("bauxite_ore", Bauxite::new);
    public static final RegistryObject<Apatite> APATITE_ORE = BLOCKS.register("apatite_ore", Apatite::new);
    public static final RegistryObject<Chromite> CHROMITE_ORE = BLOCKS.register("chromite_ore", Chromite::new);
    public static final RegistryObject<Chromite_deepslate> CHROMITE_DEEPSLATE_ORE = BLOCKS.register("chromite_deepslate_ore", Chromite_deepslate::new);
    public static final RegistryObject<Lignite> LIGNITE_ORE = BLOCKS.register("lignite_ore", Lignite::new);
    public static final RegistryObject<Cobaltite> COBALTITE_ORE = BLOCKS.register("cobaltite_ore", Cobaltite::new);
    public static final RegistryObject<Chalcocite> CHALCOCITE_ORE = BLOCKS.register("chalcocite_ore", Chalcocite::new);
    public static final RegistryObject<Cryolite> CRYOLITE_ORE = BLOCKS.register("cryolite_ore", Cryolite::new);
    public static final RegistryObject<Kimberlite> KIMBERLITE_ORE = BLOCKS.register("kimberlite_ore", Kimberlite::new);
    public static final RegistryObject<Graphite> GRAPHITE_ORE = BLOCKS.register("graphite_ore", Graphite::new);
    public static final RegistryObject<Magnetite> MAGNETITE_ORE = BLOCKS.register("magnetite_ore", Magnetite::new);
    public static final RegistryObject<Magnetite_deepslate> MAGNETITE_DEEPSLATE_ORE = BLOCKS.register("magnetite_deepslate_ore", Magnetite_deepslate::new);
    public static final RegistryObject<Hematite> HEMATITE_ORE = BLOCKS.register("hematite_ore", Hematite::new);
    public static final RegistryObject<Limonite> LIMONITE_ORE = BLOCKS.register("limonite_ore", Limonite::new);
    public static final RegistryObject<Limonite_deepslate> LIMONITE_DEEPSLATE_ORE = BLOCKS.register("limonite_deepslate_ore", Limonite_deepslate::new);
    public static final RegistryObject<Kaolinite> KAOLINITE_ORE = BLOCKS.register("kaolinite_ore", Kaolinite::new);
    public static final RegistryObject<Galena> GALENA_ORE = BLOCKS.register("galena_ore", Galena::new);
    public static final RegistryObject<Galena_deepslate> GALENA_DEEPSLATE_ORE = BLOCKS.register("galena_deepslate_ore", Galena_deepslate::new);
    public static final RegistryObject<Manganese> MANGANESE_ORE = BLOCKS.register("manganese_ore", Manganese::new);
    public static final RegistryObject<Manganese_deepslate> MANGANESE_DEEPSLATE_ORE = BLOCKS.register("manganese_deepslate_ore", Manganese_deepslate::new);
    public static final RegistryObject<Cinnabar> CINNABAR_ORE = BLOCKS.register("cinnabar_ore", Cinnabar::new);
    public static final RegistryObject<Cinnabar_deepslate> CINNABAR_DEEPSLATE_ORE = BLOCKS.register("cinnabar_deepslate_ore", Cinnabar_deepslate::new);
    public static final RegistryObject<Garnierite> GARNIERITE_ORE = BLOCKS.register("garnierite_ore", Garnierite::new);
    public static final RegistryObject<Garnierite_deepslate> GARNIERITE_DEEPSLATE_ORE = BLOCKS.register("garnierite_deepslate_ore", Garnierite_deepslate::new);
    public static final RegistryObject<Osmiridium> OSMIRIDIUM_ORE = BLOCKS.register("osmiridium_ore", Osmiridium::new);
    public static final RegistryObject<Platinium> PLATINIUM_ORE = BLOCKS.register("platinium_ore", Platinium::new);
    public static final RegistryObject<Quartz> QUARTZ_ORE = BLOCKS.register("quartz_ore", Quartz::new);
    public static final RegistryObject<Quartz_deepslate> QUARTZ_DEEPSLATE_ORE = BLOCKS.register("quartz_deepslate_ore", Quartz_deepslate::new);
    public static final RegistryObject<Saltpeter> SALTPETER_ORE = BLOCKS.register("saltpeter_ore", Saltpeter::new);
    public static final RegistryObject<Silicium> SILICIUM_ORE = BLOCKS.register("silicium_ore", Silicium::new);
    public static final RegistryObject<Silicium_deepslate> SILICIUM_DEEPSLATE_ORE = BLOCKS.register("silicium_deepslate_ore", Silicium_deepslate::new);
    public static final RegistryObject<Silver> SILVER_ORE = BLOCKS.register("silver_ore", Silver::new);
    public static final RegistryObject<Sulfur> SULFUR_ORE = BLOCKS.register("sulfur_ore", Sulfur::new);
    public static final RegistryObject<Sulfur_deepslate> SULFUR_DEEPSLATE_ORE = BLOCKS.register("sulfur_deepslate_ore", Sulfur_deepslate::new);
    public static final RegistryObject<Tantalite> TANTALITE_ORE = BLOCKS.register("tantalite_ore", Tantalite::new);
    public static final RegistryObject<Tantalite_deepslate> TANTALITE_DEEPSLATE_ORE = BLOCKS.register("tantalite_deepslate_ore", Tantalite_deepslate::new);
    public static final RegistryObject<Cassiterite> CASSITERITE_ORE = BLOCKS.register("cassiterite_ore", Cassiterite::new);
    public static final RegistryObject<Titanite> TITANITE_ORE = BLOCKS.register("titanite_ore", Titanite::new);
    public static final RegistryObject<Wolframite> WOLFRAMITE_ORE = BLOCKS.register("wolframite_ore", Wolframite::new);
    public static final RegistryObject<Uraninite> URANINITE_ORE = BLOCKS.register("uraninite_ore", Uraninite::new);
    public static final RegistryObject<Sphalerite> SPHALERITE_ORE = BLOCKS.register("sphalerite_ore", Sphalerite::new);
    public static final RegistryObject<Anthracite> ANTHRACITE_ORE = BLOCKS.register("anthracite_ore", Anthracite::new);


}
