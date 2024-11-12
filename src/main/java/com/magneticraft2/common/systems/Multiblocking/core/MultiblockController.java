package com.magneticraft2.common.systems.Multiblocking.core;

import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 11-09-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiblockController {
    public static final Logger LOGGER = LogManager.getLogger("MGCMultiblockController");
    private boolean isFormed = false;
    private final MultiblockStructure structure;
    private final Map<BlockPos, BlockState> blockMap;
    private BlockPos masterPos;
    private final Map<String, MultiblockModule> modules;

    public MultiblockController(MultiblockStructure structure) {
        this.structure = structure;
        this.blockMap = new HashMap<>();
        this.masterPos = null;
        this.modules = new HashMap<>();
    }
    public void setFormed(boolean val){
        isFormed = val;
    }

    public boolean getFormed(){
        return isFormed;
    }
    public BlockPos getmodulePos(String moduleName){
        for (IMultiblockModule module : modules.values()) {
            if (module.getModuleKey() == moduleName){
                BlockPos modulePos = module.getModuleOffset();
                return modulePos;
            }
            return null;
        }
        return null;
    }
    public boolean isMasterPosSet(){
        if (this.masterPos != null){
            return true;
        }else {
            return false;
        }
    }
    public void setMasterPos(BlockPos masterPos) {
        this.masterPos = masterPos;
    }
    public boolean isValidStructure(Level world, BlockPos pos) {
        masterPos = pos;
        LOGGER.info("blocks:" + structure.getBlocks());
        // Initialize modules
        modules.clear();
        for (IMultiblockModule module : structure.getModules()) {
            String moduleKey = module.getModuleKey();
            BlockPos modulePos = module.getModuleOffset();
            if (module.isValid(world, modulePos)) {
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                    LOGGER.info("module pos: " + modulePos);
                    LOGGER.info("structure: " + structure);
                    LOGGER.info("module key: " + moduleKey);
                }
                modules.put(moduleKey, new MultiblockModule(structure, moduleKey, modulePos));
            } else {
                return false;
            }
        }
        // Clear the blockMap to ensure it's empty before adding new entries
        blockMap.clear();

        // Get the dimensions, layout, and block definitions of the structure
        int[] dimensions = structure.getDimensions();
        Map<String, Block> blockDefinitions = structure.getBlocks();
        Map<String, List<List<String>>> layout = structure.getLayout();

        // Locate the controller block within the structure layout to determine the offset
        Block controllerBlock = world.getBlockState(masterPos).getBlock();
        BlockPos controllerOffset = FindControllerOffset(layout, blockDefinitions, controllerBlock);

        if (controllerOffset == null) {
            return false;
        }

        // Adjust the starting point (origin) based on the controller's position in the layout
        BlockPos origin = masterPos.subtract(controllerOffset);

        // Iterate through the structure dimensions to validate each block
        for (int y = 0; y < dimensions[1]; y++) {
            List<List<String>> layer = layout.get("layer" + (y + 1));
            if (layer == null) {
                System.out.println("Layer " + (y + 1) + " is missing from the layout.");
                return false;
            }

            for (int z = 0; z < dimensions[2]; z++) {
                for (int x = 0; x < dimensions[0]; x++) {
                    // Get the block key at the current position in the layer
                    String blockKey = layer.get(z).get(x);

                    // Get the expected Block based on the key from blockDefinitions
                    Block expectedBlock = blockDefinitions.get(blockKey);
                    if (expectedBlock == null) {
                        System.out.println("No block definition found for key: " + blockKey);
                        return false;
                    }

                    // Calculate the actual world position for the current block
                    BlockPos currentPos = origin.offset(x, y, z);
                    BlockState currentState = world.getBlockState(currentPos);

                    // Check if the current block matches the expected block
                    if (currentState.getBlock() != expectedBlock) {
                        LOGGER.info("Block mismatch at {}: expected {}, found {}", currentPos, expectedBlock, currentState.getBlock());
                        return false;
                    }

                    // Add the position and block state to the map
                    blockMap.put(currentPos, currentState);
                }
            }
        }

        return true;
    }
    public boolean createStructure(Level world, BlockPos pos) {
        if (!isValidStructure(world, pos)) {
            return false;
        }

        // Create the structure at the given position
        for (Map.Entry<BlockPos, BlockState> entry : blockMap.entrySet()) {
            BlockPos placePos = entry.getKey();
            Block block = world.getBlockState(placePos).getBlock();
            // Check if the current block is the controller block
            if (!placePos.equals(pos)) {
                if (!(block == Blocks.AIR)) {
                    // Replace non-controller blocks with the specified replacement block
                    world.setBlock(placePos, BlockRegistry.multiblockfiller.get().defaultBlockState(), 2);
                    // Add NBT data to the block entity
                    BlockEntity blockEntity = world.getBlockEntity(placePos);
                    if (blockEntity != null) {
                        CompoundTag tag = blockEntity.getUpdateTag();
                        tag.putInt("controller_x", pos.getX());
                        tag.putInt("controller_y", pos.getY());
                        tag.putInt("controller_z", pos.getZ());
//                        blockEntity.setChanged(); // Mark the block entity as changed to save data
                        blockEntity.handleUpdateTag(tag);
                    }
                }

            }
        }

        // Enable modules
        for (IMultiblockModule module : modules.values()) {
            BlockPos modulePos = module.getModuleOffset();
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("activating: " + module.getModuleKey());
                LOGGER.info("Module pos " + modulePos);
                LOGGER.info("Modules: " + modules.values());
                LOGGER.info("Module instance: " + module); // Log the module instance
                LOGGER.info("Entering the loop to activate modules"); // Log the entry to the loop
                LOGGER.info("Module class: " + module.getClass().getName());
            }
            module.onActivate(world, modulePos);
        }
        isFormed = true;
        return true;
    }
    public boolean identifyAndAddModules(Level world, BlockPos pos, @NotNull MultiblockStructure structure) {
        int[] dimensions = structure.getDimensions();
        Block controllerBlock = world.getBlockState(pos).getBlock();
        BlockPos controllerOffset = findControllerOffset(structure, controllerBlock);
        BlockPos adjustedPos = pos.offset(-controllerOffset.getX(), -controllerOffset.getY(), -controllerOffset.getZ());

        for (int y = 0; y < dimensions[1]; y++) {
            for (int z = 0; z < dimensions[2]; z++) {
                for (int x = 0; x < dimensions[0]; x++) {
                    BlockPos currentPos = adjustedPos.offset(x, y, z);
                    BlockEntity blockEntity = world.getBlockEntity(currentPos);

                    if (blockEntity instanceof IMultiblockModule) {
                        LOGGER.info("Module at: " + currentPos);
                        IMultiblockModule module = (IMultiblockModule) blockEntity;
                        String moduleKey = module.getModuleKey();
                        // Assuming MultiblockModule is the correct type to add to the modules map
                        structure.addModule(moduleKey, new MultiblockModule(structure, moduleKey, currentPos));
                    }
                }
            }
        }

        return true; // Return true if modules are identified and added successfully, false otherwise
    }
    private BlockPos FindControllerOffset(Map<String, List<List<String>>> layout, Map<String, Block> blockDefinitions, Block controllerBlock) {
        for (int y = 0; y < layout.size(); y++) {
            List<List<String>> layer = layout.get("layer" + (y + 1));
            for (int z = 0; z < layer.size(); z++) {
                List<String> row = layer.get(z);
                for (int x = 0; x < row.size(); x++) {
                    String blockKey = row.get(x);
                    Block expectedBlock = blockDefinitions.get(blockKey);

                    // Check if this is the controller block
                    if (expectedBlock == controllerBlock) {
                        return new BlockPos(x, y, z);  // Return the offset position of the controller
                    }
                }
            }
        }
        return null;  // Controller not found in layout
    }

    public BlockPos findControllerOffset(MultiblockStructure structure, Block controllerBlock) {
        Map<String, List<List<String>>> layout = structure.getLayout();
        int[] dimensions = structure.getDimensions();
        Map<String, Block> blocks = structure.getBlocks();

        for (int y = 0; y < dimensions[1]; y++) {
            List<List<String>> layer = layout.get("layer" + (y + 1));
            for (int z = 0; z < dimensions[2]; z++) {
                for (int x = 0; x < dimensions[0]; x++) {
                    String blockKey = layer.get(z).get(x);
                    if (blocks.get(blockKey) == controllerBlock) {
                        // Calculate the offset based on the relative position of the controller block in the structure
                        return new BlockPos(x - dimensions[0] / 2 +1, y - dimensions[1] / 2+1, z - dimensions[2] / 2+1);
                    }
                }
            }
        }
        return BlockPos.ZERO; // Default offset (0, 0, 0) if the controller block is not found in the structure
    }

    public boolean destroyStructure(Level world) {
        if (masterPos == null) {
            LOGGER.info("Master pos not found!");
            return false;
        }
//        LOGGER.info("Disabling modules");
        // Disable modules
        for (IMultiblockModule module : modules.values()) {
            module.onDeactivate(world, module.getModuleOffset());
        }
//        LOGGER.info("Destrying structure");
//        LOGGER.info(blockMap.entrySet() + " Entrymap");
        // Destroy the structure
        for (Map.Entry<BlockPos, BlockState> entry : blockMap.entrySet()) {
            BlockPos offsetPos = entry.getKey();
            Block block = entry.getValue().getBlock();
//            LOGGER.info("destroying " + offsetPos + " is block: " + block);
            world.addFreshEntity(new ItemEntity(world, offsetPos.getX(), offsetPos.getY(), offsetPos.getZ(), new ItemStack(block)));
            world.setBlock(offsetPos, Blocks.AIR.defaultBlockState(), 2);
        }

        blockMap.clear();
        masterPos = null;

        return true;
    }

    public MultiblockStructure getStructure() {
        return structure;
    }

    public Map<BlockPos, BlockState> getBlockMap() {
        return blockMap;
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();

        // Save whether the multiblock is formed
        tag.putBoolean("isFormed", isFormed);

        // Save the structure (if needed) - this can vary based on your structure handling
        if (structure != null) {
            tag.put("structure", structure.saveToNBT());
        }

        // Save the block map
        ListTag blockMapList = new ListTag();
        for (Map.Entry<BlockPos, BlockState> entry : blockMap.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            BlockPos pos = entry.getKey();
            entryTag.putInt("x", pos.getX());
            entryTag.putInt("y", pos.getY());
            entryTag.putInt("z", pos.getZ());

            // Use BlockState's Codec to serialize
            entryTag.put("blockState", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue()).result().orElse(new CompoundTag()));

            blockMapList.add(entryTag);
        }
        tag.put("blockMap", blockMapList);

        // Save the modules map
        CompoundTag modulesTag = new CompoundTag();
        for (Map.Entry<String, MultiblockModule> entry : modules.entrySet()) {
            modulesTag.put(entry.getKey(), ((MultiblockModule) entry.getValue()).saveToNBT());
        }
        tag.put("modules", modulesTag);

        return tag;
    }

    public void loadFromNBT(CompoundTag tag) {
        this.isFormed = tag.getBoolean("isFormed");

        // Load structure if needed
        if (tag.contains("structure") && this.structure != null) {
            this.structure.loadFromNBT(tag.getCompound("structure"));
        }

        // Load the block map
        this.blockMap.clear();
        ListTag blockMapList = tag.getList("blockMap", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < blockMapList.size(); i++) {
            CompoundTag entryTag = blockMapList.getCompound(i);

            BlockPos pos = new BlockPos(entryTag.getInt("x"), entryTag.getInt("y"), entryTag.getInt("z"));
            BlockState state = BlockState.CODEC.parse(NbtOps.INSTANCE, entryTag.getCompound("blockState")).result().orElse(null);

            if (state != null) {
                blockMap.put(pos, state);
            }
        }

        // Load the modules map
        this.modules.clear();
        CompoundTag modulesTag = tag.getCompound("modules");
        for (String key : modulesTag.getAllKeys()) {
            CompoundTag moduleTag = modulesTag.getCompound(key);

            // Retrieve or construct the necessary parameters
            MultiblockStructure structure = this.structure; // Assuming structure is already initialized
            String moduleKey = moduleTag.getString("moduleKey");
            BlockPos moduleOffset = new BlockPos(moduleTag.getInt("modulePosX"), moduleTag.getInt("modulePosY"), moduleTag.getInt("modulePosZ"));

            // Create the module using the constructor with parameters
            MultiblockModule module = new MultiblockModule(structure, moduleKey, moduleOffset);
            module.loadFromNBT(moduleTag);

            modules.put(key, module);
        }
    }
}
