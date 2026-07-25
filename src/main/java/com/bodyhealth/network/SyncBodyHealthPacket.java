package com.bodyhealth.network;

import com.bodyhealth.common.BodyHealthData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncBodyHealthPacket(CompoundTag data) implements CustomPacketPayload {

    public static final Type<SyncBodyHealthPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("bodyhealth", "sync_health"));

    public static final StreamCodec<FriendlyByteBuf, SyncBodyHealthPacket> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeNbt(pkt.data()),
                    buf -> new SyncBodyHealthPacket(buf.readNbt())
            );

    public static SyncBodyHealthPacket from(BodyHealthData data) {
        return new SyncBodyHealthPacket(data.serializeNBT());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /**
     * Handler côté CLIENT uniquement.
     * On évite tout import de classe client dans cette classe car elle est
     * chargée par NetworkRegistry sur les deux distributions.
     * La logique client est déléguée via reflection-safe FQCN dans enqueueWork.
     */
    public static void handle(SyncBodyHealthPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Mettre à jour l'état "enabled" transmis dans le NBT
            com.bodyhealth.client.ClientBodyHealthData.setModEnabled(
                !packet.data().contains("disabled") || !packet.data().getBoolean("disabled"));
            com.bodyhealth.client.ClientBodyHealthData.update(packet.data());
        });
    }
}
