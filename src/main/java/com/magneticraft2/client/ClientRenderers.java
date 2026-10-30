package com.magneticraft2.client;

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
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Connects block entities and entity types to their client renderers. */
public final class ClientRenderers {
    private ClientRenderers() {}

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
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
}
