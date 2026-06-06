package com.bodyhealth.client;

import com.bodyhealth.BodyHealthMod;
import net.minecraft.resources.ResourceLocation;

/**
 * Gère la sélection des textures de cœurs selon l'état du joueur.
 *
 * Priorité des états (du plus prioritaire au moins) :
 *   1. Withered  (effet Wither actif)
 *   2. Poisoned  (effet Poison actif)
 *   3. Frozen    (effet Freezing / dans la poudre de neige)
 *   4. Absorbing (effet Absorption actif = cœurs jaunes)
 *   5. Hardcore  (monde hardcore)
 *   6. Normal    (état par défaut)
 *
 * Chaque état a : container, full, half, full_blinking, half_blinking
 * Le blinking s'active quand la partie est en état critique (< 2 HP).
 */
public class HeartTextureManager {

    private static final String BASE = "textures/gui/hearts/";

    // ── Conteneur (cœur vide) ─────────────────────────────────────────────
    public static ResourceLocation getContainer(HeartStyle style, boolean blinking) {
        String name = switch (style) {
            case HARDCORE -> blinking ? "container_hardcore_blinking" : "container_hardcore";
            default       -> blinking ? "container_blinking"          : "container";
        };
        return loc(name);
    }

    // ── Cœur plein ────────────────────────────────────────────────────────
    public static ResourceLocation getFull(HeartStyle style, boolean blinking, boolean hardcore) {
        String prefix = hardcore ? toHardcorePrefix(style) : toPrefix(style);
        String suffix = blinking ? "_full_blinking" : "_full";
        return loc(prefix + suffix);
    }

    // ── Demi-cœur ─────────────────────────────────────────────────────────
    public static ResourceLocation getHalf(HeartStyle style, boolean blinking, boolean hardcore) {
        String prefix = hardcore ? toHardcorePrefix(style) : toPrefix(style);
        String suffix = blinking ? "_half_blinking" : "_half";
        return loc(prefix + suffix);
    }

    private static String toPrefix(HeartStyle style) {
        return switch (style) {
            case NORMAL    -> "";
            case ABSORBING -> "absorbing_";
            case FROZEN    -> "frozen_";
            case POISONED  -> "poisoned_";
            case WITHERED  -> "withered_";
            case HARDCORE  -> "";
        };
    }

    private static String toHardcorePrefix(HeartStyle style) {
        return switch (style) {
            case NORMAL    -> "hardcore_";
            case ABSORBING -> "absorbing_hardcore_";
            case FROZEN    -> "frozen_hardcore_";
            case POISONED  -> "poisoned_hardcore_";
            case WITHERED  -> "withered_hardcore_";
            case HARDCORE  -> "hardcore_";
        };
    }

    private static ResourceLocation loc(String name) {
        // Corrige les doubles underscores (ex: "_full" avec prefix vide)
        String cleaned = name.startsWith("_") ? name.substring(1) : name;
        return ResourceLocation.fromNamespaceAndPath(BodyHealthMod.MOD_ID,
                BASE + cleaned + ".png");
    }

    /**
     * Détermine le style de cœur selon les effets actifs du joueur local.
     * Appelé à chaque frame de rendu.
     */
    public static HeartStyle resolveStyle(net.minecraft.client.player.LocalPlayer player) {
        if (player == null) return HeartStyle.NORMAL;

        // Wither > Poison > Frozen > Absorption > Normal
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.WITHER))
            return HeartStyle.WITHERED;
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.POISON))
            return HeartStyle.POISONED;
        if (player.isFullyFrozen())
            return HeartStyle.FROZEN;
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION))
            return HeartStyle.ABSORBING;
        if (player.level() instanceof net.minecraft.client.multiplayer.ClientLevel cl
                && cl.getLevelData().isHardcore())
            return HeartStyle.HARDCORE;

        return HeartStyle.NORMAL;
    }

    public enum HeartStyle {
        NORMAL, ABSORBING, FROZEN, POISONED, WITHERED, HARDCORE
    }
}
