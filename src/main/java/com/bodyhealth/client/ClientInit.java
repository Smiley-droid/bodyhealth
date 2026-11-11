package com.bodyhealth.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;

/**
 * Tout ce qui est client-only. Appelée depuis le constructeur du mod, et
 * UNIQUEMENT quand Dist == CLIENT (jamais chargée sur un serveur dédié).
 *
 * RegisterKeyMappingsEvent est déclenché AVANT FMLClientSetupEvent : s'y abonner
 * dans clientSetup() était trop tard, la touche H n'existait donc pas.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientInit {

    private ClientInit() {}

    public static void init(IEventBus modBus) {
        HudSettings.load();
        modBus.addListener(BodyHealthKeyBindings::registerKeyMappings);
        MinecraftForge.EVENT_BUS.register(new HideVanillaHealthEvent());
        MinecraftForge.EVENT_BUS.register(new BodyHealthHUD());
        MinecraftForge.EVENT_BUS.register(new BodyHealthKeyBindings());
    }
}
