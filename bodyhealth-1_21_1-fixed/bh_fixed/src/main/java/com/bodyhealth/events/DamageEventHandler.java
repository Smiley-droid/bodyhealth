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
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        pendingAttacker.put(player.getUUID(), event.getSource().getDirectEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isDeadOrDying()) return;

        UUID uuid = player.getUUID();
        if (killingInProgress.contains(uuid)) return;

        // Dégâts bruts AVANT réduction vanilla (stockés dans Pre, mais on est en Post)
        // On repart du montant Post et on applique nos réductions manuellement
        float damage = event.getNewDamage();
        if (damage <= 0) { pendingAttacker.remove(uuid); return; }

        Entity   attacker = pendingAttacker.remove(uuid);
        BodyPart hitPart  = HitboxResolver.resolve(player, event.getSource(), attacker);

        // Facteur de zone (tête plus vulnérable, bras moins exposés)
        float adjusted = applyPartBonus(hitPart, damage);

        // ── Enchantements — appliqués manuellement car PlayerTickHandler ─────
        // remet la vie vanilla au max à chaque tick, donc les enchantements
        // qui réduisent les dégâts vanilla n'ont aucun effet réel sur le mod.
        // On relit les niveaux d'enchantement et on applique la réduction ici.
        adjusted = applyEnchantmentReduction(player, event.getSource(), hitPart, adjusted);

        BodyHealthData data = BodyHealthAPI.getData(player);

        // Absorption — réduite avant les HP comme vanilla
        float absorp = data.getAbsorption(hitPart);
        if (absorp > 0) {
            float absorbed = Math.min(absorp, adjusted);
            data.setAbsorption(hitPart, absorp - absorbed);
            adjusted -= absorbed;
            // Sync absorption vanilla pour que la barre vanilla reste cohérente
            float totalAbsorb = 0f;
            for (com.bodyhealth.common.BodyPart p : com.bodyhealth.common.BodyPart.values())
                totalAbsorb += data.getAbsorption(p);
            player.setAbsorptionAmount(totalAbsorb);
        }
        if (adjusted <= 0) {
            BodyHealthSyncManager.markDirtyAndFlush(player, data);
            return;
        }

        data.damage(hitPart, adjusted);

        FractureState fracAfter = data.getFracture(hitPart);
        SoundHelper.playHurtSound(player, hitPart, fracAfter);
        applyStatusEffects(player, hitPart, data);

        if (data.shouldDie() && !player.isDeadOrDying()) {
            // FIX double mort — player.hurt(MAX_VALUE) redéclenchait LivingDamageEvent
            // et PlayerTickHandler remettait la vie au max entre les deux appels.
            // player.kill() tue directement sans passer par le système de dégâts,
            // donc ni LivingDamageEvent ni la boucle de regen ne s'en mêlent.
            killingInProgress.add(uuid);
            try {
                player.setHealth(0f);
                player.kill();
            } finally {
                killingInProgress.remove(uuid);
            }
        }

        BodyHealthSyncManager.markDirtyAndFlush(player, data);
    }

    // ── Enchantements ─────────────────────────────────────────────────────────

    /**
     * Applique manuellement les réductions d'enchantements au dégât final.
     *
     * Pourquoi : PlayerTickHandler remet la vie vanilla au max chaque tick.
     * Les enchantements de protection vanilla réduisent les dégâts appliqués
     * à la vie vanilla, mais comme celle-ci est toujours pleine, leur effet
     * est nul sur notre système. On doit donc les relire et les appliquer ici.
     *
     * Enchantements gérés :
     *   - Protection         → réduit tous les dégâts
     *   - Protection contre le feu → réduit dégâts feu uniquement
     *   - Protection contre les projectiles → réduit dégâts projectiles
     *   - Protection contre les explosions  → réduit dégâts explosion
     *   - Chute amortie (Feather Falling)   → réduit dégâts chute, uniquement sur les jambes
     */
    private float applyEnchantmentReduction(ServerPlayer player, DamageSource source,
                                             BodyPart hitPart, float damage) {
        var level = player.level();

        // ── Protection (réduit tout) ──────────────────────────────────────────
        int protLvl = getEnchantmentSum(player, Enchantments.PROTECTION);
        // Formule vanilla : chaque niveau = 4% de réduction, max 64% (cap vanilla)
        // On applique la même formule sur nos dégâts
        if (protLvl > 0) {
            float reduction = Math.min(protLvl * 0.04f, 0.64f);
            damage *= (1f - reduction);
        }

        // ── Protection contre le feu ──────────────────────────────────────────
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

        // ── Protection contre les projectiles ─────────────────────────────────
        boolean isProjectile = source.getDirectEntity() instanceof
                net.minecraft.world.entity.projectile.Projectile;
        if (isProjectile) {
            int projLvl = getEnchantmentSum(player, Enchantments.PROJECTILE_PROTECTION);
            if (projLvl > 0) {
                float reduction = Math.min(projLvl * 0.08f, 0.64f);
                damage *= (1f - reduction);
            }
        }

        // ── Protection contre les explosions ─────────────────────────────────
        boolean isExplosion = source.is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION)
                           || source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_EXPLOSION);
        if (isExplosion) {
            int blastLvl = getEnchantmentSum(player, Enchantments.BLAST_PROTECTION);
            if (blastLvl > 0) {
                float reduction = Math.min(blastLvl * 0.08f, 0.64f);
                damage *= (1f - reduction);
            }
        }

        // ── Chute amortie — uniquement sur les jambes ─────────────────────────
        boolean isFall = msgId.equals("fall");
        if (isFall && (hitPart == BodyPart.LEG_LEFT || hitPart == BodyPart.LEG_RIGHT)) {
            // Slow Falling (effet) — annule TOTALEMENT les dégâts de chute
            // La vie vanilla est remise au max chaque tick donc l'effet vanilla est ignoré.
            // On doit le vérifier manuellement ici.
            if (player.hasEffect(MobEffects.SLOW_FALLING)) {
                return 0f;
            }

            // Feather Falling : bottes uniquement, 12% par niveau
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            int ffLvl = EnchantmentHelper.getItemEnchantmentLevel(
                    level.registryAccess()
                         .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                         .getHolderOrThrow(Enchantments.FEATHER_FALLING),
                    boots);
            if (ffLvl > 0) {
                float reduction = Math.min(ffLvl * 0.12f, 0.48f);
                damage *= (1f - reduction);
                if (damage <= 0.2f) damage = 0f;
            }
        }

        // ── Résistance (potion) — s'applique en DERNIER, après tout le reste ──
        // Formule vanilla : 20% par niveau, niveau 5 = immunité totale.
        // On doit l'appliquer manuellement car la vie vanilla est remise au max chaque tick.
        if (player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
            int resLvl = player.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1;
            if (resLvl >= 5) return 0f;
            float reduction = Math.min(resLvl * 0.20f, 0.80f);
            damage *= (1f - reduction);
        }

        return Math.max(0f, damage);
    }

    /**
     * Somme les niveaux d'un enchantement sur toute l'armure du joueur.
     * Protection = casque + plastron + jambières + bottes additionnés.
     */
    private int getEnchantmentSum(ServerPlayer player, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchKey) {
        var registry = player.level().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var holder = registry.getHolder(enchKey).orElse(null);
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

    // ── Effets de statut ──────────────────────────────────────────────────────

    private final Map<UUID, Map<String,Long>> msgCooldowns = new HashMap<>();
    private static final long MSG_CD = 3000L;

    private void sendMsg(ServerPlayer player, String key, String msg) {
        long now = System.currentTimeMillis();
        msgCooldowns.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        if (now - msgCooldowns.get(player.getUUID()).getOrDefault(key, 0L) < MSG_CD) return;
        msgCooldowns.get(player.getUUID()).put(key, now);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(msg), true);
    }

    private void applyStatusEffects(ServerPlayer player, BodyPart hitPart, BodyHealthData data) {
        switch (hitPart) {
            case HEAD -> {
                if (BodyHealthConfig.ENABLE_NAUSEA.get()) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, false));
                    sendMsg(player, "head", "§6⚡ Choc à la tête — vision perturbée !");
                }
                if (data.isCritical(BodyPart.HEAD))
                    sendMsg(player, "head_crit", "§c☠ Tête critique — soignez-vous immédiatement !");
            }
            case LEG_LEFT, LEG_RIGHT -> {
                if (BodyHealthConfig.ENABLE_SLOWNESS.get()) {
                    int lvl = data.isCritical(hitPart) ? 2 : 0;
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, lvl, false, false, false));
                    String leg = hitPart == BodyPart.LEG_LEFT ? "jambe gauche" : "jambe droite";
                    if (lvl >= 2)
                        sendMsg(player, "leg_crit", "§c☠ " + Character.toUpperCase(leg.charAt(0)) + leg.substring(1) + " critique — vous pouvez à peine marcher !");
                    else
                        sendMsg(player, "leg", "§e⚠ " + Character.toUpperCase(leg.charAt(0)) + leg.substring(1) + " blessée — ralentissement.");
                }
                if (data.isDead(hitPart)) {
                    String leg = hitPart == BodyPart.LEG_LEFT ? "Jambe gauche" : "Jambe droite";
                    sendMsg(player, "leg_dead", "§c☠ " + leg + " détruite — impossible de courir !");
                }
            }
            case ARM_RIGHT -> {
                if (data.isCritical(BodyPart.ARM_RIGHT))
                    sendMsg(player, "arm_r_crit", "§e⚠ Bras droit critique — les interactions sont difficiles !");
                if (data.isDead(BodyPart.ARM_RIGHT))
                    sendMsg(player, "arm_r_dead", "§c☠ Bras droit détruit — vous ne pouvez plus agir !");
            }
            case ARM_LEFT -> {
                if (BodyHealthConfig.ENABLE_OFFHAND_DROP.get() && data.isDead(BodyPart.ARM_LEFT)) {
                    dropOffhandItem(player);
                    sendMsg(player, "arm_l_dead", "§c☠ Bras gauche détruit — vous lâchez votre objet secondaire !");
                } else if (data.isCritical(BodyPart.ARM_LEFT)) {
                    sendMsg(player, "arm_l_crit", "§e⚠ Bras gauche critique !");
                }
            }
            case TORSO -> {
                if (data.isCritical(BodyPart.TORSO))
                    sendMsg(player, "torso_crit", "§c☠ Torse critique — danger de mort imminent !");
                if (data.isDead(BodyPart.TORSO))
                    sendMsg(player, "torso_dead", "§4✟ Torse détruit...");
            }
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
        UUID uuid = event.getEntity().getUUID();
        pendingAttacker.remove(uuid);
        killingInProgress.remove(uuid);
        msgCooldowns.remove(uuid);
        BodyHealthSyncManager.cleanup(uuid);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        pendingAttacker.remove(uuid);
        killingInProgress.remove(uuid);
        BodyHealthData data = BodyHealthAPI.getData(player);
        for (BodyPart part : BodyPart.values()) {
            data.setHealth(part, data.getMaxHealth(part));
            data.setFracture(part, FractureState.NONE);
        }
        BodyHealthSyncManager.markDirtyAndFlush(player, data);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        com.bodyhealth.common.BodyHealthData data = BodyHealthAPI.getData(player);

        // FIX config startingHearts — appliquer la valeur de config si c'est
        // la première connexion du joueur (toutes les parties ont encore 6f = défaut).
        // On détecte le "premier login" si maxHealth == 6f (valeur hardcodée de BodyPart)
        // ET que la config demande une valeur différente.
        float configHp = com.bodyhealth.config.BodyHealthConfig.STARTING_HEARTS.get() * 2f;
        boolean isFirstJoin = true;
        for (com.bodyhealth.common.BodyPart p : com.bodyhealth.common.BodyPart.values()) {
            // Si une partie a un maxHealth différent de 6f, le joueur a déjà joué
            if (Math.abs(data.getMaxHealth(p) - 6f) > 0.01f) {
                isFirstJoin = false;
                break;
            }
        }

        if (isFirstJoin && Math.abs(configHp - 6f) > 0.01f) {
            // Premier login ET config différente de 6f → appliquer la config
            for (com.bodyhealth.common.BodyPart p : com.bodyhealth.common.BodyPart.values()) {
                data.setMaxHealth(p, configHp);
                data.setHealth(p, configHp);
            }
        }

        BodyHealthSyncManager.markDirtyAndFlush(player, data);
    }
}
