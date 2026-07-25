package com.bodyhealth.network;

import com.bodyhealth.BodyHealthMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Paquet serveur → client pour le Guide de Survie.
 *
 * CRITIQUE — Cette classe ne doit JAMAIS contenir de référence directe à
 * Minecraft, LocalPlayer, ou toute autre classe client, MÊME À L'INTÉRIEUR
 * D'UN LAMBDA. Les lambdas sont compilées comme méthodes synthétiques DANS
 * la même classe .class — donc un lambda référençant LocalPlayer met cette
 * référence dans le bytecode de CETTE classe, et FML la scanne au chargement
 * peu importe si le code s'exécute. Voir ClientGuideHandler.java pour le
 * code client réel, isolé dans son propre fichier .class.
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
        // Aucune référence client ici — uniquement un appel vers une classe
        // séparée. ClientGuideHandler.class n'est chargé que si cette ligne
        // s'exécute réellement, ce qui n'arrive jamais côté serveur.
        context.enqueueWork(() ->
            com.bodyhealth.client.ClientGuideHandler.handle(packet.healthData())
        );
    }
}
