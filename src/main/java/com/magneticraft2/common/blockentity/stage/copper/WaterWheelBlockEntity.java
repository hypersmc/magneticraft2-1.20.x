package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.WaterWheelBlock;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.systems.GEAR.GearNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.magneticraft2.common.block.stage.copper.WaterWheelBlock.ACTIVE;
import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * Continuous water-driven Gear V2 source shared by the small and large wooden wheels.
 */
public class WaterWheelBlockEntity extends GearBlockEntity {
    private static final int WATER_CHECK_INTERVAL = 10;
    private static final int WATER_STOP_CONFIRMATION_SAMPLES = 3;

    private int waterCheckCooldown = 0;
    private int consecutiveDryWaterSamples = 0;

    public WaterWheelBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.WATER_WHEEL_BE.get(), pos, state);
    }

    public static <E extends BlockEntity> void serverTick(Level level,
                                                          BlockPos pos,
                                                          BlockState state,
                                                          E blockEntity) {
        if (level.isClientSide
                || !(blockEntity instanceof WaterWheelBlockEntity wheel)) {
            return;
        }

        boolean activeButSourceLost = state.hasProperty(ACTIVE)
                && state.getValue(ACTIVE)
                && !wheel.getOrCreateGearNode().isSource();

        if (wheel.waterCheckCooldown-- <= 0 || activeButSourceLost) {
            wheel.waterCheckCooldown = WATER_CHECK_INTERVAL - 1;
            wheel.updateWaterDrive();
        }

        wheel.serverTickGear();
    }

    public boolean isLarge() {
        return getBlockState().is(BlockRegistry.WATER_WHEEL_LARGE.get());
    }

    @Override
    public int getGearTeeth() {
        // The 3x3 wheel has roughly a 1.5-block pitch radius, so model its rim as
        // 24 teeth rather than pretending it is the same diameter as the normal
        // 16-tooth wooden large gear.
        return isLarge() ? 24 : 8;
    }

    @Override
    public float getGearMaxTorque() {
        return isLarge() ? 16.0F : 6.0F;
    }

    @Override
    public boolean isShaftLike() {
        return true;
    }

    @Override
    public boolean supportsExternalGearMesh() {
        // Hybrid node: shafts/gears may mount directly on the axle, while visible gears
        // may also mesh against the wheel rim.
        return true;
    }

    @Override
    public Direction.Axis getGearAxis() {
        BlockState state = getBlockState();
        return state.hasProperty(FACING)
                ? state.getValue(FACING).getAxis()
                : Direction.Axis.X;
    }

    @Override
    public float getDefaultSourceSpeed() {
        return isLarge() ? 24.0F : 16.0F;
    }

    @Override
    public float getDefaultSourceTorque() {
        return isLarge() ? 16.0F : 6.0F;
    }

    private void updateWaterDrive() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (isLarge()
                && !WaterWheelBlock.isLargeWheelIntact(
                        level,
                        worldPosition,
                        getGearAxis())) {
            stopWaterDrive();
            return;
        }

        WaterDrive drive = sampleWaterDrive();
        if (!drive.powered()) {
            // Fluid flow values can briefly report no useful tangent while neighboring
            // water updates settle (especially around waterlogged multiblock cells).
            // A real water wheel has inertia anyway, so one transient sample must not
            // hard-stop a source that was spinning normally a moment ago.
            consecutiveDryWaterSamples++;

            GearNode node = getOrCreateGearNode();
            if (node.isSource()
                    && consecutiveDryWaterSamples < WATER_STOP_CONFIRMATION_SAMPLES) {
                updateActiveState(true);
                return;
            }

            consecutiveDryWaterSamples = 0;
            stopWaterDrive();
            return;
        }

        consecutiveDryWaterSamples = 0;
        GearNode node = getOrCreateGearNode();

        boolean changed = !node.isSource()
                || Math.abs(node.getSpeed() - drive.rpm()) > 0.05F
                || Math.abs(node.getTorque() - drive.torque()) > 0.05F
                || node.getDirectionMultiplier() != drive.direction();

        if (changed) {
            node.setSource(true);
            node.setSpeed(drive.rpm());
            node.setTorque(drive.torque());
            node.setMaxTorque(getGearMaxTorque());
            node.setOverloaded(false);
            node.setDirectionMultiplier(drive.direction());
            node.setMeshPhaseDegrees(0.0F);
            node.setSourcePos(worldPosition);

            markHasEverRotatedIfMoving(drive.rpm());
            setChanged();
            updateGearNetwork();
        }

        updateActiveState(true);
    }

    private void stopWaterDrive() {
        GearNode node = getOrCreateGearNode();
        if (node.isSource()) {
            setSource(false, 0.0F, 0.0F);
        }
        updateActiveState(false);
    }

    private WaterDrive sampleWaterDrive() {
        int radius = isLarge() ? 2 : 1;
        Direction.Axis axis = getGearAxis();
        Vec3 axisVector = axis == Direction.Axis.X
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 0.0D, 1.0D);

        double signedDrive = 0.0D;
        double absoluteDrive = 0.0D;
        int flowingSamples = 0;

        Vec3 center = Vec3.atCenterOf(worldPosition);

        for (int horizontal = -radius; horizontal <= radius; horizontal++) {
            for (int vertical = -radius; vertical <= radius; vertical++) {
                if (horizontal == 0 && vertical == 0) {
                    continue;
                }

                // Large wheels sample both their immediate rim and the water just outside
                // the 3x3 structure. The outside ring is especially important because
                // generated filler cells may themselves be waterlogged.
                if (isLarge()
                        && Math.max(Math.abs(horizontal), Math.abs(vertical)) < radius - 1) {
                    continue;
                }

                BlockPos samplePos = axis == Direction.Axis.X
                        ? worldPosition.offset(0, vertical, horizontal)
                        : worldPosition.offset(horizontal, vertical, 0);

                FluidState fluidState = level.getFluidState(samplePos);
                if (!fluidState.is(FluidTags.WATER)) {
                    continue;
                }

                Vec3 flow = fluidState.getFlow(level, samplePos);

                // Vanilla FlowingFluid#getFlow() only injects its downward component for
                // FALLING water in certain solid-face situations. Around waterlogged wheel
                // cells that can produce a zero vector even though a visible falling stream
                // is still hitting the paddles. Falling water is unambiguously moving down,
                // so preserve that physical information explicitly.
                if (flow.lengthSqr() < 0.0001D
                        && fluidState.hasProperty(FlowingFluid.FALLING)
                        && fluidState.getValue(FlowingFluid.FALLING)) {
                    flow = new Vec3(0.0D, -1.0D, 0.0D);
                }

                if (flow.lengthSqr() < 0.0001D) {
                    continue;
                }

                Vec3 radiusVector = Vec3.atCenterOf(samplePos).subtract(center);
                double tangential = axisVector.dot(radiusVector.cross(flow));
                if (Math.abs(tangential) < 0.01D) {
                    continue;
                }

                // Horizontal flow interacting with the lower half is the normal undershot
                // case. Falling water above the axle also gets full leverage for an
                // overshot wheel. Upper horizontal flow gets a smaller contribution so a
                // completely submerged wheel does not unrealistically double its output.
                double weight = vertical <= 0 || flow.y < -0.15D
                        ? 1.0D
                        : 0.25D;

                signedDrive += tangential * weight;
                absoluteDrive += Math.abs(tangential) * weight;
                flowingSamples++;
            }
        }

        if (flowingSamples == 0
                || absoluteDrive < 0.05D
                || Math.abs(signedDrive) < 0.03D) {
            return WaterDrive.STOPPED;
        }

        double normalization = isLarge() ? 7.0D : 1.75D;
        float engagement = (float) Math.max(
                0.0D,
                Math.min(1.0D, absoluteDrive / normalization)
        );

        float minRpm = isLarge() ? 18.0F : 12.0F;
        float maxRpm = isLarge() ? 24.0F : 16.0F;
        float minTorque = isLarge() ? 8.0F : 3.0F;
        float maxTorque = isLarge() ? 16.0F : 6.0F;

        float rpm = minRpm + (maxRpm - minRpm) * engagement;
        float torque = minTorque + (maxTorque - minTorque) * engagement;
        // signedDrive is the actual right-hand-rule torque around the wheel axle:
        //
        //     tau = r x F
        //
        // Keep that physical sign authoritative all the way through Gear V2. The wheel
        // renderer now uses the same Gear V2 angle as the rest of the network, so there
        // must not be a second sign conversion here.
        int direction = signedDrive >= 0.0D ? 1 : -1;

        return new WaterDrive(true, rpm, torque, direction);
    }

    private void updateActiveState(boolean active) {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(ACTIVE)
                && state.getValue(ACTIVE) != active) {
            level.setBlock(
                    worldPosition,
                    state.setValue(ACTIVE, active),
                    Block.UPDATE_CLIENTS
            );
        }

        if (isLarge()) {
            for (BlockPos fillerPos : WaterWheelBlock.largeWheelFillerPositions(
                    worldPosition,
                    getGearAxis())) {
                BlockState fillerState = level.getBlockState(fillerPos);
                if (!fillerState.is(BlockRegistry.WATER_WHEEL_FILLER.get())) {
                    continue;
                }

                int horizontal = getGearAxis() == Direction.Axis.X
                        ? fillerPos.getZ() - worldPosition.getZ()
                        : fillerPos.getX() - worldPosition.getX();
                int vertical = fillerPos.getY() - worldPosition.getY();

                com.magneticraft2.common.block.stage.copper.WaterWheelFillerBlock.Part expectedPart =
                        WaterWheelBlock.partForOffset(horizontal, vertical);

                BlockState correctedState = fillerState
                        .setValue(
                                com.magneticraft2.common.block.stage.copper.WaterWheelFillerBlock.PART,
                                expectedPart
                        )
                        .setValue(
                                com.magneticraft2.common.block.stage.copper.WaterWheelFillerBlock.ACTIVE,
                                active
                        );

                if (!correctedState.equals(fillerState)) {
                    level.setBlock(
                            fillerPos,
                            correctedState,
                            Block.UPDATE_CLIENTS
                    );
                }
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (!isLarge()) {
            // Small paddles intentionally protrude slightly into the neighboring water
            // cell, so the default one-block BE render box is too tight.
            return new AABB(
                    worldPosition.getX() - 0.25D,
                    worldPosition.getY() - 0.25D,
                    worldPosition.getZ() - 0.25D,
                    worldPosition.getX() + 1.25D,
                    worldPosition.getY() + 1.25D,
                    worldPosition.getZ() + 1.25D
            );
        }

        Direction.Axis axis = getGearAxis();
        if (axis == Direction.Axis.X) {
            return new AABB(
                    worldPosition.getX() - 0.10D,
                    worldPosition.getY() - 1.30D,
                    worldPosition.getZ() - 1.30D,
                    worldPosition.getX() + 1.10D,
                    worldPosition.getY() + 2.30D,
                    worldPosition.getZ() + 2.30D
            );
        }

        return new AABB(
                worldPosition.getX() - 1.30D,
                worldPosition.getY() - 1.30D,
                worldPosition.getZ() - 0.10D,
                worldPosition.getX() + 2.30D,
                worldPosition.getY() + 2.30D,
                worldPosition.getZ() + 1.10D
        );
    }

    private record WaterDrive(boolean powered,
                              float rpm,
                              float torque,
                              int direction) {
        private static final WaterDrive STOPPED =
                new WaterDrive(false, 0.0F, 0.0F, 1);
    }
}
