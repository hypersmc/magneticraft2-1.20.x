package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.MultiblockItemPortBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.data.ModelData;
import com.magneticraft2.common.utils.MultiBlockProperties;
import com.magneticraft2.common.block.stage.bronze.CharcoalRetortBlock;
import com.magneticraft2.common.block.stage.bronze.RefractoryKilnBlock;
import com.magneticraft2.common.block.stage.bronze.OreRoastingKilnBlock;
import com.magneticraft2.common.blockentity.stage.bronze.OreRoastingKilnBlockEntity;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeForgeHammerBlockEntity;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeRollingMillBlockEntity;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeMechanicalPressBlockEntity;
import com.magneticraft2.common.blockentity.stage.iron.IronStockShearBlockEntity;
import com.magneticraft2.common.blockentity.stage.iron.IronForgeHearthBlockEntity;
import com.magneticraft2.common.blockentity.stage.bronze.BronzePrecisionBoringMachineBlockEntity;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeMechanicalSawmillBlockEntity;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeMechanicalAssemblyStationBlockEntity;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeFoundrySandMixerBlockEntity;
import com.magneticraft2.common.blockentity.stage.bronze.RefractoryKilnBlockEntity;
import com.magneticraft2.common.blockentity.stage.bronze.BloomeryFurnaceBlockEntity;
import com.magneticraft2.common.block.stage.bronze.BloomeryFurnaceBlock;
import com.magneticraft2.common.blockentity.stage.bronze.CharcoalRetortBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Item I/O module that forwards directly into the formed controller inventory.
 *
 * Input port -> controller slot 0, insertion only.
 * Legacy combined output -> controller slots 1 and 2, extraction only.
 * Primary output -> controller slot 1 only.
 * Byproduct output -> controller slot 2 only.
 */
