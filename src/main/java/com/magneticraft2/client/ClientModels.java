package com.magneticraft2.client;

import com.magneticraft2.client.model.MultiBlockModelLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.client.event.ModelEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/** Registers models used directly by renderers and the multiblock geometry loader. */
public final class ClientModels {
    private static final Logger LOGGER = LogManager.getLogger("magneticraft2_clientmodels");

    private ClientModels() {}

    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        LOGGER.info("Models are being registered!");

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        String folderPath = "models/multiblock";

        // BER-only models need standalone baked IDs, not only blockstate models.
        event.register(new ResourceLocation(MOD_ID, "block/shaft_wood"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_input_module_housing"));
        event.register(new ResourceLocation(MOD_ID, "block/pulley_small_wood"));
        event.register(new ResourceLocation(MOD_ID, "block/pulley_large_wood"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_water_pump_drive"));
        event.register(new ResourceLocation(MOD_ID, "block/gear_medium_wood"));
        event.register(new ResourceLocation(MOD_ID, "multiblock/mechanical_sifter_crank_arm"));
        event.register(new ResourceLocation(MOD_ID, "multiblock/mechanical_sifter_connecting_rod"));
        event.register(new ResourceLocation(MOD_ID, "multiblock/mechanical_sifter_follower"));
        event.register(new ResourceLocation(MOD_ID, "multiblock/mechanical_sifter_guide_rails"));
        event.register(new ResourceLocation(MOD_ID, "multiblock/mechanical_sifter_slide_shoes"));
        // Bake wear variants explicitly so they survive resource reloads.
        for (int stage = 1; stage <= 5; stage++) {
            event.register(new ResourceLocation(
                    MOD_ID, "multiblock/mechanical_sifter_upper_tray_wear_" + stage));
        }
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_transfer_arm_drive_gear"));
        event.register(new ResourceLocation(MOD_ID, "block/mechanical_transfer_arm_drive_shaft"));
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
            String modelPath = resourceLocation.getPath().substring("models/".length(), resourceLocation.getPath().length() - ".json".length());
            ResourceLocation modelResourceLocation = new ResourceLocation(resourceLocation.getNamespace(), modelPath);

            event.register(modelResourceLocation);
            LOGGER.info("Registered model: " + modelResourceLocation);
        }
    }

    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("multiblock", new MultiBlockModelLoader());
    }
}
