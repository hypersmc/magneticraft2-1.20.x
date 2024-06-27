package com.magneticraft2.common.systems.networking;

import com.magneticraft2.client.world.ClientPollutionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author JumpWatch on 24-09-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public record PollutionPacket(ChunkPos pos, int pollutionLevel) {
    public PollutionPacket(FriendlyByteBuf buffer){
        this(buffer.readChunkPos(), buffer.readInt());
    }
    public static final Logger LOGGER = LogManager.getLogger("MGC2PollutionPacket");
    public static PollutionPacket decode(FriendlyByteBuf buffer) {
        ChunkPos pos = buffer.readChunkPos();
        int pollutionLevel = buffer.readInt();

        return new PollutionPacket(pos, pollutionLevel);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeChunkPos(pos);
        buffer.writeInt(pollutionLevel);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPollutionData.handlePollutionPacket(this));
        });
        ctx.get().setPacketHandled(true);
    }
}
