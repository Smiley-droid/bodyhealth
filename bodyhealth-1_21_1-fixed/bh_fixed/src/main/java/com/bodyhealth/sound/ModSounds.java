package com.bodyhealth.sound;

import com.bodyhealth.BodyHealthMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Sons de douleur par partie du corps.
 *
 * Chaque son est défini dans sounds.json et mappé ici.
 * On utilise des sons vanilla existants comme base (pas besoin de fichiers audio custom)
 * mais avec des paramètres différents (pitch, volume) pour chaque partie.
 *
 * Sons utilisés depuis vanilla :
 *   Tête      → entity.player.hurt  (pitch haut = douleur aiguë)
 *   Torse     → entity.player.hurt  (pitch normal)
 *   Bras      → entity.player.hurt  (pitch medium)
 *   Jambes    → entity.player.hurt  (pitch bas = craquement)
 *   Fracture  → entity.player.hurt_on_fire (son sec = os qui craque)
 *   Broyée    → entity.player.death (pitch très bas = grave)
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, BodyHealthMod.MOD_ID);

    public static final Supplier<SoundEvent> HURT_HEAD =
            register("hurt_head");
    public static final Supplier<SoundEvent> HURT_TORSO =
            register("hurt_torso");
    public static final Supplier<SoundEvent> HURT_ARM =
            register("hurt_arm");
    public static final Supplier<SoundEvent> HURT_LEG =
            register("hurt_leg");
    public static final Supplier<SoundEvent> FRACTURE_CRACK =
            register("fracture_crack");   // os qui craque
    public static final Supplier<SoundEvent> FRACTURE_SHATTER =
            register("fracture_shatter"); // os broyé


    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(BodyHealthMod.MOD_ID, name)));
    }
}
