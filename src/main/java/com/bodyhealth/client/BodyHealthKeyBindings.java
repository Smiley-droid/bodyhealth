package com.bodyhealth.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@OnlyIn(Dist.CLIENT)
public class BodyHealthKeyBindings {

    public static final KeyMapping OPEN_HUD_SETTINGS = new KeyMapping(
            "key.bodyhealth.hud_settings",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            "key.categories.bodyhealth"
    );

    /**
     * Enregistrement de la touche — sur le MOD event bus via modEventBus.addListener()
     * appelé depuis BodyHealthMod.clientSetup()
     */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_HUD_SETTINGS);
    }

    /**
     * Handler tick — sur NeoForge.EVENT_BUS via instance enregistrée dans clientSetup()
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
