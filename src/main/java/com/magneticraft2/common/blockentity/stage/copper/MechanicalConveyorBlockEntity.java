package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalConveyorBlock;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * Gear V2 conveyor roller + deterministic item transport.
 *
 * Items remain normal ItemEntity instances. While they are on the transport surface,
 * their horizontal velocity is authored every tick from the conveyor RPM and they are
 * gently centered on the belt. At the end of a line they simply leave the capture box
 * and continue with the last belt velocity under normal Minecraft physics.
 */
public class MechanicalConveyorBlockEntity extends GearBlockEntity {
    public static final float TORQUE_DEMAND = 0.25F;
    public static final float MIN_TRANSPORT_RPM = 5.0F;

    // One full roller revolution advances the belt one texture/block repeat. This keeps
    // physical item speed and the phase-locked Gear V2 belt animation in agreement.
    private static final double BELT_TRAVEL_PER_REVOLUTION = 1.0D;
    private static final double MAX_BLOCKS_PER_TICK = 0.18D;
    private static final double CENTERING_PER_TICK = 0.075D;
    private static final String ITEM_TICK_TAG = "MGC2ConveyorTick";

    public MechanicalConveyorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.MECHANICAL_CONVEYOR_BE.get(), pos, state);
    }

    @Override
    public int getGearTeeth() {
        return 8;
    }

    @Override
    public float getGearMaxTorque() {
        return 16.0F;
    }

    @Override
    public boolean isShaftLike() {
        return true;
    }

    @Override
    public Direction.Axis getGearAxis() {
        Direction facing = getConveyorFacing();
        return facing.getAxis() == Direction.Axis.Z
                ? Direction.Axis.X
                : Direction.Axis.Z;
    }

    public Direction getConveyorFacing() {
        BlockState state = getBlockState();
        if (!state.hasProperty(FACING)) {
            return Direction.NORTH;
        }

        Direction facing = state.getValue(FACING);
        return facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
    }

    public Direction getTravelDirection() {
        return getDirectionMultiplier() < 0
                ? getConveyorFacing().getOpposite()
                : getConveyorFacing();
    }

    public boolean isTransportRunning() {
        GearNode node = getOrCreateGearNode();
        if (node.isOverloaded() || Math.abs(node.getSpeed()) < MIN_TRANSPORT_RPM) {
            return false;
        }

        if (level == null || level.isClientSide) {
            return Math.abs(node.getClientSpeed()) >= MIN_TRANSPORT_RPM
                    && !node.isClientOverloaded();
        }

        GearNetworkManager.MechanicalLoadState loadState =
                GearNetworkManager.getInstance().getMechanicalLoadState(level, worldPosition);
        return loadState.supplied();
    }

    public double getVisualBeltTravel(float partialTicks) {
        return getVisualRotationDegrees(partialTicks) / 360.0D;
    }

    public double getTransportBlocksPerTick() {
        GearNode node = getOrCreateGearNode();
        float rpm = level != null && level.isClientSide
                ? node.getClientSpeed()
                : node.getEffectiveSpeed();

        if (Math.abs(rpm) < MIN_TRANSPORT_RPM) {
            return 0.0D;
        }

        return Math.min(
                MAX_BLOCKS_PER_TICK,
                Math.abs(rpm) * BELT_TRAVEL_PER_REVOLUTION / 1200.0D
        );
    }

    public static <E extends BlockEntity> void serverTick(Level level,
                                                          BlockPos pos,
                                                          BlockState state,
                                                          E blockEntity) {
        if (level.isClientSide || !(blockEntity instanceof MechanicalConveyorBlockEntity conveyor)) {
            return;
        }

        GearNetworkManager network = GearNetworkManager.getInstance();

        // A long conveyor line must not trigger a complete Gear V2 recalculation once per
        // conveyor block every tick. GearBlock#onPlace registers fresh sections, and this
        // fallback covers chunk/world reloads where the runtime network map starts empty.
        if (network.getGear(conveyor.worldPosition, level) == null) {
            conveyor.updateGearNetwork();
        }

        network.setMechanicalLoad(
                level,
                conveyor.worldPosition,
                conveyor.worldPosition,
                TORQUE_DEMAND,
                true
        );

        conveyor.moveItems(level);
    }

    private void moveItems(Level level) {
        GearNode node = getOrCreateGearNode();
        GearNetworkManager.MechanicalLoadState loadState =
                GearNetworkManager.getInstance().getMechanicalLoadState(level, worldPosition);

        double speed = getTransportBlocksPerTick();
        if (node.isOverloaded()
                || speed <= 0.0001D
                || !loadState.supplied()) {
            stopItemsOnBelt(level);
            return;
        }

        Direction travelDirection = getTravelDirection();
        Vec3 travel = new Vec3(
                travelDirection.getStepX() * speed,
                0.0D,
                travelDirection.getStepZ() * speed
        );

        long gameTick = level.getGameTime();
        for (ItemEntity item : getItemsOnBelt(level)) {
            if (item.getPersistentData().getLong(ITEM_TICK_TAG) == gameTick) {
                continue;
            }
            item.getPersistentData().putLong(ITEM_TICK_TAG, gameTick);

            Vec3 currentMotion = item.getDeltaMovement();
            double x = item.getX();
            double z = item.getZ();

            // A real conveyor guides material toward its center rather than allowing
            // dropped items to drift off the side from tiny pre-existing velocities.
            if (travelDirection.getAxis() == Direction.Axis.Z) {
                x = approach(x, worldPosition.getX() + 0.5D, CENTERING_PER_TICK);
            } else {
                z = approach(z, worldPosition.getZ() + 0.5D, CENTERING_PER_TICK);
            }

            item.setPos(x, item.getY(), z);
            item.setDeltaMovement(travel.x, currentMotion.y, travel.z);
            item.hurtMarked = true;
        }
    }

    private void stopItemsOnBelt(Level level) {
        long gameTick = level.getGameTime();
        for (ItemEntity item : getItemsOnBelt(level)) {
            if (item.getPersistentData().getLong(ITEM_TICK_TAG) == gameTick) {
                continue;
            }
            item.getPersistentData().putLong(ITEM_TICK_TAG, gameTick);

            Vec3 motion = item.getDeltaMovement();
            item.setDeltaMovement(0.0D, motion.y, 0.0D);
            item.hurtMarked = true;
        }
    }

    private List<ItemEntity> getItemsOnBelt(Level level) {
        double minX = worldPosition.getX() + 0.08D;
        double maxX = worldPosition.getX() + 0.92D;
        double minZ = worldPosition.getZ() + 0.08D;
        double maxZ = worldPosition.getZ() + 0.92D;

        // Block collision top is Y + 0.375. ItemEntity origin settles slightly above it.
        AABB capture = new AABB(
                minX,
                worldPosition.getY() + 0.34D,
                minZ,
                maxX,
                worldPosition.getY() + 0.90D,
                maxZ
        );

        return level.getEntitiesOfClass(
                ItemEntity.class,
                capture,
                item -> item.isAlive() && !item.getItem().isEmpty()
        );
    }

    private static double approach(double current, double target, double amount) {
        double difference = target - current;
        if (Math.abs(difference) <= amount) {
            return target;
        }
        return current + Math.copySign(amount, difference);
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            GearNetworkManager.getInstance().removeMechanicalLoad(level, worldPosition);
        }
        super.setRemoved();
    }
}
