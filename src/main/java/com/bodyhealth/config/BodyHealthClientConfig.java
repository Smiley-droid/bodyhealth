package com.bodyhealth.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Options CLIENT (par joueur, fichier bodyhealth-client.toml).
 * Cette classe ne référence aucune classe Minecraft client : elle peut être
 * chargée sans risque sur un serveur dédié.
 */
public class BodyHealthClientConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue COMPACT_GUIDE;
    public static final ModConfigSpec SPEC;

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
