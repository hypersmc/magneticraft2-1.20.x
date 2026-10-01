package com.magneticraft2.common.systems.networking;

import com.magneticraft2.common.systems.Blueprint.core.BlueprintManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server -> client blueprint sync for the Blueprint Projector.
 *
 * The server sends the full JSON for blueprints the player is allowed to see.
 * The client registers/replaces those blueprints in its local BlueprintRegistry so
 * the Projector GUI and renderer can use them on dedicated servers.
 */
public class SyncBlueprintsPacket {
    private static final int MAX_BLUEPRINTS_PER_PACKET = 256;
    private static final int MAX_BLUEPRINT_JSON_BYTES = 1024 * 1024;

    private final List<String> blueprintJsons;

    public SyncBlueprintsPacket(List<String> blueprintJsons) {
        this.blueprintJsons = blueprintJsons == null ? List.of() : List.copyOf(blueprintJsons);
    }

    public static void encode(SyncBlueprintsPacket packet, FriendlyByteBuf buffer) {
        int count = Math.min(packet.blueprintJsons.size(), MAX_BLUEPRINTS_PER_PACKET);
        buffer.writeVarInt(count);

        for (int i = 0; i < count; i++) {
            byte[] bytes = packet.blueprintJsons.get(i).getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_BLUEPRINT_JSON_BYTES) {
                buffer.writeVarInt(0);
                continue;
            }

            buffer.writeVarInt(bytes.length);
            buffer.writeBytes(bytes);
        }
    }

    public static SyncBlueprintsPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_BLUEPRINTS_PER_PACKET) {
            throw new IllegalArgumentException("Invalid blueprint sync count: " + count);
        }

        List<String> blueprintJsons = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int length = buffer.readVarInt();
            if (length < 0 || length > MAX_BLUEPRINT_JSON_BYTES) {
                throw new IllegalArgumentException("Invalid blueprint sync json length: " + length);
            }
            if (length == 0) {
                continue;
            }

            byte[] bytes = new byte[length];
            buffer.readBytes(bytes);
            blueprintJsons.add(new String(bytes, StandardCharsets.UTF_8));
        }

        return new SyncBlueprintsPacket(blueprintJsons);
    }

    public static void handle(SyncBlueprintsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> BlueprintManager.receiveSyncedBlueprints(packet.blueprintJsons));
        context.setPacketHandled(true);
    }
}
