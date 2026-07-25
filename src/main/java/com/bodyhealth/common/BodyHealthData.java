package com.bodyhealth.common;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.Map;

public class BodyHealthData {

    private final Map<BodyPart, Float>   currentHealth = new EnumMap<>(BodyPart.class);
    private final Map<BodyPart, Float>   maxHealth     = new EnumMap<>(BodyPart.class);
    private final Map<BodyPart, Float>   absorption    = new EnumMap<>(BodyPart.class);
    private final Map<BodyPart, Boolean> regenerating  = new EnumMap<>(BodyPart.class);

    public static final Codec<BodyHealthData> CODEC =
            CompoundTag.CODEC.xmap(
                    tag -> { BodyHealthData d = new BodyHealthData(); d.deserializeNBT(tag); return d; },
                    BodyHealthData::serializeNBT
            );

    public BodyHealthData() {
        for (BodyPart part : BodyPart.values()) {
            currentHealth.put(part, part.getDefaultMaxHealth());
            maxHealth.put(part, part.getDefaultMaxHealth());
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

    /**
     * Seuil critique façon SrGnis Body-Health-System :
     *   - HEAD et TORSO  → critique sous maxHealth/3 (mort possible)
     *   - ARM et LEG     → critique uniquement à 0 HP (partie détruite)
     * Voir DamageEventHandler.applyStatusEffects() pour la logique d'amplifier.
     */
    public boolean isCritical(BodyPart part) {
        if (part == BodyPart.HEAD || part == BodyPart.TORSO) {
            return getHealth(part) <= getMaxHealth(part) / 3f && !isDead(part);
        }
        return false; // ARM/LEG : pas d'état "critique" intermédiaire, seulement isDead()
    }

    // ── Setters HP ───────────────────────────────────────────────────────────

    public void setHealth(BodyPart part, float value) {
        float clamped = Math.max(0f, Math.min(value, getMaxHealth(part)));
        currentHealth.put(part, clamped);
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
    }

    public void heal(BodyPart part, float amount) {
        float prev   = getHealth(part);
        float newVal = Math.max(0f, Math.min(prev + amount, getMaxHealth(part)));
        currentHealth.put(part, newVal);
    }

    public void healAll(float amount) {
        for (BodyPart p : BodyPart.values()) heal(p, amount);
    }

    public void addMaxHealthAll(float amount) {
        for (BodyPart p : BodyPart.values()) setMaxHealth(p, getMaxHealth(p) + amount);
    }

    public float   getAbsorption(BodyPart part)    { return absorption.getOrDefault(part, 0f); }
    public void    setAbsorption(BodyPart part, float v) { absorption.put(part, Math.max(0f, v)); }
    public boolean isRegenerating(BodyPart part)   { return regenerating.getOrDefault(part, false); }
    public void    setRegenerating(BodyPart part, boolean v) { regenerating.put(part, v); }

    // ── NBT ──────────────────────────────────────────────────────────────────

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag h   = new CompoundTag();
        CompoundTag m   = new CompoundTag();
        CompoundTag a   = new CompoundTag();
        for (BodyPart p : BodyPart.values()) {
            h.putFloat(p.getId(), currentHealth.get(p));
            m.putFloat(p.getId(), maxHealth.get(p));
            a.putFloat(p.getId(), getAbsorption(p));
        }
        tag.put("health",     h);
        tag.put("maxHealth",  m);
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
        if (tag.contains("absorption")) {
            CompoundTag a = tag.getCompound("absorption");
            for (BodyPart p : BodyPart.values())
                if (a.contains(p.getId()))
                    absorption.put(p, a.getFloat(p.getId()));
        }
    }
}
