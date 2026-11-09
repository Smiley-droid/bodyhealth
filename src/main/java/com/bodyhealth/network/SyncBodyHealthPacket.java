package com.bodyhealth.network;

import com.bodyhealth.common.BodyHealthData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncBodyHealthPacket(CompoundTag data) implements CustomPacketPayload {

    public static final Type<SyncBodyHealthPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bodyhealth", "sync_health"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, SyncBodyHealthPacket> CODEC =
            ByteBufCodecs.fromCodec(CompoundTag.CODEC).map(SyncBodyHealthPacket::new, SyncBodyHealthPacket::data);

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
                !packet.data().getBoolean("disabled").orElse(false));
            com.bodyhealth.client.ClientBodyHealthData.update(packet.data());
        });
    }
}
