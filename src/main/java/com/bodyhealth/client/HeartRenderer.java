package com.bodyhealth.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;

/**
 * Rendu des cœurs vanilla 1.21.1.
 *
 * FIX ABSORPTION — Les cœurs jaunes d'absorption s'affichent maintenant
 * EN SURPLUS à droite des cœurs normaux, exactement comme vanilla.
 * Avant ce fix, detectType() retournait ABSORPTION ce qui rendait TOUS
 * les cœurs en jaune, masquant les rouges.
 *
 * Nouveau comportement :
 *   [❤❤❤❤❤] [💛💛]   ← normaux + absorption en surplus
 *
 * Sources d'absorption gérées :
 *   - Pomme enchantée dorée (Golden Apple)  → 4 HP absorption
 *   - Pomme de Notch (Enchanted Golden Apple) → 8 HP absorption
 *   - Totem d'immortalité → 4 HP absorption (géré dans TotemHandler)
 */
public class HeartRenderer {

    // On garde l'enum mais ABSORPTION n'est plus utilisé dans detectType()
    public enum HeartType { NORMAL, WITHER, POISON, FROZEN }

    private static final int H   = 9;
    private static final int GAP = 1;

    /** Type de cœur selon les effets actifs (sans absorption — gérée séparément) */
    public static HeartType detectType(LocalPlayer player) {
        if (player == null) return HeartType.NORMAL;
        if (player.hasEffect(MobEffects.WITHER)) return HeartType.WITHER;
        if (player.hasEffect(MobEffects.POISON)) return HeartType.POISON;
        if (player.isFullyFrozen())              return HeartType.FROZEN;
        return HeartType.NORMAL;
    }

    /** Rendu sans maxWidth (ligne unique). */
    public static void renderHearts(GuiGraphics gui, int x, int y,
                                     float hp, float maxHp,
                                     boolean critical, HeartType type) {
        renderHearts(gui, x, y, hp, maxHp, 0f, critical, type, Integer.MAX_VALUE);
    }

    /** Rendu avec maxWidth et absorption. */
    public static void renderHearts(GuiGraphics gui, int x, int y,
                                     float hp, float maxHp,
                                     boolean critical, HeartType type,
                                     int maxWidth) {
        renderHearts(gui, x, y, hp, maxHp, 0f, critical, type, maxWidth);
    }

    /**
     * Rendu complet avec cœurs normaux + cœurs absorption en surplus.
     *
     * @param absorption HP d'absorption à afficher en jaune APRÈS les cœurs normaux.
     *                   Ces cœurs s'ajoutent à droite, ils ne remplacent pas les rouges.
     */
    public static void renderHearts(GuiGraphics gui, int x, int y,
                                     float hp, float maxHp, float absorption,
                                     boolean critical, HeartType type,
                                     int maxWidth) {
        if (maxHp <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        boolean hc   = mc.level != null && mc.level.getLevelData().isHardcore();

        int heartsPerRow = Math.max(1, maxWidth / (H + GAP));

        // ── 1. Cœurs normaux ─────────────────────────────────────────────────
        int   total    = (int) Math.ceil(maxHp / 2f);
        int   full     = (int) Math.floor(hp / 2f);
        boolean half   = (hp % 2f) >= 1f;
        boolean blink  = critical && (System.currentTimeMillis() / 400) % 2 == 0;

        String prefix = switch (type) {
            case WITHER -> "withered_";
            case POISON -> "poisoned_";
            case FROZEN -> "frozen_";
            default     -> "";
        };
        String hcInfix     = hc ? "hardcore_" : "";
        String blinkSuffix = blink ? "_blinking" : "";
        ResourceLocation container = sprite(hc ? "container_hardcore" : "container");

        for (int i = 0; i < total; i++) {
            int col = i % heartsPerRow;
            int row = i / heartsPerRow;
            int hx  = x + col * (H + GAP);
            int hy  = y + row * (H + GAP);
            gui.blitSprite(container, hx, hy, H, H);
            if (hp > 0) {
                if (i < full)
                    gui.blitSprite(sprite(prefix + hcInfix + "full" + blinkSuffix), hx, hy, H, H);
                else if (i == full && half)
                    gui.blitSprite(sprite(prefix + hcInfix + "half" + blinkSuffix), hx, hy, H, H);
            }
        }

        // ── 2. Cœurs absorption EN SURPLUS (jaunes) ──────────────────────────
        // Placés immédiatement après le dernier cœur normal, sur la même ligne ou
        // à la suivante si débordement.
        if (absorption > 0) {
            int absTotal = (int) Math.ceil(absorption / 2f);
            int absFull  = (int) Math.floor(absorption / 2f);
            boolean absHalf = (absorption % 2f) >= 1f;

            for (int i = 0; i < absTotal; i++) {
                int absIndex = total + i; // position globale après les normaux
                int col = absIndex % heartsPerRow;
                int row = absIndex / heartsPerRow;
                int hx  = x + col * (H + GAP);
                int hy  = y + row * (H + GAP);

                // Conteneur absorption (doré)
                gui.blitSprite(sprite("container"), hx, hy, H, H);

                if (i < absFull)
                    gui.blitSprite(sprite("absorbing_" + hcInfix + "full"), hx, hy, H, H);
                else if (i == absFull && absHalf)
                    gui.blitSprite(sprite("absorbing_" + hcInfix + "half"), hx, hy, H, H);
            }
        }
    }

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/" + name);
    }
}
