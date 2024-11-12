package com.magneticraft2.common.systems.Multiblocking.json;

import com.magneticraft2.common.systems.Multiblocking.core.MultiblockModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.*;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiblockStructure {
    private int[] dimensions;
    private final Map<String, List<List<String>>> layout;
    private final Map<String, Block> blocks;
    private final Map<String, MultiblockModule> modules;


    public MultiblockStructure(int[] dimensions, Map<String, List<List<String>>> layout, Map<String, Block> blocks) {
        this.dimensions = dimensions;
        this.layout = layout;
        this.blocks = blocks;
        this.modules = new HashMap<>();
    }
    public void addModule(String key, MultiblockModule module) {
        this.modules.put(key, module);
    }


    public int[] getDimensions() {
        return dimensions;
    }

    public Map<String, List<List<String>>> getLayout() {
        return layout;
    }

    public Map<String, Block> getBlocks() {
        return blocks;
    }
    public Collection<MultiblockModule> getModules() {
        return modules.values();
    }


    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();

        // Save dimensions
        ListTag dimensionsList = new ListTag();
        for (int dimension : dimensions) {
            dimensionsList.add(StringTag.valueOf(Integer.toString(dimension)));
        }
        tag.put("dimensions", dimensionsList);

        // Save layout
        CompoundTag layoutTag = new CompoundTag();
        for (Map.Entry<String, List<List<String>>> entry : layout.entrySet()) {
            ListTag layerList = new ListTag();
            for (List<String> row : entry.getValue()) {
                ListTag rowList = new ListTag();
                for (String cell : row) {
                    rowList.add(StringTag.valueOf(cell));
                }
                layerList.add(rowList);
            }
            layoutTag.put(entry.getKey(), layerList);
        }
        tag.put("layout", layoutTag);

        // Save blocks
        CompoundTag blocksTag = new CompoundTag();
        for (Map.Entry<String, Block> entry : blocks.entrySet()) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(entry.getValue());
            if (blockId != null) {
                blocksTag.putString(entry.getKey(), blockId.toString());
            }
        }
        tag.put("blocks", blocksTag);

        return tag;
    }

    public void loadFromNBT(CompoundTag tag) {
        // Load dimensions
        ListTag dimensionsList = tag.getList("dimensions", 8); // 8 = StringTag
        dimensions = new int[dimensionsList.size()];
        for (int i = 0; i < dimensionsList.size(); i++) {
            dimensions[i] = Integer.parseInt(dimensionsList.getString(i));
        }

        // Load layout
        layout.clear();
        CompoundTag layoutTag = tag.getCompound("layout");
        for (String key : layoutTag.getAllKeys()) {
            ListTag layerList = layoutTag.getList(key, 10); // 10 = ListTag
            List<List<String>> layers = new ArrayList<>();
            for (int j = 0; j < layerList.size(); j++) {
                ListTag rowList = layerList.getList(j);
                List<String> row = new ArrayList<>();
                for (int k = 0; k < rowList.size(); k++) {
                    row.add(rowList.getString(k));
                }
                layers.add(row);
            }
            layout.put(key, layers);
        }

        // Load blocks
        blocks.clear();
        CompoundTag blocksTag = tag.getCompound("blocks");
        for (String key : blocksTag.getAllKeys()) {
            Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(blocksTag.getString(key)));
            if (block != null) {
                blocks.put(key, block);
            }
        }
    }
}