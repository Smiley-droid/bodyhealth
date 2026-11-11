package com.bodyhealth.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;

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
     * appelé depuis ClientInit.init() (constructeur du mod)
     */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_HUD_SETTINGS);
    }

    /**
     * Handler tick — sur MinecraftForge.EVENT_BUS via instance enregistrée dans ClientInit.init()
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof net.minecraft.client.player.LocalPlayer)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return; // pas de capture si menu ouvert
        while (OPEN_HUD_SETTINGS.consumeClick()) {
            mc.setScreen(new HudSettingsScreen());
        }
    }
}
