package com.magneticraft2.common.item.stage.stone.tools;

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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * @author JumpWatch on 14-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class FireStarter extends Item {
    private static final Logger LOGGER = LogManager.getLogger("FireStarter");

    public FireStarter() {
        super(new Properties().stacksTo(1).setNoRepair().defaultDurability(5).setNoRepair().durability(5));
    }


    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return 72;
    }

    @Override
    public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity, int pTimeCharged) {
        if (pLivingEntity instanceof final Player player) {
            final BlockHitResult hit = getPlayerPOVHitResult(pLevel, player, ClipContext.Fluid.NONE);
            final BlockPos pos = hit.getBlockPos();
            final BlockPos above = pos.above();
            if (pLevel.isClientSide()){
                Vec3 loc = hit.getLocation();
                makeEffects(pLevel, player, loc.x, loc.y, loc.z, pTimeCharged, getUseDuration(pStack), pLevel.random);
            }else if (pTimeCharged == 1){
                if (!player.isCreative()){
                    pStack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                }
                pLevel.setBlock(above, Blocks.FIRE.defaultBlockState(), 11);
            }


        }
        super.releaseUsing(pStack, pLevel, pLivingEntity, pTimeCharged);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level world = pContext.getLevel();
        if (pContext.getHand() != InteractionHand.MAIN_HAND || world.isClientSide)
            return InteractionResult.PASS;
        Player p = pContext.getPlayer();
        if (p == null)
            return InteractionResult.FAIL;
        p.startUsingItem(InteractionHand.MAIN_HAND);
        return InteractionResult.SUCCESS;
    }

    private void makeEffects(Level world, Player player, double x, double y, double z, int pTimeCharged, int total, RandomSource random)
    {
        int count = total - pTimeCharged;
        if (random.nextFloat() + 0.3 < count / (double) total)
        {
            world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0F, 0.1F, 0.0F);
        }
        if (pTimeCharged < 10 && random.nextFloat() + 0.3 < count / (double) total)
        {
            world.addParticle(ParticleTypes.FLAME, x, y, z, 0.0F, 0.1F, 0.0F);
        }
        if (count % 3 == 1)
        {
            /// TODO: 29-03-2023 We need to have a sound for firestarter
            //player.playSound(, 0.5F, 0.05F);
        }
    }
}
