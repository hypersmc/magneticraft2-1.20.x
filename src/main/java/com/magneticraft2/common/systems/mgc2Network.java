package com.magneticraft2.common.systems;

import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.systems.networking.PollutionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class mgc2Network {
    public static final String NETWORK_VERSION = "1.0.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(magneticraft2.MOD_ID, "network"), () -> NETWORK_VERSION,
            version -> version.equals(NETWORK_VERSION), version -> version.equals(NETWORK_VERSION)
    );
    public static void init(){
        CHANNEL.messageBuilder(PollutionPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PollutionPacket::encode)
                .decoder(PollutionPacket::decode)
                .consumerMainThread(PollutionPacket::handle)
                .add();

    }
}
