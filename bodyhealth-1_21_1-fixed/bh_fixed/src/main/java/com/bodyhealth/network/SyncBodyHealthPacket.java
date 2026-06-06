package com.bodyhealth.network;

import com.bodyhealth.BodyHealthMod;
import com.bodyhealth.client.ClientBodyHealthData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Paquet serveur → client.
 * Transmet les HP de chaque partie + le type de dégât reçu
 * pour que le client puisse afficher la bonne couleur de flash.
 */
public record SyncBodyHealthPacket(CompoundTag data) implements CustomPacketPayload {

    public static final Type<SyncBodyHealthPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BodyHealthMod.MOD_ID, "sync_body_health"));

    public static final StreamCodec<FriendlyByteBuf, SyncBodyHealthPacket> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeNbt(pkt.data()),
                    buf -> new SyncBodyHealthPacket(buf.readNbt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncBodyHealthPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientBodyHealthData.update(packet.data()));
    }
}
