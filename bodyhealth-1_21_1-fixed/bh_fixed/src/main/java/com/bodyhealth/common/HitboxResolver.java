package com.bodyhealth.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.Entity;

/**
 * Détermine quelle partie du corps est touchée selon la POSITION réelle de l'impact.
 *
 * Hitbox du joueur (hauteur totale = 1.8 blocs) :
 *
 *   Y+1.80 ┌──────────┐
 *          │  TÊTE    │  1.62 → 1.80  (18% de la hauteur)
 *   Y+1.62 ├──────────┤
 *          │          │
 *          │  TORSE   │  1.20 → 1.62  (23%)
 *          │          │
 *   Y+1.20 ├────┬─────┤
 *          │BrasG TORSE BrasD│  bras = côtés du torse
 *   Y+0.90 ├────┴─────┤
 *          │  JAMBES  │  0.00 → 0.90  (50%)
 *          │ G  │  D  │  gauche/droite selon X
 *   Y+0.00 └────┴─────┘
 *
 * Pour les attaques mêlée → on utilise la position de l'attaquant vs le joueur.
 * Pour les projectiles → on utilise le point d'impact réel.
 * Pour les dégâts sans position (poison, feu, chute) → fallback par type.
 */
public class HitboxResolver {

    // Seuils verticaux (relatifs au bas du joueur)
    private static final double HEAD_MIN   = 1.62;  // tête
    private static final double TORSO_MIN  = 0.90;  // torse
    private static final double LEG_MAX    = 0.90;  // jambes en dessous

    // Seuil horizontal pour distinguer gauche/droite (relatif au centre)
    private static final double SIDE_THRESHOLD = 0.2;

    /**
     * Point d'entrée principal.
     * Détermine la partie touchée selon la source et la position d'impact.
     *
     * @param player   Le joueur qui reçoit les dégâts
     * @param source   La source de dégâts
     * @param attacker L'entité attaquante (peut être null)
     * @return La partie du corps touchée
     */
    public static BodyPart resolve(Player player,
                                   net.minecraft.world.damagesource.DamageSource source,
                                   Entity attacker) {

        Vec3 playerFeet = player.position(); // bas du joueur

        // ── Projectile → position d'impact précise ────────────────────────
        if (attacker instanceof Projectile proj) {
            return resolveFromImpact(player, playerFeet,
                    proj.position(), true);
        }

        // ── Attaque mêlée → hauteur des yeux de l'attaquant ───────────────
        if (attacker != null) {
            Vec3 attackerEyes = attacker.getEyePosition();
            return resolveFromAttackerHeight(player, playerFeet, attackerEyes);
        }

        // ── Dégâts sans position (fall, fire, poison, magic…) ─────────────
        return resolveFallback(source);
    }

    /**
     * Résolution par position d'impact réelle (projectiles).
     * On calcule la hauteur relative au bas du joueur.
     */
    private static BodyPart resolveFromImpact(Player player, Vec3 feet,
                                               Vec3 impact, boolean precise) {
        double relY = impact.y - feet.y;  // hauteur relative (0 = sol, 1.8 = tête)
        double relX = impact.x - (feet.x + player.getBbWidth() / 2.0); // gauche/droite

        return resolveFromRelativePos(relY, relX, player.getBbHeight());
    }

    /**
     * Résolution par hauteur des yeux de l'attaquant.
     * Un mob grand touche le torse, un mob petit touche les jambes.
     */
    private static BodyPart resolveFromAttackerHeight(Player player,
                                                       Vec3 feet, Vec3 attackerEyes) {
        double targetHeight = player.getBbHeight(); // 1.8 normalement

        // Hauteur des yeux de l'attaquant relative au bas du joueur
        double relY = attackerEyes.y - feet.y;

        // Offset latéral : attaque vient de gauche ou droite ?
        double relX = attackerEyes.x - feet.x;

        return resolveFromRelativePos(relY, relX, targetHeight);
    }

    /**
     * Logique principale : position relative → partie du corps.
     */
    private static BodyPart resolveFromRelativePos(double relY, double relX, double height) {
        // Normalise la hauteur (0.0 = sol, 1.0 = sommet)
        double normY = relY / height;

        if (normY >= 0.90) {
            // Tête (90-100% de la hauteur)
            return BodyPart.HEAD;
        }

        if (normY >= 0.67) {
            // Zone torse/bras (67-90%)
            // Les bras sont sur les côtés du torse
            if (Math.abs(relX) > SIDE_THRESHOLD) {
                return relX < 0 ? BodyPart.ARM_LEFT : BodyPart.ARM_RIGHT;
            }
            return BodyPart.TORSO;
        }

        if (normY >= 0.50) {
            // Torse bas (50-67%)
            return BodyPart.TORSO;
        }

        // Jambes (0-50%) — gauche ou droite selon X
        return relX < 0 ? BodyPart.LEG_LEFT : BodyPart.LEG_RIGHT;
    }

    /**
     * Fallback pour les dégâts sans position précise.
     * On garde un minimum de logique par type sans être aléatoire.
     */
    private static BodyPart resolveFallback(net.minecraft.world.damagesource.DamageSource source) {
        var types = net.minecraft.world.damagesource.DamageTypes.class;

        // Chute → jambes (logique physique réelle)
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL))
            return BodyPart.LEG_LEFT; // jambe gauche par défaut

        // Noyade → tête (retenir sa respiration)
        if (source.is(net.minecraft.world.damagesource.DamageTypes.DROWN))
            return BodyPart.HEAD;

        // Suffocation → torse (pression)
        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)
         || source.is(net.minecraft.world.damagesource.DamageTypes.CRAMMING))
            return BodyPart.TORSO;

        // Feu / Lave → torse (surface la plus grande)
        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)
         || source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)
         || source.is(net.minecraft.world.damagesource.DamageTypes.LAVA))
            return BodyPart.TORSO;

        // Poison / Wither / Magie → torse (effets internes)
        return BodyPart.TORSO;
    }
}
