package com.bodyhealth.api;

import com.bodyhealth.common.BodyHealthAttachment;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.common.FractureState;
import com.bodyhealth.network.BodyHealthSyncManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.File;
import java.nio.file.Path;
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

    public static FractureState getFracture(Player player, BodyPart part) {
        return getData(player).getFracture(part);
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

    public static void heal(Player player, BodyPart part, float amount) {
        getData(player).heal(part, amount);
        sync(player);
    }

    public static void healAll(Player player, float amount) {
        getData(player).healAll(amount);
        sync(player);
    }

    public static void fullHeal(Player player) {
        BodyHealthData data = getData(player);
        for (BodyPart part : BodyPart.values())
            data.setHealth(part, data.getMaxHealth(part));
        sync(player);
    }

    public static void setFracture(Player player, BodyPart part, FractureState state) {
        getData(player).setFracture(part, state);
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
     * Modifie directement le fichier .dat sur disque.
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
     * Définit la fracture d'une partie pour un joueur déconnecté.
     */
    public static void setFractureOfflinePlayer(UUID uuid, BodyPart part, FractureState state) {
        modifyOfflinePlayer(uuid, data -> data.setFracture(part, state));
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

        // Déconnectés — parcourir tous les fichiers .dat du playerdata
        Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        File dir = playerDataDir.toFile();
        if (!dir.exists()) return;

        // UUIDs des connectés (à ignorer, déjà traités)
        java.util.Set<String> online = new java.util.HashSet<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            online.add(p.getUUID().toString());

        for (File datFile : dir.listFiles((d, n) -> n.endsWith(".dat"))) {
            String uuidStr = datFile.getName().replace(".dat", "");
            if (online.contains(uuidStr)) continue; // déjà traité
            try {
                UUID uuid = UUID.fromString(uuidStr);
                modifyOfflinePlayer(uuid, action);
            } catch (IllegalArgumentException ignored) {}
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

    /**
     * Lit le fichier .dat d'un joueur hors ligne, applique l'action,
     * puis réécrit le fichier sur disque.
     *
     * Les données Body Health sont stockées dans
     * playerdata/<UUID>.dat → ForgeCaps → bodyhealth:body_health
     */
    private static void modifyOfflinePlayer(UUID uuid,
                                             java.util.function.Consumer<BodyHealthData> action) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Si le joueur est connecté, utiliser le chemin normal
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            action.accept(getData(online));
            sync(online);
            return;
        }

        Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        File datFile = playerDataDir.resolve(uuid + ".dat").toFile();
        if (!datFile.exists()) return;

        try {
            // Lire le NBT complet du joueur
            CompoundTag playerNbt = NbtIo.readCompressed(datFile.toPath(),
                    net.minecraft.nbt.NbtAccounter.unlimitedHeap());

            // Lire les données BH depuis le NBT NeoForge Attachments
            // Chemin : playerNbt → "neoforge:attachments" → "bodyhealth:body_health"
            CompoundTag attachments = playerNbt.contains("neoforge:attachments")
                    ? playerNbt.getCompound("neoforge:attachments")
                    : new CompoundTag();

            CompoundTag bhTag = attachments.contains("bodyhealth:body_health")
                    ? attachments.getCompound("bodyhealth:body_health")
                    : new CompoundTag();

            // Désérialiser, appliquer, resérialiser
            BodyHealthData data = new BodyHealthData();
            if (!bhTag.isEmpty()) data.deserializeNBT(bhTag);

            action.accept(data);

            // Réécrire
            attachments.put("bodyhealth:body_health", data.serializeNBT());
            playerNbt.put("neoforge:attachments", attachments);

            NbtIo.writeCompressed(playerNbt, datFile.toPath());

        } catch (Exception e) {
            net.minecraft.server.Bootstrap.STDOUT.println(
                "[BodyHealth] Erreur modification joueur hors ligne " + uuid + ": " + e.getMessage());
        }
    }
}
