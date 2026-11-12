package com.magneticraft2.mixin.client;

import com.magneticraft2.client.menu.LivingMenuSpawnBudget;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Minecraft 1.20.1 hardcodes two mutually dependent values in prepareLevels:
 * START ticket level 11 and the 441 ticking chunks that ticket provides.
 * Change neither without the other. The budget class returns the vanilla
 * values for every world except the marked, currently loading menu runtime.
 *
 * Client-only Mixin: no changes to dedicated servers or normal saved games.
 */
@Mixin(MinecraftServer.class)
public abstract class LivingMenuSpawnRadiusMixin {
    // Keep the startup progress listener's expected radius in sync with the
    // reduced START ticket. Otherwise the loading progress would be misleading.
    @ModifyConstant(method = "loadLevel", constant = @Constant(intValue = 11), require = 1)
    private int magneticraft2$menuSpawnProgressRadius(int original) {
        return LivingMenuSpawnBudget.ticketLevel((MinecraftServer) (Object) this, original);
    }

    @ModifyConstant(method = "prepareLevels", constant = @Constant(intValue = 11), require = 1)
    private int magneticraft2$menuStartTicketLevel(int original) {
        return LivingMenuSpawnBudget.ticketLevel((MinecraftServer) (Object) this, original);
    }

    @ModifyConstant(method = "prepareLevels", constant = @Constant(intValue = 441), require = 1)
    private int magneticraft2$menuTickingCount(int original) {
        return LivingMenuSpawnBudget.expectedTickingChunks((MinecraftServer) (Object) this, original);
    }
}
