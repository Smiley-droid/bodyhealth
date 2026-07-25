package com.bodyhealth.sound;

import com.bodyhealth.common.BodyPart;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Joue le son approprié selon la partie touchée.
 *
 * Sons par partie (pitch différent = douleur différente) :
 *   Tête   → pitch haut   (1.4-1.5) = douleur aiguë
 *   Torse  → pitch normal (0.9-1.0)
 *   Bras   → pitch medium (1.1-1.2)
 *   Jambes → pitch bas    (0.6-0.7) = craquement sourd
 */
public class SoundHelper {

    /**
     * Joue le son de douleur pour la partie touchée.
     * Appelé depuis DamageEventHandler après chaque impact.
     */
    public static void playHurtSound(ServerPlayer player, BodyPart part) {
        var sound = switch (part) {
            case HEAD      -> ModSounds.HURT_HEAD.get();
            case TORSO     -> ModSounds.HURT_TORSO.get();
            case ARM_LEFT,
                 ARM_RIGHT -> ModSounds.HURT_ARM.get();
            case LEG_LEFT,
                 LEG_RIGHT -> ModSounds.HURT_LEG.get();
        };

        player.level().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                sound,
                SoundSource.PLAYERS,
                1.0f,
                1.0f
        );
    }
}
