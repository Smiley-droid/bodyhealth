package com.bodyhealth.events;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.*;
import com.bodyhealth.config.BodyHealthConfig;
import com.bodyhealth.network.BodyHealthSyncManager;
import com.bodyhealth.sound.SoundHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DamageEventHandler {

    private final Map<UUID, Entity> pendingAttacker   = new HashMap<>();
    private final Set<UUID>         killingInProgress = new HashSet<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!BodyHealthConfig.ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        pendingAttacker.put(player.getUUID(), event.getSource().getDirectEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerDamage(LivingDamageEvent.Post event) {
        if (!BodyHealthConfig.ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isDeadOrDying()) return;

        UUID uuid = player.getUUID();
        if (killingInProgress.contains(uuid)) return;

        float damage = event.getNewDamage();
        if (damage <= 0) { pendingAttacker.remove(uuid); return; }

        Entity   attacker = pendingAttacker.remove(uuid);
        BodyPart hitPart  = HitboxResolver.resolve(player, event.getSource(), attacker);

        float adjusted = applyPartBonus(hitPart, damage);
        adjusted = applyEnchantmentReduction(player, event.getSource(), hitPart, adjusted);

        BodyHealthData data = BodyHealthAPI.getData(player);

        // Absorption — réduite avant les HP comme vanilla.
        // FIX double-absorption — on NE remet PLUS player.setAbsorptionAmount()
        // à une valeur positive ici. Voir AbsorptionHandler pour l'explication
        // complète : notre data.getAbsorption() est la seule source de vérité,
        // player.getAbsorptionAmount() doit TOUJOURS rester à 0 pour empêcher
        // vanilla de consommer sa propre copie sur le coup suivant.
        float absorp = data.getAbsorption(hitPart);
        if (absorp > 0) {
            float absorbed = Math.min(absorp, adjusted);
            data.setAbsorption(hitPart, absorp - absorbed);
            adjusted -= absorbed;
            if (player.getAbsorptionAmount() != 0f) player.setAbsorptionAmount(0f);
        }
        if (adjusted <= 0) {
            BodyHealthSyncManager.markDirtyAndFlush(player, data);
            return;
        }

        data.damage(hitPart, adjusted);

        SoundHelper.playHurtSound(player, hitPart);
        applyCriticalPartsEffects(player, data);
        sendDamageMessage(player, hitPart, data);

        if (data.shouldDie() && !player.isDeadOrDying()) {
            killingInProgress.add(uuid);
            try {
                player.setHealth(0f);
                player.kill((net.minecraft.server.level.ServerLevel) player.level());
            } finally {
                killingInProgress.remove(uuid);
            }
        }

        BodyHealthSyncManager.markDirtyAndFlush(player, data);
    }

    // ── Enchantements ─────────────────────────────────────────────────────────

    private float applyEnchantmentReduction(ServerPlayer player, DamageSource source,
                                             BodyPart hitPart, float damage) {
        var level = player.level();

        int protLvl = getEnchantmentSum(player, Enchantments.PROTECTION);
        if (protLvl > 0) {
            float reduction = Math.min(protLvl * 0.04f, 0.64f);
            damage *= (1f - reduction);
        }

        String msgId = source.type().msgId();
        boolean isFire = msgId.equals("inFire") || msgId.equals("onFire")
                      || msgId.equals("lava")   || msgId.equals("hotFloor");
        if (isFire) {
            int fireLvl = getEnchantmentSum(player, Enchantments.FIRE_PROTECTION);
            if (fireLvl > 0) {
                float reduction = Math.min(fireLvl * 0.08f, 0.64f);
                damage *= (1f - reduction);
            }
        }

        boolean isProjectile = source.getDirectEntity() instanceof
                net.minecraft.world.entity.projectile.Projectile;
        if (isProjectile) {
            int projLvl = getEnchantmentSum(player, Enchantments.PROJECTILE_PROTECTION);
            if (projLvl > 0) {
                float reduction = Math.min(projLvl * 0.08f, 0.64f);
                damage *= (1f - reduction);
            }
        }

        boolean isExplosion = source.is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION)
                           || source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_EXPLOSION);
        if (isExplosion) {
            int blastLvl = getEnchantmentSum(player, Enchantments.BLAST_PROTECTION);
            if (blastLvl > 0) {
                float reduction = Math.min(blastLvl * 0.08f, 0.64f);
                damage *= (1f - reduction);
            }
        }

        boolean isFall = msgId.equals("fall");
        if (isFall && (hitPart == BodyPart.LEG_LEFT || hitPart == BodyPart.LEG_RIGHT)) {
            if (player.hasEffect(MobEffects.SLOW_FALLING)) {
                return 0f;
            }
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            int ffLvl = EnchantmentHelper.getItemEnchantmentLevel(
                    level.registryAccess()
                         .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                         .getOrThrow(Enchantments.FEATHER_FALLING),
                    boots);
            if (ffLvl > 0) {
                float reduction = Math.min(ffLvl * 0.12f, 0.48f);
                damage *= (1f - reduction);
                if (damage <= 0.2f) damage = 0f;
            }
        }

        if (player.hasEffect(MobEffects.RESISTANCE)) {
            int resLvl = player.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1;
            if (resLvl >= 5) return 0f;
            float reduction = Math.min(resLvl * 0.20f, 0.80f);
            damage *= (1f - reduction);
        }

        return Math.max(0f, damage);
    }

    private int getEnchantmentSum(ServerPlayer player, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchKey) {
        var registry = player.level().registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var holder = registry.get(enchKey).orElse(null);
        if (holder == null) return 0;

        int total = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            total += EnchantmentHelper.getItemEnchantmentLevel(holder, player.getItemBySlot(slot));
        }
        return total;
    }

    // ── Facteur de zone ───────────────────────────────────────────────────────

    private float applyPartBonus(BodyPart part, float damage) {
        return damage * switch (part) {
            case HEAD      -> 1.5f;
            case TORSO     -> 1.0f;
            case ARM_RIGHT,
                 ARM_LEFT  -> 0.85f;
            case LEG_RIGHT,
                 LEG_LEFT  -> 0.90f;
        };
    }

    // ── Effets de statut façon SrGnis Body-Health-System ───────────────────────
    //
    // Système d'amplifier basé sur le NOMBRE de parties critiques dans un groupe,
    // adapté au modèle 6-parties de ce mod (SrGnis a 8 parties avec pieds séparés).
    //
    //   Faiblesse (Weakness)     ← HEAD + TORSO   critiques (amplifier -1 + somme, 0..1)
    //   Fatigue minière (Mining) ← ARM_LEFT + ARM_RIGHT détruits (0..1)
    //   Lenteur (Slowness)       ← LEG_LEFT + LEG_RIGHT détruites (0..1)
    //
    // L'effet n'est appliqué que si amplifier >= 0 (donc au moins 1 partie touchée).
    // Durée courte (40 ticks = 2s) et réappliqué chaque tick de dégât tant que
    // la condition persiste — exactement le pattern SrGnis applyStatusEffectWithAmplifier.

    private void applyCriticalPartsEffects(ServerPlayer player, BodyHealthData data) {
        // HEAD + TORSO → Faiblesse
        int headTorsoAmp = -1
                + criticalAmplifier(data, BodyPart.HEAD)
                + criticalAmplifier(data, BodyPart.TORSO);
        applyEffectWithAmplifier(player, MobEffects.WEAKNESS, headTorsoAmp);

        // ARM_LEFT + ARM_RIGHT → Fatigue minière (DIG_SLOWDOWN)
        if (BodyHealthConfig.ENABLE_OFFHAND_DROP.get()) {
            int armAmp = -1
                    + destroyedAmplifier(data, BodyPart.ARM_LEFT)
                    + destroyedAmplifier(data, BodyPart.ARM_RIGHT);
            applyEffectWithAmplifier(player, MobEffects.MINING_FATIGUE, armAmp);
        }

        // LEG_LEFT + LEG_RIGHT → Lenteur
        if (BodyHealthConfig.ENABLE_SLOWNESS.get()) {
            int legAmp = -1
                    + destroyedAmplifier(data, BodyPart.LEG_LEFT)
                    + destroyedAmplifier(data, BodyPart.LEG_RIGHT);
            applyEffectWithAmplifier(player, MobEffects.SLOWNESS, legAmp);
        }

        // Nausée tête — appliquée séparément (pas dans le système amplifier SrGnis,
        // gardé tel quel car déjà présent et apprécié)
        if (BodyHealthConfig.ENABLE_NAUSEA.get() && data.isCritical(BodyPart.HEAD)) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0, false, false, false));
        }

        // Bras droit détruit → empêche casser/poser (géré dans BlockInteractionHandler,
        // ce handler ne fait que vérifier isDead pour cohérence)
    }

    /** 1 si la partie est critique (HEAD/TORSO sous maxHealth/3), sinon 0. */
    private int criticalAmplifier(BodyHealthData data, BodyPart part) {
        return data.isCritical(part) ? 1 : 0;
    }

    /** 1 si la partie est totalement détruite (0 HP), sinon 0. */
    private int destroyedAmplifier(BodyHealthData data, BodyPart part) {
        return data.isDead(part) ? 1 : 0;
    }

    /**
     * Applique un effet avec un amplifier donné, façon SrGnis
     * Body.applyStatusEffectWithAmplifier() : ignore si amplifier < 0,
     * réapplique seulement si la durée devient trop courte ou si
     * l'amplifier a changé (évite le spam de paquets réseau).
     */
    private void applyEffectWithAmplifier(ServerPlayer player,
                                           net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                           int amplifier) {
        if (amplifier < 0) return;
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() <= 5 || current.getAmplifier() != amplifier) {
            player.addEffect(new MobEffectInstance(effect, 40, amplifier, false, false, false));
        }
    }

    // ── Messages action bar (limités, avec cooldown) ────────────────────────────

    private final Map<UUID, Map<String,Long>> msgCooldowns = new HashMap<>();
    private static final long MSG_CD = 3000L;

    private void sendMsg(ServerPlayer player, String key) {
        if (!BodyHealthConfig.ACTION_BAR_MESSAGES.get()) return; // option serveur "actionBarMessages"
        long now = System.currentTimeMillis();
        msgCooldowns.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        if (now - msgCooldowns.get(player.getUUID()).getOrDefault(key, 0L) < MSG_CD) return;
        msgCooldowns.get(player.getUUID()).put(key, now);
        // Clé de traduction : le client affiche le message dans SA langue
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("bodyhealth.msg." + key), true);
    }

    private void sendDamageMessage(ServerPlayer player, BodyPart hitPart, BodyHealthData data) {
        boolean crit = data.isCritical(hitPart);
        boolean dead = data.isDead(hitPart);
        if (!crit && !dead) return; // ne notifie que si critique ou détruit

        switch (hitPart) {
            case HEAD -> {
                if (dead)       sendMsg(player, "head_dead");
                else if (crit)  sendMsg(player, "head_crit");
            }
            case TORSO -> {
                if (dead)       sendMsg(player, "torso_dead");
                else if (crit)  sendMsg(player, "torso_crit");
            }
            case ARM_RIGHT -> {
                if (dead) sendMsg(player, "arm_r_dead");
            }
            case ARM_LEFT -> {
                if (dead) {
                    if (BodyHealthConfig.ENABLE_OFFHAND_DROP.get()) dropOffhandItem(player);
                    sendMsg(player, "arm_l_dead");
                }
            }
            case LEG_LEFT  -> { if (dead) sendMsg(player, "leg_l_dead"); }
            case LEG_RIGHT -> { if (dead) sendMsg(player, "leg_r_dead"); }
        }
    }

    private void dropOffhandItem(ServerPlayer player) {
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty()) {
            player.drop(off.copy(), false);
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    // ── Nettoyage ─────────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // JSON à jour à la déconnexion (la régénération passive ne l'écrit pas)
        if (event.getEntity() instanceof ServerPlayer sp && BodyHealthConfig.ENABLED.get())
            com.bodyhealth.common.BodyHealthDiskManager.saveJson(sp, BodyHealthAPI.getData(sp));
        UUID uuid = event.getEntity().getUUID();
        pendingAttacker.remove(uuid);
        killingInProgress.remove(uuid);
        msgCooldowns.remove(uuid);
        BodyHealthSyncManager.cleanup(uuid);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!BodyHealthConfig.ENABLED.get()) return; // mod désactivé → respawn vanilla normal
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        pendingAttacker.remove(uuid);
        killingInProgress.remove(uuid);
        BodyHealthData data = BodyHealthAPI.getData(player);
        for (BodyPart part : BodyPart.values()) {
            data.setHealth(part, data.getMaxHealth(part));
        }
        BodyHealthSyncManager.markDirtyAndFlush(player, data);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BodyHealthData data = BodyHealthAPI.getData(player);

        // ── Restauration vie vanilla quand le mod est désactivé ───────────────
        if (!BodyHealthConfig.ENABLED.get()) {
            // Remettre la vie vanilla à 20 HP (10 cœurs) via l'attribut MAX_HEALTH
            var maxHealthAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) maxHealthAttr.setBaseValue(20.0);
            if (player.getHealth() > 20f || player.getHealth() <= 0f)
                player.setHealth(20f);
            BodyHealthSyncManager.markDirtyAndFlush(player, data);
            return;
        }

        // ── Modification faite pendant que le joueur était hors ligne ─────────
        com.bodyhealth.common.BodyHealthDiskManager.applyPending(player, data);

        // ── Fallback JSON si les données NBT sont vides (migration/corruption) ─
        boolean isDefault = true;
        for (BodyPart p : BodyPart.values()) {
            if (Math.abs(data.getMaxHealth(p) - p.getDefaultMaxHealth()) > 0.01f) {
                isDefault = false;
                break;
            }
        }
        if (isDefault) {
            // Tenter de charger depuis le JSON si le NBT semble vierge
            com.bodyhealth.common.BodyHealthDiskManager.loadJsonFallback(player, data);
        }

        // ── Premier join : appliquer la config STARTING_HEARTS ───────────────
        float configHp    = BodyHealthConfig.STARTING_HEARTS.get() * 2f;
        boolean isFirstJoin = true;
        for (BodyPart p : BodyPart.values()) {
            if (Math.abs(data.getMaxHealth(p) - 6f) > 0.01f) {
                isFirstJoin = false;
                break;
            }
        }
        if (isFirstJoin && Math.abs(configHp - 6f) > 0.01f) {
            for (BodyPart p : BodyPart.values()) {
                data.setMaxHealth(p, configHp);
                data.setHealth(p, configHp);
            }
        }

        // Clamp santé si le max a changé hors ligne
        for (BodyPart p : BodyPart.values()) {
            if (data.getHealth(p) > data.getMaxHealth(p))
                data.setHealth(p, data.getMaxHealth(p));
        }

        BodyHealthSyncManager.markDirtyAndFlush(player, data);
    }
}
