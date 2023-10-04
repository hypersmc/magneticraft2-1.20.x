package com.magneticraft2.common.systems.Multiblocking.core.modules;

import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * @author JumpWatch on 14-05-2023
 * @Project magneticraft2-1.18.2
 * v1.0.0
 */
public class ItemsModule implements IMultiblockModule {
    public static final Logger LOGGER = LogManager.getLogger("ItemsModule");
    public final ItemStackHandler itemHandler; //Item
    public final LazyOptional<IItemHandler> item;
    public ItemsModule() {
        this(10);
    }
    public ItemsModule(int size){
        this.itemHandler = new ItemStackHandler(size){
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return super.isItemValid(slot, stack);
            }

            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                return super.insertItem(slot, stack, simulate);
            }
        };
        this.item = LazyOptional.of(() -> itemHandler);
    }





    @Override
    public boolean isValid(Level world, BlockPos pos) {
        return true;
    }

    @Override
    public void onActivate(Level world, BlockPos pos) {

    }

    @Override
    public void onDeactivate(Level world, BlockPos pos) {

    }

    @Override
    public String getModuleKey() {
        return "item_storage";
    }

    @Override
    public BlockPos getModuleOffset() {
        return new BlockPos(0, 0, 0);
    }

    @Override
    public IMultiblockModule createModule(Level world, BlockPos pos) {
        return new ItemsModule();
    }
}
