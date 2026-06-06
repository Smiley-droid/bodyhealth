package com.bodyhealth.common;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.Map;

public class BodyHealthData {

    private final Map<BodyPart, Float>         currentHealth = new EnumMap<>(BodyPart.class);
    private final Map<BodyPart, Float>         maxHealth     = new EnumMap<>(BodyPart.class);
    private final Map<BodyPart, FractureState> fractures     = new EnumMap<>(BodyPart.class);
    private final Map<BodyPart, Float>         absorption    = new EnumMap<>(BodyPart.class);
    private final Map<BodyPart, Boolean>       regenerating  = new EnumMap<>(BodyPart.class);

    public static final Codec<BodyHealthData> CODEC =
            CompoundTag.CODEC.xmap(
                    tag -> { BodyHealthData d = new BodyHealthData(); d.deserializeNBT(tag); return d; },
                    BodyHealthData::serializeNBT
            );

    public BodyHealthData() {
        for (BodyPart part : BodyPart.values()) {
            currentHealth.put(part, part.getDefaultMaxHealth());
            maxHealth.put(part, part.getDefaultMaxHealth());
            fractures.put(part, FractureState.NONE);
            absorption.put(part, 0f);
            regenerating.put(part, false);
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public float getHealth(BodyPart part)        { return currentHealth.getOrDefault(part, 0f); }
    public float getMaxHealth(BodyPart part)     { return maxHealth.getOrDefault(part, part.getDefaultMaxHealth()); }
    public boolean isDead(BodyPart part)         { return getHealth(part) <= 0f; }
    public boolean shouldDie()                   { return isDead(BodyPart.TORSO); }
    public float getHealthPercent(BodyPart part) {
        float max = getMaxHealth(part);
        return max <= 0 ? 0f : getHealth(part) / max;
    }
    public boolean isCritical(BodyPart part) {
        return getHealth(part) <= 2.0f && !isDead(part);
    }
    public FractureState getFracture(BodyPart part) {
        return fractures.getOrDefault(part, FractureState.NONE);
    }
    public boolean isFractured(BodyPart part) {
        return getFracture(part).isAtLeast(FractureState.BROKEN);
    }
    public boolean isShattered(BodyPart part) {
        return getFracture(part) == FractureState.SHATTERED;
    }

    // ── Setters HP ───────────────────────────────────────────────────────────

    /**
     * BUG 2 FIX — setHealth() ne met plus à jour les fractures.
     *
     * Avant ce fix, setHealth() appelait updateFractureOnDamage() dans TOUS les cas,
     * y compris lors des soins (heal, healForced, setHealth direct).
     * Résultat : soigner une partie qui remontait brièvement sous 33% pendant le
     * calcul pouvait déclencher une fracture SPRAINED de manière erronée.
     *
     * Correction : setHealth() est maintenant neutre (clamp uniquement).
     * Les mises à jour de fracture sont gérées EXPLICITEMENT par :
     *   - damage()      → appelle updateFractureOnDamage()
     *   - heal()        → appelle updateFractureOnHeal()
     *   - healForced()  → améliore la fracture manuellement si demandé
     * Ainsi setHealth() utilisé directement (admin, API, totem) ne touche jamais aux fractures.
     */
    public void setHealth(BodyPart part, float value) {
        float clamped = Math.max(0f, Math.min(value, getMaxHealth(part)));
        currentHealth.put(part, clamped);
        // NOTE : pas d'update fracture ici — voir damage() et heal()
    }

    public void setMaxHealth(BodyPart part, float value) {
        float max = Math.max(2.0f, value);
        maxHealth.put(part, max);
        if (getHealth(part) > max) setHealth(part, max);
    }

    public void damage(BodyPart part, float amount) {
        float prev   = getHealth(part);
        float newVal = Math.max(0f, Math.min(prev - amount, getMaxHealth(part)));
        currentHealth.put(part, newVal);
        updateFractureOnDamage(part, newVal); // ← uniquement sur dégâts
    }

    public void heal(BodyPart part, float amount) {
        float prev   = getHealth(part);
        float newVal = Math.max(0f, Math.min(prev + amount, getMaxHealth(part)));
        currentHealth.put(part, newVal);
        updateFractureOnHeal(part); // ← uniquement sur soins
    }

    public void healAll(float amount) {
        for (BodyPart p : BodyPart.values()) heal(p, amount);
    }

    public void addMaxHealthAll(float amount) {
        for (BodyPart p : BodyPart.values()) setMaxHealth(p, getMaxHealth(p) + amount);
    }

    /**
     * Soin forcé (bandage, kit médical) — améliore la fracture si demandé.
     * Utilise heal() pour déclencher updateFractureOnHeal() normalement,
     * puis améliore d'un cran supplémentaire si improveFracture == true.
     */
    public void healForced(BodyPart part, float amount, boolean improveFracture) {
        heal(part, amount); // déclenche updateFractureOnHeal()
        if (improveFracture && fractures.get(part) != FractureState.NONE) {
            fractures.put(part, getFracture(part).improve());
        }
    }

    public float   getAbsorption(BodyPart part)    { return absorption.getOrDefault(part, 0f); }
    public void    setAbsorption(BodyPart part, float v) { absorption.put(part, Math.max(0f, v)); }
    public boolean isRegenerating(BodyPart part)   { return regenerating.getOrDefault(part, false); }
    public void    setRegenerating(BodyPart part, boolean v) { regenerating.put(part, v); }

    public void setFracture(BodyPart part, FractureState state) {
        fractures.put(part, state);
    }

    // ── Logique fracture ─────────────────────────────────────────────────────

    private void updateFractureOnDamage(BodyPart part, float newHp) {
        float pct = getMaxHealth(part) > 0 ? newHp / getMaxHealth(part) : 0f;
        FractureState current = getFracture(part);

        if (newHp <= 0) {
            // À 0 HP : BROKEN si sain/foulé, SHATTERED si déjà BROKEN
            if (current == FractureState.BROKEN || current == FractureState.SHATTERED) {
                fractures.put(part, FractureState.SHATTERED);
            } else {
                fractures.put(part, FractureState.BROKEN);
            }
        } else if (pct <= 0.33f && current == FractureState.NONE) {
            fractures.put(part, FractureState.SPRAINED);
        }
    }

    private void updateFractureOnHeal(BodyPart part) {
        float pct = getHealthPercent(part);
        FractureState current = getFracture(part);

        if (current == FractureState.SHATTERED) return; // kit médical requis

        if (current == FractureState.BROKEN && pct > 0.25f) {
            fractures.put(part, FractureState.SPRAINED);
        }
        if (current == FractureState.SPRAINED && pct > 0.60f) {
            fractures.put(part, FractureState.NONE);
            absorption.put(part, 0f);
            regenerating.put(part, false);
        }
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag h   = new CompoundTag();
        CompoundTag m   = new CompoundTag();
        CompoundTag f   = new CompoundTag();
        CompoundTag a   = new CompoundTag(); // absorption
        for (BodyPart p : BodyPart.values()) {
            h.putFloat(p.getId(), currentHealth.get(p));
            m.putFloat(p.getId(), maxHealth.get(p));
            f.putInt(p.getId(), getFracture(p).getLevel());
            a.putFloat(p.getId(), getAbsorption(p));
        }
        tag.put("health",     h);
        tag.put("maxHealth",  m);
        tag.put("fractures",  f);
        tag.put("absorption", a);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("health")) {
            CompoundTag h = tag.getCompound("health");
            for (BodyPart p : BodyPart.values())
                if (h.contains(p.getId()))
                    currentHealth.put(p, h.getFloat(p.getId()));
        }
        if (tag.contains("maxHealth")) {
            CompoundTag m = tag.getCompound("maxHealth");
            for (BodyPart p : BodyPart.values())
                if (m.contains(p.getId()))
                    maxHealth.put(p, m.getFloat(p.getId()));
        }
        if (tag.contains("fractures")) {
            CompoundTag f = tag.getCompound("fractures");
            for (BodyPart p : BodyPart.values())
                if (f.contains(p.getId()))
                    fractures.put(p, FractureState.fromLevel(f.getInt(p.getId())));
        }
        if (tag.contains("absorption")) {
            CompoundTag a = tag.getCompound("absorption");
            for (BodyPart p : BodyPart.values())
                if (a.contains(p.getId()))
                    absorption.put(p, a.getFloat(p.getId()));
        }
    }
}
