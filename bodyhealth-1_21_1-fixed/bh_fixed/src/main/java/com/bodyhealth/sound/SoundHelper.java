package com.bodyhealth.sound;

import com.bodyhealth.common.BodyPart;
import com.bodyhealth.common.FractureState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Joue le son approprié selon la partie touchée et l'état de fracture.
 *
 * Sons par partie (pitch différent = douleur différente) :
 *   Tête     → pitch haut  (1.4-1.5) = douleur aiguë
 *   Torse    → pitch normal (0.9-1.0)
 *   Bras     → pitch medium (1.1-1.2)
 *   Jambes   → pitch bas   (0.6-0.7) = craquement sourd
 *
 * Sons de fracture (joués EN PLUS du son de douleur) :
 *   BROKEN   → fracture_crack   (os qui craque)
 *   SHATTERED→ fracture_shatter (os broyé — très grave)
 */
public class SoundHelper {

    /**
     * Joue le son de douleur pour la partie touchée.
     * Appelé depuis DamageEventHandler après chaque impact.
     *
     * @param player     Le joueur blessé
     * @param part       La partie du corps touchée
     * @param newFracture L'état de fracture APRÈS les dégâts (pour son additionnel)
     */
    public static void playHurtSound(ServerPlayer player, BodyPart part,
                                      FractureState newFracture) {
        // Son de douleur principal selon la partie
        var sound = switch (part) {
            case HEAD      -> ModSounds.HURT_HEAD.get();
            case TORSO     -> ModSounds.HURT_TORSO.get();
            case ARM_LEFT,
                 ARM_RIGHT -> ModSounds.HURT_ARM.get();
            case LEG_LEFT,
                 LEG_RIGHT -> ModSounds.HURT_LEG.get();
        };

        player.level().playSound(
                null,              // null = joue pour tous les joueurs proches
                player.getX(), player.getY(), player.getZ(),
                sound,
                SoundSource.PLAYERS,
                1.0f,              // volume
                1.0f               // pitch (défini dans sounds.json)
        );

        // Son additionnel si fracture nouvelle ou aggravée
        if (newFracture == FractureState.SHATTERED) {
            player.level().playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    ModSounds.FRACTURE_SHATTER.get(),
                    SoundSource.PLAYERS, 1.0f, 1.0f);
        } else if (newFracture == FractureState.BROKEN) {
            player.level().playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    ModSounds.FRACTURE_CRACK.get(),
                    SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }
}
