package com.bodyhealth.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BodyHealthConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue     STARTING_HEARTS;
    public static final ModConfigSpec.BooleanValue ENABLE_NAUSEA;
    public static final ModConfigSpec.BooleanValue ENABLE_SLOWNESS;
    public static final ModConfigSpec.BooleanValue ENABLE_OFFHAND_DROP;
    public static final ModConfigSpec.BooleanValue ENABLE_BLOCKING_DISABLE;
    public static final ModConfigSpec.BooleanValue ENABLE_PARTIAL_ARMOR;
    public static final ModConfigSpec.BooleanValue ENABLE_CRACKED_OVERLAY;
    public static final ModConfigSpec.DoubleValue  STATUS_EFFECT_THRESHOLD;

    // Regen passive — très lente par défaut
    public static final ModConfigSpec.BooleanValue REGEN_ENABLED;
    public static final ModConfigSpec.DoubleValue  REGEN_RATE;
    public static final ModConfigSpec.BooleanValue REGEN_REQUIRE_FOOD;

    // Nourriture — lente
    public static final ModConfigSpec.DoubleValue  FOOD_HEAL_MULTIPLIER;

    static {
        BUILDER.push("general");
        STARTING_HEARTS = BUILDER
                .comment("Cœurs de départ par partie (défaut: 3)")
                .defineInRange("startingHearts", 3, 1, 20);
        BUILDER.pop();

        BUILDER.push("status_effects");
        ENABLE_NAUSEA           = BUILDER.define("enableNausea", true);
        ENABLE_SLOWNESS         = BUILDER.define("enableSlowness", true);
        ENABLE_OFFHAND_DROP     = BUILDER.define("enableOffhandDrop", true);
        ENABLE_BLOCKING_DISABLE = BUILDER.define("enableBlockingDisable", true);
        BUILDER.pop();

        BUILDER.push("armor");
        ENABLE_PARTIAL_ARMOR    = BUILDER.define("enablePartialArmorSystem", true);
        ENABLE_CRACKED_OVERLAY  = BUILDER.define("enableCrackedOverlay", true);
        STATUS_EFFECT_THRESHOLD = BUILDER.defineInRange("statusEffectThreshold", 2.0, 0.5, 10.0);
        BUILDER.pop();

        BUILDER.push("regeneration");
        REGEN_ENABLED = BUILDER
                .comment("Régénération passive (très lente sans bandage/kit)")
                .define("enabled", true);
        REGEN_RATE = BUILDER
                .comment("HP/seconde passifs — défaut 0.02 = 1 cœur toutes les 50 secondes")
                .defineInRange("ratePerSecond", 0.02, 0.001, 2.0); // ← TRÈS LENT
        REGEN_REQUIRE_FOOD = BUILDER
                .comment("Nécessite faim > 18 pour la regen passive")
                .define("requireFood", true);
        BUILDER.pop();

        BUILDER.push("food");
        FOOD_HEAL_MULTIPLIER = BUILDER
                .comment("Multiplicateur de soin de la nourriture (défaut 0.4 = lent)")
                .defineInRange("foodHealMultiplier", 0.4, 0.1, 2.0); // ← LENT
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
