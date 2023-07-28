package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.world.modifiers.*;
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
    public static RegistryObject<Codec<stickModifier>> stickmodifier = BIOME_MODIFIER_SERIALIZERS.register("stick", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(stickModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(stickModifier::feature)
            ).apply(builder, stickModifier::new)));



    //ores
    public static RegistryObject<Codec<anthracite_oreModifier>> anthracite_ore = BIOME_MODIFIER_SERIALIZERS.register("anthracite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(anthracite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(anthracite_oreModifier::feature)
            ).apply(builder, anthracite_oreModifier::new)));
    public static RegistryObject<Codec<apatite_oreModifier>> apatite_ore = BIOME_MODIFIER_SERIALIZERS.register("apatite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(apatite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(apatite_oreModifier::feature)
            ).apply(builder, apatite_oreModifier::new)));
    public static RegistryObject<Codec<bauxite_oreModifier>> bauxite_ore = BIOME_MODIFIER_SERIALIZERS.register("bauxite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(bauxite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(bauxite_oreModifier::feature)
            ).apply(builder, bauxite_oreModifier::new)));
    public static RegistryObject<Codec<cassiterite_oreModifier>> cassiterite_ore = BIOME_MODIFIER_SERIALIZERS.register("cassiterite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(cassiterite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(cassiterite_oreModifier::feature)
            ).apply(builder, cassiterite_oreModifier::new)));
    public static RegistryObject<Codec<chalcocite_oreModifier>> chalcocite_ore = BIOME_MODIFIER_SERIALIZERS.register("chalcocite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(chalcocite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(chalcocite_oreModifier::feature)
            ).apply(builder, chalcocite_oreModifier::new)));
    public static RegistryObject<Codec<chromite_oreModifier>> chromite_ore = BIOME_MODIFIER_SERIALIZERS.register("chromite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(chromite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(chromite_oreModifier::feature)
            ).apply(builder, chromite_oreModifier::new)));
    public static RegistryObject<Codec<cinnabar_oreModifier>> cinnabar_ore = BIOME_MODIFIER_SERIALIZERS.register("cinnabar_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(cinnabar_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(cinnabar_oreModifier::feature)
            ).apply(builder, cinnabar_oreModifier::new)));
    public static RegistryObject<Codec<cobaltite_oreModifier>> cobaltite_ore = BIOME_MODIFIER_SERIALIZERS.register("cobaltite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(cobaltite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(cobaltite_oreModifier::feature)
            ).apply(builder, cobaltite_oreModifier::new)));
    public static RegistryObject<Codec<cryolite_oreModifier>> cryolite_ore = BIOME_MODIFIER_SERIALIZERS.register("cryolite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(cryolite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(cryolite_oreModifier::feature)
            ).apply(builder, cryolite_oreModifier::new)));
    public static RegistryObject<Codec<galena_oreModifier>> galena_ore = BIOME_MODIFIER_SERIALIZERS.register("galena_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(galena_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(galena_oreModifier::feature)
            ).apply(builder, galena_oreModifier::new)));
    public static RegistryObject<Codec<garnierite_oreModifier>> garnierite_ore = BIOME_MODIFIER_SERIALIZERS.register("garnierite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(garnierite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(garnierite_oreModifier::feature)
            ).apply(builder, garnierite_oreModifier::new)));
    public static RegistryObject<Codec<graphite_oreModifier>> graphite_ore = BIOME_MODIFIER_SERIALIZERS.register("graphite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(graphite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(graphite_oreModifier::feature)
            ).apply(builder, graphite_oreModifier::new)));
    public static RegistryObject<Codec<hematite_oreModifier>> hematite_ore = BIOME_MODIFIER_SERIALIZERS.register("hematite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(hematite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(hematite_oreModifier::feature)
            ).apply(builder, hematite_oreModifier::new)));
    public static RegistryObject<Codec<kaolinite_oreModifier>> kaolinite_ore = BIOME_MODIFIER_SERIALIZERS.register("kaolinite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(kaolinite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(kaolinite_oreModifier::feature)
            ).apply(builder, kaolinite_oreModifier::new)));
    public static RegistryObject<Codec<kimberlite_oreModifier>> kimberlite_ore = BIOME_MODIFIER_SERIALIZERS.register("kimberlite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(kimberlite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(kimberlite_oreModifier::feature)
            ).apply(builder, kimberlite_oreModifier::new)));
    public static RegistryObject<Codec<lignite_oreModifier>> lignite_ore = BIOME_MODIFIER_SERIALIZERS.register("lignite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(lignite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(lignite_oreModifier::feature)
            ).apply(builder, lignite_oreModifier::new)));
    public static RegistryObject<Codec<limonite_oreModifier>> limonite_ore = BIOME_MODIFIER_SERIALIZERS.register("limonite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(limonite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(limonite_oreModifier::feature)
            ).apply(builder, limonite_oreModifier::new)));
    public static RegistryObject<Codec<magnetite_oreModifier>> magnetite_ore = BIOME_MODIFIER_SERIALIZERS.register("magnetite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(magnetite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(magnetite_oreModifier::feature)
            ).apply(builder, magnetite_oreModifier::new)));
    public static RegistryObject<Codec<manganese_oreModifier>> manganese_ore = BIOME_MODIFIER_SERIALIZERS.register("manganese_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(manganese_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(manganese_oreModifier::feature)
            ).apply(builder, manganese_oreModifier::new)));
    public static RegistryObject<Codec<osmiridium_oreModifier>> osmiridium_ore = BIOME_MODIFIER_SERIALIZERS.register("osmiridium_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(osmiridium_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(osmiridium_oreModifier::feature)
            ).apply(builder, osmiridium_oreModifier::new)));
    public static RegistryObject<Codec<platinium_oreModifier>> platinium_ore = BIOME_MODIFIER_SERIALIZERS.register("platinium_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(platinium_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(platinium_oreModifier::feature)
            ).apply(builder, platinium_oreModifier::new)));
    public static RegistryObject<Codec<quartz_oreModifier>> quartz_ore = BIOME_MODIFIER_SERIALIZERS.register("quartz_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(quartz_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(quartz_oreModifier::feature)
            ).apply(builder, quartz_oreModifier::new)));
    public static RegistryObject<Codec<saltpeter_oreModifier>> saltpeter_ore = BIOME_MODIFIER_SERIALIZERS.register("saltpeter_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(saltpeter_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(saltpeter_oreModifier::feature)
            ).apply(builder, saltpeter_oreModifier::new)));
    public static RegistryObject<Codec<silicium_oreModifier>> silicium_ore = BIOME_MODIFIER_SERIALIZERS.register("silicium_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(silicium_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(silicium_oreModifier::feature)
            ).apply(builder, silicium_oreModifier::new)));
    public static RegistryObject<Codec<silver_oreModifier>> silver_ore = BIOME_MODIFIER_SERIALIZERS.register("silver_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(silver_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(silver_oreModifier::feature)
            ).apply(builder, silver_oreModifier::new)));
    public static RegistryObject<Codec<sphalerite_oreModifier>> sphalerite_ore = BIOME_MODIFIER_SERIALIZERS.register("sphalerite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(sphalerite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(sphalerite_oreModifier::feature)
            ).apply(builder, sphalerite_oreModifier::new)));
    public static RegistryObject<Codec<sulfur_oreModifier>> sulfur_ore = BIOME_MODIFIER_SERIALIZERS.register("sulfur_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(sulfur_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(sulfur_oreModifier::feature)
            ).apply(builder, sulfur_oreModifier::new)));
    public static RegistryObject<Codec<tantalite_oreModifier>> tantalite_ore = BIOME_MODIFIER_SERIALIZERS.register("tantalite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(tantalite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(tantalite_oreModifier::feature)
            ).apply(builder, tantalite_oreModifier::new)));
    public static RegistryObject<Codec<titanite_oreModifier>> titanite_ore = BIOME_MODIFIER_SERIALIZERS.register("titanite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(titanite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(titanite_oreModifier::feature)
            ).apply(builder, titanite_oreModifier::new)));
    public static RegistryObject<Codec<uraninite_oreModifier>> uraninite_ore = BIOME_MODIFIER_SERIALIZERS.register("uraninite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(uraninite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(uraninite_oreModifier::feature)
            ).apply(builder, uraninite_oreModifier::new)));
    public static RegistryObject<Codec<wolframite_oreModifier>> wolframite_ore = BIOME_MODIFIER_SERIALIZERS.register("wolframite_ore", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(wolframite_oreModifier::biome),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(wolframite_oreModifier::feature)
            ).apply(builder, wolframite_oreModifier::new)));

}
