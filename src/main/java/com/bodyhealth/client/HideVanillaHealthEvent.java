package com.bodyhealth.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

@OnlyIn(Dist.CLIENT)
public class HideVanillaHealthEvent {

    @SubscribeEvent
    public void hideVanillaHud(RenderGuiOverlayEvent.Pre event) {
        // Si le mod est désactivé, on laisse vanilla afficher sa barre normalement
        if (!ClientBodyHealthData.isModEnabled()) return;

        var overlay = event.getOverlay();

        // Masque la barre de vie vanilla
        if (overlay == VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            event.setCanceled(true);
        }

        // Masque la barre d'armure vanilla
        if (overlay == VanillaGuiOverlay.ARMOR_LEVEL.type()) {
            event.setCanceled(true);
        }
    }
}
