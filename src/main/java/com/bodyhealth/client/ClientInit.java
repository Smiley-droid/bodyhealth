package com.bodyhealth.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Tout ce qui est client-only. Appelée depuis le constructeur du mod, et
 * UNIQUEMENT quand Dist == CLIENT (jamais chargée sur un serveur dédié).
 *
 * RegisterKeyMappingsEvent est déclenché AVANT FMLClientSetupEvent : s'y abonner
 * dans clientSetup() était trop tard, la touche H n'existait donc pas.
 */
public final class ClientInit {

    private ClientInit() {}

    public static void init(IEventBus modBus) {
        HudSettings.load();
        modBus.addListener(BodyHealthKeyBindings::registerKeyMappings);
        NeoForge.EVENT_BUS.register(new HideVanillaHealthEvent());
        NeoForge.EVENT_BUS.register(new BodyHealthHUD());
        NeoForge.EVENT_BUS.register(new BodyHealthKeyBindings());
    }
}
