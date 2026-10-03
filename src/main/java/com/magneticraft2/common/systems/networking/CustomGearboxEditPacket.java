package com.magneticraft2.common.systems.networking;

import com.magneticraft2.client.gui.container.gearbox.CustomGearboxMenu;
import com.magneticraft2.common.blockentity.stage.copper.CustomGearboxBlockEntity_wood;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server request to place one explicit component in a custom-gearbox cell.
 */
public class CustomGearboxEditPacket {
    private final BlockPos blockEntityPos;
    private final int cellIndex;
    private final int componentOrdinal;

    public CustomGearboxEditPacket(BlockPos blockEntityPos,
                                   int cellIndex,
                                   int componentOrdinal) {
        this.blockEntityPos = blockEntityPos;
        this.cellIndex = cellIndex;
        this.componentOrdinal = componentOrdinal;
    }

    public static void encode(CustomGearboxEditPacket packet,
                              FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockEntityPos);
        buffer.writeVarInt(packet.cellIndex);
        buffer.writeVarInt(packet.componentOrdinal);
    }

    public static CustomGearboxEditPacket decode(FriendlyByteBuf buffer) {
        return new CustomGearboxEditPacket(
                buffer.readBlockPos(),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }

    public static void handle(CustomGearboxEditPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            CustomGearboxBlockEntity_wood.InternalComponent[] values =
                    CustomGearboxBlockEntity_wood.InternalComponent.values();

            if (player == null
                    || packet.cellIndex < 0
                    || packet.cellIndex >= CustomGearboxBlockEntity_wood.CELL_COUNT
                    || packet.componentOrdinal < 0
                    || packet.componentOrdinal >= values.length) {
                return;
            }

            if (!(player.containerMenu instanceof CustomGearboxMenu menu)
                    || !menu.getBlockEntityPos().equals(packet.blockEntityPos)) {
                return;
            }

            if (player.distanceToSqr(
                    packet.blockEntityPos.getX() + 0.5D,
                    packet.blockEntityPos.getY() + 0.5D,
                    packet.blockEntityPos.getZ() + 0.5D
            ) > 64.0D) {
                return;
            }

            BlockEntity blockEntity = player.level()
                    .getBlockEntity(packet.blockEntityPos);

            if (blockEntity instanceof CustomGearboxBlockEntity_wood gearbox) {
                gearbox.setComponentFromPlayer(
                        player,
                        packet.cellIndex,
                        values[packet.componentOrdinal]
                );
            }
        });

        context.setPacketHandled(true);
    }
}
