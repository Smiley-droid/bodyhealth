package com.bodyhealth.events;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.config.BodyHealthConfig;
import com.bodyhealth.item.MorphineSyringeItem;
import com.bodyhealth.network.BodyHealthSyncManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerTickHandler {

    private static final int REGEN_INTERVAL = 20;
    private final Map<UUID, Integer> tickCounters  = new HashMap<>();

    // BUG 3 FIX : évite de déclencher la mort plusieurs ticks de suite.
    // Une fois que player.hurt() est appelé, isDeadOrDying() devient vrai
    // au tick suivant, mais pas forcément au même tick sur certains chemins.
    // Ce Set garantit qu'on n'appelle hurt() qu'une seule fois par joueur.

    @SubscribeEvent
    public void cancelVanillaHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Annuler TOUS les soins vanilla — notre regen passive gère les HP.
        // L'absorption vanilla est ignorée car on gère notre propre absorption
        // via AbsorptionHandler qui lit l'effet ABSORPTION directement.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();

        // Nettoyer le flag de mort si le joueur est respawné
        if (!player.isDeadOrDying()) {
        }

        if (player.isDeadOrDying()) return;

        int counter = tickCounters.merge(uuid, 1, Integer::sum);

        BodyHealthData data = BodyHealthAPI.getData(player);

        // Maintenir la vie vanilla au maximum pour éviter mort vanilla
        // FIX double mort — ne pas remettre la vie si le joueur est en train de mourir
        if (!player.isDeadOrDying() && player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        applyOngoingEffects(player, data);

        if (counter >= REGEN_INTERVAL) {
            tickCounters.put(uuid, 0);
            handlePassiveRegen(player, data);
        }

        // NOTE — la mort est gérée UNIQUEMENT dans DamageEventHandler.onPlayerDamage()
        // via player.setHealth(0f) + player.kill().
        // On ne tue plus depuis le tick handler pour éviter la double mort.
    }

    private void applyOngoingEffects(ServerPlayer player, BodyHealthData data) {
        // FIX — Weakness uniquement si bras DROIT = 0 HP (mort), pas critique.
        // isCritical (≤33%) bloquait le joueur alors que le bras avait encore des HP.
        // BlockInteractionHandler gère déjà l'annulation des events d'interaction.
        // Ici on applique Weakness uniquement pour empêcher les dégâts au corps-à-corps.
        if (BodyHealthConfig.ENABLE_BLOCKING_DISABLE.get() && data.isDead(BodyPart.ARM_RIGHT)) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 2, false, false, false));
        }

        if (BodyHealthConfig.ENABLE_SLOWNESS.get()) {
            boolean lc = data.isCritical(BodyPart.LEG_LEFT)  || data.isDead(BodyPart.LEG_LEFT);
            boolean rc = data.isCritical(BodyPart.LEG_RIGHT) || data.isDead(BodyPart.LEG_RIGHT);
            if (lc && rc)      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 2, false, false, false));
            else if (lc || rc) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, false, false, false));
        }
    }

    private void handlePassiveRegen(ServerPlayer player, BodyHealthData data) {
        if (!BodyHealthConfig.REGEN_ENABLED.get()) return;
        if (isMorphineActive(player)) return;
        if (BodyHealthConfig.REGEN_REQUIRE_FOOD.get()
                && player.getFoodData().getFoodLevel() < 18) return;

        float   rate    = BodyHealthConfig.REGEN_RATE.get().floatValue();
        boolean changed = false;
        for (BodyPart part : BodyPart.values()) {
            if (data.getHealth(part) < data.getMaxHealth(part)) {
                data.heal(part, rate);
                changed = true;
            }
        }
        if (changed) BodyHealthSyncManager.markDirtyLazy(player, data);
    }

    private boolean isMorphineActive(ServerPlayer player) {
        return System.currentTimeMillis() <
                player.getPersistentData().getLong(MorphineSyringeItem.NBT_MORPHINE_ACTIVE);
    }
}
