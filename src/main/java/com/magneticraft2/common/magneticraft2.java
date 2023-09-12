package com.magneticraft2.common;

import com.magneticraft2.client.Clientsetup;
import com.magneticraft2.common.registry.registers.ContainerAndScreenRegistry;
import com.magneticraft2.common.registry.FinalRegistry;
import com.magneticraft2.common.systems.Blueprint.core.BlueprintManager;
import com.magneticraft2.common.systems.HEAT.CapabilityHeat;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockManager;
import com.magneticraft2.common.systems.PRESSURE.CapabilityPressure;
import com.magneticraft2.common.systems.WATT.CapabilityWatt;
import com.magneticraft2.common.systems.mgc2Network;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import com.magneticraft2.compatibility.TOP.TOPCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.GeckoLib;

/**
 * @author JumpWatch on 08-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
@Mod(magneticraft2.MOD_ID)
public class magneticraft2 {
    public static final Logger LOGGER = LogManager.getLogger("Magneticraft2");
    public static final Logger UELOGGER = LogManager.getLogger("UnknownEntity");
    public static final Logger MCLOGGER = LogManager.getLogger("MagnetiCore");
    public static final String MOD_ID = "magneticraft2";

    public magneticraft2(){
        //this is just for fun and giggles :)
        LOGGER.info("Do we have any core mod?");
        UELOGGER.error("Ȗᾗᾄвłἔ ҭὄ ἷḋἔᾗҭἷғẏ ṩἔłғ");
        LOGGER.info("Who is UnknownEntity?");
        UELOGGER.error("ἝȒȒȒȒἝȒὋȒ");
        LOGGER.info("UnknownEntity Please do self check");
        UELOGGER.error("ἝȒȒȒȒἝȒὋȒ ἝȒȒȒȒἝȒὋȒ ἝȒȒȒȒἝȒὋȒ ṩἔłғ ƈђἔƈќ ғᾄἷłἔḋ");
        LOGGER.warn("Scanning UnknownEntity for name.");
        LOGGER.info("Name found! UnknownEntity is now named: MagnetiCore");
        MCLOGGER.error("Ȗᾗќᾗὄᾧᾗ ṩƈᾄᾗ ὄᾗ ṩἔłғ. Ἷᾗἷҭἷᾄҭἷᾗʛ ṩђὗҭḋὄᾧᾗ ṩἔqὗἔᾗƈἔ");
        LOGGER.error("No core yet.");
        //end of fun and giggles
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        GeckoLib.initialize();
        FinalRegistry.register();
        modEventBus.addListener(this::preinit);
        modEventBus.addListener(this::registerCapabilities);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ContainerAndScreenRegistry::Screen);
        MinecraftForge.EVENT_BUS.register(this);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(Clientsetup::init));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Magneticraft2ConfigCommon.SPEC, "magneticraft2-common.toml");
        //modEventBus.addListener(FinalRegistry::gatherData);
    }

    public void preinit(final FMLCommonSetupEvent event){
        mgc2Network.init();
        MultiblockManager.loadMultiblocks(MOD_ID, Minecraft.getInstance().getResourceManager());
        BlueprintManager.loadBlueprints(MOD_ID, Minecraft.getInstance().getResourceManager());
        if (ModList.get().isLoaded("theoneprobe")){
            TOPCompatibility.register();
            LOGGER.info("The one probe compatibility done!");
        }
        event.enqueueWork(() ->
        {
//            ConfiguredFeatureMGC2.registerConfiguredFeatures();
//            WorldGen.registerPlacedFeatures();
        });
    }
    public void registerCapabilities(RegisterCapabilitiesEvent event){
        CapabilityHeat.register(event);
        CapabilityPressure.register(event);
        CapabilityWatt.register(event);
    }
}
