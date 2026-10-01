package com.magneticraft2.common.systems.networking;

import com.magneticraft2.common.blockentity.general.projectortestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server request for selecting a blueprint in the Blueprint Projector.
 */
public class SetProjectorBlueprintPacket {
    private static final int MAX_BLUEPRINT_NAME_LENGTH = 64;

    private final BlockPos blockEntityPos;
    private final String blueprintName;

    public SetProjectorBlueprintPacket(BlockPos blockEntityPos, String blueprintName) {
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

    public static void encode(SetProjectorBlueprintPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockEntityPos);
        buffer.writeUtf(packet.blueprintName, MAX_BLUEPRINT_NAME_LENGTH);
    }

    public static SetProjectorBlueprintPacket decode(FriendlyByteBuf buffer) {
        BlockPos blockEntityPos = buffer.readBlockPos();
        String blueprintName = buffer.readUtf(MAX_BLUEPRINT_NAME_LENGTH);
        return new SetProjectorBlueprintPacket(blockEntityPos, blueprintName);
    }

    public static void handle(SetProjectorBlueprintPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(packet.blockEntityPos);
            if (blockEntity instanceof projectortestBlockEntity projector) {
                projector.setBlueprintFromPlayer(player, packet.blueprintName);
            }
        });

        context.setPacketHandled(true);
    }
}
