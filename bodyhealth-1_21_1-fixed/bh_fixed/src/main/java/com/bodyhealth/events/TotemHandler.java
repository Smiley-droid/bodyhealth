package com.bodyhealth.events;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.common.FractureState;
import com.bodyhealth.network.BodyHealthSyncManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public class TotemHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;

        // Chercher le totem en main — AVANT toute vérification de torse
        ItemStack totem = findTotem(player);
        if (totem == null) return;

        BodyHealthData data = BodyHealthAPI.getData(player);

        // BUG 1 FIX — On annule TOUTE mort si le joueur a un totem en main,
        // comme vanilla. Ensuite on distingue deux cas :
        // A) Torse mort → c'est notre système qui a tué → soin d'urgence torse
        // B) Torse vivant → mort externe (enderman, /kill, noyade rapide...) 
        //    → remettre le torse à 2 HP minimum pour éviter mort immédiate
        event.setCanceled(true);
        player.setHealth(1.0f);

        if (data.isDead(BodyPart.TORSO)) {
            // Cas A : mort via notre système
            data.setHealth(BodyPart.TORSO, 2.0f);
        } else {
            // Cas B : mort externe — le torse avait encore des HP
            // On s'assure qu'il en a au moins 2 pour éviter boucle de mort
            if (data.getHealth(BodyPart.TORSO) < 2f)
                data.setHealth(BodyPart.TORSO, 2.0f);
        }

        // Absorption sur le torse
        data.setAbsorption(BodyPart.TORSO, 4.0f);

        // Effets vanilla du totem (invisibles)
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,      900, 1, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 900, 1, false, false, false));

        // Animation totem
        if (player.level() instanceof net.minecraft.server.level.ServerLevel sl)
            sl.broadcastEntityEvent(player, (byte) 35);

        totem.shrink(1);
        BodyHealthSyncManager.markDirtyAndFlush(player, data);

        player.displayClientMessage(
            Component.literal("§6✦ Le totem vous a sauvé ! §eSoignez-vous vite."), true);
    }

    private ItemStack findTotem(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off  = player.getOffhandItem();
        if (main.getItem() == Items.TOTEM_OF_UNDYING) return main;
        if (off.getItem()  == Items.TOTEM_OF_UNDYING) return off;
        return null;
    }
}
