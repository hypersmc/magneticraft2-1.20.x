package com.magneticraft2.common.systems.Multiblocking.core;

import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiblockModule implements IMultiblockModule {
    public static final Logger LOGGER = LogManager.getLogger("MGCMultiblockModule");
    private final MultiblockStructure structure;
    private final Map<BlockPos, BlockState> blockMap;
    private final List<BlockPos> modulePositions;
    private String moduleKey;
    private BlockPos moduleOffset;

    public MultiblockModule(MultiblockStructure structure, String moduleKey, BlockPos moduleOffset) {
        this.moduleKey = moduleKey;
        this.moduleOffset = moduleOffset;
        this.structure = structure;
        this.blockMap = new HashMap<>();
        this.modulePositions = new ArrayList<>();
    }

    public boolean isValid(Level world, BlockPos pos) {
        // Check if the module is valid at the given position
        Map<BlockPos, BlockState> tempMap = new HashMap<>();
        List<BlockPos> tempPositions = new ArrayList<>();

        for (String layerKey : structure.getLayout().keySet()) {
            // Extract numeric part from the layer string
            String numericPart = layerKey.replaceAll("[^0-9]", "");

            // Validate the numeric part
            if (numericPart.isEmpty()) {
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                    LOGGER.error("Invalid layer format: " + layerKey + ". Skipping...");
                }
                continue; // Skip to the next iteration
            }

            // Safely parse the layer index
            try {
                int layerIndex = Integer.parseInt(numericPart) - 1;
                List<List<String>> rowList = structure.getLayout().get(layerKey);

                // Validate each block in the layer
                for (int rowIndex = 0; rowIndex < rowList.size(); rowIndex++) {
                    List<String> row = rowList.get(rowIndex);

                    for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                        String blockId = row.get(columnIndex);

                        if (blockId.equals(" ")) continue;

                        // Check if the blockId corresponds to a module
                        if (!structure.getModules().contains(blockId)) continue;

                        BlockPos offsetPos = new BlockPos(columnIndex, layerIndex, rowIndex);
                        BlockPos checkPos = pos.offset(offsetPos);
                        BlockState checkState = world.getBlockState(checkPos);

                        if (checkState.isAir()) {
                            return false;
                        }

                        if (!structure.getBlocks().get(blockId).equals(checkState.getBlock())) {
                            return false;
                        }

                        tempMap.put(offsetPos, checkState);
                        tempPositions.add(offsetPos);
                    }
                }
            } catch (NumberFormatException e) {
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                    LOGGER.error("Invalid layer index: " + numericPart + " in layer: " + layerKey + ". Skipping...", e);
                }
            }
        }

        blockMap.clear();
        blockMap.putAll(tempMap);
        modulePositions.clear();
        modulePositions.addAll(tempPositions);

        return true;
    }


    public void onActivate(Level world, BlockPos pos) {
        BlockEntity block = world.getBlockEntity(pos);
        if (block instanceof IMultiblockModule){
            ((IMultiblockModule) block).onActivate(world,pos);
        }else{
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.warn("Unexpected BlockEntity type at position " + pos + ": " + block.getClass().getName());
            }
        }
    }

    public void onDeactivate(Level world, BlockPos pos) {
        BlockEntity block = world.getBlockEntity(pos);
        if (block instanceof IMultiblockModule){
            ((IMultiblockModule) block).onDeactivate(world,pos);
        }else{
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.warn("Unexpected BlockEntity type at position " + pos + ": " + block.getClass().getName());
            }
        }
    }

    @Override
    public String getModuleKey() {
        return moduleKey;
    }

    @Override
    public BlockPos getModuleOffset() {
        return moduleOffset;
    }

    @Override
    public IMultiblockModule createModule(Level world, BlockPos pos) {
        for (Map.Entry<BlockPos, BlockState> entry : blockMap.entrySet()) {
            BlockPos offsetPos = entry.getKey();
            BlockPos placePos = pos.offset(offsetPos);
            world.setBlock(placePos, entry.getValue(), 2);
        }
        return null;
    }


    public Map<BlockPos, BlockState> getBlockMap() {
        return blockMap;
    }

    public List<BlockPos> getModulePositions() {
        return modulePositions;
    }

    public MultiblockStructure getStructure() {
        return structure;
    }
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();

        // Save the module key and position
        tag.putString("moduleKey", moduleKey);
        if (moduleOffset != null) {
            tag.putInt("modulePosX", moduleOffset.getX());
            tag.putInt("modulePosY", moduleOffset.getY());
            tag.putInt("modulePosZ", moduleOffset.getZ());
        }

        // Save any additional properties relevant to this module

        return tag;
    }

    public void loadFromNBT(CompoundTag tag) {
        this.moduleKey = tag.getString("moduleKey");
        if (tag.contains("modulePosX") && tag.contains("modulePosY") && tag.contains("modulePosZ")) {
            this.moduleOffset = new BlockPos(tag.getInt("modulePosX"), tag.getInt("modulePosY"), tag.getInt("modulePosZ"));
        }

        // Load any additional properties relevant to this module
    }
}