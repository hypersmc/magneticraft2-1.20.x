package com.magneticraft2.client.systems.debug;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.ConveyorRollerBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.WaterWheelBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.PulleyBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.stone.PrimitiveGrinderBMultiblockEntity;
import com.magneticraft2.common.systems.GEAR.BeltConnectionManager;
import com.magneticraft2.common.systems.GEAR.ItemBeltConnectionManager;
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

        if (blockEntity instanceof PrimitiveGrinderBMultiblockEntity grinder) {
            String connectionText = grinder.isMechanicalInputConnected() ? "CONNECTED" : "NO SHAFT";
            String overloadText = grinder.isMechanicalInputOverloaded() ? " | OVERLOADED" : "";
            String powerText;
            if (!grinder.isMechanicalLoadActive()) {
                powerText = "IDLE";
            } else if (grinder.isMechanicalLoadSupplied()) {
                powerText = "RUNNING";
            } else {
                powerText = "STARVED";
            }

            minecraft.player.displayClientMessage(Component.literal(String.format(Locale.ROOT,
                    "Grinder %s | %s | %.1f RPM | Available: %.2fT | Load: %.2fT (%.2fT source) | Network: %.2f / %.2fT | %s%s",
                    grinder.getMechanicalInputDirection().getName().toUpperCase(Locale.ROOT),
                    connectionText,
                    grinder.getMechanicalSpeed(),
                    grinder.getMechanicalTorque(),
                    grinder.isMechanicalLoadActive() ? PrimitiveGrinderBMultiblockEntity.REQUIRED_TORQUE : 0.0F,
                    grinder.getMechanicalSourceEquivalentDemand(),
                    grinder.getMechanicalTotalSourceDemand(),
                    grinder.getMechanicalSourceTorqueCapacity(),
                    powerText,
                    overloadText
            )), true);

            ticksUntilNextMessage = 5;
            return;
        }

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
        String nodeType = gearBlockEntity instanceof PulleyBlockEntity_wood ? "Pulley"
                : gearBlockEntity instanceof ConveyorRollerBlockEntity ? "Belt Roller"
                : gearBlockEntity instanceof WaterWheelBlockEntity waterWheel
                ? (waterWheel.isLarge() ? "Large Water Wheel" : "Small Water Wheel")
                : (gearBlockEntity.isShaftLike() ? "Shaft" : "Gear");
        String beltText = "";
        if (gearBlockEntity instanceof PulleyBlockEntity_wood pulley && pulley.getBeltPartner() != null) {
            int colliderCount = BeltConnectionManager.getCollisionProxyCount(
                    minecraft.level,
                    pulley.getBlockPos(),
                    pulley.getBeltPartner()
            );
            beltText = " | Belt -> " + pulley.getBeltPartner().toShortString()
                    + " | Colliders: " + colliderCount;
        } else if (gearBlockEntity instanceof ConveyorRollerBlockEntity roller
                && roller.getItemBeltPartner() != null) {
            ItemBeltConnectionManager.PhysicalBeltState physicalState =
                    ItemBeltConnectionManager.getPhysicalBeltState(
                            minecraft.level,
                            roller.getBlockPos(),
                            roller.getItemBeltPartner()
                    );
            beltText = " | Item Belt -> " + roller.getItemBeltPartner().toShortString()
                    + " | Cells: " + physicalState.presentCells()
                    + "/" + physicalState.expectedCells();
        }

        minecraft.player.displayClientMessage(Component.literal(String.format(Locale.ROOT,
                "%s: %.1f RPM | Torque: %.2f / %.2f | Dir: %s | Teeth: %d | Axis: %s | Phase: %.1f | Rot: %.1f%s%s",
                nodeType,
                speed,
                torque,
                maxTorque,
                directionText,
                gearBlockEntity.getGearTeeth(),
                gearBlockEntity.getGearAxis().getName(),
                gearBlockEntity.getClientMeshPhaseDegrees(),
                gearBlockEntity.getGearNode().getClientRotationDegrees(),
                overloadText,
                beltText
        )), true);

        ticksUntilNextMessage = 5;
    }
}
