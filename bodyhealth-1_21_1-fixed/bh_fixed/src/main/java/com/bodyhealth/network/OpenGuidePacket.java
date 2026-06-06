package com.bodyhealth.network;

import com.bodyhealth.BodyHealthMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Paquet serveur → client pour déclencher l'affichage du Guide de Survie.
 *
 * FIX — SurvivalGuideItem appelait ClientBodyHealthData sur le serveur dédié
 * car level.isClientSide = false côté serveur → le guide était silencieux.
 *
 * Solution : le serveur envoie ce paquet avec les données de santé actuelles
 * (NBT), le client l'affiche dans le chat.
 */
public record OpenGuidePacket(CompoundTag healthData) implements CustomPacketPayload {

    public static final Type<OpenGuidePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BodyHealthMod.MOD_ID, "open_guide"));

    public static final StreamCodec<FriendlyByteBuf, OpenGuidePacket> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeNbt(pkt.healthData()),
                    buf -> new OpenGuidePacket(buf.readNbt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(OpenGuidePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;

            // Mettre à jour le snapshot client avec les données fraîches du serveur
            com.bodyhealth.client.ClientBodyHealthData.update(packet.healthData());

            // Afficher le guide en chat
            com.bodyhealth.item.SurvivalGuideItem.displayGuideClient(mc.player);
        });
    }
}
