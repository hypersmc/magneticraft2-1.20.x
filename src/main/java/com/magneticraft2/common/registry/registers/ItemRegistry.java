package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.item.stage.stone.ceramicPot;
import com.magneticraft2.common.item.stage.stone.clayPot;
import com.magneticraft2.common.magneticraft2;
import net.minecraft.world.item.Item;
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
public class ItemRegistry {
    private static final Logger LOGGER = LogManager.getLogger("MGC2-ItemRegistry");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static void init(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
    //Items after this line

    public static final RegistryObject<clayPot> item_clay_pot = ITEMS.register("clay_pot", clayPot::new);
    public static final RegistryObject<ceramicPot> item_ceramic_pot = ITEMS.register("ceramic_pot", ceramicPot::new);
}
