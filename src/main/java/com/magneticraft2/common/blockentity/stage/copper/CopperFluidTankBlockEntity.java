package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.systems.fluid.FluidPipeNetwork;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generic 8-bucket Copper Age fluid tank.
 *
 * The tank is the active endpoint for the otherwise-passive pipe network. Every
 * few ticks it can pull fluid from a formed multiblock Fluid Output reachable
 * through the pipe connected to its top socket. It never causes pipes
 * themselves to tick.
 */
public class CopperFluidTankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 8000;
    public static final int PULL_INTERVAL_TICKS = 5;
    public static final int MAX_PULL_PER_CYCLE = 250;

    private final FluidTank fluidTank =
            new FluidTank(CAPACITY) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                    sync();
                }
            };

    private LazyOptional<IFluidHandler> fluidCapability =
            LazyOptional.of(() -> fluidTank);

    public CopperFluidTankBlockEntity(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry
                        .COPPER_FLUID_TANK_BE
                        .get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CopperFluidTankBlockEntity tank) {
        if (level.isClientSide) {
            return;
        }

        if (Math.floorMod(
                level.getGameTime() + pos.asLong(),
                PULL_INTERVAL_TICKS
        ) != 0) {
            return;
        }

        tank.pullFromPipeNetwork();
    }

    private void pullFromPipeNetwork() {
        if (level == null
                || fluidTank.getSpace() <= 0) {
            return;
        }

        BlockPos connectionPos =
                worldPosition.above();

        if (level.getBlockState(connectionPos).is(
                BlockRegistry.WATER_PIPE.get()
        )) {
            for (FluidPipeNetwork.Endpoint endpoint :
                    FluidPipeNetwork.findEndpoints(
                            level,
                            connectionPos,
                            worldPosition
                    )) {
                BlockEntity endpointEntity =
                        level.getBlockEntity(
                                endpoint.pos()
                        );

                if (!(endpointEntity
                        instanceof MultiblockFluidOutputBlockEntity)) {
                    continue;
                }

                if (pullFrom(endpoint.handler()) > 0) {
                    return;
                }
            }

            return;
        }

        BlockEntity direct =
                level.getBlockEntity(connectionPos);

        if (!(direct
                instanceof MultiblockFluidOutputBlockEntity)) {
            return;
        }

        direct.getCapability(
                        ForgeCapabilities.FLUID_HANDLER,
                        Direction.DOWN
                )
                .resolve()
                .ifPresent(this::pullFrom);
    }

    private int pullFrom(IFluidHandler source) {
        int request =
                Math.min(
                        MAX_PULL_PER_CYCLE,
                        fluidTank.getSpace()
                );

        if (request <= 0) {
            return 0;
        }

        FluidStack simulated;

        if (fluidTank.getFluid().isEmpty()) {
            simulated =
                    source.drain(
                            request,
                            IFluidHandler.FluidAction.SIMULATE
                    );
        } else {
            FluidStack wanted =
                    fluidTank.getFluid().copy();
            wanted.setAmount(request);

            simulated =
                    source.drain(
                            wanted,
                            IFluidHandler.FluidAction.SIMULATE
                    );
        }

        if (simulated.isEmpty()) {
            return 0;
        }

        int accepted =
                fluidTank.fill(
                        simulated,
                        IFluidHandler.FluidAction.SIMULATE
                );

        if (accepted <= 0) {
            return 0;
        }

        FluidStack requestStack =
                simulated.copy();
        requestStack.setAmount(accepted);

        FluidStack drained =
                source.drain(
                        requestStack,
                        IFluidHandler.FluidAction.EXECUTE
                );

        if (drained.isEmpty()) {
            return 0;
        }

        return fluidTank.fill(
                drained,
                IFluidHandler.FluidAction.EXECUTE
        );
    }

    public FluidStack getFluidForRender() {
        return fluidTank.getFluid().copy();
    }

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public int getCapacity() {
        return fluidTank.getCapacity();
    }

    public float getFillRatio() {
        return fluidTank.getCapacity() <= 0
                ? 0.0F
                : Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                fluidTank.getFluidAmount()
                                        / (float) fluidTank.getCapacity()
                        )
                );
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap,
            @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCapability.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fluidCapability =
                LazyOptional.of(() -> fluidTank);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains("Tank")) {
            fluidTank.readFromNBT(
                    tag.getCompound("Tank")
            );
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.put(
                "Tank",
                fluidTank.writeToNBT(
                        new CompoundTag()
                )
        );
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag =
                new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener>
    getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            Connection net,
            ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag =
                packet.getTag();

        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    private void sync() {
        if (level != null) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_CLIENTS
            );
        }
    }
}
