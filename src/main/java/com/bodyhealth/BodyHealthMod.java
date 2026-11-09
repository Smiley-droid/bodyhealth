package com.bodyhealth;

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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(BodyHealthMod.MOD_ID)
public class BodyHealthMod {

    public static final String MOD_ID = "bodyhealth";

    private final IEventBus modEventBus;

    public BodyHealthMod(IEventBus modEventBus, ModContainer container) {
        this.modEventBus = modEventBus;
        container.registerConfig(ModConfig.Type.SERVER, BodyHealthConfig.SPEC);
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

        // Client — enregistré dès la construction du mod (voir ClientInit).
        // Le test de distribution garantit que ClientInit n'est jamais chargée
        // sur un serveur dédié.
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            com.bodyhealth.client.ClientInit.init(modEventBus);
        }
    }
}
