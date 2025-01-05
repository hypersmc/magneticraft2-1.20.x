package com.magneticraft2.common.datagen;

import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.registries.RegistryObject;

/**
 * @author JumpWatch on 15-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class ModLangProvider extends LanguageProvider {
    public ModLangProvider(DataGenerator gen, String locale) {
        super(gen.getPackOutput(), "magneticraft2", locale);
    }

    @Override
    protected void addTranslations() {
        // Automatically add translations only for ore blocks
        for (RegistryObject<Block> block : BlockRegistry.BLOCKS.getEntries()) {
            String blockName = block.getId().getPath();

            // Check if the block name contains "ore"
            if (blockName.contains("ore")) {
                String translatedName = convertToTitleCase(blockName); // Converts "iron_ore" to "Iron Ore"
                add("block.magneticraft2." + blockName, translatedName);
            }
        }

        // Add other manual translations if needed
    }

    // Utility method to convert registry names to title case (e.g., "iron_ore" -> "Iron Ore")
    private String convertToTitleCase(String name) {
        String[] parts = name.split("_");
        StringBuilder titleCaseName = new StringBuilder();
        for (String part : parts) {
            titleCaseName.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return titleCaseName.toString().trim();
    }
}
