package com.bodyhealth.sound;

import com.bodyhealth.BodyHealthMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Sons de douleur par partie du corps.
 *
 * Chaque son est défini dans sounds.json et mappé ici.
 * On utilise des sons vanilla existants comme base mais avec des
 * paramètres différents (pitch, volume) pour chaque partie.
 *
 *   Tête   → entity.player.hurt (pitch haut = douleur aiguë)
 *   Torse  → entity.player.hurt (pitch normal)
 *   Bras   → entity.player.hurt (pitch medium)
 *   Jambes → entity.player.hurt (pitch bas)
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, BodyHealthMod.MOD_ID);

    public static final Supplier<SoundEvent> HURT_HEAD  = register("hurt_head");
    public static final Supplier<SoundEvent> HURT_TORSO = register("hurt_torso");
    public static final Supplier<SoundEvent> HURT_ARM   = register("hurt_arm");
    public static final Supplier<SoundEvent> HURT_LEG   = register("hurt_leg");

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(BodyHealthMod.MOD_ID, name)));
    }
}
