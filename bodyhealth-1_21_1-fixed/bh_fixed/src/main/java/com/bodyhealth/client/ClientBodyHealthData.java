package com.bodyhealth.client;

import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.common.FractureState;
import net.minecraft.nbt.CompoundTag;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Cache client-side des données de santé, mis à jour par SyncBodyHealthPacket.
 *
 * BUG 2 FIX — Race condition inter-thread.
 *
 * Situation avant le fix :
 *   - Thread réseau (Netty)  → appelle update(tag) → écrit dans LOCAL_DATA
 *   - Thread de rendu (GL)   → appelle getHealth() → lit LOCAL_DATA
 *   BodyHealthData n'est pas thread-safe → lectures corrompues possibles
 *   (valeurs à moitié écrites, NullPointerException sur les EnumMaps, etc.)
 *
 * Solution : AtomicReference<BodyHealthData>
 *   - update() crée un NOUVEL objet BodyHealthData et le publie atomiquement
 *   - get() lit l'objet courant atomiquement — snapshot cohérent garanti
 *   - Aucun synchronized nécessaire (write-once, read-many sur chaque snapshot)
 *   - Pas de contention — les lectures du thread de rendu ne bloquent jamais
 */
public class ClientBodyHealthData {

    // Snapshot courant — remplacé atomiquement à chaque paquet réseau
    private static final AtomicReference<BodyHealthData> SNAPSHOT =
            new AtomicReference<>(new BodyHealthData());

    /**
     * Appelé par SyncBodyHealthPacket.handle() sur le thread client (via enqueueWork).
     * Crée un nouvel objet et le publie atomiquement.
     */
    public static void update(CompoundTag tag) {
        BodyHealthData fresh = new BodyHealthData();
        fresh.deserializeNBT(tag);
        SNAPSHOT.set(fresh); // publication atomique — visible immédiatement par tous les threads
    }

    // ── Getters — lisent le snapshot courant (thread-safe) ───────────────────

    private static BodyHealthData snap() { return SNAPSHOT.get(); }

    public static float         getHealth(BodyPart p)        { return snap().getHealth(p); }
    public static float         getMaxHealth(BodyPart p)     { return snap().getMaxHealth(p); }
    public static float         getHealthPercent(BodyPart p) { return snap().getHealthPercent(p); }
    public static boolean       isDead(BodyPart p)           { return snap().isDead(p); }
    public static boolean       isCritical(BodyPart p)       { return snap().isCritical(p); }
    public static FractureState getFracture(BodyPart p)      { return snap().getFracture(p); }
    public static float         getAbsorption(BodyPart p)    { return snap().getAbsorption(p); }
}
