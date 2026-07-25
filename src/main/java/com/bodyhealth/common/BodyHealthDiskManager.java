package com.bodyhealth.common;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

/**
 * Sauvegarde double des données Body Health :
 *
 * 1. NBT via Attachment NeoForge (dans playerdata/<UUID>.dat) — c'est le système
 *    principal, géré automatiquement par NeoForge à chaque save du joueur.
 *    C'est déjà de la sauvegarde disque, mais elle n'arrive que lors des saves
 *    périodiques du serveur (toutes les 6000 ticks = 5 minutes par défaut).
 *
 * 2. JSON dans world/bodyhealth/<UUID>.json — fichier lisible, modifiable
 *    manuellement, sauvegardé IMMÉDIATEMENT à chaque modification importante
 *    (dégât, soin, commande admin). Sert aussi de backup en cas de corruption
 *    du playerdata.
 *
 * Flux de sauvegarde :
 *   Modification → markDirtyAndFlush() → réseau client + saveJson() immédiat
 *
 * Flux de chargement (login) :
 *   - NeoForge charge le NBT depuis playerdata automatiquement (priorité 1)
 *   - Si le NBT est absent/vide, on tente de charger depuis le JSON (fallback)
 */
public class BodyHealthDiskManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("BodyHealth");
    private static final Gson   GSON   = new GsonBuilder().setPrettyPrinting().create();

    // ── Sauvegarde JSON ───────────────────────────────────────────────────────

    /**
     * Sauvegarde immédiate des données du joueur dans world/bodyhealth/<UUID>.json.
     * Appelé à chaque markDirtyAndFlush() — garantit qu'on ne perd rien si le
     * serveur crash entre deux saves périodiques.
     */
    public static void saveJson(ServerPlayer player, BodyHealthData data) {
        try {
            Path dir  = getBodyHealthDir();
            Path file = dir.resolve(player.getUUID() + ".json");
            Files.createDirectories(dir);

            JsonObject root = dataToJson(player, data);

            try (Writer w = new OutputStreamWriter(
                    Files.newOutputStream(file, StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING),
                    StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            LOGGER.error("[BodyHealth] Erreur sauvegarde JSON pour {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Tente de charger les données depuis le JSON si le NBT attachment est vide
     * (nouveau joueur après migration, corruption, etc.).
     *
     * @return true si des données ont été chargées depuis le JSON
     */
    public static boolean loadJsonFallback(ServerPlayer player, BodyHealthData data) {
        try {
            Path file = getBodyHealthDir().resolve(player.getUUID() + ".json");
            if (!Files.exists(file)) return false;

            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            // Vérifier que le fichier correspond bien à ce joueur
            if (root.has("uuid") &&
                !root.get("uuid").getAsString().equals(player.getUUID().toString()))
                return false;

            jsonToData(root, data);
            LOGGER.info("[BodyHealth] Données chargées depuis JSON pour {}",
                    player.getName().getString());
            return true;

        } catch (Exception e) {
            LOGGER.warn("[BodyHealth] Impossible de lire le JSON pour {}: {}",
                    player.getName().getString(), e.getMessage());
            return false;
        }
    }

    /**
     * Supprime le fichier JSON d'un joueur (utilisé par /bodyhealth reset).
     */
    public static void deleteJson(UUID uuid) {
        try {
            Path file = getBodyHealthDir().resolve(uuid + ".json");
            Files.deleteIfExists(file);
        } catch (Exception e) {
            LOGGER.warn("[BodyHealth] Impossible de supprimer le JSON pour {}: {}", uuid, e.getMessage());
        }
    }

    // ── Sérialisation JSON ────────────────────────────────────────────────────

    private static JsonObject dataToJson(ServerPlayer player, BodyHealthData data) {
        JsonObject root = new JsonObject();
        root.addProperty("uuid",     player.getUUID().toString());
        root.addProperty("name",     player.getName().getString());
        root.addProperty("savedAt",  System.currentTimeMillis());

        JsonObject health    = new JsonObject();
        JsonObject maxHealth = new JsonObject();
        JsonObject absorb    = new JsonObject();

        for (BodyPart part : BodyPart.values()) {
            String id = part.getId();
            health   .addProperty(id, data.getHealth(part));
            maxHealth.addProperty(id, data.getMaxHealth(part));
            absorb   .addProperty(id, data.getAbsorption(part));
        }

        root.add("health",     health);
        root.add("maxHealth",  maxHealth);
        root.add("absorption", absorb);
        return root;
    }

    private static void jsonToData(JsonObject root, BodyHealthData data) {
        if (root.has("health")) {
            JsonObject h = root.getAsJsonObject("health");
            for (BodyPart p : BodyPart.values())
                if (h.has(p.getId()))
                    data.setHealth(p, h.get(p.getId()).getAsFloat());
        }
        if (root.has("maxHealth")) {
            JsonObject m = root.getAsJsonObject("maxHealth");
            for (BodyPart p : BodyPart.values())
                if (m.has(p.getId()))
                    data.setMaxHealth(p, m.get(p.getId()).getAsFloat());
        }
        if (root.has("absorption")) {
            JsonObject a = root.getAsJsonObject("absorption");
            for (BodyPart p : BodyPart.values())
                if (a.has(p.getId()))
                    data.setAbsorption(p, a.get(p.getId()).getAsFloat());
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private static Path getBodyHealthDir() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            throw new IllegalStateException("Serveur non disponible");
        return server.getWorldPath(LevelResource.ROOT).resolve("bodyhealth");
    }
}
