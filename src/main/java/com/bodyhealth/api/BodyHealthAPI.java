package com.bodyhealth.api;

import com.bodyhealth.common.BodyHealthAttachment;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.network.BodyHealthSyncManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * API publique Body Health System.
 *
 * Deux modes de ciblage :
 *
 * 1. Joueur connecté (Player / ServerPlayer) — effet immédiat, sync réseau automatique.
 * 2. Joueur déconnecté (UUID) — modifie le fichier NBT sur disque.
 *    Les changements sont appliqués au prochain login du joueur.
 *
 * MCreator : utilise les méthodes avec Player pour les procedures normales.
 * Utilise les méthodes avec UUID pour cibler des joueurs hors ligne.
 */
public final class BodyHealthAPI {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("BodyHealth");

    private BodyHealthAPI() {}

    // ── Lecture (joueur connecté) ─────────────────────────────────────────────

    public static float getHealth(Player player, BodyPart part) {
        return getData(player).getHealth(part);
    }

    public static float getMaxHealth(Player player, BodyPart part) {
        return getData(player).getMaxHealth(part);
    }

    public static boolean isDead(Player player, BodyPart part) {
        return getData(player).isDead(part);
    }

    public static boolean isCritical(Player player, BodyPart part) {
        return getData(player).isCritical(part);
    }

    public static float getHealthPercent(Player player, BodyPart part) {
        return getData(player).getHealthPercent(part);
    }

    public static float getAbsorption(Player player, BodyPart part) {
        return getData(player).getAbsorption(part);
    }

    public static boolean isRegenerating(Player player, BodyPart part) {
        return getData(player).isRegenerating(part);
    }

    // ── Modification (joueur connecté) ────────────────────────────────────────

    public static void setHealth(Player player, BodyPart part, float value) {
        getData(player).setHealth(part, value);
        sync(player);
    }

    public static void setMaxHealth(Player player, BodyPart part, float value) {
        getData(player).setMaxHealth(part, value);
        sync(player);
    }

    public static void addMaxHealth(Player player, BodyPart part, float amount) {
        BodyHealthData data = getData(player);
        data.setMaxHealth(part, data.getMaxHealth(part) + amount);
        sync(player);
    }

    public static void addMaxHealthAll(Player player, float amount) {
        getData(player).addMaxHealthAll(amount);
        sync(player);
    }

    public static void damage(Player player, BodyPart part, float amount) {
        getData(player).damage(part, amount);
        sync(player);
    }

    public static void setAbsorption(Player player, BodyPart part, float amount) {
        getData(player).setAbsorption(part, amount);
        sync(player);
    }

    public static void heal(Player player, BodyPart part, float amount) {
        getData(player).heal(part, amount);
        sync(player);
    }

    public static void healAll(Player player, float amount) {
        getData(player).healAll(amount);
        sync(player);
    }

    public static void fullHeal(Player player) {
        getData(player).healAll(Float.MAX_VALUE / 2f); // clampé au max par heal() en interne
        sync(player);
    }

    // ── Tous les joueurs connectés ────────────────────────────────────────────

    /** Applique un soin sur TOUS les joueurs actuellement connectés. */
    public static void healAllPlayers(float amount) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            healAll(p, amount);
    }

    /** Soin complet sur TOUS les joueurs connectés. */
    public static void fullHealAllPlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            fullHeal(p);
    }

    /** Ajoute des cœurs max à TOUS les joueurs connectés. */
    public static void addMaxHealthAllPlayers(float amount) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            addMaxHealthAll(p, amount);
    }

    // ── Joueur déconnecté (UUID) — effet au prochain login ────────────────────

    /**
     * Soigne toutes les parties d'un joueur déconnecté.
     * Modifie leur fichier JSON ; appliqué à leur prochaine connexion.
     * Effet visible au prochain login du joueur.
     *
     * @param uuid UUID du joueur hors ligne
     * @param amount HP à soigner sur chaque partie
     */
    public static void healOfflinePlayer(UUID uuid, float amount) {
        modifyOfflinePlayer(uuid, data -> data.healAll(amount));
    }

    /**
     * Soin complet d'un joueur déconnecté.
     */
    public static void fullHealOfflinePlayer(UUID uuid) {
        modifyOfflinePlayer(uuid, data -> {
            for (BodyPart p : BodyPart.values())
                data.setHealth(p, data.getMaxHealth(p));
        });
    }

    /**
     * Ajoute des cœurs max à un joueur déconnecté.
     */
    public static void addMaxHealthOfflinePlayer(UUID uuid, float amount) {
        modifyOfflinePlayer(uuid, data -> data.addMaxHealthAll(amount));
    }

    /**
     * Applique une modification à TOUS les joueurs — connectés ET déconnectés.
     * Les connectés reçoivent le changement immédiatement.
     * Les déconnectés le verront au prochain login.
     *
     * Exemple :
     *   BodyHealthAPI.applyToAll(data -> data.addMaxHealthAll(4f));
     */
    public static void applyToAll(java.util.function.Consumer<BodyHealthData> action) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Connectés
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            action.accept(getData(p));
            sync(p);
        }

        // Déconnectés — on modifie leur fichier JSON (appliqué à leur prochaine connexion)
        java.util.Set<UUID> online = new java.util.HashSet<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) online.add(p.getUUID());
        for (UUID uuid : com.bodyhealth.common.BodyHealthDiskManager.knownPlayers()) {
            if (online.contains(uuid)) continue;
            BodyHealthData data = com.bodyhealth.common.BodyHealthDiskManager.loadSnapshot(uuid);
            if (data == null) continue;
            action.accept(data);
            com.bodyhealth.common.BodyHealthDiskManager.saveOfflineSnapshot(uuid, data);
        }
    }

    // ── Interne ───────────────────────────────────────────────────────────────

    public static BodyHealthData getData(Player player) {
        return player.getData(BodyHealthAttachment.BODY_HEALTH.get());
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer sp)
            BodyHealthSyncManager.markDirtyAndFlush(sp, getData(sp));
    }
}
