package com.magneticraft2.common.item.stage.stone.pots;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author JumpWatch on 01-07-2023
 * @Project mgc2-1.20
 * @version 1.0.0
 */
public class ceramicPot extends Item {
    private static final String WATER_TAG = "Water";

    public ceramicPot() {
        super(new Properties().stacksTo(1).setNoRepair());
    }

    public static boolean containsWater(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(WATER_TAG);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (containsWater(stack)) {
            return InteractionResultHolder.pass(stack);
        }

        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos pos = hit.getBlockPos();
        if (!level.getFluidState(pos).is(FluidTags.WATER) || !level.getFluidState(pos).isSource()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            stack.getOrCreateTag().putBoolean(WATER_TAG, true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!containsWater(stack)) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        boolean canBecomeMud = state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT);

        if (!canBecomeMud) {
            return super.useOn(context);
        }

        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, Blocks.MUD.defaultBlockState());
            if (stack.hasTag()) {
                stack.getTag().remove(WATER_TAG);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable Level level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable(
                containsWater(stack)
                        ? "tooltip.magneticraft2.ceramic_pot.water"
                        : "tooltip.magneticraft2.ceramic_pot.empty"
        ));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
