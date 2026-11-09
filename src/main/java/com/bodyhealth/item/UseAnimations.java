package com.bodyhealth.item;

import net.minecraft.world.item.ItemUseAnimation;

/**
 * Animation de bras des objets de soin (bandage, kit, seringue).
 * On cherche la constante par son nom à l'exécution : si elle n'existait pas dans
 * une version de Minecraft, on retombe sur EAT au lieu de faire échouer le chargement.
 */
public final class UseAnimations {

    public static final ItemUseAnimation HEAL = resolve("BOW");

    private UseAnimations() {}

    private static ItemUseAnimation resolve(String name) {
        try {
            return ItemUseAnimation.valueOf(name);
        } catch (IllegalArgumentException e) {
            return ItemUseAnimation.EAT;
        }
    }
}
