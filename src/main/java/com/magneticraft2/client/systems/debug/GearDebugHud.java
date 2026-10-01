package com.magneticraft2.client.systems.debug;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * Small temporary client-side gear debug readout.
 * Shows synced visual gear data in the action bar while looking at a gear block.
 */
@Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GearDebugHud {
    private static int ticksUntilNextMessage = 0;

    private GearDebugHud() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (ticksUntilNextMessage > 0) {
            ticksUntilNextMessage--;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.hitResult == null) {
            return;
        }

        if (minecraft.hitResult.getType() != HitResult.Type.BLOCK || !(minecraft.hitResult instanceof BlockHitResult blockHitResult)) {
            return;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
        if (!(blockEntity instanceof GearBlockEntity gearBlockEntity)) {
            return;
        }

        float speed = gearBlockEntity.getClientSpeed();
        float torque = gearBlockEntity.getClientTorque();
        float maxTorque = gearBlockEntity.getClientMaxTorque();
        boolean overloaded = gearBlockEntity.isClientOverloaded();
        int direction = gearBlockEntity.getDirectionMultiplier();
        String directionText = direction < 0 ? "CCW" : "CW";
        String overloadText = overloaded ? " | OVERLOADED" : "";
        String nodeType = gearBlockEntity.isShaftLike() ? "Shaft" : "Gear";

        minecraft.player.displayClientMessage(Component.literal(String.format(Locale.ROOT,
                "%s: %.1f RPM | Torque: %.2f / %.2f | Dir: %s | Teeth: %d | Axis: %s | Phase: %.1f | Rot: %.1f%s",
                nodeType,
                speed,
                torque,
                maxTorque,
                directionText,
                gearBlockEntity.getGearTeeth(),
                gearBlockEntity.getGearAxis().getName(),
                gearBlockEntity.getClientMeshPhaseDegrees(),
                gearBlockEntity.getGearNode().getClientRotationDegrees(),
                overloadText
        )), true);

        ticksUntilNextMessage = 5;
    }
}
