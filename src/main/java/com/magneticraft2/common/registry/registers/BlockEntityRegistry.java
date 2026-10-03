package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.blockentity.general.*;
import com.magneticraft2.common.blockentity.stage.copper.LargeGearBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.LargeGearWithHandleBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.ConveyorRollerBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.ItemBeltBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.MediumGearBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.PulleyBlockEntity_wood;
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
    public static final RegistryObject<BlockEntityType<ConveyorRollerBlockEntity>> CONVEYOR_ROLLER_BE =
            BLOCK_ENTITIES.register("conveyor_roller", () ->
                    BlockEntityType.Builder.of(
                            ConveyorRollerBlockEntity::new,
                            BlockRegistry.CONVEYOR_ROLLER.get()
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
