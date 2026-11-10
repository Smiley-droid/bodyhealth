package com.bodyhealth.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class BodyHealthKeyBindings {

    /** Catégorie affichée dans Options > Contrôles (clé de langue key.category.bodyhealth.main). */
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(net.minecraft.resources.Identifier.fromNamespaceAndPath(
                    com.bodyhealth.BodyHealthMod.MOD_ID, "main"));

    public static final KeyMapping OPEN_HUD_SETTINGS = new KeyMapping(
            "key.bodyhealth.hud_settings",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            CATEGORY
    );

    /**
     * Enregistrement de la touche — sur le MOD event bus via modEventBus.addListener()
     * appelé depuis ClientInit.init() (constructeur du mod)
     */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_HUD_SETTINGS);
    }

    /**
     * Handler tick — sur NeoForge.EVENT_BUS via instance enregistrée dans ClientInit.init()
     */
    @SubscribeEvent
    public void onClientTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof net.minecraft.client.player.LocalPlayer)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return; // pas de capture si menu ouvert
        while (OPEN_HUD_SETTINGS.consumeClick()) {
            mc.setScreen(new HudSettingsScreen());
        }
    }
}
