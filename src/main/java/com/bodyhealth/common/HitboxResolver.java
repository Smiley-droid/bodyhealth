package com.bodyhealth.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.Entity;

import java.util.Random;

/**
 * Détermine quelle partie du corps est touchée — système HYBRIDE.
 *
 * 1. Coups directs (mêlée, projectiles) → POSITION réelle de l'impact
 *    (trajectoire/hauteur), c'est le modèle le plus réaliste pour un coup
 *    physique localisé.
 *
 * 2. Dégâts sans position physique (chute, explosion, feu, faim...) →
 *    règles par TYPE inspirées de SrGnis Body-Health-System, qui collent
 *    mieux à la réalité anatomique pour ces cas précis :
 *      - Chute / sol brûlant     → jambe aléatoire (on ne retombe pas
 *                                   toujours du même côté)
 *      - Chute d'objet (enclume,
 *        bloc, stalactite, mur)  → tête (l'objet tombe par le haut)
 *      - Explosion / foudre      → partie totalement aléatoire (l'onde de
 *                                   choc / les éclats frappent partout)
 *      - Noyade                  → torse (poumons, pas la tête)
 *      - Faim                    → torse (organe digestif)
 *      - Feu / lave              → torse (plus grande surface exposée)
 *      - Poison / Wither / magie → partie aléatoire non létale
 *
 * Hitbox du joueur pour la résolution par position (hauteur totale = 1.8 blocs) :
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
 */
public class HitboxResolver {

    private static final double SIDE_THRESHOLD = 0.2;
    private static final Random RANDOM = new Random();

    public static BodyPart resolve(Player player,
                                   net.minecraft.world.damagesource.DamageSource source,
                                   Entity attacker) {

        Vec3 playerFeet = player.position();

        // ── Projectile → position d'impact précise ────────────────────────
        if (attacker instanceof Projectile proj) {
            return resolveFromImpact(player, playerFeet, proj.position());
        }

        // ── Attaque mêlée → hauteur des yeux de l'attaquant ───────────────
        if (attacker != null) {
            Vec3 attackerEyes = attacker.getEyePosition();
            return resolveFromAttackerHeight(player, playerFeet, attackerEyes);
        }

        // ── Dégâts sans position → règles par type façon SrGnis ──────────
        return resolveByDamageType(source);
    }

    private static BodyPart resolveFromImpact(Player player, Vec3 feet, Vec3 impact) {
        double relY = impact.y - feet.y;
        double relX = impact.x - (feet.x + player.getBbWidth() / 2.0);
        return resolveFromRelativePos(relY, relX, player.getBbHeight());
    }

    private static BodyPart resolveFromAttackerHeight(Player player,
                                                       Vec3 feet, Vec3 attackerEyes) {
        double targetHeight = player.getBbHeight();
        double relY = attackerEyes.y - feet.y;
        double relX = attackerEyes.x - feet.x;
        return resolveFromRelativePos(relY, relX, targetHeight);
    }

    private static BodyPart resolveFromRelativePos(double relY, double relX, double height) {
        double normY = relY / height;

        if (normY >= 0.90) return BodyPart.HEAD;

        if (normY >= 0.67) {
            if (Math.abs(relX) > SIDE_THRESHOLD)
                return relX < 0 ? BodyPart.ARM_LEFT : BodyPart.ARM_RIGHT;
            return BodyPart.TORSO;
        }

        if (normY >= 0.50) return BodyPart.TORSO;

        return relX < 0 ? BodyPart.LEG_LEFT : BodyPart.LEG_RIGHT;
    }

    /**
     * Règles par type de dégâts, inspirées de SrGnis Body-Health-System
     * PlayerBody.applyDamageBySource() mais adaptées à notre modèle 6-parties
     * (pas de pieds séparés des jambes).
     */
    private static BodyPart resolveByDamageType(net.minecraft.world.damagesource.DamageSource source) {
        var types = net.minecraft.world.damagesource.DamageTypes.class;

        // Chute / sol brûlant → jambe aléatoire (on ne tombe pas toujours pareil)
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL)
         || source.is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR)) {
            return RANDOM.nextBoolean() ? BodyPart.LEG_LEFT : BodyPart.LEG_RIGHT;
        }

        // Chute d'objet par le haut → tête
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FALLING_ANVIL)
         || source.is(net.minecraft.world.damagesource.DamageTypes.FALLING_BLOCK)
         || source.is(net.minecraft.world.damagesource.DamageTypes.FALLING_STALACTITE)
         || source.is(net.minecraft.world.damagesource.DamageTypes.FLY_INTO_WALL)) {
            return BodyPart.HEAD;
        }

        // Explosion / foudre → partie totalement aléatoire (éclats/onde de choc)
        if (source.is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION)
         || source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_EXPLOSION)
         || source.is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT)) {
            return randomPart();
        }

        // Noyade → torse (poumons, pas la tête)
        if (source.is(net.minecraft.world.damagesource.DamageTypes.DROWN))
            return BodyPart.TORSO;

        // Faim → torse (organe digestif)
        if (source.is(net.minecraft.world.damagesource.DamageTypes.STARVE))
            return BodyPart.TORSO;

        // Suffocation → torse (pression)
        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)
         || source.is(net.minecraft.world.damagesource.DamageTypes.CRAMMING))
            return BodyPart.TORSO;

        // Feu / lave → torse (plus grande surface exposée)
        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)
         || source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)
         || source.is(net.minecraft.world.damagesource.DamageTypes.LAVA))
            return BodyPart.TORSO;

        // Poison / Wither / magie / autre → partie aléatoire non létale
        return randomNonCriticalPart();
    }

    private static BodyPart randomPart() {
        BodyPart[] parts = BodyPart.values();
        return parts[RANDOM.nextInt(parts.length)];
    }

    /** Exclut TORSO et HEAD pour les dégâts internes type poison — évite la mort instantanée par tick. */
    private static BodyPart randomNonCriticalPart() {
        BodyPart[] parts = { BodyPart.ARM_LEFT, BodyPart.ARM_RIGHT,
                              BodyPart.LEG_LEFT, BodyPart.LEG_RIGHT };
        return parts[RANDOM.nextInt(parts.length)];
    }
}

