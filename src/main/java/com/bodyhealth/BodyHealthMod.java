package com.bodyhealth;

import com.bodyhealth.command.BodyHealthCommand;
import com.bodyhealth.common.BodyHealthAttachment;
import com.bodyhealth.config.BodyHealthConfig;
import com.bodyhealth.events.*;
import com.bodyhealth.item.ModCreativeTab;
import com.bodyhealth.item.ModItems;
import com.bodyhealth.network.NetworkRegistry;
import com.bodyhealth.sound.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(BodyHealthMod.MOD_ID)
public class BodyHealthMod {

    public static final String MOD_ID = "bodyhealth";

    private final IEventBus modEventBus;

    public BodyHealthMod(IEventBus modEventBus, ModContainer container) {
        this.modEventBus = modEventBus;
        // Le type de config serveur a été renommé : SERVER (26.1, 26.2) -> SYNCED (26.3+).
        // On le choisit à l'exécution pour que le même code source compile sur toute la famille 26.x.
        ModConfig.Type serverType;
        try {
            serverType = ModConfig.Type.valueOf("SYNCED");
        } catch (IllegalArgumentException e) {
            serverType = ModConfig.Type.valueOf("SERVER");
        }
        container.registerConfig(serverType, BodyHealthConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, com.bodyhealth.config.BodyHealthClientConfig.SPEC);

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

        // Commandes
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) ->
                BodyHealthCommand.register(e.getDispatcher()));
    }
}
