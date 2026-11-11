package com.bodyhealth.events;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.network.BodyHealthSyncManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Gère les cœurs d'absorption.
 *
 * FIX MAJEUR — Double-absorption corrigée.
 *
 * AVANT : on forçait player.setAbsorptionAmount(totalAbsorb) à chaque tick
 * tant que la valeur vanilla était inférieure à notre total interne. Le
 * problème : vanilla consomme SA PROPRE copie de player.getAbsorptionAmount()
 * pour réduire les dégâts AVANT que notre DamageEventHandler ne s'exécute
 * (dans le pipeline natif Player.hurt()/actuallyHurt()). Notre handler
 * consommait ENSUITE data.getAbsorption(part) EN PLUS, sur le MÊME coup.
 * Résultat : double-absorption + re-remplissage automatique chaque tick =
 * bouclier quasi infini, le joueur ne prenait presque plus de dégâts tant
 * que l'effet ABSORPTION restait actif (~2 minutes).
 *
 * MAINTENANT : notre système (data.getAbsorption()) est la SEULE source de
 * vérité. player.getAbsorptionAmount() reste TOUJOURS à 0 — vanilla n'a
 * donc jamais rien à consommer de son côté. L'affichage HUD utilise déjà
 * exclusivement notre propre donnée (ClientBodyHealthData.getAbsorption()),
 * pas la valeur vanilla — donc la mettre à 0 n'affecte rien visuellement.
 *
 * L'effet ABSORPTION reste actif uniquement pour conserver son timer de
 * durée (~2 min) : quand il expire naturellement, effect == null déclenche
 * clearAbsorption() comme avant.
 *
 * Flux :
 *  1. Joueur mange pomme dorée → vanilla pose effet ABSORPTION + HP absorption
 *  2. onTick() détecte le nouvel amplifier → distributeAbsorption() vers data
 *  3. On force IMMÉDIATEMENT player.setAbsorptionAmount(0f) → vanilla n'a
 *     plus rien à consommer, seul notre système gère les dégâts absorbés
 *  4. DamageEventHandler réduit data.getAbsorption(part) sur chaque coup
 *  5. Quand l'effet ABSORPTION expire → clearAbsorption()
 */
public class AbsorptionHandler {

    private static final Random RANDOM = new Random();

    // Dernier amplifier de l'effet ABSORPTION vu — pour détecter l'ajout
    private final Map<UUID, Integer> lastAbsAmplifier = new HashMap<>();
    private final Map<UUID, Float>   lastAbsHp        = new HashMap<>();

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Mod désactivé → ne plus toucher à player.getAbsorptionAmount()
        // pour laisser les pommes dorées et le totem fonctionner en vanilla pur
        if (!com.bodyhealth.config.BodyHealthConfig.ENABLED.get()) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isDeadOrDying()) return;

        UUID uuid = player.getUUID();
        var effect = player.getEffect(net.minecraft.world.effect.MobEffects.ABSORPTION);

        if (effect == null) {
            if (lastAbsAmplifier.containsKey(uuid)) {
                clearAbsorption(player);
                lastAbsAmplifier.remove(uuid);
                lastAbsHp.remove(uuid);
            }
            return;
        }

        // FIX double-absorption — on garde TOUJOURS player.getAbsorptionAmount()
        // à 0 pour que le pipeline de dégâts vanilla n'ait jamais rien à
        // consommer de son côté. Notre data.getAbsorption() est la seule
        // source de vérité, gérée exclusivement par DamageEventHandler.
        if (player.getAbsorptionAmount() != 0f) {
            player.setAbsorptionAmount(0f);
        }

        int   amp    = effect.getAmplifier();
        // HP d'absorption = (amplifier + 1) * 4
        float absHp  = (amp + 1) * 4f;
        int   prevAmp = lastAbsAmplifier.getOrDefault(uuid, -1);

        // Nouvel effet ou amplifier supérieur → distribuer la différence
        if (amp != prevAmp) {
            float prevHp = lastAbsHp.getOrDefault(uuid, 0f);
            float added  = Math.max(0f, absHp - prevHp);
            if (added > 0f) distributeAbsorption(player, added);
            lastAbsAmplifier.put(uuid, amp);
            lastAbsHp.put(uuid, absHp);
        }
    }

    private void distributeAbsorption(ServerPlayer player, float amount) {
        BodyHealthData data = BodyHealthAPI.getData(player);

        // Priorité : partie la plus blessée
        BodyPart target = getMostInjured(data);
        if (target == null) target = randomPart();

        data.setAbsorption(target, data.getAbsorption(target) + amount);
        // Neutraliser immédiatement la copie vanilla — voir explication en tête de classe
        player.setAbsorptionAmount(0f);
        BodyHealthSyncManager.markDirtyAndFlush(player, data);
    }

    private void clearAbsorption(ServerPlayer player) {
        BodyHealthData data = BodyHealthAPI.getData(player);
        boolean changed = false;
        for (BodyPart p : BodyPart.values()) {
            if (data.getAbsorption(p) > 0f) {
                data.setAbsorption(p, 0f);
                changed = true;
            }
        }
        if (changed) BodyHealthSyncManager.markDirtyAndFlush(player, data);
    }

    private BodyPart getMostInjured(BodyHealthData data) {
        BodyPart worst = null;
        float worstPct = 1f;
        for (BodyPart p : BodyPart.values()) {
            float max = data.getMaxHealth(p);
            if (max <= 0) continue;
            float pct = data.getHealth(p) / max;
            if (pct < worstPct) { worstPct = pct; worst = p; }
        }
        return worstPct >= 1f ? null : worst;
    }

    private BodyPart randomPart() {
        BodyPart[] parts = BodyPart.values();
        return parts[RANDOM.nextInt(parts.length)];
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        lastAbsAmplifier.remove(uuid);
        lastAbsHp.remove(uuid);
    }
}
