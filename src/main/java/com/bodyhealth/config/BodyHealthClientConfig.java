package com.bodyhealth.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Options CLIENT (par joueur, fichier bodyhealth-client.toml).
 * Cette classe ne référence aucune classe Minecraft client : elle peut être
 * chargée sans risque sur un serveur dédié.
 */
public class BodyHealthClientConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue COMPACT_GUIDE;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("text");
        COMPACT_GUIDE = BUILDER
                .comment("Compact Survival Guide: only lists injured body parts and hides",
                         "the effects / absorption / items / crafting sections.")
                .define("compactGuide", false);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
