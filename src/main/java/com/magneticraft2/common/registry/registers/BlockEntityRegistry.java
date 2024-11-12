package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.blockentity.general.*;
import com.magneticraft2.common.blockentity.stage.stone.PitKilnBlockEntity;
import com.magneticraft2.common.magneticraft2;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * v1.0.0
 */
@Mod.EventBusSubscriber(modid = magneticraft2.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BlockEntityRegistry {
    private static final Logger LOGGER = LogManager.getLogger("MGC2-BlockEntityRegistry");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static void init(IEventBus eventBus){
        BLOCK_ENTITIES.register(eventBus);
    }
    //Block Entities after this line
    public static final RegistryObject<BlockEntityType<PitKilnBlockEntity>> PitKilnblockEntity = BLOCK_ENTITIES.register("pitkilnblockentity", () -> BlockEntityType.Builder.of(PitKilnBlockEntity::new, BlockRegistry.PitKilnblock.get()).build(null));
    public static final RegistryObject<BlockEntityType<stonepebbleBlockEntity>> stonepebbleBlockEntity = BLOCK_ENTITIES.register("stonepebbleblockentity", () -> BlockEntityType.Builder.of(stonepebbleBlockEntity::new, BlockRegistry.stonepebble.get()).build(null));
    public static final RegistryObject<BlockEntityType<projectortestBlockEntity>> projectortestBlockEntity = BLOCK_ENTITIES.register("projectortestblockentity", () -> BlockEntityType.Builder.of(projectortestBlockEntity::new, BlockRegistry.protectortest.get()).build(null));
    public static final RegistryObject<BlockEntityType<testpowermodule>> testpowermodule = BLOCK_ENTITIES.register("testpowermodule", () -> BlockEntityType.Builder.of(testpowermodule::new, BlockRegistry.testpowermoduleblock.get()).build(null));
    public static final RegistryObject<BlockEntityType<testpollutionblockentity>> testpollutionblock = BLOCK_ENTITIES.register("testpollutionblock", () -> BlockEntityType.Builder.of(testpollutionblockentity::new, BlockRegistry.testpollutionblock.get()).build(null));
    public static final RegistryObject<BlockEntityType<Multiblockfiller_tile>> multiblockfillerBlockEntity = BLOCK_ENTITIES.register("multiblock_filler", () -> BlockEntityType.Builder.of(Multiblockfiller_tile::new, BlockRegistry.multiblockfiller.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlueprintMultiblockEntity>> blueprintmultiblockentity = BLOCK_ENTITIES.register("blueprint_multiblock", () -> BlockEntityType.Builder.of(BlueprintMultiblockEntity::new, BlockRegistry.blueprintmultiblock.get()).build(null));
}
