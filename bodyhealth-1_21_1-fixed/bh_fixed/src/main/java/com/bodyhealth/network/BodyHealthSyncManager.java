package com.bodyhealth.network;

import com.bodyhealth.common.BodyHealthData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BodyHealthSyncManager {

    // Dernier snapshot envoyé — compare HP + fractures + absorption
    private static final Map<UUID, CompoundTag> lastSent = new HashMap<>();

    /** Envoi immédiat — dégâts, mort, soin, commande */
    public static void markDirtyAndFlush(ServerPlayer player, BodyHealthData data) {
        CompoundTag nbt = data.serializeNBT();
        lastSent.put(player.getUUID(), nbt.copy());
        PacketDistributor.sendToPlayer(player, new SyncBodyHealthPacket(nbt));
    }

    /**
     * Envoi lazy — regen passive, tick handlers.
     *
     * BUG 4 FIX — L'ancienne version comparait uniquement le bloc "health" du NBT.
     * Si seule la fracture ou l'absorption changeait (ex: guérison via kit médical,
     * absorption Totem), le client n'était jamais mis à jour.
     *
     * Correction : on compare maintenant le NBT complet (health + maxHealth +
     * fractures + absorption). CompoundTag.equals() fait une comparaison profonde.
     */
    public static void markDirtyLazy(ServerPlayer player, BodyHealthData data) {
        UUID        uuid   = player.getUUID();
        CompoundTag newNbt = data.serializeNBT();
        CompoundTag prev   = lastSent.get(uuid);

        // BUG 4 FIX — equals() sur le tag complet, pas uniquement sur "health"
        if (prev != null && prev.equals(newNbt)) return;

        lastSent.put(uuid, newNbt.copy());
        PacketDistributor.sendToPlayer(player, new SyncBodyHealthPacket(newNbt));
    }

    public static void cleanup(UUID uuid) {
        lastSent.remove(uuid);
    }
}
