package com.magneticraft2.common.item.stage.bronze;

import com.magneticraft2.common.registry.registers.ItemRegistry;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One ingot-equivalent of bronze, stored as the original quality-tagged ingot.
 * No Forge fluid capability and no placeable molten-bronze block.
 */
public final class BronzePouringLadleItem extends Item {
    private static final String CONTENT = "BronzeInLadle";
    private static final String SOURCE = "OriginalIngot";
    private static final String LOADED_AT = "LoadedAt";
    private static final String DIMENSION = "LoadedDimension";

    public BronzePouringLadleItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static boolean hasMetal(ItemStack ladle) {
        return !storedIngot(ladle).isEmpty();
    }

    public static ItemStack storedIngot(ItemStack ladle) {
        if (!ladle.is(ItemRegistry.ITEM_BRONZE_POURING_LADLE.get()) ||
                !ladle.hasTag() || !ladle.getTag().contains(CONTENT, 10)) {
            return ItemStack.EMPTY;
        }
        ItemStack source = ItemStack.of(ladle.getTag().getCompound(CONTENT).getCompound(SOURCE));
        if (!source.is(ItemRegistry.ITEM_BRONZE_INGOT.get()) || source.getCount() != 1) {
            return ItemStack.EMPTY;
        }
        return source;
    }

    public static boolean isHot(ItemStack ladle, Level world) {
        if (!hasMetal(ladle)) return false;
        CompoundTag info = ladle.getTag().getCompound(CONTENT);
        if (!world.dimension().location().toString().equals(info.getString(DIMENSION))) return false;
        long age = world.getGameTime() - info.getLong(LOADED_AT);
        return age >= 0 && age < Magneticraft2ConfigCommon.GENERAL.FoundryLadleHotTicks.get();
    }

    /** Called only after the Foundry validated a molten unit and empty ladle. */
    public static boolean fill(ItemStack ladle, ItemStack original, Level world) {
        if (!ladle.is(ItemRegistry.ITEM_BRONZE_POURING_LADLE.get()) || hasMetal(ladle) ||
                !original.is(ItemRegistry.ITEM_BRONZE_INGOT.get()) || original.getCount() != 1 ||
                world.isClientSide) return false;
        CompoundTag info = new CompoundTag();
        info.put(SOURCE, original.save(new CompoundTag()));
        info.putLong(LOADED_AT, world.getGameTime());
        info.putString(DIMENSION, world.dimension().location().toString());
        ladle.getOrCreateTag().put(CONTENT, info);
        return true;
    }

    /** Called only after the Basin checks the mold and available output slot. */
    public static ItemStack takeHot(ItemStack ladle, Level world) {
        if (world.isClientSide || !isHot(ladle, world)) return ItemStack.EMPTY;
        ItemStack metal = storedIngot(ladle).copy();
        clear(ladle);
        return metal;
    }

    private static void clear(ItemStack ladle) {
        if (!ladle.hasTag()) return;
        ladle.getTag().remove(CONTENT);
        if (ladle.getTag().isEmpty()) ladle.setTag(null);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!hasMetal(stack)) return InteractionResultHolder.pass(stack);
        if (world.isClientSide) return InteractionResultHolder.success(stack);

        if (isHot(stack, world)) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft2.ladle.hot"), true);
        } else if (player.isShiftKeyDown()) {
            // Cold ladle recovery: one solid ingot with its exact original NBT.
            ItemStack returnMetal = storedIngot(stack).copy();
            clear(stack);
            player.getInventory().setChanged();
            if (!player.getInventory().add(returnMetal)) player.drop(returnMetal, false);
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft2.ladle.recovered"), true);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft2.ladle.cold"), true);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        ItemStack ingot = storedIngot(stack);
        if (ingot.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.magneticraft2.ladle.empty"));
        } else {
            tooltip.add(Component.translatable("tooltip.magneticraft2.ladle.loaded",
                    ingot.getHoverName()));
            if (world != null) {
                tooltip.add(Component.translatable(isHot(stack, world)
                        ? "tooltip.magneticraft2.ladle.hot"
                        : "tooltip.magneticraft2.ladle.cold"));
            }
        }
    }
}
