package com.bodyhealth.events;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.network.BodyHealthSyncManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Gère les cœurs d'absorption.
 *
 * Architecture entièrement revue :
 *
 * On ne dépend PLUS de player.getAbsorptionAmount() car PlayerTickHandler
 * appelle setHealth() chaque tick ce qui perturbe la valeur d'absorption vanilla.
 *
 * On écoute à la place LivingIncomingDamageEvent AVANT que les dégâts
 * soient appliqués, et on détecte l'ajout d'absorption via un tag NBT posé
 * par les items (GoldenAppleItem, EnchantedGoldenApple) ou on intercepte
 * directement l'effet ABSORPTION via un tick comparatif sur getEffect().
 *
 * Flux :
 *  1. Joueur mange pomme dorée → vanilla pose effet ABSORPTION (4 HP)
 *  2. onTick() détecte que l'effet vient d'apparaître → distributeAbsorption()
 *  3. data.setAbsorption(part, 4f) → markDirtyAndFlush() → client affiche cœurs jaunes
 *  4. Quand la partie reçoit des dégâts → DamageEventHandler réduit absorption en premier
 *  5. Quand l'effet ABSORPTION disparaît → clearAbsorption()
 */
public class AbsorptionHandler {

    private static final Random RANDOM = new Random();

    // Dernier amplifier de l'effet ABSORPTION vu — pour détecter l'ajout
    private final Map<UUID, Integer> lastAbsAmplifier = new HashMap<>();
    private final Map<UUID, Float>   lastAbsHp        = new HashMap<>();

    @SubscribeEvent
    public void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isDeadOrDying()) return;

        UUID uuid = player.getUUID();
        var effect = player.getEffect(net.minecraft.world.effect.MobEffects.ABSORPTION);

        if (effect == null) {
            // BUG 2 FIX — Effacer l'absorption UNIQUEMENT quand l'effet vanilla
            // a réellement expiré (durée complète écoulée), pas avant.
            // Avant ce fix, clearAbsorption() était appelé dès que l'effet
            // disparaissait côté serveur — mais setAbsorptionAmount(0) appelé
            // dans le flux vanilla pouvait supprimer l'effet prématurément (~5s).
            // On vérifie que lastAbsAmplifier existe (= on avait bien distribué)
            // avant d'effacer, ce qui évite les faux positifs.
            if (lastAbsAmplifier.containsKey(uuid)) {
                clearAbsorption(player);
                lastAbsAmplifier.remove(uuid);
                lastAbsHp.remove(uuid);
            }
            return;
        }

        // Maintenir l'absorption vanilla à la valeur correcte pour éviter
        // que le jeu l'efface avant la fin de l'effet
        float totalAbsorb = 0f;
        com.bodyhealth.common.BodyHealthData bhData =
                com.bodyhealth.api.BodyHealthAPI.getData(player);
        for (com.bodyhealth.common.BodyPart p :
                com.bodyhealth.common.BodyPart.values())
            totalAbsorb += bhData.getAbsorption(p);
        // Forcer la valeur vanilla = notre total pour éviter expiration prématurée
        if (totalAbsorb > 0 && player.getAbsorptionAmount() < totalAbsorb)
            player.setAbsorptionAmount(totalAbsorb);

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
