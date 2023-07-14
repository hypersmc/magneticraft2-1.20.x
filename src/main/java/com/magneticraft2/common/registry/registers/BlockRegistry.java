package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.block.stage.stone.PitKilnBlock;
import com.magneticraft2.common.magneticraft2;
import net.minecraft.world.level.block.Block;
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
public class BlockRegistry {
    private static final Logger LOGGER = LogManager.getLogger("MGC2-BlockRegistry");
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static void init(IEventBus eventBus){
        BLOCKS.register(eventBus);
    }
    //Blocks after this line

    public static final RegistryObject<PitKilnBlock> PitKilnblock = BLOCKS.register("pitkilnblock", PitKilnBlock::new);

}
