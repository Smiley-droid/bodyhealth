package com.bodyhealth.network;

import com.bodyhealth.common.BodyHealthData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Serveur -> client : état de santé complet du joueur. */
public record SyncBodyHealthPacket(CompoundTag data) {

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBodyHealthPacket> STREAM_CODEC =
            StreamCodec.ofMember(SyncBodyHealthPacket::encode, SyncBodyHealthPacket::decode);

    public static SyncBodyHealthPacket from(BodyHealthData data) {
        return new SyncBodyHealthPacket(data.serializeNBT());
    }

    public static void encode(SyncBodyHealthPacket packet, RegistryFriendlyByteBuf buf) {
        buf.writeNbt(packet.data());
    }

    public static SyncBodyHealthPacket decode(RegistryFriendlyByteBuf buf) {
        return new SyncBodyHealthPacket(buf.readNbt());
    }

    public static void handle(SyncBodyHealthPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            com.bodyhealth.client.ClientBodyHealthData.setModEnabled(
                !packet.data().contains("disabled") || !packet.data().getBoolean("disabled"));
            com.bodyhealth.client.ClientBodyHealthData.update(packet.data());
        });
    }
}
