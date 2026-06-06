package com.bodyhealth;

import com.bodyhealth.client.BodyHealthHUD;
import com.bodyhealth.client.BodyHealthKeyBindings;
import com.bodyhealth.client.HideVanillaHealthEvent;
import com.bodyhealth.command.BodyHealthCommand;
import com.bodyhealth.common.BodyHealthAttachment;
import com.bodyhealth.config.BodyHealthConfig;
import com.bodyhealth.events.*;
import com.bodyhealth.item.ModCreativeTab;
import com.bodyhealth.item.ModItems;
import com.bodyhealth.network.NetworkRegistry;
import com.bodyhealth.sound.ModSounds;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(BodyHealthMod.MOD_ID)
public class BodyHealthMod {

    public static final String MOD_ID = "bodyhealth";

    public BodyHealthMod(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, BodyHealthConfig.SPEC);

        // Registres
        BodyHealthAttachment.ATTACHMENT_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTab.CREATIVE_TABS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);

        // Réseau
        modEventBus.addListener(NetworkRegistry::register);

        // Handlers serveur
        NeoForge.EVENT_BUS.register(new DamageEventHandler());
        NeoForge.EVENT_BUS.register(new PlayerTickHandler());
        NeoForge.EVENT_BUS.register(new BlockInteractionHandler());
        NeoForge.EVENT_BUS.register(new FoodHealHandler());
        NeoForge.EVENT_BUS.register(new PotionHealHandler());
        NeoForge.EVENT_BUS.register(new TotemHandler());
        NeoForge.EVENT_BUS.register(new AbsorptionHandler());
        NeoForge.EVENT_BUS.register(new FractureEffectHandler());

        // Commandes
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) ->
                BodyHealthCommand.register(e.getDispatcher()));

        // Keybindings — RegisterKeyMappingsEvent est un IModBusEvent
        // → DOIT être sur le mod event bus, jamais sur NeoForge.EVENT_BUS
        modEventBus.addListener(BodyHealthKeyBindings::registerKeyMappings);

        // Client
        modEventBus.addListener(this::clientSetup);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        // Handlers client sur NeoForge.EVENT_BUS
        NeoForge.EVENT_BUS.register(new HideVanillaHealthEvent());
        NeoForge.EVENT_BUS.register(new BodyHealthHUD());

        // Tick handler keybindings sur NeoForge.EVENT_BUS (game bus)
        NeoForge.EVENT_BUS.register(new BodyHealthKeyBindings());
    }
}
