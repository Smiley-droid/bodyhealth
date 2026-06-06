package com.bodyhealth.events;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.common.FractureState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applique les effets continus des fractures chaque tick.
 *
 * BUG CORRIGÉ : MobEffects.LEVITATION avec amplifier négatif (-3) est illégal
 * en 1.21.1 et provoque un crash. Remplacé par une combinaison légale
 * Slowness IV + Jump Boost 0 (annule les sauts sans crash).
 */
public class FractureEffectHandler {

    private final Map<UUID, Integer> tickMap    = new HashMap<>();
    private final Map<UUID, Map<String,Long>> msgCD = new HashMap<>();
    private static final long MSG_COOLDOWN = 4000L;

    private void msg(net.minecraft.server.level.ServerPlayer p, String key, String text) {
        long now = System.currentTimeMillis();
        msgCD.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        if (now - msgCD.get(p.getUUID()).getOrDefault(key,0L) < MSG_COOLDOWN) return;
        msgCD.get(p.getUUID()).put(key, now);
        p.displayClientMessage(net.minecraft.network.chat.Component.literal(text), true);
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isDeadOrDying()) return;

        UUID uuid = player.getUUID();
        int  tick = tickMap.merge(uuid, 1, Integer::sum);

        BodyHealthData data = BodyHealthAPI.getData(player);
        boolean anyFracture = false;

        for (BodyPart part : BodyPart.values()) {
            FractureState state = data.getFracture(part);
            if (state == FractureState.NONE) continue;
            anyFracture = true;
            applyFractureEffects(player, part, state, tick, data);
        }

        if (anyFracture && tick % 600 == 0) {
            notifyFractures(player, data);
        }
    }

    private void applyFractureEffects(ServerPlayer player, BodyPart part,
                                       FractureState state, int tick,
                                       BodyHealthData data) {
        switch (part) {
            case HEAD      -> applyHeadFracture(player, state, tick);
            case TORSO     -> applyTorsoFracture(player, state);
            case ARM_RIGHT -> applyArmRightFracture(player, state);
            case ARM_LEFT  -> applyArmLeftFracture(player, state, tick);
            case LEG_LEFT, LEG_RIGHT -> applyLegFracture(player, state, part, data);
        }
    }

    private void applyHeadFracture(ServerPlayer p, FractureState s, int tick) {
        switch (s) {
            case SPRAINED -> {
                if (tick % 200 == 0) {
                    p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false, false));
                    msg(p, "head_sp", "§e⚠ Tête foulée — vision légèrement trouble.");
                }
            }
            case BROKEN -> {
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 25, 1, false, false, false));
                if (tick % 100 == 0) {
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, false));
                    msg(p, "head_br", "§c⚠ Tête fracturée — cécité intermittente !");
                }
            }
            case SHATTERED -> {
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 25, 1, false, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 25, 0, false, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 1, false, false, false));
            }
            default -> {}
        }
    }

    private void applyTorsoFracture(ServerPlayer p, FractureState s) {
        switch (s) {
            case SPRAINED ->
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 0, false, false, false));
            case BROKEN -> {
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 1, false, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 0, false, false, false));
            }
            case SHATTERED -> {
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 2, false, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, false, false, false));
            }
            default -> {}
        }
    }

    private void applyArmRightFracture(ServerPlayer p, FractureState s) {
        switch (s) {
            case SPRAINED ->
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 25, 0, false, false, false));
            case BROKEN, SHATTERED ->
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 25, 2, false, false, false));
            default -> {}
        }
    }

    private void applyArmLeftFracture(ServerPlayer p, FractureState s, int tick) {
        if (s == FractureState.SHATTERED) {
            var off = p.getOffhandItem();
            if (!off.isEmpty()) {
                p.drop(off.copy(), false);
                p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,
                        net.minecraft.world.item.ItemStack.EMPTY);
            }
        } else if (s == FractureState.BROKEN && tick % 200 == 0) {
            var off = p.getOffhandItem();
            if (!off.isEmpty()) {
                p.drop(off.copy(), false);
                p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,
                        net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }

    private void applyLegFracture(ServerPlayer p, FractureState s,
                                   BodyPart part, BodyHealthData data) {
        FractureState other = data.getFracture(
                part == BodyPart.LEG_LEFT ? BodyPart.LEG_RIGHT : BodyPart.LEG_LEFT);
        FractureState worst = s.getLevel() >= other.getLevel() ? s : other;

        switch (worst) {
            case SPRAINED ->
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 0, false, false, false));
            case BROKEN -> {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, false, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 25, 0, false, false, false));
            }
            case SHATTERED -> {
                msg(p, "leg_sh", "§4☠ Jambe broyée — vous ne pouvez presque plus bouger !");
                // BUG FIX : LEVITATION avec amplifier négatif (-3) crash en 1.21.1.
                // Remplacement : Slowness IV + Jump Boost 0 appliqués ensemble.
                // Jump Boost 0 = saut vanilla (hauteur neutre), combiné à Slowness IV
                // → le joueur peut à peine avancer, les sauts sont très limités.
                // C'est le comportement le plus proche sans utiliser d'amplifier illégal.
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 3, false, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 25, 0, false, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.JUMP, 25, 0, false, false, false));
            }
            default -> {}
        }
    }

    private void notifyFractures(ServerPlayer player, BodyHealthData data) {
        StringBuilder msg = new StringBuilder("§c⚠ Fractures : ");
        boolean first = true;
        for (BodyPart part : BodyPart.values()) {
            FractureState s = data.getFracture(part);
            if (s == FractureState.NONE) continue;
            if (!first) msg.append(", ");
            msg.append("§f").append(getPartName(part))
               .append(" §c(").append(s.getDisplayName()).append(")");
            first = false;
        }
        player.displayClientMessage(Component.literal(msg.toString()), true);
    }

    private String getPartName(BodyPart part) {
        return switch (part) {
            case HEAD      -> "Tête";
            case TORSO     -> "Torse";
            case ARM_RIGHT -> "Bras D";
            case ARM_LEFT  -> "Bras G";
            case LEG_RIGHT -> "Jambe D";
            case LEG_LEFT  -> "Jambe G";
        };
    }
}
