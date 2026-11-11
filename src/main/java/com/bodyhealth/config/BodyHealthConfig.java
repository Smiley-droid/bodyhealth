package com.bodyhealth.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class BodyHealthConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue ACTION_BAR_MESSAGES;
    public static final ForgeConfigSpec.IntValue     STARTING_HEARTS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_NAUSEA;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SLOWNESS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_OFFHAND_DROP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BLOCKING_DISABLE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PARTIAL_ARMOR;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CRACKED_OVERLAY;
    public static final ForgeConfigSpec.DoubleValue  STATUS_EFFECT_THRESHOLD;

    // Regen passive — très lente par défaut
    public static final ForgeConfigSpec.BooleanValue REGEN_ENABLED;
    public static final ForgeConfigSpec.DoubleValue  REGEN_RATE;
    public static final ForgeConfigSpec.BooleanValue REGEN_REQUIRE_FOOD;

    // Nourriture — lente
    public static final ForgeConfigSpec.DoubleValue  FOOD_HEAL_MULTIPLIER;

    static {
        BUILDER.push("general");
        ENABLED = BUILDER
                .comment("Enable or disable the entire Body Health System.",
                         "Set to false to fully deactivate the mod without uninstalling it.",
                         "Players will use vanilla health when disabled.")
                .define("enabled", true);
        ACTION_BAR_MESSAGES = BUILDER
                .comment("Show warning messages above the hotbar when a body part becomes critical or destroyed.",
                         "Set to false for a quieter experience (effects still apply).")
                .define("actionBarMessages", true);
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
