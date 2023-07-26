package com.magneticraft2.common.world.modiifiers;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * @author JumpWatch on 26-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public record stonepebbleModifier(HolderSet<Biome> biome, Holder<PlacedFeature> feature) implements BiomeModifier {
    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && this.biome.contains(biome) ){
            builder.getGenerationSettings().addFeature(GenerationStep.Decoration.RAW_GENERATION, this.feature);
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return com.magneticraft2.common.registry.registers.BiomeModifier.stonepebblemodifier.get();
    }
}
