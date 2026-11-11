package com.bodyhealth;

import com.bodyhealth.command.BodyHealthCommand;
import com.bodyhealth.config.BodyHealthConfig;
import com.bodyhealth.events.*;
import com.bodyhealth.item.ModCreativeTab;
import com.bodyhealth.item.ModItems;
import com.bodyhealth.network.NetworkRegistry;
import com.bodyhealth.sound.ModSounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;

@Mod(BodyHealthMod.MOD_ID)
public class BodyHealthMod {

    public static final String MOD_ID = "bodyhealth";

    private final IEventBus modEventBus;

    public BodyHealthMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        this.modEventBus = modEventBus;
        context.registerConfig(ModConfig.Type.SERVER, BodyHealthConfig.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, com.bodyhealth.config.BodyHealthClientConfig.SPEC);

        // Registres
        MinecraftForge.EVENT_BUS.register(com.bodyhealth.common.BodyHealthCapability.class);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTab.CREATIVE_TABS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);

        // Réseau
        NetworkRegistry.init();

        // Handlers serveur
        MinecraftForge.EVENT_BUS.register(new DamageEventHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerTickHandler());
        MinecraftForge.EVENT_BUS.register(new BlockInteractionHandler());
        MinecraftForge.EVENT_BUS.register(new FoodHealHandler());
        MinecraftForge.EVENT_BUS.register(new PotionHealHandler());
        MinecraftForge.EVENT_BUS.register(new TotemHandler());
        MinecraftForge.EVENT_BUS.register(new AbsorptionHandler());

        // Commandes
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) ->
                BodyHealthCommand.register(e.getDispatcher()));

        // Client — enregistré dès la construction du mod (voir ClientInit).
        // Le test de distribution garantit que ClientInit n'est jamais chargée
        // sur un serveur dédié.
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            com.bodyhealth.client.ClientInit.init(modEventBus);
        }
    }
}
