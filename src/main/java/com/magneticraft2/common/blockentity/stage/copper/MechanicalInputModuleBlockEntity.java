package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalInputModuleBlock;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Gear V2 shaft node that is allowed to survive multiblock formation.
 */
public class MechanicalInputModuleBlockEntity
        extends GearBlockEntity
        implements IMultiblockModule {

    private int controllerX;
    private int controllerY;
    private int controllerZ;
    private boolean formedModule;

    public MechanicalInputModuleBlockEntity(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry
                        .MECHANICAL_INPUT_MODULE_BE
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
                instanceof MechanicalInputModuleBlockEntity module)) {
            return;
        }

        module.serverTickGear();
        module.markHasEverRotatedIfMoving(
                module.getServerSpeed()
        );
    }

    public boolean isFormedModule() {
        return formedModule;
    }

    @Override
    public boolean isValid(Level world, BlockPos pos) {
        return world.getBlockEntity(pos) == this;
    }

    @Override
    public void onActivate(Level world, BlockPos pos) {
        if (world.isClientSide) {
            return;
        }

        formedModule = true;
        updateFormedState(true);
        setChanged();
        sync();
    }

    @Override
    public void onDeactivate(Level world, BlockPos pos) {
        if (world.isClientSide) {
            return;
        }

        formedModule = false;
        updateFormedState(false);
        setChanged();
        sync();
    }

    private void updateFormedState(boolean formed) {
        if (level == null) {
            return;
        }

        BlockState state =
                level.getBlockState(worldPosition);

        if (state.hasProperty(
                MechanicalInputModuleBlock.FORMED
        ) && state.getValue(
                MechanicalInputModuleBlock.FORMED
        ) != formed) {
            level.setBlock(
                    worldPosition,
                    state.setValue(
                            MechanicalInputModuleBlock.FORMED,
                            formed
                    ),
                    Block.UPDATE_CLIENTS
            );
        }
    }

    @Override
    public String getModuleKey() {
        return "mechanical_input";
    }

    @Override
    public BlockPos getModuleOffset() {
        return worldPosition;
    }

    @Override
    public IMultiblockModule createModule(
            Level world,
            BlockPos pos) {
        return this;
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

        return state.hasProperty(DirectionalBlock.FACING)
                ? state.getValue(DirectionalBlock.FACING)
                        .getAxis()
                : Direction.Axis.X;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        controllerX = tag.getInt("controller_x");
        controllerY = tag.getInt("controller_y");
        controllerZ = tag.getInt("controller_z");
        formedModule = tag.getBoolean("isformed");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putInt("controller_x", controllerX);
        tag.putInt("controller_y", controllerY);
        tag.putInt("controller_z", controllerZ);
        tag.putBoolean("isformed", formedModule);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener>
    getUpdatePacket() {
        return ClientboundBlockEntityDataPacket
                .create(this);
    }

    @Override
    public void onDataPacket(
            Connection net,
            ClientboundBlockEntityDataPacket packet) {
        handleUpdateTag(packet.getTag());
    }

    private void sync() {
        if (level != null) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_ALL
            );
        }
    }
}
