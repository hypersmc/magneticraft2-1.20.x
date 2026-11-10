package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.block.general.*;
import com.magneticraft2.common.hologram.AncientProjectorBlock;
import com.magneticraft2.common.block.general.ores.deepslate.*;
import com.magneticraft2.common.block.general.ores.normal.*;
import com.magneticraft2.common.block.stage.copper.LargeGearBlock_wood;
import com.magneticraft2.common.block.stage.iron.WroughtIronMediumGearBlock;
import com.magneticraft2.common.block.stage.iron.WroughtIronLargeGearBlock;
import com.magneticraft2.common.block.stage.iron.WroughtIronShaftBlock;
import com.magneticraft2.common.block.stage.iron.WroughtIronGearboxBlock;
import com.magneticraft2.common.block.stage.steel.SteelMediumGearBlock;
import com.magneticraft2.common.block.stage.steel.SteelLargeGearBlock;
import com.magneticraft2.common.block.stage.steel.SteelShaftBlock;
import com.magneticraft2.common.block.stage.steel.SteelGearboxBlock;
import com.magneticraft2.common.block.stage.copper.LargeGearWithHandleBlock_wood;
import com.magneticraft2.common.block.stage.copper.ClutchBlock_wood;
import com.magneticraft2.common.block.stage.copper.ConveyorRollerBlock;
import com.magneticraft2.common.block.stage.copper.GearboxBlock_wood;
import com.magneticraft2.common.block.stage.copper.CustomGearboxBlock_wood;
import com.magneticraft2.common.block.stage.copper.MechanicalBrakeBlock_wood;
import com.magneticraft2.common.block.stage.copper.MechanicalBellowsBlock;
import com.magneticraft2.common.block.stage.copper.MechanicalOreWasherBlock;
import com.magneticraft2.common.block.stage.copper.MechanicalSifterBlock;
import com.magneticraft2.common.block.stage.copper.MechanicalInputModuleBlock;
import com.magneticraft2.common.block.stage.copper.MultiblockItemPortBlock;
import com.magneticraft2.common.block.stage.copper.MultiblockFluidInputBlock;
import com.magneticraft2.common.block.stage.copper.MultiblockFluidOutputBlock;
import com.magneticraft2.common.block.stage.copper.WaterPipeBlock;
import com.magneticraft2.common.block.stage.copper.DiagonalWaterPipeBlock;
import com.magneticraft2.common.block.stage.bronze.BronzeAirDuctBlock;
import com.magneticraft2.common.block.stage.steam.SteamPipeBlock;
import com.magneticraft2.common.block.stage.steam.RupturedSteamPipeBlock;
import com.magneticraft2.common.block.stage.copper.MechanicalWaterPumpBlock;
import com.magneticraft2.common.block.stage.copper.MechanicalTransferArmBlock;
import com.magneticraft2.common.block.stage.copper.MechanicalTransferArmUpperBlock;
import com.magneticraft2.common.block.stage.copper.CopperFluidTankBlock;
import com.magneticraft2.common.block.stage.copper.FlywheelBlock_wood;
import com.magneticraft2.common.block.stage.iron.WroughtIronFlywheelBlock;
import com.magneticraft2.common.block.stage.copper.CrankBlock_wood;
import com.magneticraft2.common.block.stage.copper.ItemBeltBlock;
import com.magneticraft2.common.block.stage.copper.MediumGearBlock_wood;
import com.magneticraft2.common.block.stage.copper.PulleyBlock_wood;
import com.magneticraft2.common.block.stage.copper.OverloadDisconnectBlock_wood;
import com.magneticraft2.common.block.stage.copper.ShaftBlock_wood;
import com.magneticraft2.common.block.stage.copper.WaterWheelBlock;
import com.magneticraft2.common.block.stage.copper.WaterWheelFillerBlock;
import com.magneticraft2.common.block.stage.stone.*;
import com.magneticraft2.common.block.stage.bronze.BronzePatternMakerBlock;
import com.magneticraft2.common.block.stage.bronze.BronzeMoldTableBlock;
import com.magneticraft2.common.block.stage.bronze.BronzeFoundryBlock;
import com.magneticraft2.common.block.stage.bronze.BronzeMechanicalLatheBlock;
import com.magneticraft2.common.block.stage.bronze.BronzeStampMillBlock;
import com.magneticraft2.common.block.stage.bronze.BronzeForgeHammerBlock;
import com.magneticraft2.common.block.stage.bronze.BronzeRollingMillBlock;
import com.magneticraft2.common.block.stage.bronze.BronzeMechanicalPressBlock;
import com.magneticraft2.common.block.stage.bronze.BronzePrecisionBoringMachineBlock;
import com.magneticraft2.common.block.stage.bronze.BronzeMechanicalSawmillBlock;
import com.magneticraft2.common.block.stage.bronze.BronzeMechanicalAssemblyStationBlock;
import com.magneticraft2.common.block.stage.bronze.BronzeFoundrySandMixerBlock;
import com.magneticraft2.common.block.stage.bronze.BronzeIndustrialAirBlowerBlock;
import com.magneticraft2.common.block.stage.bronze.BloomeryFurnaceBlock;
import com.magneticraft2.common.block.stage.bronze.RotaryOreClassifierBlock;
import com.magneticraft2.common.block.stage.bronze.WaterSettlingTankBlock;
import com.magneticraft2.common.block.stage.bronze.CharcoalRetortBlock;
import com.magneticraft2.common.block.stage.bronze.RefractoryKilnBlock;
import com.magneticraft2.common.block.stage.bronze.OreRoastingKilnBlock;
import com.magneticraft2.common.block.stage.bronze.BronzeCastingBasinBlock;
import com.magneticraft2.common.block.stage.iron.IronStructuralBeamBlock;
import com.magneticraft2.common.block.stage.iron.IronPlatePanelBlock;
import com.magneticraft2.common.block.stage.iron.IronFloorGratingBlock;
import com.magneticraft2.common.block.stage.iron.IronServiceHatchBlock;
import net.minecraft.world.level.block.FenceBlock;
import com.magneticraft2.common.block.stage.iron.IronStockShearBlock;
import com.magneticraft2.common.block.stage.iron.IronForgeHearthBlock;
import com.magneticraft2.common.block.stage.steel.SteelProcessBlock;
import com.magneticraft2.common.block.stage.steel.SteelBoilerStructuralBlock;
import com.magneticraft2.common.block.stage.steel.SteelBoilerVesselBlock;
import com.magneticraft2.common.block.stage.steam.BoilerServiceFittingBlock;
import com.magneticraft2.common.block.stage.iron.RivetedIronFrameBlock;
import com.magneticraft2.common.item.general.foods.RicePlantBlock;
import com.magneticraft2.common.magneticraft2;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
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
* @version 1.0.0
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

    public static final RegistryObject<Block> rice_plant = registerBlockWithoutBlockItem("rice_plant", () -> new RicePlantBlock(BlockBehaviour.Properties.of().noOcclusion()));

    public static final RegistryObject<AncientProjectorBlock> ANCIENT_PROJECTOR = BLOCKS.register("ancient_projector", AncientProjectorBlock::new);
    public static final RegistryObject<projectortest> protectortest = BLOCKS.register("projector", projectortest::new);
    public static final RegistryObject<testpowermoduleblock> testpowermoduleblock = BLOCKS.register("testpowermodule", testpowermoduleblock::new);
    public static final RegistryObject<testpollutionblock> testpollutionblock = BLOCKS.register("testpollutionblock", testpollutionblock::new);
    public static final RegistryObject<BeltBlock> beltblock = BLOCKS.register("beltblock", BeltBlock::new);
    public static final RegistryObject<Multiblockfiller> multiblockfiller = BLOCKS.register("multiblock_filler", Multiblockfiller::new);
    public static final RegistryObject<BlueprintMultiblock> blueprintmultiblock = BLOCKS.register("blueprint_multiblock", BlueprintMultiblock::new);
    public static final RegistryObject<BellowsMultiblockModule> bellowsmultiblockmodule = BLOCKS.register("bellows_multiblock_module", BellowsMultiblockModule::new);

    //Stone

    public static final RegistryObject<stick> stickblock = BLOCKS.register("stick", stick::new);
    public static final RegistryObject<stonepebble> stonepebble = BLOCKS.register("stonepebble", stonepebble::new);
    public static final RegistryObject<PitKilnBlock> PitKilnblock = BLOCKS.register("pitkilnblock", PitKilnBlock::new);
    public static final RegistryObject<Primitive_anvilBlock> Primitive_anvillBlock = BLOCKS.register("primitive_anvil", Primitive_anvilBlock::new);
    public static final RegistryObject<PrimitiveStorageCellarMultiblock> primitivestoragecellarmultiblock = BLOCKS.register("primitivestoragecellar_multiblock", PrimitiveStorageCellarMultiblock::new);
    public static final RegistryObject<PrimitiveFurnaceMultiblock> primitivefurnacemultiblock = BLOCKS.register("primitivefurnace_multiblock", PrimitiveFurnaceMultiblock::new);
    public static final RegistryObject<PrimitiveFurnaceMultiblock_nogui> primitivefurnace_multiblock_nogui = BLOCKS.register("primitivefurnace_multiblock_nogui", PrimitiveFurnaceMultiblock_nogui::new);
    public static final RegistryObject<PrimitiveGrinderBMultiblock> primitive_grinder_bmultiblock = BLOCKS.register("primitive_grinder_bmultiblock", PrimitiveGrinderBMultiblock::new);
    public static final RegistryObject<PrimitiveGrinderTop> primitive_grindertop = BLOCKS.register("primitive_grindertop", PrimitiveGrinderTop::new);

    //Copper
    public static final RegistryObject<MediumGearBlock_wood> GEAR_MEDIUM_WOOD = BLOCKS.register("gear_medium_wood", MediumGearBlock_wood::new);
    public static final RegistryObject<LargeGearBlock_wood> GEAR_LARGE_WOOD = BLOCKS.register("gear_large_wood", LargeGearBlock_wood::new);
    public static final RegistryObject<LargeGearWithHandleBlock_wood> GEAR_LARGE_WITH_HANDLE_WOOD = BLOCKS.register("gear_large_wood_with_handle", LargeGearWithHandleBlock_wood::new);
    public static final RegistryObject<ShaftBlock_wood> SHAFT_WOOD = BLOCKS.register("shaft_wood", ShaftBlock_wood::new);
    public static final RegistryObject<ClutchBlock_wood> CLUTCH_WOOD = BLOCKS.register("clutch_wood", ClutchBlock_wood::new);
    public static final RegistryObject<OverloadDisconnectBlock_wood> OVERLOAD_DISCONNECT_WOOD = BLOCKS.register("overload_disconnect_wood", OverloadDisconnectBlock_wood::new);
    public static final RegistryObject<MechanicalBrakeBlock_wood> MECHANICAL_BRAKE_WOOD = BLOCKS.register("mechanical_brake_wood", MechanicalBrakeBlock_wood::new);
    public static final RegistryObject<CrankBlock_wood> CRANK_WOOD = BLOCKS.register("crank_wood", CrankBlock_wood::new);
    public static final RegistryObject<MechanicalBellowsBlock> MECHANICAL_BELLOWS = BLOCKS.register("mechanical_bellows", MechanicalBellowsBlock::new);
    public static final RegistryObject<FlywheelBlock_wood> FLYWHEEL_WOOD = BLOCKS.register("flywheel_wood", FlywheelBlock_wood::new);
    public static final RegistryObject<WroughtIronFlywheelBlock> IRON_FLYWHEEL =
            BLOCKS.register("iron_flywheel",WroughtIronFlywheelBlock::new);
    public static final RegistryObject<MechanicalOreWasherBlock> MECHANICAL_ORE_WASHER = BLOCKS.register("mechanical_ore_washer", MechanicalOreWasherBlock::new);
    public static final RegistryObject<MechanicalSifterBlock> MECHANICAL_SIFTER = BLOCKS.register("mechanical_sifter", MechanicalSifterBlock::new);
    public static final RegistryObject<BronzePatternMakerBlock> BRONZE_PATTERN_MAKER =
            BLOCKS.register("bronze_pattern_maker", BronzePatternMakerBlock::new);
    public static final RegistryObject<BronzeMoldTableBlock> BRONZE_MOLD_TABLE =
            BLOCKS.register("bronze_mold_table", BronzeMoldTableBlock::new);
    public static final RegistryObject<BronzeMechanicalLatheBlock> BRONZE_MECHANICAL_LATHE =
            BLOCKS.register("bronze_mechanical_lathe", BronzeMechanicalLatheBlock::new);
    public static final RegistryObject<BronzeStampMillBlock> BRONZE_STAMP_MILL =
            BLOCKS.register("bronze_stamp_mill", BronzeStampMillBlock::new);
    public static final RegistryObject<BronzeForgeHammerBlock> BRONZE_FORGE_HAMMER =
            BLOCKS.register("bronze_forge_hammer", BronzeForgeHammerBlock::new);
    public static final RegistryObject<BronzeRollingMillBlock> BRONZE_ROLLING_MILL =
            BLOCKS.register("bronze_rolling_mill", BronzeRollingMillBlock::new);
    public static final RegistryObject<BronzeMechanicalPressBlock> BRONZE_MECHANICAL_PRESS =
            BLOCKS.register("bronze_mechanical_press", BronzeMechanicalPressBlock::new);
    public static final RegistryObject<BronzePrecisionBoringMachineBlock> BRONZE_PRECISION_BORING_MACHINE =
            BLOCKS.register("bronze_precision_boring_machine", BronzePrecisionBoringMachineBlock::new);
    public static final RegistryObject<BronzeMechanicalSawmillBlock> BRONZE_MECHANICAL_SAWMILL =
            BLOCKS.register("bronze_mechanical_sawmill", BronzeMechanicalSawmillBlock::new);
    public static final RegistryObject<BronzeMechanicalAssemblyStationBlock> BRONZE_MECHANICAL_ASSEMBLY_STATION =
            BLOCKS.register("bronze_mechanical_assembly_station", BronzeMechanicalAssemblyStationBlock::new);
    public static final RegistryObject<BronzeFoundrySandMixerBlock> BRONZE_FOUNDRY_SAND_MIXER =
            BLOCKS.register("bronze_foundry_sand_mixer", BronzeFoundrySandMixerBlock::new);
    public static final RegistryObject<BronzeIndustrialAirBlowerBlock> BRONZE_INDUSTRIAL_AIR_BLOWER =
            BLOCKS.register("bronze_industrial_air_blower", BronzeIndustrialAirBlowerBlock::new);
    public static final RegistryObject<RotaryOreClassifierBlock> ROTARY_ORE_CLASSIFIER =
            BLOCKS.register("rotary_ore_classifier", RotaryOreClassifierBlock::new);
    public static final RegistryObject<WaterSettlingTankBlock> WATER_SETTLING_TANK =
            BLOCKS.register("water_settling_tank", WaterSettlingTankBlock::new);
    public static final RegistryObject<CharcoalRetortBlock> CHARCOAL_RETORT =
            BLOCKS.register("charcoal_retort", CharcoalRetortBlock::new);
    public static final RegistryObject<BloomeryFurnaceBlock> BLOOMERY_FURNACE =
            BLOCKS.register("bloomery_furnace", BloomeryFurnaceBlock::new);
    public static final RegistryObject<RefractoryKilnBlock> REFRACTORY_KILN =
            BLOCKS.register("refractory_kiln", RefractoryKilnBlock::new);
    public static final RegistryObject<IronForgeHearthBlock> IRON_FORGE_HEARTH =
            BLOCKS.register("iron_forge_hearth", IronForgeHearthBlock::new);
    // Steel IV-C inert assembly pieces: no pressure/steam capabilities or block entities.
    public static final RegistryObject<SteelBoilerStructuralBlock> BOILER_SHELL_PANEL =
            BLOCKS.register("boiler_shell_panel", () -> new SteelBoilerStructuralBlock(true));
    public static final RegistryObject<SteelBoilerStructuralBlock> BOILER_END_JOINT =
            BLOCKS.register("boiler_end_joint", () -> new SteelBoilerStructuralBlock(false));
    // Steel IV-C physically constructed vessel; entirely inert until Steam Age.
    public static final RegistryObject<SteelBoilerVesselBlock> STEEL_BOILER_VESSEL =
            BLOCKS.register("steel_boiler_vessel", SteelBoilerVesselBlock::new);

    // Steam 0-A: separate inert, physically supported service mounts; no capabilities or ticks.
    public static final RegistryObject<BoilerServiceFittingBlock> BOILER_WATER_INLET =
            BLOCKS.register("boiler_water_inlet", () -> new BoilerServiceFittingBlock(BoilerServiceFittingBlock.Role.WATER));
    public static final RegistryObject<BoilerServiceFittingBlock> BOILER_STEAM_OUTLET =
            BLOCKS.register("boiler_steam_outlet", () -> new BoilerServiceFittingBlock(BoilerServiceFittingBlock.Role.STEAM));
    public static final RegistryObject<BoilerServiceFittingBlock> BOILER_FIREBOX_MOUNT =
            BLOCKS.register("boiler_firebox_mount", () -> new BoilerServiceFittingBlock(BoilerServiceFittingBlock.Role.FIREBOX));
    public static final RegistryObject<BoilerServiceFittingBlock> BOILER_SAFETY_MOUNT =
            BLOCKS.register("boiler_safety_mount", () -> new BoilerServiceFittingBlock(BoilerServiceFittingBlock.Role.SAFETY));
    // Steel entry: three distinct JSON-formed machines, shared thermal backend.
    public static final RegistryObject<SteelProcessBlock> STEEL_COKE_OVEN =
            BLOCKS.register("steel_coke_oven", () -> new SteelProcessBlock("steel_coke_oven"));
    public static final RegistryObject<SteelProcessBlock> STEEL_BLAST_FURNACE =
            BLOCKS.register("steel_blast_furnace", () -> new SteelProcessBlock("steel_blast_furnace"));
    public static final RegistryObject<SteelProcessBlock> STEEL_REFINING_FURNACE =
            BLOCKS.register("steel_refining_furnace", () -> new SteelProcessBlock("steel_refining_furnace"));
    public static final RegistryObject<OreRoastingKilnBlock> ORE_ROASTING_KILN =
            BLOCKS.register("ore_roasting_kiln", OreRoastingKilnBlock::new);
    public static final RegistryObject<BronzeFoundryBlock> BRONZE_FOUNDRY =
            BLOCKS.register("bronze_foundry", BronzeFoundryBlock::new);
    public static final RegistryObject<BronzeCastingBasinBlock> BRONZE_CASTING_BASIN =
            BLOCKS.register("bronze_casting_basin", BronzeCastingBasinBlock::new);
    // Iron II-A: a stronger 8-tooth Gear V2 node, not a new power source.
    public static final RegistryObject<WroughtIronMediumGearBlock> WROUGHT_IRON_GEAR_MEDIUM =
            BLOCKS.register("wrought_iron_gear_medium", WroughtIronMediumGearBlock::new);
    public static final RegistryObject<WroughtIronLargeGearBlock> WROUGHT_IRON_GEAR_LARGE =
            BLOCKS.register("wrought_iron_gear_large", WroughtIronLargeGearBlock::new);
    // Iron II-B: reinforced axial and right-angle transmission.
    public static final RegistryObject<WroughtIronShaftBlock> WROUGHT_IRON_SHAFT =
            BLOCKS.register("wrought_iron_shaft", WroughtIronShaftBlock::new);
    public static final RegistryObject<WroughtIronGearboxBlock> WROUGHT_IRON_GEARBOX =
            BLOCKS.register("wrought_iron_gearbox", WroughtIronGearboxBlock::new);

    // Steel III: only stronger transmission, never extra source power.
    public static final RegistryObject<SteelMediumGearBlock> STEEL_GEAR_MEDIUM =
            BLOCKS.register("steel_gear_medium", SteelMediumGearBlock::new);
    public static final RegistryObject<SteelLargeGearBlock> STEEL_GEAR_LARGE =
            BLOCKS.register("steel_gear_large", SteelLargeGearBlock::new);
    public static final RegistryObject<SteelShaftBlock> STEEL_SHAFT =
            BLOCKS.register("steel_shaft", SteelShaftBlock::new);
    public static final RegistryObject<SteelGearboxBlock> STEEL_GEARBOX =
            BLOCKS.register("steel_gearbox", SteelGearboxBlock::new);

    // Iron III-A: heavy stock cutting, with a replaceable blade.
    public static final RegistryObject<IronStockShearBlock> IRON_STOCK_SHEAR =
            BLOCKS.register("iron_stock_shear", IronStockShearBlock::new);
    public static final RegistryObject<RivetedIronFrameBlock> RIVETED_IRON_FRAME =
            BLOCKS.register("riveted_iron_frame", RivetedIronFrameBlock::new);
    // Iron III-A: structural casing uses the established frame and iron plates.
    // It is an ordinary block with no special renderer, entity or ticker.
    public static final RegistryObject<Block> RIVETED_IRON_CASING =
            BLOCKS.register("riveted_iron_casing", () -> new Block(
                    BlockBehaviour.Properties.of().strength(5.0F, 6.0F)
                            .sound(SoundType.METAL).requiresCorrectToolForDrops()));
    // Iron 0D: simple factory construction blocks (no block entities/tickers).
    public static final RegistryObject<IronStructuralBeamBlock> IRON_STRUCTURAL_BEAM =
            BLOCKS.register("iron_structural_beam", IronStructuralBeamBlock::new);
    public static final RegistryObject<IronPlatePanelBlock> IRON_PLATE_PANEL =
            BLOCKS.register("iron_plate_panel", IronPlatePanelBlock::new);
    public static final RegistryObject<Block> IRON_MACHINE_FOUNDATION =
            BLOCKS.register("iron_machine_foundation", () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(5.0F, 6.0F).sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()));
    // Iron factory access: walkable, waterloggable open-grid decking.
    public static final RegistryObject<IronFloorGratingBlock> IRON_FLOOR_GRATING =
            BLOCKS.register("iron_floor_grating", IronFloorGratingBlock::new);
    // Vanilla fence topology/shape gives safe automatically connected catwalk rails.
    public static final RegistryObject<FenceBlock> IRON_CATWALK_RAILING =
            BLOCKS.register("iron_catwalk_railing", () -> new FenceBlock(
                    BlockBehaviour.Properties.of().strength(5.0F, 6.0F)
                            .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    // Iron service access: vanilla trapdoor redstone, manual hinge and waterlogging.
    public static final RegistryObject<IronServiceHatchBlock> IRON_SERVICE_HATCH =
            BLOCKS.register("iron_service_hatch", IronServiceHatchBlock::new);
    public static final RegistryObject<MechanicalInputModuleBlock> MECHANICAL_INPUT_MODULE = BLOCKS.register("mechanical_input_module", MechanicalInputModuleBlock::new);
    public static final RegistryObject<MultiblockItemPortBlock> MULTIBLOCK_ITEM_INPUT =
            BLOCKS.register(
                    "multiblock_item_input",
                    () -> new MultiblockItemPortBlock(
                            MultiblockItemPortBlock.PortMode.INPUT
                    )
            );
    public static final RegistryObject<MultiblockItemPortBlock> MULTIBLOCK_ITEM_OUTPUT =
            BLOCKS.register(
                    "multiblock_item_output",
                    () -> new MultiblockItemPortBlock(
                            MultiblockItemPortBlock.PortMode.COMBINED_OUTPUT
                    )
            );
    public static final RegistryObject<MultiblockItemPortBlock> MULTIBLOCK_ITEM_PRIMARY_OUTPUT =
            BLOCKS.register(
                    "multiblock_item_primary_output",
                    () -> new MultiblockItemPortBlock(
                            MultiblockItemPortBlock.PortMode.PRIMARY_OUTPUT
                    )
            );
    public static final RegistryObject<MultiblockItemPortBlock> MULTIBLOCK_ITEM_BYPRODUCT_OUTPUT =
            BLOCKS.register(
                    "multiblock_item_byproduct_output",
                    () -> new MultiblockItemPortBlock(
                            MultiblockItemPortBlock.PortMode.BYPRODUCT_OUTPUT
                    )
            );
    public static final RegistryObject<MultiblockFluidInputBlock> MULTIBLOCK_FLUID_INPUT = BLOCKS.register("multiblock_fluid_input", MultiblockFluidInputBlock::new);
    public static final RegistryObject<MultiblockFluidOutputBlock> MULTIBLOCK_FLUID_OUTPUT = BLOCKS.register("multiblock_fluid_output", MultiblockFluidOutputBlock::new);
    public static final RegistryObject<WaterPipeBlock> WATER_PIPE = BLOCKS.register("water_pipe", WaterPipeBlock::new);
    // Generated physical 45-degree sections, never separately obtainable items.
    public static final RegistryObject<DiagonalWaterPipeBlock> DIAGONAL_WATER_PIPE =
            registerBlockWithoutBlockItem("diagonal_water_pipe", DiagonalWaterPipeBlock::new);
    public static final RegistryObject<BronzeAirDuctBlock> BRONZE_AIR_DUCT = BLOCKS.register("bronze_air_duct", BronzeAirDuctBlock::new);
    public static final RegistryObject<SteamPipeBlock> STEAM_PIPE = BLOCKS.register("steam_pipe", SteamPipeBlock::new);
    public static final RegistryObject<RupturedSteamPipeBlock> RUPTURED_STEAM_PIPE = BLOCKS.register("ruptured_steam_pipe", RupturedSteamPipeBlock::new);
    public static final RegistryObject<MechanicalWaterPumpBlock> MECHANICAL_WATER_PUMP = BLOCKS.register("mechanical_water_pump", MechanicalWaterPumpBlock::new);
    public static final RegistryObject<MechanicalTransferArmBlock> MECHANICAL_TRANSFER_ARM = BLOCKS.register("mechanical_transfer_arm", MechanicalTransferArmBlock::new);
    public static final RegistryObject<MechanicalTransferArmUpperBlock> MECHANICAL_TRANSFER_ARM_UPPER =
            registerBlockWithoutBlockItem(
                    "mechanical_transfer_arm_upper",
                    MechanicalTransferArmUpperBlock::new
            );
    public static final RegistryObject<CopperFluidTankBlock> COPPER_FLUID_TANK = BLOCKS.register("copper_fluid_tank", CopperFluidTankBlock::new);
    public static final RegistryObject<PulleyBlock_wood> PULLEY_SMALL_WOOD = BLOCKS.register("pulley_small_wood", () -> new PulleyBlock_wood(8));
    public static final RegistryObject<PulleyBlock_wood> PULLEY_LARGE_WOOD = BLOCKS.register("pulley_large_wood", () -> new PulleyBlock_wood(16));
    public static final RegistryObject<ConveyorRollerBlock> CONVEYOR_ROLLER = BLOCKS.register("conveyor_roller", ConveyorRollerBlock::new);
    public static final RegistryObject<GearboxBlock_wood> GEARBOX_WOOD = BLOCKS.register("gearbox_wood", GearboxBlock_wood::new);
    public static final RegistryObject<CustomGearboxBlock_wood> CUSTOM_GEARBOX_WOOD = BLOCKS.register("custom_gearbox_wood", CustomGearboxBlock_wood::new);
    public static final RegistryObject<ItemBeltBlock> ITEM_BELT_BLOCK = registerBlockWithoutBlockItem("item_belt_block", ItemBeltBlock::new);
    public static final RegistryObject<WaterWheelBlock> WATER_WHEEL_SMALL = BLOCKS.register("water_wheel_small", () -> new WaterWheelBlock(false));
    public static final RegistryObject<WaterWheelBlock> WATER_WHEEL_LARGE = BLOCKS.register("water_wheel_large", () -> new WaterWheelBlock(true));
    public static final RegistryObject<WaterWheelFillerBlock> WATER_WHEEL_FILLER = registerBlockWithoutBlockItem("water_wheel_filler", WaterWheelFillerBlock::new);






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
