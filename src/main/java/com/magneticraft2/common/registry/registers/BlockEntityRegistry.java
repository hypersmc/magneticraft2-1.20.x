package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.blockentity.general.*;
import com.magneticraft2.common.blockentity.stage.copper.LargeGearBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.LargeGearWithHandleBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.ClutchBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.ConveyorRollerBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.GearboxBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.CustomGearboxBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalBrakeBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalBellowsBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalOreWasherBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalSifterBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalInputModuleBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.FlywheelBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.CrankBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.ItemBeltBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.MediumGearBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.PulleyBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.OverloadDisconnectBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.ShaftBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.WaterWheelBlockEntity;
import com.magneticraft2.common.blockentity.stage.stone.*;
import com.magneticraft2.common.magneticraft2;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 30-06-2023
 * @Project mgc2-1.20
* @version 1.0.0
 */
@Mod.EventBusSubscriber(modid = magneticraft2.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BlockEntityRegistry {
    private static final Logger LOGGER = LogManager.getLogger("MGC2-BlockEntityRegistry");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static void init(IEventBus eventBus){
        BLOCK_ENTITIES.register(eventBus);
    }

    //Block Entities after this line
    //Should sort this for each stage

    public static final RegistryObject<BlockEntityType<PitKilnBlockEntity>> PitKilnblockEntity = BLOCK_ENTITIES.register("pitkilnblockentity", () -> BlockEntityType.Builder.of(PitKilnBlockEntity::new, BlockRegistry.PitKilnblock.get()).build(null));
    public static final RegistryObject<BlockEntityType<stonepebbleBlockEntity>> stonepebbleBlockEntity = BLOCK_ENTITIES.register("stonepebbleblockentity", () -> BlockEntityType.Builder.of(stonepebbleBlockEntity::new, BlockRegistry.stonepebble.get()).build(null));
    public static final RegistryObject<BlockEntityType<projectortestBlockEntity>> projectortestBlockEntity = BLOCK_ENTITIES.register("projectortestblockentity", () -> BlockEntityType.Builder.of(projectortestBlockEntity::new, BlockRegistry.protectortest.get()).build(null));
    public static final RegistryObject<BlockEntityType<testpowermodule>> testpowermodule = BLOCK_ENTITIES.register("testpowermodule", () -> BlockEntityType.Builder.of(testpowermodule::new, BlockRegistry.testpowermoduleblock.get()).build(null));
    public static final RegistryObject<BlockEntityType<testpollutionblockentity>> testpollutionblock = BLOCK_ENTITIES.register("testpollutionblock", () -> BlockEntityType.Builder.of(testpollutionblockentity::new, BlockRegistry.testpollutionblock.get()).build(null));
    public static final RegistryObject<BlockEntityType<Multiblockfiller_tile>> multiblockfillerBlockEntity = BLOCK_ENTITIES.register("multiblock_filler", () -> BlockEntityType.Builder.of(Multiblockfiller_tile::new, BlockRegistry.multiblockfiller.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlueprintMultiblockEntity>> blueprintmultiblockentity = BLOCK_ENTITIES.register("blueprint_multiblock", () -> BlockEntityType.Builder.of(BlueprintMultiblockEntity::new, BlockRegistry.blueprintmultiblock.get()).build(null));
    public static final RegistryObject<BlockEntityType<PrimitiveStorageCellarMultiblockEntity>> storagecellarblockentity = BLOCK_ENTITIES.register("primitivestoragecellar_multiblock", () -> BlockEntityType.Builder.of(PrimitiveStorageCellarMultiblockEntity::new, BlockRegistry.primitivestoragecellarmultiblock.get()).build(null));
    public static final RegistryObject<BlockEntityType<PrimitiveFurnaceMultiblockEntity>> primitivefurnacemultiblockentity = BLOCK_ENTITIES.register("primitivefurnace_multiblock", () -> BlockEntityType.Builder.of(PrimitiveFurnaceMultiblockEntity::new, BlockRegistry.primitivefurnacemultiblock.get()).build(null));
    public static final RegistryObject<BlockEntityType<PrimitiveFurnaceMultiblockEntity_nogui>> primitivefurnacemultiblockentity_nogui = BLOCK_ENTITIES.register("primitivefurnace_multiblock_nogui", () -> BlockEntityType.Builder.of(PrimitiveFurnaceMultiblockEntity_nogui::new, BlockRegistry.primitivefurnace_multiblock_nogui.get()).build(null));
    public static final RegistryObject<BlockEntityType<BellowsMultiblockModuleEntity>> bellowsmultiblockmoduleentity = BLOCK_ENTITIES.register("bellows_multiblock_module", () -> BlockEntityType.Builder.of(BellowsMultiblockModuleEntity::new, BlockRegistry.bellowsmultiblockmodule.get()).build(null));
    public static final RegistryObject<BlockEntityType<PrimitiveGrinderBMultiblockEntity>> primitivegrinderbmultiblockentity = BLOCK_ENTITIES.register("primitive_grinder_bmultiblock", () -> BlockEntityType.Builder.of(PrimitiveGrinderBMultiblockEntity::new, BlockRegistry.primitive_grinder_bmultiblock.get()).build(null));

    //Stone
    public static final RegistryObject<BlockEntityType<Primitive_anvilEntity>> Primitive_anvilEntity = BLOCK_ENTITIES.register("primitive_anvil", () -> BlockEntityType.Builder.of(com.magneticraft2.common.blockentity.stage.stone.Primitive_anvilEntity::new, BlockRegistry.Primitive_anvillBlock.get()).build(null));

    //Copper

    public static final RegistryObject<BlockEntityType<MediumGearBlockEntity_wood>> GEAR_MEDIUM_BE_WOOD = BLOCK_ENTITIES.register("gear_medium_wood", () -> BlockEntityType.Builder.of(MediumGearBlockEntity_wood::new, BlockRegistry.GEAR_MEDIUM_WOOD.get()).build(null));
    public static final RegistryObject<BlockEntityType<LargeGearBlockEntity_wood>> GEAR_LARGE_BE_WOOD = BLOCK_ENTITIES.register("gear_large_wood", () -> BlockEntityType.Builder.of(LargeGearBlockEntity_wood::new, BlockRegistry.GEAR_LARGE_WOOD.get()).build(null));
    public static final RegistryObject<BlockEntityType<LargeGearWithHandleBlockEntity_wood>> GEAR_LARGE_WITH_HANDLE_BE_WOOD = BLOCK_ENTITIES.register("gear_large_wood_with_handle", () -> BlockEntityType.Builder.of(LargeGearWithHandleBlockEntity_wood::new, BlockRegistry.GEAR_LARGE_WITH_HANDLE_WOOD.get()).build(null));
    public static final RegistryObject<BlockEntityType<ShaftBlockEntity_wood>> SHAFT_BE_WOOD = BLOCK_ENTITIES.register("shaft_wood", () -> BlockEntityType.Builder.of(ShaftBlockEntity_wood::new, BlockRegistry.SHAFT_WOOD.get()).build(null));
    public static final RegistryObject<BlockEntityType<ClutchBlockEntity_wood>> CLUTCH_BE_WOOD = BLOCK_ENTITIES.register("clutch_wood", () -> BlockEntityType.Builder.of(ClutchBlockEntity_wood::new, BlockRegistry.CLUTCH_WOOD.get()).build(null));
    public static final RegistryObject<BlockEntityType<OverloadDisconnectBlockEntity_wood>> OVERLOAD_DISCONNECT_BE_WOOD = BLOCK_ENTITIES.register("overload_disconnect_wood", () -> BlockEntityType.Builder.of(OverloadDisconnectBlockEntity_wood::new, BlockRegistry.OVERLOAD_DISCONNECT_WOOD.get()).build(null));
    public static final RegistryObject<BlockEntityType<MechanicalBrakeBlockEntity_wood>> MECHANICAL_BRAKE_BE_WOOD = BLOCK_ENTITIES.register("mechanical_brake_wood", () -> BlockEntityType.Builder.of(MechanicalBrakeBlockEntity_wood::new, BlockRegistry.MECHANICAL_BRAKE_WOOD.get()).build(null));
    public static final RegistryObject<BlockEntityType<CrankBlockEntity_wood>> CRANK_BE_WOOD = BLOCK_ENTITIES.register("crank_wood", () -> BlockEntityType.Builder.of(CrankBlockEntity_wood::new, BlockRegistry.CRANK_WOOD.get()).build(null));
    public static final RegistryObject<BlockEntityType<MechanicalBellowsBlockEntity>> MECHANICAL_BELLOWS_BE = BLOCK_ENTITIES.register("mechanical_bellows", () -> BlockEntityType.Builder.of(MechanicalBellowsBlockEntity::new, BlockRegistry.MECHANICAL_BELLOWS.get()).build(null));
    public static final RegistryObject<BlockEntityType<FlywheelBlockEntity_wood>> FLYWHEEL_BE_WOOD = BLOCK_ENTITIES.register("flywheel_wood", () -> BlockEntityType.Builder.of(FlywheelBlockEntity_wood::new, BlockRegistry.FLYWHEEL_WOOD.get()).build(null));
    public static final RegistryObject<BlockEntityType<MechanicalOreWasherBlockEntity>> MECHANICAL_ORE_WASHER_BE = BLOCK_ENTITIES.register("mechanical_ore_washer", () -> BlockEntityType.Builder.of(MechanicalOreWasherBlockEntity::new, BlockRegistry.MECHANICAL_ORE_WASHER.get()).build(null));
    public static final RegistryObject<BlockEntityType<MechanicalSifterBlockEntity>> MECHANICAL_SIFTER_BE = BLOCK_ENTITIES.register("mechanical_sifter", () -> BlockEntityType.Builder.of(MechanicalSifterBlockEntity::new, BlockRegistry.MECHANICAL_SIFTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<MechanicalInputModuleBlockEntity>> MECHANICAL_INPUT_MODULE_BE = BLOCK_ENTITIES.register("mechanical_input_module", () -> BlockEntityType.Builder.of(MechanicalInputModuleBlockEntity::new, BlockRegistry.MECHANICAL_INPUT_MODULE.get()).build(null));
    public static final RegistryObject<BlockEntityType<ConveyorRollerBlockEntity>> CONVEYOR_ROLLER_BE =
            BLOCK_ENTITIES.register("conveyor_roller", () ->
                    BlockEntityType.Builder.of(
                            ConveyorRollerBlockEntity::new,
                            BlockRegistry.CONVEYOR_ROLLER.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<GearboxBlockEntity_wood>> GEARBOX_BE_WOOD =
            BLOCK_ENTITIES.register("gearbox_wood", () ->
                    BlockEntityType.Builder.of(
                            GearboxBlockEntity_wood::new,
                            BlockRegistry.GEARBOX_WOOD.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<CustomGearboxBlockEntity_wood>> CUSTOM_GEARBOX_BE_WOOD =
            BLOCK_ENTITIES.register("custom_gearbox_wood", () ->
                    BlockEntityType.Builder.of(
                            CustomGearboxBlockEntity_wood::new,
                            BlockRegistry.CUSTOM_GEARBOX_WOOD.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<ItemBeltBlockEntity>> ITEM_BELT_BE =
            BLOCK_ENTITIES.register("item_belt_block", () ->
                    BlockEntityType.Builder.of(
                            ItemBeltBlockEntity::new,
                            BlockRegistry.ITEM_BELT_BLOCK.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<WaterWheelBlockEntity>> WATER_WHEEL_BE =
            BLOCK_ENTITIES.register("water_wheel", () ->
                    BlockEntityType.Builder.of(
                            WaterWheelBlockEntity::new,
                            BlockRegistry.WATER_WHEEL_SMALL.get(),
                            BlockRegistry.WATER_WHEEL_LARGE.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<PulleyBlockEntity_wood>> PULLEY_BE_WOOD = BLOCK_ENTITIES.register(
            "pulley_wood",
            () -> BlockEntityType.Builder.of(
                    PulleyBlockEntity_wood::new,
                    BlockRegistry.PULLEY_SMALL_WOOD.get(),
                    BlockRegistry.PULLEY_LARGE_WOOD.get()
            ).build(null));

}
