package com.bodyhealth.common;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * FIX BUG 1 : ARM_RIGHT/LEFT ne correspondent pas à des slots d'armure.
 * On utilise CHEST comme fallback pour les bras (pas de slot dédié en vanilla).
 * Les mobs ne portent pas d'armure aux bras — la réduction bras passe par l'API custom.
 */
public enum BodyPart {
    HEAD      ("head",       EquipmentSlot.HEAD,  6.0f),
    TORSO     ("torso",      EquipmentSlot.CHEST, 6.0f),
    ARM_RIGHT ("arm_right",  EquipmentSlot.CHEST, 6.0f), // FIX: pas de slot bras, fallback CHEST
    ARM_LEFT  ("arm_left",   EquipmentSlot.CHEST, 6.0f), // FIX: idem
    LEG_RIGHT ("leg_right",  EquipmentSlot.LEGS,  6.0f),
    LEG_LEFT  ("leg_left",   EquipmentSlot.FEET,  6.0f);

    private final String        id;
    private final EquipmentSlot armorSlot;        // slot d'armure le plus proche
    private final float         defaultMaxHealth;

    BodyPart(String id, EquipmentSlot armorSlot, float defaultMaxHealth) {
        this.id               = id;
        this.armorSlot        = armorSlot;
        this.defaultMaxHealth = defaultMaxHealth;
    }

    public String        getId()               { return id; }
    public EquipmentSlot getArmorSlot()        { return armorSlot; }
    public float         getDefaultMaxHealth() { return defaultMaxHealth; }

    public boolean isLeg() { return this == LEG_LEFT || this == LEG_RIGHT; }
    public boolean isArm() { return this == ARM_LEFT || this == ARM_RIGHT; }

    public static BodyPart fromId(String id) {
        for (BodyPart p : values()) if (p.id.equals(id)) return p;
        return TORSO;
    }
}
