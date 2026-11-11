package com.bodyhealth.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Sauvegarde des données Body Health dans world/data/bodyhealth/<UUID>.json,
 * en plus de l'attachement NeoForge (NBT du joueur).
 *
 * Le JSON sert à trois choses :
 *  1. sauvegarde immédiate à chaque modification (pas seulement aux saves périodiques) ;
 *  2. repli au login si l'attachement semble vierge (corruption, migration) ;
 *  3. modification des joueurs HORS LIGNE : BodyHealthAPI.applyToAll() modifie leur JSON
 *     et le marque "pendingApply" ; il est appliqué à leur prochaine connexion.
 *     Cette méthode ne dépend pas du format des fichiers playerdata de Minecraft,
 *     qui change d'une version à l'autre.
 */
public class BodyHealthDiskManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("BodyHealth");
    private static final Gson   GSON   = new GsonBuilder().setPrettyPrinting().create();

    // ── Joueur en ligne ───────────────────────────────────────────────────────

    /** Sauvegarde immédiate (appelé à chaque markDirtyAndFlush et à la déconnexion). */
    public static void saveJson(ServerPlayer player, BodyHealthData data) {
        writeJson(player.getUUID(), player.getName().getString(), data, false);
    }

    /** Repli : charge le JSON si l'attachement est vierge. @return true si chargé. */
    public static boolean loadJsonFallback(ServerPlayer player, BodyHealthData data) {
        JsonObject root = readRoot(player.getUUID());
        if (root == null) return false;
        jsonToData(root, data);
        LOGGER.info("[BodyHealth] Données chargées depuis JSON pour {}", player.getName().getString());
        return true;
    }

    /**
     * Applique au login une modification faite pendant que le joueur était hors ligne.
     * @return true si une modification en attente a été appliquée.
     */
    public static boolean applyPending(ServerPlayer player, BodyHealthData data) {
        JsonObject root = readRoot(player.getUUID());
        if (root == null) return false;
        if (!root.has("pendingApply") || !root.get("pendingApply").getAsBoolean()) return false;
        jsonToData(root, data);
        LOGGER.info("[BodyHealth] Modification hors ligne appliquée pour {}", player.getName().getString());
        return true; // le prochain markDirtyAndFlush réécrit le JSON avec pendingApply=false
    }

    // ── Joueurs hors ligne ────────────────────────────────────────────────────

    /** UUID de tous les joueurs ayant un fichier JSON. */
    public static List<UUID> knownPlayers() {
        List<UUID> result = new ArrayList<>();
        try {
            Path dir = getBodyHealthDir();
            if (!Files.isDirectory(dir)) return result;
            try (Stream<Path> files = Files.list(dir)) {
                files.forEach(f -> {
                    String n = f.getFileName().toString();
                    if (!n.endsWith(".json")) return;
                    try { result.add(UUID.fromString(n.substring(0, n.length() - 5))); }
                    catch (IllegalArgumentException ignored) {}
                });
            }
        } catch (Exception e) {
            LOGGER.warn("[BodyHealth] Impossible de lister les joueurs: {}", e.getMessage());
        }
        return result;
    }

    /** Charge le dernier état connu d'un joueur (null si inconnu). */
    public static BodyHealthData loadSnapshot(UUID uuid) {
        JsonObject root = readRoot(uuid);
        if (root == null) return null;
        BodyHealthData data = new BodyHealthData();
        jsonToData(root, data);
        return data;
    }

    /** Enregistre un état modifié hors ligne ; sera appliqué à la prochaine connexion. */
    public static void saveOfflineSnapshot(UUID uuid, BodyHealthData data) {
        JsonObject root = readRoot(uuid);
        String name = (root != null && root.has("name")) ? root.get("name").getAsString() : uuid.toString();
        writeJson(uuid, name, data, true);
    }

    public static void deleteJson(UUID uuid) {
        try {
            Files.deleteIfExists(getBodyHealthDir().resolve(uuid + ".json"));
        } catch (Exception e) {
            LOGGER.warn("[BodyHealth] Impossible de supprimer le JSON pour {}: {}", uuid, e.getMessage());
        }
    }

    // ── Interne ───────────────────────────────────────────────────────────────

    private static void writeJson(UUID uuid, String name, BodyHealthData data, boolean pending) {
        try {
            Path dir = getBodyHealthDir();
            Files.createDirectories(dir);

            JsonObject root = new JsonObject();
            root.addProperty("uuid", uuid.toString());
            root.addProperty("name", name);
            root.addProperty("savedAt", System.currentTimeMillis());
            root.addProperty("pendingApply", pending);

            JsonObject health = new JsonObject();
            JsonObject maxHealth = new JsonObject();
            JsonObject absorb = new JsonObject();
            for (BodyPart part : BodyPart.values()) {
                health.addProperty(part.getId(), data.getHealth(part));
                maxHealth.addProperty(part.getId(), data.getMaxHealth(part));
                absorb.addProperty(part.getId(), data.getAbsorption(part));
            }
            root.add("health", health);
            root.add("maxHealth", maxHealth);
            root.add("absorption", absorb);

            try (Writer w = new OutputStreamWriter(
                    Files.newOutputStream(dir.resolve(uuid + ".json"),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
                    StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            LOGGER.error("[BodyHealth] Erreur sauvegarde JSON pour {}: {}", name, e.getMessage());
        }
    }

    private static JsonObject readRoot(UUID uuid) {
        try {
            Path file = getBodyHealthDir().resolve(uuid + ".json");
            if (!Files.exists(file)) return null;
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("uuid") && !root.get("uuid").getAsString().equals(uuid.toString())) return null;
            return root;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("[BodyHealth] Impossible de lire le JSON pour {}: {}", uuid, e.getMessage());
            return null;
        }
    }

    private static void jsonToData(JsonObject root, BodyHealthData data) {
        if (root.has("health")) {
            JsonObject h = root.getAsJsonObject("health");
            for (BodyPart p : BodyPart.values())
                if (h.has(p.getId())) data.setHealth(p, h.get(p.getId()).getAsFloat());
        }
        if (root.has("maxHealth")) {
            JsonObject m = root.getAsJsonObject("maxHealth");
            for (BodyPart p : BodyPart.values())
                if (m.has(p.getId())) data.setMaxHealth(p, m.get(p.getId()).getAsFloat());
        }
        if (root.has("absorption")) {
            JsonObject a = root.getAsJsonObject("absorption");
            for (BodyPart p : BodyPart.values())
                if (a.has(p.getId())) data.setAbsorption(p, a.get(p.getId()).getAsFloat());
        }
    }

    private static Path getBodyHealthDir() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) throw new IllegalStateException("Serveur non disponible");
        return server.getWorldPath(LevelResource.ROOT).resolve("bodyhealth");
    }
}
