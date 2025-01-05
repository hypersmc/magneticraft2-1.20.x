package com.magneticraft2.common.blockentity.stage.stone;

import com.magneticraft2.common.block.stage.stone.BellowsMultiblockModule;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
import com.magneticraft2.common.systems.Multiblocking.core.modules.PSIModule;
import com.magneticraft2.common.systems.PRESSURE.CapabilityPressure;
import com.magneticraft2.common.systems.PRESSURE.IPressureStorage;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import com.magneticraft2.common.utils.PressureStorages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 14-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BellowsMultiblockModuleEntity extends BlockEntity implements IMultiblockModule {
    private int Master_X;
    private int Master_Y;
    private int Master_Z;
    public static final Logger LOGGER = LogManager.getLogger("PSIModule");
    private final PressureStorages pressureStorage;
    private final LazyOptional<IPressureStorage> pressure;

    public BellowsMultiblockModuleEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityRegistry.bellowsmultiblockmoduleentity.get(), pPos, pBlockState);
        this.pressureStorage = new PressureStorages(5, 2){
            @Override
            public boolean canReceive() {
                return true;
            }

            @Override
            public boolean canSend() {
                return true;
            }
        };
        this.pressure = LazyOptional.of(() -> pressureStorage);
    }

    @Override
    public boolean isValid(Level world, BlockPos pos) {
        return true;
    }
    public int getStored(){
        return pressureStorage.getPressureStored();
    }
    @Override
    public void onActivate(Level world, BlockPos pos) {
        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
            LOGGER.info("Trying to activate: " + getModuleKey());
        }
        if (!world.isClientSide()) {
            pressure.ifPresent(handler -> handler.setReceive(true));
            pressure.ifPresent(handler -> handler.setSend(true));
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("Successfully activated: " + getModuleKey());
            }
        }
    }

    @Override
    public void onDeactivate(Level world, BlockPos pos) {
        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
            LOGGER.info("Trying to deactivate: " + getModuleKey());
        }
        if (!world.isClientSide()) {
            pressure.ifPresent(handler -> handler.setReceive(false));
            pressure.ifPresent(handler -> handler.setSend(false));
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("Successfully deactivated: " + getModuleKey());
            }
        }
    }
    public void setActive(boolean active) {
        if (this.level != null) {
            BlockState state = this.getBlockState();
            if (state.getValue(BellowsMultiblockModule.ACTIVE) != active) {
                this.level.setBlock(this.worldPosition, state.setValue(BellowsMultiblockModule.ACTIVE, active), 3);
            }
        }
    }

    // Animation trigger example
    public void animateBellows() {
        this.setActive(true); // Compress the bellows
        this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), 20); // Schedule expansion after 1 second
    }

    @Override
    public String getModuleKey() {
        return "psi_module";
    }

    @Override
    public BlockPos getModuleOffset() {
        return new BlockPos(0, 0, 0);
    }

    @Override
    public IMultiblockModule createModule(Level world, BlockPos pos) {
        return new PSIModule();
    }
    public LazyOptional<IPressureStorage> getPressure() {
        return pressure;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityPressure.PRESSURE){
            return pressure.cast();
        }
        return LazyOptional.empty();
    }
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    public CompoundTag sync() {
        level.sendBlockUpdated( worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL );
        CompoundTag tag = super.getUpdateTag();
        loadClientData(tag);
        return null;
    }
    private void loadClientData(CompoundTag tag) {
        this.Master_X = tag.getInt("controller_x");
        this.Master_Y = tag.getInt("controller_y");
        this.Master_Z = tag.getInt("controller_z");
    }
    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        this.Master_X = pTag.getInt("controller_x");
        this.Master_Y = pTag.getInt("controller_y");
        this.Master_Z = pTag.getInt("controller_z");
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.putInt("controller_x", this.Master_X);
        pTag.putInt("controller_y", this.Master_Y);
        pTag.putInt("controller_z", this.Master_Z);
    }
}
