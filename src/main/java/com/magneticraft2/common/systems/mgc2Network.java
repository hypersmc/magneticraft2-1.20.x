package com.magneticraft2.common.systems;

import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.systems.networking.GearSyncPacket;
import com.magneticraft2.common.systems.networking.PollutionPacket;
import com.magneticraft2.common.systems.networking.SaveBlueprintPacket;
import com.magneticraft2.common.systems.networking.SetProjectorBlueprintPacket;
import com.magneticraft2.common.systems.networking.SyncBlueprintsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
* @version 1.0.0
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
                .add(); //This channel is purely for pollution data send to the client's renderer.
        CHANNEL.messageBuilder(GearSyncPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GearSyncPacket::encode)
                .decoder(GearSyncPacket::decode)
                .consumerMainThread(GearSyncPacket::handle)
                .add(); //This is so we can sync the torque and speed of each gear.
        CHANNEL.messageBuilder(SaveBlueprintPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveBlueprintPacket::encode)
                .decoder(SaveBlueprintPacket::decode)
                .consumerMainThread(SaveBlueprintPacket::handle)
                .add(); //Client request to save a blueprint on the server.
        CHANNEL.messageBuilder(SyncBlueprintsPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBlueprintsPacket::encode)
                .decoder(SyncBlueprintsPacket::decode)
                .consumerMainThread(SyncBlueprintsPacket::handle)
                .add(); //Server syncs allowed blueprints to the client's Projector registry.
        CHANNEL.messageBuilder(SetProjectorBlueprintPacket.class, 4, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetProjectorBlueprintPacket::encode)
                .decoder(SetProjectorBlueprintPacket::decode)
                .consumerMainThread(SetProjectorBlueprintPacket::handle)
                .add(); //Client selects the active blueprint on a Projector block entity.
    }
}
