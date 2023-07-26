package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.world.modiifiers.magnetite_oreModifier;
import com.magneticraft2.common.world.modiifiers.stickModifier;
import com.magneticraft2.common.world.modiifiers.stonepebbleModifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 26-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BiomeModifier {
    private static final Logger LOGGER = LogManager.getLogger("MGC2-BiomeModifier");
    static DeferredRegister<Codec<? extends net.minecraftforge.common.world.BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MOD_ID);
    public static void init(IEventBus eventBus){
        BIOME_MODIFIER_SERIALIZERS.register(eventBus);
    }
    //Biomemodifiers after this line

    public static RegistryObject<Codec<stonepebbleModifier>> stonepebblemodifier = BIOME_MODIFIER_SERIALIZERS.register("stonepebble", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(stonepebbleModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(stonepebbleModifier::feature)
            ).apply(builder, stonepebbleModifier::new)));

    public static RegistryObject<Codec<magnetite_oreModifier>> magnetite_ore = BIOME_MODIFIER_SERIALIZERS.register("magnetite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(magnetite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(magnetite_oreModifier::feature)
            ).apply(builder, magnetite_oreModifier::new)));
    public static RegistryObject<Codec<stickModifier>> stickmodifier = BIOME_MODIFIER_SERIALIZERS.register("stick", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(stickModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(stickModifier::feature)
            ).apply(builder, stickModifier::new)));
}
