package com.bodyhealth.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Réglages de l'overlay, sauvegardés dans config/bodyhealth-hud.json
 * (position, taille, mode d'affichage). Chargés une fois au démarrage du client.
 */
@OnlyIn(Dist.CLIENT)
public final class HudSettings {

    /** Ce que l'overlay affiche. */
    public enum Mode {
        FULL("full"),     // silhouette + noms + coeurs
        HEARTS("hearts"), // noms + coeurs (sans silhouette)
        TEXT("text");     // noms + valeurs de vie (sans silhouette)

        public final String key;
        Mode(String key) { this.key = key; }

        public Mode next() {
            Mode[] all = values();
            return all[(ordinal() + 1) % all.length];
        }

        public static Mode fromName(String s) {
            for (Mode m : values()) if (m.name().equalsIgnoreCase(s)) return m;
            return FULL;
        }
    }

    public static final int DEFAULT_OFFSET = 5;
    public static final int DEFAULT_SCALE  = 3;
    public static final int MIN_SCALE      = 1;
    public static final int MAX_SCALE      = 6;

    public static int  offsetX = DEFAULT_OFFSET; // distance au bord droit
    public static int  offsetY = DEFAULT_OFFSET; // distance au bord bas
    public static int  scale   = DEFAULT_SCALE;  // 1..6 (3 = taille d'origine)
    public static Mode mode    = Mode.FULL;

    private static final Logger LOGGER = LoggerFactory.getLogger("BodyHealth");
    private static final Gson   GSON   = new GsonBuilder().setPrettyPrinting().create();

    private HudSettings() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("bodyhealth-hud.json");
    }

    public static void reset() {
        offsetX = DEFAULT_OFFSET;
        offsetY = DEFAULT_OFFSET;
        scale   = DEFAULT_SCALE;
        mode    = Mode.FULL;
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            if (o.has("offsetX")) offsetX = Math.max(0, o.get("offsetX").getAsInt());
            if (o.has("offsetY")) offsetY = Math.max(0, o.get("offsetY").getAsInt());
            if (o.has("scale"))   scale   = Math.max(MIN_SCALE, Math.min(MAX_SCALE, o.get("scale").getAsInt()));
            if (o.has("mode"))    mode    = Mode.fromName(o.get("mode").getAsString());
        } catch (Exception e) {
            LOGGER.warn("[BodyHealth] Impossible de lire les réglages du HUD: {}", e.getMessage());
        }
    }

    public static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("offsetX", offsetX);
            o.addProperty("offsetY", offsetY);
            o.addProperty("scale",   scale);
            o.addProperty("mode",    mode.name());
            Files.writeString(file(), GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.warn("[BodyHealth] Impossible de sauvegarder les réglages du HUD: {}", e.getMessage());
        }
    }
}
