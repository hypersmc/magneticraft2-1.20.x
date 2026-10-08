package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import com.magneticraft2.common.systems.networking.GearSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

import static com.magneticraft2.common.systems.mgc2Network.CHANNEL;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * @version 1.0.0
 */
public abstract class GearBlockEntity extends BlockEntity {
    protected static final float VISUAL_STOP_EPSILON = 0.01F;

    protected GearNode gearNode;
    private boolean hasEverRotated = false;
    private float clientVisualRotationDegrees = 0.0F;
    private double lastClientVisualTime = Double.NaN;
    private boolean clientVisualInitialized = false;

    // Latest authoritative server angle and the client game time at which it arrived.
    // Rendering never hard-snaps to this after initialization; it continuously integrates
    // local RPM and gently corrects toward the predicted authoritative phase.
    private float clientAuthoritativeRotationDegrees = 0.0F;
    private double clientAuthoritativeSyncTime = Double.NaN;

    public GearBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.gearNode = new GearNode(pos);
        this.gearNode.setTeeth(getGearTeeth());
        this.gearNode.setAxis(getGearAxisFromState(state));
        this.gearNode.setMaxTorque(getGearMaxTorque());
    }

    public GearNode getGearNode() {
        return gearNode;
    }

    public GearNode getOrCreateGearNode() {
        if (gearNode == null) {
            gearNode = new GearNode(worldPosition);
        }
        gearNode.setTeeth(getGearTeeth());
        gearNode.setAxis(getGearAxis());
        gearNode.setMaxTorque(getGearMaxTorque());
        gearNode.setShaftLike(isShaftLike());
        return gearNode;
    }

    /**
     * Number used for simple RPM ratio calculation.
     * Larger number = larger/slower gear when driven by a smaller gear.
     */
    public int getGearTeeth() {
        return 8;
    }

    /**
     * Temporary wood gear torque limit.
     * This is intentionally simple until real machines/loads exist.
     */
    public float getGearMaxTorque() {
        return 8.0F;
    }

    /**
     * Shaft-like nodes transfer motion only along their own spin axis.
     * They do not side-mesh like external gear teeth.
     */
    public boolean isShaftLike() {
        return false;
    }

    /**
     * Whether an adjacent axial shaft is allowed to connect through this face.
     *
     * Ordinary shafts accept both ends. Mechanical consumers can override this
     * to expose a true one-sided input without becoming an inline transmission.
     */
    public boolean acceptsShaftConnection(Direction side) {
        return side != null
                && side.getAxis() == getGearAxis();
    }

    /**
     * Whether this node can also participate in external tooth/rim meshing.
     *
     * Most shaft-like nodes return false. Hybrid source components such as Water Wheels
     * can override this so they still accept shafts on the axle while also driving a
     * gear placed against the wheel rim.
     */
    public boolean supportsExternalGearMesh() {
        return !isShaftLike();
    }

    /**
     * The axis this gear visually spins around.
     * Subclasses with vertical placement flags should override this.
     */
    public Direction.Axis getGearAxis() {
        return getGearAxisFromState(getBlockState());
    }

    protected Direction.Axis getGearAxisFromState(BlockState state) {
        if (state != null && state.hasProperty(DirectionalBlock.FACING)) {
            return state.getValue(DirectionalBlock.FACING).getAxis();
        }
        return Direction.Axis.Y;
    }

    public float getDefaultSourceSpeed() {
        return 60.0F;
    }

    public float getDefaultSourceTorque() {
        return 8.0F;
    }

    public void setSource(boolean source, float speed, float torque) {
        GearNode node = getOrCreateGearNode();
        node.setSource(source);
        if (source) {
            node.setSpeed(speed);
            node.setTorque(torque);
            node.setOverloaded(torque > node.getMaxTorque());
            node.setDirectionMultiplier(1);
            node.setMeshPhaseDegrees(0.0F);
            node.setSourcePos(worldPosition);
            markHasEverRotatedIfMoving(speed);
        } else {
            // Do not instantly stop. Leave current speed/torque for network decay.
            node.setSource(false);
        }
        setChanged();
        updateGearNetwork();
    }

    public boolean isSourceGear() {
        return getOrCreateGearNode().isSource();
    }

    public float getServerSpeed() {
        return getOrCreateGearNode().getSpeed();
    }

    public float getClientSpeed() {
        return getOrCreateGearNode().getClientSpeed();
    }

    public float getClientTorque() {
        return getOrCreateGearNode().getClientTorque();
    }

    public float getClientMaxTorque() {
        return getOrCreateGearNode().getClientMaxTorque();
    }

    public boolean isClientOverloaded() {
        return getOrCreateGearNode().isClientOverloaded();
    }

    public float getClientMeshPhaseDegrees() {
        return getOrCreateGearNode().getClientMeshPhaseDegrees();
    }

    public int getDirectionMultiplier() {
        return getOrCreateGearNode().getDirectionMultiplier();
    }

    public boolean shouldRenderGearWithBlockEntity() {
        GearNode node = getOrCreateGearNode();
        return hasEverRotated
                || hasVisiblePhaseOffset(node.getMeshPhaseDegrees())
                || hasVisiblePhaseOffset(node.getClientMeshPhaseDegrees());
    }

    private boolean hasVisiblePhaseOffset(float phaseDegrees) {
        float normalized = normalizeVisualDegrees(phaseDegrees);
        return normalized > VISUAL_STOP_EPSILON && normalized < 360.0F - VISUAL_STOP_EPSILON;
    }

    public void markHasEverRotatedIfMoving(float speed) {
        if (Math.abs(speed) > VISUAL_STOP_EPSILON) {
            hasEverRotated = true;
        }
    }

    /**
     * Redstone/debug source hook. Existing subclasses can override it, but the base behavior is useful for testing.
     */
    public void setPowered(boolean powered) {
        if (powered) {
            setSource(true, getDefaultSourceSpeed(), getDefaultSourceTorque());
        } else if (isSourceGear()) {
            setSource(false, 0.0F, 0.0F);
        }
    }

    public void updateGearNetwork() {
        if (level != null && !level.isClientSide) {
            GearNetworkManager.getInstance().addOrUpdateGear(this);
        }
    }

    public void serverTickGear() {
        if (level != null && !level.isClientSide) {
            GearNetworkManager.getInstance().tickGear(this);
            markHasEverRotatedIfMoving(getServerSpeed());
        }
    }

    public void sendGearSyncPacket() {
        if (level != null && !level.isClientSide) {
            GearNode node = getOrCreateGearNode();
            CHANNEL.send(PacketDistributor.ALL.noArg(), new GearSyncPacket(
                    node.getPosition(),
                    node.getSpeed(),
                    node.getTorque(),
                    node.getMaxTorque(),
                    node.isOverloaded(),
                    node.getMeshPhaseDegrees(),
                    node.getRotationDegrees(),
                    node.getDirectionMultiplier(),
                    node.getSourcePos()
            ));
        }
    }

    public void syncGearState(float speed, float torque, float maxTorque, boolean overloaded, float meshPhaseDegrees, float rotationDegrees, int directionMultiplier, BlockPos sourcePos) {
        GearNode node = getOrCreateGearNode();

        node.updateClientData(speed, torque, maxTorque, overloaded, meshPhaseDegrees, rotationDegrees);
        node.setDirectionMultiplier(directionMultiplier);
        node.setSourcePos(sourcePos);

        Level currentLevel = getLevel();
        double currentGameTime = currentLevel == null
                ? Double.NaN
                : (double) currentLevel.getGameTime();

        clientAuthoritativeRotationDegrees = node.getClientRotationDegrees();
        clientAuthoritativeSyncTime = currentGameTime;

        if (!clientVisualInitialized) {
            clientVisualRotationDegrees = clientAuthoritativeRotationDegrees;
            lastClientVisualTime = currentGameTime;
            clientVisualInitialized = true;
        }

        markHasEverRotatedIfMoving(speed);
    }

    public void checkAndUpdatePower() {
        if (level == null || level.isClientSide) {
            return;
        }
        GearNode node = getOrCreateGearNode();
        GearNode sourceGear = GearNetworkManager.getInstance().getGear(node.getSourcePos(), level);
        setPowered(sourceGear != null && sourceGear.getSpeed() > 0.0F);
    }

    public float getVisualRotationDegrees(float partialTicks) {
        Level currentLevel = getLevel();
        GearNode node = getOrCreateGearNode();

        if (currentLevel == null) {
            return normalizeVisualDegrees(
                    clientVisualRotationDegrees + node.getClientMeshPhaseDegrees()
            );
        }

        double currentVisualTime =
                (double) currentLevel.getGameTime()
                        + (double) partialTicks;

        if (!clientVisualInitialized || Double.isNaN(lastClientVisualTime)) {
            clientVisualRotationDegrees = node.getClientRotationDegrees();
            clientAuthoritativeRotationDegrees = node.getClientRotationDegrees();
            clientAuthoritativeSyncTime = currentVisualTime;
            lastClientVisualTime = currentVisualTime;
            clientVisualInitialized = true;
        }

        double deltaTicks = currentVisualTime - lastClientVisualTime;
        lastClientVisualTime = currentVisualTime;

        if (deltaTicks < 0.0F) {
            deltaTicks = 0.0F;
        } else if (deltaTicks > 2.0F) {
            // A render/chunk pause should not turn into one giant visual jump.
            deltaTicks = 2.0F;
        }

        float rpm = node.isClientOverloaded() ? 0.0F : node.getClientSpeed();
        float degreesPerTick = rpm * 360.0F / 1200.0F;

        if (rpm > VISUAL_STOP_EPSILON) {
            clientVisualRotationDegrees = normalizeVisualDegrees(
                    clientVisualRotationDegrees
                            + degreesPerTick
                            * (float) deltaTicks
                            * node.getDirectionMultiplier()
            );
        }

        // Predict where the authoritative server phase should be *now*, not where it was
        // when the packet happened to arrive. Then remove only a fraction of the shortest
        // angular error each rendered frame. This keeps every node phase-locked without
        // the visible 20 Hz packet snapping that caused Water Wheel/gear judder.
        if (!Double.isNaN(clientAuthoritativeSyncTime)) {
            double authoritativeElapsed =
                    currentVisualTime - clientAuthoritativeSyncTime;
            if (authoritativeElapsed < 0.0F) {
                authoritativeElapsed = 0.0F;
            } else if (authoritativeElapsed > 20.0F) {
                authoritativeElapsed = 20.0F;
            }

            float predictedAuthoritative = normalizeVisualDegrees(
                    clientAuthoritativeRotationDegrees
                            + degreesPerTick
                            * (float) authoritativeElapsed
                            * node.getDirectionMultiplier()
            );

            float phaseError = shortestVisualAngle(
                    predictedAuthoritative - clientVisualRotationDegrees
            );

            // Roughly a quarter of the remaining phase error per game tick. At 60 FPS
            // this is only a few percent per rendered frame, so corrections are invisible
            // but accumulated drift still converges quickly.
            float correctionFactor =
                    (float) Math.min(
                            1.0D,
                            deltaTicks * 0.25D
                    );
            clientVisualRotationDegrees = normalizeVisualDegrees(
                    clientVisualRotationDegrees + phaseError * correctionFactor
            );
        }

        return normalizeVisualDegrees(
                clientVisualRotationDegrees + node.getClientMeshPhaseDegrees()
        );
    }

    private float shortestVisualAngle(float degrees) {
        float wrapped = normalizeVisualDegrees(degrees);
        return wrapped > 180.0F ? wrapped - 360.0F : wrapped;
    }

    private float normalizeVisualDegrees(float degrees) {
        float normalized = degrees % 360.0F;
        if (normalized < 0.0F) {
            normalized += 360.0F;
        }
        return normalized;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (gearNode != null) {
            tag.put("GearNode", gearNode.saveToNBT());
        }
        tag.putBoolean("HasEverRotated", hasEverRotated);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        getOrCreateGearNode();
        if (tag.contains("GearNode")) {
            gearNode.loadFromNBT(tag.getCompound("GearNode"));
        } else {
            gearNode.loadFromNBT(tag);
        }
        gearNode.setTeeth(getGearTeeth());
        gearNode.setAxis(getGearAxis());
        gearNode.setMaxTorque(getGearMaxTorque());
        gearNode.setShaftLike(isShaftLike());
        hasEverRotated = tag.getBoolean("HasEverRotated") || gearNode.getSpeed() > VISUAL_STOP_EPSILON;
    }
}
