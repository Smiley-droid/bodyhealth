package com.bodyhealth.events;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.item.MorphineSyringeItem;
import com.bodyhealth.network.SyncBodyHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BUG 3 FIX : compteurs de ticks par UUID au lieu de variables d'instance partagées.
 */
public class PotionHealHandler {

    // BUG 3 FIX : Map par joueur
    private final Map<UUID, Integer> regenITicks  = new HashMap<>();
    private final Map<UUID, Integer> regenIITicks = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isDeadOrDying()) return;

        // Si morphine active → potion de regen bloquée aussi
        long blockedUntil = player.getPersistentData()
                .getLong(MorphineSyringeItem.NBT_MORPHINE_ACTIVE);
        if (System.currentTimeMillis() < blockedUntil) return;

        boolean hasRegen = player.hasEffect(MobEffects.REGENERATION);
        if (!hasRegen) {
            regenITicks.remove(player.getUUID());
            regenIITicks.remove(player.getUUID());
            return;
        }

        boolean isLevelII = player.getEffect(MobEffects.REGENERATION).getAmplifier() >= 1;
        UUID uuid = player.getUUID();
        BodyHealthData data = BodyHealthAPI.getData(player);
        boolean changed = false;

        if (isLevelII) {
            int t = regenIITicks.merge(uuid, 1, Integer::sum);
            if (t >= 25) {
                regenIITicks.put(uuid, 0);
                changed = applyRegenTick(data, 1.0f);
            }
        } else {
            int t = regenITicks.merge(uuid, 1, Integer::sum);
            if (t >= 50) {
                regenITicks.put(uuid, 0);
                changed = applyRegenTick(data, 0.5f);
            }
        }

        if (changed) {
            PacketDistributor.sendToPlayer(player,
                    new SyncBodyHealthPacket(data.serializeNBT()));
        }
    }

    private boolean applyRegenTick(BodyHealthData data, float amount) {
        BodyPart target = getMostInjured(data);
        if (target == null) return false;
        data.heal(target, amount);
        return true;
    }

    private BodyPart getMostInjured(BodyHealthData data) {
        BodyPart worst = null;
        float worstPct = 1.0f;
        for (BodyPart part : BodyPart.values()) {
            float pct = data.getHealthPercent(part);
            if (pct < worstPct) { worstPct = pct; worst = part; }
        }
        return worstPct >= 1.0f ? null : worst;
    }
}