public class MultiblockItemPortBlockEntity
        extends BlockEntity
        implements IMultiblockModule {

    private int controllerX;
    private int controllerY;
    private int controllerZ;
    private boolean formedModule;

    private LazyOptional<IItemHandler> itemCapability =
            LazyOptional.of(
                    () -> new PortItemHandler()
            );

    public MultiblockItemPortBlockEntity(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry
                        .MULTIBLOCK_ITEM_PORT_BE
                        .get(),
                pos,
                state
        );
    }

    private MultiblockItemPortBlock.PortMode getPortMode() {
        if (getBlockState().getBlock()
                instanceof MultiblockItemPortBlock portBlock) {
            return portBlock.getPortMode();
        }

        return MultiblockItemPortBlock.PortMode.INPUT;
    }


    /**
     * This changes only the baked appearance of a REAL, still-interactable
     * item module. Other multiblocks and unformed modules keep their normal
     * wooden/copper port appearance. No port coordinates or capabilities move.
     */
    @Override
    public @NotNull ModelData getModelData() {
        if (formedModule && level != null
                && getController() instanceof CharcoalRetortBlockEntity retort) {
            BlockState controllerState = retort.getBlockState();
            if (controllerState.getBlock() instanceof CharcoalRetortBlock) {
                String role = switch (getPortMode()) {
                    case INPUT -> "input";
                    case PRIMARY_OUTPUT -> "primary";
                    case BYPRODUCT_OUTPUT -> "byproduct";
                    default -> "";
                };
                if (!role.isEmpty()) {
                    String key = "charcoal_retort_"
                            + controllerState.getValue(CharcoalRetortBlock.FACING).getName()
                            + "/port_" + role;
                    return ModelData.builder()
                            .with(MultiBlockProperties.MODEL_NAME, key)
                            .build();
                }
            }
        }
        // The Refractory Kiln uses the same REAL port block entities as the
        // Retort, with a baked iron/refractory skin only while formed.
        if (formedModule && level != null
                && getController() instanceof RefractoryKilnBlockEntity kiln) {
            BlockState controllerState = kiln.getBlockState();
            if (controllerState.getBlock() instanceof RefractoryKilnBlock) {
                String role = switch (getPortMode()) {
                    case INPUT -> "input";
                    case PRIMARY_OUTPUT -> "primary";
                    default -> "";
                };
                if (!role.isEmpty()) {
                    String key = "refractory_kiln_"
                            + controllerState.getValue(RefractoryKilnBlock.FACING).getName()
                            + "/port_" + role;
                    return ModelData.builder()
                            .with(MultiBlockProperties.MODEL_NAME, key)
                            .build();
                }
            }
        }
        if (formedModule && level != null
                && getController() instanceof OreRoastingKilnBlockEntity kiln) {
            BlockState state = kiln.getBlockState();
            if (state.getBlock() instanceof OreRoastingKilnBlock) {
                String role = switch (getPortMode()) {
                    case INPUT -> "input";
                    case PRIMARY_OUTPUT -> "primary";
                    default -> "";
                };
                if (!role.isEmpty()) {
                    return ModelData.builder().with(MultiBlockProperties.MODEL_NAME,
                            "ore_roasting_kiln_" +
                            state.getValue(OreRoastingKilnBlock.FACING).getName() +
                            "/port_" + role).build();
                }
            }
        }
        // Real formed-only item ports; this changes skin, not storage logic.
        if (formedModule && level != null
                && getController() instanceof BloomeryFurnaceBlockEntity bloomery) {
            BlockState state = bloomery.getBlockState();
            if (state.getBlock() instanceof BloomeryFurnaceBlock) {
                String role = switch (getPortMode()) {
                    case INPUT -> "input";
                    case PRIMARY_OUTPUT -> "primary";
                    case BYPRODUCT_OUTPUT -> "byproduct";
                    default -> "";
                };
                if (!role.isEmpty()) {
                    String key = "bloomery_furnace_"
                            + state.getValue(BloomeryFurnaceBlock.FACING).getName()
                            + "/port_" + role;
                    return ModelData.builder()
                            .with(MultiBlockProperties.MODEL_NAME, key).build();
                }
            }
        }
        return ModelData.EMPTY;
    }

    public boolean isOutputPort() {
        return getPortMode()
                != MultiblockItemPortBlock.PortMode.INPUT;
    }

    @Nullable
    public BlockPos getControllerPos() {
        if (!formedModule) {
            return null;
        }

        return new BlockPos(
                controllerX,
                controllerY,
                controllerZ
        );
    }

    @Nullable
    private BaseBlockEntityMagneticraft2 getController() {
        if (level == null || !formedModule) {
            return null;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(
                        new BlockPos(
                                controllerX,
                                controllerY,
                                controllerZ
                        )
                );

        return blockEntity
                instanceof BaseBlockEntityMagneticraft2 controller
                ? controller
                : null;
    }

    public ItemStack getInputStackForDisplay() {
        if (isOutputPort()) {
            return ItemStack.EMPTY;
        }

        BaseBlockEntityMagneticraft2 controller =
                getController();

        if (controller == null
                || controller.itemHandler == null
                || controller.itemHandler.getSlots() <= 0) {
            return ItemStack.EMPTY;
        }

        if (controller instanceof BronzeMechanicalAssemblyStationBlockEntity) {
            for (int i = 0; i < 4; i++)
                if (!controller.itemHandler.getStackInSlot(i).isEmpty())
                    return controller.itemHandler.getStackInSlot(i).copy();
            return ItemStack.EMPTY;
        }
        return controller.itemHandler
                .getStackInSlot(0)
                .copy();
    }

    private boolean acceptsControllerInput(
            @Nullable BaseBlockEntityMagneticraft2 controller,
            ItemStack stack) {
        if(controller instanceof MechanicalSifterBlockEntity sifter)
            return sifter.acceptsRecipeInput(stack);
        if(controller instanceof com.magneticraft2.common.blockentity.stage.bronze.BronzeStampMillBlockEntity mill)
            return mill.acceptsRecipeInput(stack);
        if(controller instanceof com.magneticraft2.common.blockentity.stage.bronze.RotaryOreClassifierBlockEntity classifier)
            return classifier.acceptsRecipeInput(stack);
        if(controller instanceof com.magneticraft2.common.blockentity.stage.bronze.CharcoalRetortBlockEntity retort)
            return retort.acceptsRecipeInput(stack);
        // Refractory Kiln has real firing recipes, including reheating
        // Iron Blooms. Only accept matching items through its real port.
        if(controller instanceof com.magneticraft2.common.blockentity.stage.bronze.RefractoryKilnBlockEntity kiln)
            return kiln.acceptsRecipeInput(stack);
        if(controller instanceof IronForgeHearthBlockEntity hearth)
            return hearth.acceptsRecipeInput(stack);
        if (controller instanceof OreRoastingKilnBlockEntity roaster)
            return roaster.acceptsRecipeInput(stack);
        if (controller instanceof BloomeryFurnaceBlockEntity bloomery)
            return bloomery.acceptsIngredientSlot(0,stack)
                    || bloomery.acceptsIngredientSlot(3,stack);
        if (controller instanceof BronzeForgeHammerBlockEntity hammer)
            return hammer.acceptsRecipeInput(stack);
        if (controller instanceof BronzeRollingMillBlockEntity mill)
            return mill.acceptsRecipeInput(stack);
        if (controller instanceof BronzeMechanicalPressBlockEntity press)
            return press.acceptsRecipeInput(stack);
        if (controller instanceof IronStockShearBlockEntity shear)
            return shear.acceptsRecipeInput(stack);
        if (controller instanceof BronzePrecisionBoringMachineBlockEntity borer)
            return borer.acceptsRecipeInput(stack);
        if (controller instanceof BronzeMechanicalSawmillBlockEntity sawmill)
            return sawmill.acceptsRecipeInput(stack);
        if (controller instanceof BronzeMechanicalAssemblyStationBlockEntity assembly) {
            for (int i = 0; i < 4; i++)
                if (assembly.acceptsIngredientSlot(i, stack)) return true;
            return false;
        }
        if (controller instanceof BronzeFoundrySandMixerBlockEntity mixer)
            return mixer.acceptsRecipeInput(stack);
        return true;
    }

    public boolean insertFromPlayer(ItemStack stack) {
        if (isOutputPort()) {
            return false;
        }

        BaseBlockEntityMagneticraft2 controller =
                getController();

        if (controller == null
                || controller.itemHandler == null
                || !acceptsControllerInput(controller, stack)) {
            return false;
        }

        if (controller instanceof BronzeMechanicalAssemblyStationBlockEntity assembly) {
            for (int i = 0; i < 4; i++) {
                if (!assembly.acceptsIngredientSlot(i, stack)) continue;
                if (controller.itemHandler.insertItem(i, stack, false).isEmpty()) return true;
            }
            return false;
        }
        if (controller instanceof BloomeryFurnaceBlockEntity bloomery) {
            for (int slot : new int[]{0,3}) {
                if (bloomery.acceptsIngredientSlot(slot,stack)
                        && controller.itemHandler.insertItem(slot,stack,false).isEmpty())
                    return true;
            }
            return false;
        }
        return controller.itemHandler
                .insertItem(
                        0,
                        stack,
                        false
                )
                .isEmpty();
    }

    public ItemStack extractInputForPlayer() {
        if (isOutputPort()) {
            return ItemStack.EMPTY;
        }

        BaseBlockEntityMagneticraft2 controller =
                getController();

        if (controller == null
                || controller.itemHandler == null
                || controller.itemHandler.getSlots() <= 0) {
            return ItemStack.EMPTY;
        }

        if (controller instanceof BronzeMechanicalAssemblyStationBlockEntity) {
            for (int i = 0; i < 4; i++) {
                ItemStack taken = controller.itemHandler.extractItem(i, 64, false);
                if (!taken.isEmpty()) return taken;
            }
            return ItemStack.EMPTY;
        }
        if (controller instanceof BloomeryFurnaceBlockEntity) {
            ItemStack ore=controller.itemHandler.extractItem(0,64,false);
            return ore.isEmpty() ? controller.itemHandler.extractItem(3,64,false) : ore;
        }
        return controller.itemHandler
                .extractItem(
                        0,
                        64,
                        false
                );
    }

    public ItemStack extractForPlayer() {
        if (!isOutputPort()) {
            return ItemStack.EMPTY;
        }

        BaseBlockEntityMagneticraft2 controller =
                getController();

        if (controller == null
                || controller.itemHandler == null) {
            return ItemStack.EMPTY;
        }

        MultiblockItemPortBlock.PortMode mode =
                getPortMode();

        if (mode
                == MultiblockItemPortBlock.PortMode.COMBINED_OUTPUT) {
            ItemStack primary =
                    controller.itemHandler
                            .extractItem(
                                    1,
                                    64,
                                    false
                            );

            if (!primary.isEmpty()) {
                return primary;
            }

            return controller.itemHandler
                    .extractItem(
                            2,
                            64,
                            false
                    );
        }

        int mapped = controller instanceof BronzeMechanicalAssemblyStationBlockEntity
                ? BronzeMechanicalAssemblyStationBlockEntity.OUTPUT_SLOT
                : (mode == MultiblockItemPortBlock.PortMode.BYPRODUCT_OUTPUT ? 2 : 1);

        return controller.itemHandler
                .extractItem(
                        mapped,
                        64,
                        false
                );
    }

    @Override
    public boolean isValid(Level world, BlockPos pos) {
        return world.getBlockEntity(pos) == this;
    }

    /**
     * The Retort links its own three existing ports after formation.
     * No extra block is added and other multiblocks retain their behavior.
     */
    public void linkRetortController(BlockPos controller) {
        linkFormedController(controller);
    }

    /** Bind a real formed item module to its owning controller. */
    public void linkFormedController(BlockPos controller) {
        if (level == null || level.isClientSide) return;
        controllerX = controller.getX();
        controllerY = controller.getY();
        controllerZ = controller.getZ();
        formedModule = true;
        setChanged();
        sync();
    }

    @Override
    public void onActivate(Level world, BlockPos pos) {
        if (world.isClientSide) {
            return;
        }

        formedModule = true;
        setChanged();
        sync();
    }

    @Override
    public void onDeactivate(Level world, BlockPos pos) {
        if (world.isClientSide) {
            return;
        }

        formedModule = false;
        setChanged();
        sync();
    }

    @Override
    public String getModuleKey() {
        return switch (getPortMode()) {
            case INPUT -> "item_input";
            case COMBINED_OUTPUT -> "item_output";
            case PRIMARY_OUTPUT -> "item_primary_output";
            case BYPRODUCT_OUTPUT -> "item_byproduct_output";
        };
    }

    @Override
    public BlockPos getModuleOffset() {
        return worldPosition;
    }

    @Override
    public IMultiblockModule createModule(
            Level world,
            BlockPos pos) {
        return this;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap,
            @Nullable Direction side) {
        // Compatibility path for previously formed Retorts: the original
        // byproduct item module remains a drain-only fluid outlet. New
        // Retorts use the dedicated fluid_output module instead.
        if (cap == ForgeCapabilities.FLUID_HANDLER
                && formedModule
                && getPortMode() == MultiblockItemPortBlock.PortMode.BYPRODUCT_OUTPUT
                && getController() instanceof CharcoalRetortBlockEntity retort) {
            return retort.getCapability(cap, side);
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }

        return super.getCapability(
                cap,
                side
        );
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemCapability =
                LazyOptional.of(
                        () -> new PortItemHandler()
                );
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        controllerX =
                tag.getInt("controller_x");
        controllerY =
                tag.getInt("controller_y");
        controllerZ =
                tag.getInt("controller_z");
        formedModule =
                tag.getBoolean("isformed");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(
                "controller_x",
                controllerX
        );
        tag.putInt(
                "controller_y",
                controllerY
        );
        tag.putInt(
                "controller_z",
                controllerZ
        );
        tag.putBoolean(
                "isformed",
                formedModule
        );
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag =
                new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
        if (level != null && level.isClientSide) requestModelDataUpdate();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener>
    getUpdatePacket() {
        return ClientboundBlockEntityDataPacket
                .create(this);
    }

    @Override
    public void onDataPacket(
            Connection net,
            ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag =
                packet.getTag();

        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    private void sync() {
        requestModelDataUpdate();
        if (level != null) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_ALL
            );
        }
    }

    private final class PortItemHandler
            implements IItemHandler {

        @Override
        public int getSlots() {
            if (getPortMode() == MultiblockItemPortBlock.PortMode.INPUT
                    && getController() instanceof BronzeMechanicalAssemblyStationBlockEntity)
                return 4;
            if (getPortMode() == MultiblockItemPortBlock.PortMode.INPUT
                    && getController() instanceof BloomeryFurnaceBlockEntity)
                return 2;
            return getPortMode()
                    == MultiblockItemPortBlock.PortMode.COMBINED_OUTPUT
                    ? 2
                    : 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(
                int slot) {
            BaseBlockEntityMagneticraft2 controller =
                    getController();

            if (controller == null
                    || controller.itemHandler == null) {
                return ItemStack.EMPTY;
            }

            int mapped =
                    mapSlot(slot);

            if (mapped < 0
                    || mapped
                    >= controller.itemHandler
                    .getSlots()) {
                return ItemStack.EMPTY;
            }

            return controller.itemHandler
                    .getStackInSlot(mapped);
        }

        @Override
        public @NotNull ItemStack insertItem(
                int slot,
                @NotNull ItemStack stack,
                boolean simulate) {
            if (isOutputPort()) {
                return stack;
            }

            BaseBlockEntityMagneticraft2 controller =
                    getController();

            if (controller == null
                    || controller.itemHandler == null
                    || !acceptsControllerInput(controller, stack)) {
                return stack;
            }

            int mapped = mapSlot(slot);
            if (mapped < 0 || mapped >= controller.itemHandler.getSlots()) return stack;
            if (controller instanceof BronzeMechanicalAssemblyStationBlockEntity assembly
                    && !assembly.acceptsIngredientSlot(mapped, stack)) return stack;
            if (controller instanceof BloomeryFurnaceBlockEntity bloomery
                    && !bloomery.acceptsIngredientSlot(mapped,stack)) return stack;
            return controller.itemHandler.insertItem(mapped, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(
                int slot,
                int amount,
                boolean simulate) {
            if (!isOutputPort()) {
                return ItemStack.EMPTY;
            }

            BaseBlockEntityMagneticraft2 controller =
                    getController();

            if (controller == null
                    || controller.itemHandler == null) {
                return ItemStack.EMPTY;
            }

            int mapped =
                    mapSlot(slot);

            if (mapped < 0
                    || mapped
                    >= controller.itemHandler
                    .getSlots()) {
                return ItemStack.EMPTY;
            }

            return controller.itemHandler
                    .extractItem(
                            mapped,
                            amount,
                            simulate
                    );
        }

        @Override
        public int getSlotLimit(int slot) {
            BaseBlockEntityMagneticraft2 controller =
                    getController();

            if (controller == null
                    || controller.itemHandler == null) {
                return 64;
            }

            int mapped =
                    mapSlot(slot);

            return mapped >= 0
                    && mapped
                    < controller.itemHandler
                    .getSlots()
                    ? controller.itemHandler
                    .getSlotLimit(mapped)
                    : 64;
        }

        @Override
        public boolean isItemValid(
                int slot,
                @NotNull ItemStack stack) {
            if (isOutputPort()) {
                return false;
            }

            BaseBlockEntityMagneticraft2 controller =
                    getController();

            int mapped = mapSlot(slot);
            return controller != null && controller.itemHandler != null
                    && mapped >= 0 && mapped < controller.itemHandler.getSlots()
                    && acceptsControllerInput(controller, stack)
                    && (!(controller instanceof BronzeMechanicalAssemblyStationBlockEntity assembly)
                        || assembly.acceptsIngredientSlot(mapped, stack))
                    && (!(controller instanceof BloomeryFurnaceBlockEntity bloomery)
                        || bloomery.acceptsIngredientSlot(mapped, stack))
                    && controller.itemHandler.isItemValid(mapped, stack);
        }

        private int mapSlot(int slot) {
            MultiblockItemPortBlock.PortMode mode =
                    getPortMode();

            BaseBlockEntityMagneticraft2 controller = getController();
            if (controller instanceof BloomeryFurnaceBlockEntity
                    && mode == MultiblockItemPortBlock.PortMode.INPUT)
                return slot == 0 ? 0 : slot == 1 ? 3 : -1;
            if (controller instanceof BronzeMechanicalAssemblyStationBlockEntity) {
                if (mode == MultiblockItemPortBlock.PortMode.INPUT)
                    return slot >= 0 && slot < 4 ? slot : -1;
                if (mode == MultiblockItemPortBlock.PortMode.PRIMARY_OUTPUT)
                    return slot == 0 ? BronzeMechanicalAssemblyStationBlockEntity.OUTPUT_SLOT : -1;
            }
            if (mode
                    == MultiblockItemPortBlock.PortMode.INPUT) {
                return slot == 0
                        ? 0
                        : -1;
            }

            if (mode
                    == MultiblockItemPortBlock.PortMode.COMBINED_OUTPUT) {
                return switch (slot) {
                    case 0 -> 1;
                    case 1 -> 2;
                    default -> -1;
                };
            }

            if (slot != 0) {
                return -1;
            }

            return mode
                    == MultiblockItemPortBlock.PortMode.BYPRODUCT_OUTPUT
                    ? 2
                    : 1;
        }
    }
}
