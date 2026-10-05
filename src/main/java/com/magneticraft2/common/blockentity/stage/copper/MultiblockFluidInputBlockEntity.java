package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.MultiblockFluidInputBlock;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MultiblockFluidInputBlockEntity
        extends BlockEntity
        implements IMultiblockModule {

    private int controllerX;
    private int controllerY;
    private int controllerZ;
    private boolean formedModule;

    private LazyOptional<IFluidHandler> fluidCapability =
            LazyOptional.of(() -> new PortFluidHandler());

    public MultiblockFluidInputBlockEntity(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry
                        .MULTIBLOCK_FLUID_INPUT_BE
                        .get(),
                pos,
                state
        );
    }

    public static final int TRANSFER_INTERVAL_TICKS = 5;
    public static final int MAX_TRANSFER_PER_CYCLE = 250;

    public static <E extends BlockEntity> void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            E blockEntity) {
        if (level.isClientSide
                || !(blockEntity
                instanceof MultiblockFluidInputBlockEntity port)
                || !port.formedModule) {
            return;
        }

        if ((level.getGameTime()
                + pos.asLong())
                % TRANSFER_INTERVAL_TICKS != 0L) {
            return;
        }

        port.pullFromExternal();
    }

    private void pullFromExternal() {
        if (level == null
                || !formedModule) {
            return;
        }

        IFluidHandler target =
                getControllerHandler();

        if (target == null) {
            return;
        }

        BlockState state =
                getBlockState();

        if (!state.hasProperty(
                MultiblockFluidInputBlock.FACING
        )) {
            return;
        }

        Direction outward =
                state.getValue(
                        MultiblockFluidInputBlock.FACING
                );
        BlockPos neighbour =
                worldPosition.relative(outward);

        if (level.getBlockState(neighbour).is(
                BlockRegistry.WATER_PIPE.get()
        )) {
            for (FluidPipeNetwork.Endpoint endpoint :
                    FluidPipeNetwork.findEndpoints(
                            level,
                            neighbour,
                            worldPosition
                    )) {
                if (pullFrom(
                        endpoint.handler(),
                        target
                ) > 0) {
                    return;
                }
            }

            return;
        }

        IFluidHandler source =
                getExternalHandler(
                        neighbour,
                        outward.getOpposite()
                );

        if (source != null) {
            pullFrom(
                    source,
                    target
            );
        }
    }

    private int pullFrom(
            IFluidHandler source,
            IFluidHandler target) {
        FluidStack simulated =
                source.drain(
                        MAX_TRANSFER_PER_CYCLE,
                        IFluidHandler.FluidAction.SIMULATE
                );

        if (simulated.isEmpty()) {
            return 0;
        }

        int accepted =
                target.fill(
                        simulated,
                        IFluidHandler.FluidAction.SIMULATE
                );

        if (accepted <= 0) {
            return 0;
        }

        FluidStack request =
                simulated.copy();
        request.setAmount(
                Math.min(
                        accepted,
                        simulated.getAmount()
                )
        );

        FluidStack drained =
                source.drain(
                        request,
                        IFluidHandler.FluidAction.EXECUTE
                );

        if (drained.isEmpty()) {
            return 0;
        }

        return target.fill(
                drained,
                IFluidHandler.FluidAction.EXECUTE
        );
    }

    @Nullable
    private IFluidHandler getExternalHandler(
            BlockPos pos,
            Direction side) {
        if (level == null) {
            return null;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (blockEntity == null) {
            return null;
        }

        IFluidHandler sided =
                blockEntity
                        .getCapability(
                                ForgeCapabilities.FLUID_HANDLER,
                                side
                        )
                        .orElse(null);

        if (sided != null) {
            return sided;
        }

        return blockEntity
                .getCapability(
                        ForgeCapabilities.FLUID_HANDLER,
                        null
                )
                .orElse(null);
    }

    @Nullable
    public BlockPos getControllerPos() {
        if (!formedModule) {
            return null;
        }

        return new BlockPos(
                controllerX,
                controllerY,
                controllerZ
        );
    }

    @Nullable
    private IFluidHandler getControllerHandler() {
        if (level == null || !formedModule) {
            return null;
        }

        BlockEntity controller =
                level.getBlockEntity(
                        new BlockPos(
                                controllerX,
                                controllerY,
                                controllerZ
                        )
                );

        if (controller == null) {
            return null;
        }

        return controller
                .getCapability(
                        ForgeCapabilities.FLUID_HANDLER
                )
                .resolve()
                .orElse(null);
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
        updateFacingFromController();
        setChanged();
        sync();
    }

    @Override
    public void onDeactivate(Level world, BlockPos pos) {
        if (world.isClientSide) {
            return;
        }

        formedModule = false;
        setChanged();
        sync();
    }

    private void updateFacingFromController() {
        if (level == null) {
            return;
        }

        int dx = worldPosition.getX() - controllerX;
        int dy = worldPosition.getY() - controllerY;
        int dz = worldPosition.getZ() - controllerZ;

        Direction outward;
        if (Math.abs(dx) >= Math.abs(dz)
                && Math.abs(dx) >= Math.abs(dy)
                && dx != 0) {
            outward = dx > 0
                    ? Direction.EAST
                    : Direction.WEST;
        } else if (Math.abs(dz) >= Math.abs(dy)
                && dz != 0) {
            outward = dz > 0
                    ? Direction.SOUTH
                    : Direction.NORTH;
        } else if (dy != 0) {
            outward = dy > 0
                    ? Direction.UP
                    : Direction.DOWN;
        } else {
            return;
        }

        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(MultiblockFluidInputBlock.FACING)
                && state.getValue(MultiblockFluidInputBlock.FACING)
                != outward) {
            level.setBlock(
                    worldPosition,
                    state.setValue(
                            MultiblockFluidInputBlock.FACING,
                            outward
                    ),
                    Block.UPDATE_ALL
            );
        }
    }

    @Override
    public String getModuleKey() {
        return "fluid_input";
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
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap,
            @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            BlockState state = getBlockState();

            if (side == null
                    || (state.hasProperty(
                            MultiblockFluidInputBlock.FACING
                    )
                    && side == state.getValue(
                            MultiblockFluidInputBlock.FACING
                    ))) {
                return fluidCapability.cast();
            }

            return LazyOptional.empty();
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
                LazyOptional.of(() -> new PortFluidHandler());
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
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
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
        CompoundTag tag = packet.getTag();

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
                    Block.UPDATE_ALL
            );
        }
    }

    private final class PortFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            IFluidHandler handler = getControllerHandler();
            return handler == null ? 0 : handler.getTanks();
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            IFluidHandler handler = getControllerHandler();
            return handler == null
                    ? FluidStack.EMPTY
                    : handler.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            IFluidHandler handler = getControllerHandler();
            return handler == null
                    ? 0
                    : handler.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(
                int tank,
                @NotNull FluidStack stack) {
            IFluidHandler handler = getControllerHandler();
            return handler != null
                    && handler.isFluidValid(tank, stack);
        }

        @Override
        public int fill(
                FluidStack resource,
                FluidAction action) {
            IFluidHandler handler = getControllerHandler();
            return handler == null
                    ? 0
                    : handler.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(
                FluidStack resource,
                FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(
                int maxDrain,
                FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
