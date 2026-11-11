package com.bodyhealth.network;

import com.bodyhealth.BodyHealthMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel.VersionTest;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** Canal réseau Forge (serveur -> client). */
public class NetworkRegistry {

    private static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(BodyHealthMod.MOD_ID, "main"))
            .clientAcceptedVersions(VersionTest.exact(PROTOCOL_VERSION))
            .serverAcceptedVersions(VersionTest.exact(PROTOCOL_VERSION))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel()
                .play()
                    .clientbound()
                        .addMain(SyncBodyHealthPacket.class, SyncBodyHealthPacket.STREAM_CODEC, SyncBodyHealthPacket::handle)
                        .addMain(OpenGuidePacket.class, OpenGuidePacket.STREAM_CODEC, OpenGuidePacket::handle)
            .build();

    /** Force le chargement de la classe (donc l'enregistrement du canal) au démarrage. */
    public static void init() {}

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }
}
