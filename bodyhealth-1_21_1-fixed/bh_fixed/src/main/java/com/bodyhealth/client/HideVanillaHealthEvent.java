package com.bodyhealth.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@OnlyIn(Dist.CLIENT)
public class HideVanillaHealthEvent {

    @SubscribeEvent
    public void hideVanillaHud(RenderGuiLayerEvent.Pre event) {
        var name = event.getName();

        // Masque la barre de vie vanilla
        if (name.equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            event.setCanceled(true);
        }

        // Masque la barre d'armure vanilla
        if (name.equals(VanillaGuiLayers.ARMOR_LEVEL)) {
            event.setCanceled(true);
        }
    }
}
