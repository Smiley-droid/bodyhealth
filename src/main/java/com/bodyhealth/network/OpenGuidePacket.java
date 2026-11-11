package com.bodyhealth.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Serveur -> client : ouvre le guide de survie.
 * Aucune classe client n'est référencée ici (le code client est dans ClientGuideHandler).
 */
public record OpenGuidePacket(CompoundTag healthData) {

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenGuidePacket> STREAM_CODEC =
            StreamCodec.ofMember(OpenGuidePacket::encode, OpenGuidePacket::decode);

    public static void encode(OpenGuidePacket packet, RegistryFriendlyByteBuf buf) {
        buf.writeNbt(packet.healthData());
    }

    public static OpenGuidePacket decode(RegistryFriendlyByteBuf buf) {
        return new OpenGuidePacket(buf.readNbt());
    }

    public static void handle(OpenGuidePacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() ->
            com.bodyhealth.client.ClientGuideHandler.handle(packet.healthData())
        );
    }
}
