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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerTickHandler {

    private static final int REGEN_INTERVAL = 20;
    private final Map<UUID, Integer> tickCounters = new HashMap<>();

    /**
     * Annulation des soins vanilla — UNIQUEMENT quand le mod est actif.
     * Quand ENABLED = false, on laisse passer tous les soins vanilla
     * (régénération naturelle, nourriture, potions) pour un comportement 100% vanilla.
     */
    @SubscribeEvent
    public void cancelVanillaHeal(LivingHealEvent event) {
        if (!BodyHealthConfig.ENABLED.get()) return; // mod désactivé → soins vanilla normaux
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // Mod désactivé → vanilla gère tout
        // On remet la vie vanilla à une valeur normale (20 HP = 10 cœurs)
        // au cas où elle aurait été figée au max par un tick précédent
        if (!BodyHealthConfig.ENABLED.get()) {
            if (player.getMaxHealth() != 20f) {
                // S'assurer que le max vanilla est bien à 20 (10 cœurs)
                // getAttribute() gère ça — on ne touche pas manuellement
            }
            // On ne fait RIEN d'autre — vanilla reprend entièrement le contrôle
            return;
        }

        if (player.isDeadOrDying()) return;

        UUID uuid = player.getUUID();
        int counter = tickCounters.merge(uuid, 1, Integer::sum);

        BodyHealthData data = BodyHealthAPI.getData(player);

        // Maintenir la vie vanilla au maximum pour éviter mort vanilla
        if (!player.isDeadOrDying() && player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        applyOngoingEffects(player, data);

        if (counter >= REGEN_INTERVAL) {
            tickCounters.put(uuid, 0);
            handlePassiveRegen(player, data);
        }
    }

    private void applyOngoingEffects(ServerPlayer player, BodyHealthData data) {
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
