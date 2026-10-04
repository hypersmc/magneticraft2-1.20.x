package com.magneticraft2.client;

import com.magneticraft2.client.model.MultiBlockModelLoader;
import com.magneticraft2.client.render.blocks.*;
import com.magneticraft2.client.render.blocks.stage.copper.LargeGearBlock_woodRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.LargeGearWithHandleBlock_woodRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.ClutchBlockEntity_woodRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.ConveyorRollerBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.GearboxBlockEntity_woodRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.CustomGearboxBlockEntity_woodRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.MediumGearBlockEntity_woodRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.PulleyBlockEntity_woodRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.OverloadDisconnectBlockEntity_woodRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.MechanicalBrakeBlockEntity_woodRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.MechanicalBellowsBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.FlywheelBlockEntity_woodRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.MechanicalOreWasherBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.MechanicalWaterPumpBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.MechanicalTransferArmBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.CopperFluidTankBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.MechanicalInputModuleBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.MechanicalSifterBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.CrankBlockEntity_woodRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.ShaftBlockEntity_woodRenderer;
import com.magneticraft2.client.render.blocks.stage.copper.WaterWheelBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.stone.PitKilnBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.stone.PrimitiveAnvilBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.stone.PrimitiveFurnaceBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.stone.PrimitiveFurnaceNoGUIBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.stone.PrimitiveGrinderBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.stone.PrimitiveStorageCellarBlockRenderer;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.EntitiesRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 *
 * @author JumpWatch on 30-06-2023
 * @Project mgc2-1.20
 * @version 1.0.0
 * Clientsetup is responsible for registering client-specific configurations, renderers,
 * models, and geometry loaders for the mod.
 *
 * This class subscribes to relevant Forge events to handle the following tasks:
 *
 * - Initializing client-specific settings during the client setup phase.
 * - Registering block entity renderers for visual representation of block entities.
 * - Registering layer definitions required for models with custom rendering layers.
 * - Registering additional models from the mod's resource folder.
 * - Registering custom geometry loaders to enhance or extend rendering functionality.
 *
 * The event handlers implemented in this class are automatically invoked during the
 * mod loading process on the client side.
 */
@Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Clientsetup {
    private static final Logger LOGGER = LogManager.getLogger("magneticraft2_clientsetup");
    public static void init(FMLClientSetupEvent e){

    }
    @SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {
        LOGGER.info("Renders are being registered!");
        event.registerBlockEntityRenderer(BlockEntityRegistry.PitKilnblockEntity.get(), PitKilnBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.projectortestBlockEntity.get(), ProjectorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.blueprintmultiblockentity.get(), BlueprintMultiblockRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.primitivefurnacemultiblockentity.get(), PrimitiveFurnaceBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.primitivefurnacemultiblockentity_nogui.get(), PrimitiveFurnaceNoGUIBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.storagecellarblockentity.get(), PrimitiveStorageCellarBlockRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.Primitive_anvilEntity.get(), PrimitiveAnvilBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.primitivegrinderbmultiblockentity.get(), PrimitiveGrinderBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.GEAR_LARGE_WITH_HANDLE_BE_WOOD.get(), LargeGearWithHandleBlock_woodRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.GEAR_LARGE_BE_WOOD.get(), LargeGearBlock_woodRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.GEAR_MEDIUM_BE_WOOD.get(), MediumGearBlockEntity_woodRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.SHAFT_BE_WOOD.get(), ShaftBlockEntity_woodRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.CLUTCH_BE_WOOD.get(), ClutchBlockEntity_woodRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.OVERLOAD_DISCONNECT_BE_WOOD.get(), OverloadDisconnectBlockEntity_woodRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.MECHANICAL_BRAKE_BE_WOOD.get(), MechanicalBrakeBlockEntity_woodRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.CRANK_BE_WOOD.get(), CrankBlockEntity_woodRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.MECHANICAL_BELLOWS_BE.get(), MechanicalBellowsBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.FLYWHEEL_BE_WOOD.get(), FlywheelBlockEntity_woodRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.MECHANICAL_ORE_WASHER_BE.get(), MechanicalOreWasherBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.MECHANICAL_WATER_PUMP_BE.get(), MechanicalWaterPumpBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.MECHANICAL_TRANSFER_ARM_BE.get(), MechanicalTransferArmBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.COPPER_FLUID_TANK_BE.get(), CopperFluidTankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.MECHANICAL_INPUT_MODULE_BE.get(), MechanicalInputModuleBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.MECHANICAL_SIFTER_BE.get(), MechanicalSifterBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.PULLEY_BE_WOOD.get(), PulleyBlockEntity_woodRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.CONVEYOR_ROLLER_BE.get(), ConveyorRollerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.GEARBOX_BE_WOOD.get(), GearboxBlockEntity_woodRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.CUSTOM_GEARBOX_BE_WOOD.get(), CustomGearboxBlockEntity_woodRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.WATER_WHEEL_BE.get(), WaterWheelBlockEntityRenderer::new);
        event.registerEntityRenderer(EntitiesRegistry.BELT_COLLISION.get(), NoopRenderer::new);
    }
    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        LOGGER.info("Models are being registered!");

    }
    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        LOGGER.info("Models are being registered!");

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        String folderPath = "models/multiblock";

        // Get all resources in the multiblock folder
        // BER-only block models are requested directly from ModelManager by
        // ResourceLocation. Register them as top-level models so the baked model
        // lookup cannot fall back to an empty/missing entry.
        event.register(new ResourceLocation(MOD_ID, "block/shaft_wood"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_input_module_housing"));
        event.register(new ResourceLocation(MOD_ID, "block/pulley_small_wood"));
        event.register(new ResourceLocation(MOD_ID, "block/pulley_large_wood"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_water_pump_drive"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_transfer_arm_gear"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_transfer_arm_turret"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_transfer_arm_lower_arm"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_transfer_arm_forearm"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_transfer_arm_claw_body"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_transfer_arm_claw_left"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_transfer_arm_claw_right"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_transfer_arm_button_orange"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_transfer_arm_button_blue"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_transfer_arm_filter_allow"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_transfer_arm_filter_deny"));

        for (ResourceLocation resourceLocation : resourceManager.listResources(folderPath, path -> path.toString().endsWith(".json")).keySet()) {
            // Remove the "models/" prefix and ".json" suffix for registering the model
            String modelPath = resourceLocation.getPath().substring("models/".length(), resourceLocation.getPath().length() - ".json".length());
            ResourceLocation modelResourceLocation = new ResourceLocation(resourceLocation.getNamespace(), modelPath);

            // Register the model
            event.register(modelResourceLocation);
            LOGGER.info("Registered model: " + modelResourceLocation);
        }
    }

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        LOGGER.info("Registering custom geometry loaders!");
        event.register("multiblock", new MultiBlockModelLoader());
    }
}
