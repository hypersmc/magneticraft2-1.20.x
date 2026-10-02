package com.magneticraft2.common.item.stage.stone.tools;

import com.magneticraft2.common.blockentity.stage.stone.PitKilnBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * @author JumpWatch on 14-07-2023
 * @Project mgc2-1.20
 * @version 1.0.0
 */
public class FireStarter extends Item {
    public FireStarter() {
        super(new Properties().stacksTo(1).setNoRepair().defaultDurability(5).durability(5));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player)) {
            super.releaseUsing(stack, level, livingEntity, timeLeft);
            return;
        }

        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            super.releaseUsing(stack, level, livingEntity, timeLeft);
            return;
        }

        BlockPos pos = hit.getBlockPos();
        BlockPos above = pos.above();

        if (level.isClientSide()) {
            Vec3 loc = hit.getLocation();
            makeEffects(level, loc.x, loc.y, loc.z, timeLeft, getUseDuration(stack), level.random);
        } else if (timeLeft <= 1) {
            boolean ignited = false;
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof PitKilnBlockEntity pitKiln) {
                ignited = pitKiln.activate(level.getBlockState(pos), level, pos);
                if (ignited && level.isEmptyBlock(above)) {
                    level.setBlock(above, Blocks.FIRE.defaultBlockState(), 11);
                }
            } else if (level.isEmptyBlock(above)) {
                level.setBlock(above, Blocks.FIRE.defaultBlockState(), 11);
                ignited = true;
            }

            if (ignited && !player.isCreative()) {
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            }
        }

        super.releaseUsing(stack, level, livingEntity, timeLeft);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (context.getHand() != InteractionHand.MAIN_HAND || world.isClientSide) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        player.startUsingItem(InteractionHand.MAIN_HAND);
        return InteractionResult.SUCCESS;
    }

    private void makeEffects(Level world,
                             double x,
                             double y,
                             double z,
                             int timeLeft,
                             int total,
                             RandomSource random) {
        int count = total - timeLeft;
        if (random.nextFloat() + 0.3 < count / (double) total) {
            world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0F, 0.1F, 0.0F);
        }
        if (timeLeft < 10 && random.nextFloat() + 0.3 < count / (double) total) {
            world.addParticle(ParticleTypes.FLAME, x, y, z, 0.0F, 0.1F, 0.0F);
        }
    }
}
