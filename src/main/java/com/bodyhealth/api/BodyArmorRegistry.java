package com.bodyhealth.api;

import com.bodyhealth.common.BodyPart;
import net.minecraft.world.item.Item;

import java.util.*;

/**
 * Registre d'armures custom pour la compatibilité avec les mods tiers.
 *
 * Utilisation :
 *   // Armure simple (une partie)
 *   BodyArmorRegistry.register(MyItems.MY_HELMET, BodyPart.HEAD, 0.40f);
 *
 *   // Armure multi-parties (combinaison, etc.)
 *   BodyArmorRegistry.registerMultiPart(MyItems.FULL_SUIT, Map.of(
 *       BodyPart.TORSO,     0.50f,
 *       BodyPart.ARM_LEFT,  0.30f,
 *       BodyPart.ARM_RIGHT, 0.30f
 *   ));
 */
public final class BodyArmorRegistry {

    private BodyArmorRegistry() {}

    // Registre principal : item → map(partie → réduction)
    private static final Map<Item, Map<BodyPart, Float>> REGISTRY = new HashMap<>();

    /**
     * Enregistre une armure qui protège une seule partie du corps.
     *
     * @param item      L'item d'armure à enregistrer
     * @param part      La partie du corps protégée
     * @param reduction Taux de réduction (0.0 = aucune, 1.0 = immunité totale)
     */
    public static void register(Item item, BodyPart part, float reduction) {
        float clamped = Math.max(0f, Math.min(1f, reduction));
        REGISTRY.computeIfAbsent(item, k -> new EnumMap<>(BodyPart.class))
                .put(part, clamped);
    }

    /**
     * Enregistre une armure qui protège plusieurs parties du corps.
     *
     * @param item       L'item d'armure
     * @param protections Map de partie → réduction
     */
    public static void registerMultiPart(Item item, Map<BodyPart, Float> protections) {
        Map<BodyPart, Float> entry = REGISTRY.computeIfAbsent(item, k -> new EnumMap<>(BodyPart.class));
        protections.forEach((part, reduction) ->
                entry.put(part, Math.max(0f, Math.min(1f, reduction))));
    }

    /**
     * Vérifie si un item est enregistré dans le registre custom.
     */
    public static boolean isRegistered(Item item) {
        return REGISTRY.containsKey(item);
    }

    /**
     * Retourne le taux de réduction d'un item pour une partie spécifique.
     * Retourne 0.0f si l'item n'est pas enregistré pour cette partie.
     */
    public static float getReduction(Item item, BodyPart part) {
        Map<BodyPart, Float> partMap = REGISTRY.get(item);
        if (partMap == null) return 0f;
        return partMap.getOrDefault(part, 0f);
    }

    /**
     * Retourne toutes les protections d'un item (lecture seule).
     */
    public static Optional<Map<BodyPart, Float>> getProtections(Item item) {
        Map<BodyPart, Float> result = REGISTRY.get(item);
        return result == null ? Optional.empty() : Optional.of(Collections.unmodifiableMap(result));
    }

    /**
     * Applique la réduction custom d'un item sur des dégâts donnés.
     * @return Dégâts après réduction
     */
    public static float applyReduction(Item item, BodyPart part, float damage) {
        float reduction = getReduction(item, part);
        return damage * (1f - reduction);
    }
}
