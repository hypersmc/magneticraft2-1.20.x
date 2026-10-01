package com.magneticraft2.common.systems.networking;

import com.magneticraft2.common.blockentity.general.BlueprintMultiblockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server request for saving a blueprint from a Blueprint Maker block entity.
 *
 * The client only sends the requested name and the block entity position.
 * The server decides the owner, reads the marker positions from the server-side block entity,
 * builds the blueprint from the server world, and saves it in the server world folder.
 */
public class SaveBlueprintPacket {
    private static final int MAX_BLUEPRINT_NAME_LENGTH = 64;

    private final BlockPos blockEntityPos;
    private final String blueprintName;

    public SaveBlueprintPacket(BlockPos blockEntityPos, String blueprintName) {
        this.blockEntityPos = blockEntityPos;
        this.blueprintName = cleanNameForPacket(blueprintName);
    }

    private static String cleanNameForPacket(String blueprintName) {
        if (blueprintName == null) {
            return "";
        }

        String cleaned = blueprintName.trim();
        if (cleaned.length() > MAX_BLUEPRINT_NAME_LENGTH) {
            cleaned = cleaned.substring(0, MAX_BLUEPRINT_NAME_LENGTH);
        }
        return cleaned;
    }

    public static void encode(SaveBlueprintPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockEntityPos);
        buffer.writeUtf(packet.blueprintName, MAX_BLUEPRINT_NAME_LENGTH);
    }

    public static SaveBlueprintPacket decode(FriendlyByteBuf buffer) {
        BlockPos blockEntityPos = buffer.readBlockPos();
        String blueprintName = buffer.readUtf(MAX_BLUEPRINT_NAME_LENGTH);
        return new SaveBlueprintPacket(blockEntityPos, blueprintName);
    }

    public static void handle(SaveBlueprintPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(packet.blockEntityPos);
            if (blockEntity instanceof BlueprintMultiblockEntity blueprintMultiblockEntity) {
                blueprintMultiblockEntity.saveBlueprintForPlayer(player, packet.blueprintName);
            }
        });

        context.setPacketHandled(true);
    }
}
