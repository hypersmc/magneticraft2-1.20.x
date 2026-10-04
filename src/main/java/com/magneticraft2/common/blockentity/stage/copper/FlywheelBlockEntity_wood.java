package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wooden/copper flywheel with stored rotational inertia.
 *
 * GearNetworkManager treats a charged flywheel as a temporary source only when
 * no real source remains connected to its component. It therefore carries a
 * light branch through short source interruptions without fighting an active
 * Water Wheel or hand crank.
 */
public class FlywheelBlockEntity_wood
        extends GearBlockEntity {

    public static final float INERTIA_TORQUE = 6.0F;
    private static final float STOP_SPEED = 0.25F;

    private float storedSpeed = 0.0F;
    private int storedDirection = 1;

    public FlywheelBlockEntity_wood(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry
                        .FLYWHEEL_BE_WOOD
                        .get(),
                pos,
                state
        );
    }

    public static <E extends BlockEntity> void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            E blockEntity) {
        if (level.isClientSide
                || !(blockEntity
                instanceof FlywheelBlockEntity_wood flywheel)) {
            return;
        }

        flywheel.serverTickGear();
        flywheel.markHasEverRotatedIfMoving(
                flywheel.getServerSpeed()
        );
    }

    public float getStoredSpeed() {
        return storedSpeed;
    }

    public boolean hasStoredInertia() {
        return storedSpeed > STOP_SPEED;
    }

    public void setPassiveForExternalDrive() {
        GearNodeAccess.setSource(
                getOrCreateGearNode(),
                false
        );
    }

    public void prepareAsInertialSource() {
        if (!hasStoredInertia()) {
            GearNodeAccess.setSource(
                    getOrCreateGearNode(),
                    false
            );
            return;
        }

        var node = getOrCreateGearNode();
        node.setSource(true);
        node.setSpeed(storedSpeed);
        node.setTorque(INERTIA_TORQUE);
        node.setMaxTorque(getGearMaxTorque());
        node.setOverloaded(false);
        node.setDirectionMultiplier(
                storedDirection
        );
        node.setSourcePos(
                worldPosition
        );
    }

    public void captureExternalDrive(
            float speed,
            int directionMultiplier) {
        if (speed <= STOP_SPEED) {
            return;
        }

        storedSpeed = speed;
        storedDirection =
                directionMultiplier < 0
                        ? -1
                        : 1;

        setChanged();
    }

    public void coastTick(
            float sourceEquivalentDemand,
            boolean overloaded) {
        if (storedSpeed <= STOP_SPEED) {
            stopInertia();
            return;
        }

        float loadRatio =
                Math.max(
                        0.0F,
                        sourceEquivalentDemand
                                / INERTIA_TORQUE
                );

        float decay =
                overloaded
                        ? 0.15F
                        : Math.min(
                                0.06F,
                                0.0025F
                                        + loadRatio
                                        * 0.010F
                        );

        storedSpeed *=
                Math.max(
                        0.0F,
                        1.0F - decay
                );

        var node = getOrCreateGearNode();

        if (storedSpeed <= STOP_SPEED) {
            stopInertia();
            return;
        }

        node.setSpeed(storedSpeed);
        node.setTorque(INERTIA_TORQUE);
        node.setDirectionMultiplier(
                storedDirection
        );

        setChanged();
    }

    private void stopInertia() {
        storedSpeed = 0.0F;

        var node = getOrCreateGearNode();
        node.setSource(false);
        node.setSourcePos(null);
        node.setSpeed(0.0F);
        node.setTorque(0.0F);
        node.setOverloaded(false);

        setChanged();
    }

    @Override
    public int getGearTeeth() {
        return 1;
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
        BlockState state = getBlockState();

        return state.hasProperty(
                DirectionalBlock.FACING
        )
                ? state.getValue(
                        DirectionalBlock.FACING
                ).getAxis()
                : Direction.Axis.X;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putFloat(
                "StoredSpeed",
                storedSpeed
        );
        tag.putInt(
                "StoredDirection",
                storedDirection
        );
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        storedSpeed =
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "StoredSpeed"
                        )
                );

        storedDirection =
                tag.getInt(
                        "StoredDirection"
                ) < 0
                        ? -1
                        : 1;
    }

    /**
     * Tiny helper only to keep the intent of setPassiveForExternalDrive obvious.
     */
    private static final class GearNodeAccess {
        private static void setSource(
                com.magneticraft2.common.systems.GEAR.GearNode node,
                boolean source) {
            node.setSource(source);
        }
    }
}
