package com.bodyhealth.item;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyPart;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 💉 Seringue de Morphine
 *
 * Effet : supprime TOUS les effets négatifs liés aux blessures
 *         pendant 30 secondes, mais bloque toute régénération
 *         pendant cette durée (le corps est anesthésié).
 *
 * Effets supprimés :
 *   - Nausée (tête blessée)
 *   - Ralentissement (jambes blessées)
 *   - Faiblesse (bras droit critique)
 *
 * Effets appliqués :
 *   - Résistance II (30 sec) — atténue la douleur
 *   - Regen bloquée pendant 30 sec via tag NBT
 *
 * Effets secondaires si utilisé trop souvent :
 *   - 3 utilisations en moins de 2 min → Nausée forcée 10 sec
 *     (overdose)
 *
 * Usage    : clic droit instantané (1 sec d'animation)
 * Stackable: 8
 */
public class MorphineSyringeItem extends Item {

    private static final int USE_DURATION       = 20;   // 1 seconde
    private static final int EFFECT_DURATION    = 600;  // 30 secondes (en ticks)
    private static final int OVERDOSE_THRESHOLD = 3;    // utilisations avant overdose
    private static final long OVERDOSE_WINDOW   = 120_000L; // 2 minutes en ms

    // Clé NBT pour tracker les utilisations (overdose)
    private static final String NBT_USE_COUNT   = "morphine_uses";
    private static final String NBT_FIRST_USE   = "morphine_first_use";
    // Clé NBT pour bloquer la regen dans PlayerTickHandler
    public  static final String NBT_MORPHINE_ACTIVE = "morphine_active_until";

    public MorphineSyringeItem(Item.Properties props) {
        super(props.stacksTo(8));
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return UseAnimations.HEAL;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;

        long now = System.currentTimeMillis();

        // ── Vérification overdose ─────────────────────────────────────────
        var persistentData = player.getPersistentData();
        int  useCount  = persistentData.getIntOr(NBT_USE_COUNT, 0);
        long firstUse  = persistentData.getLongOr(NBT_FIRST_USE, 0L);

        // Reset fenêtre si plus de 2 minutes depuis la première injection
        if (now - firstUse > OVERDOSE_WINDOW) {
            useCount = 0;
            persistentData.putLong(NBT_FIRST_USE, now);
        }

        useCount++;
        persistentData.putInt(NBT_USE_COUNT, useCount);

        if (useCount == 1) {
            persistentData.putLong(NBT_FIRST_USE, now);
        }

        // ── Overdose ──────────────────────────────────────────────────────
        if (useCount >= OVERDOSE_THRESHOLD) {
            player.sendOverlayMessage(Component.translatable("bodyhealth.msg.morphine_overdose"));

            // Nausée forcée 10 sec + confusion
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA,    200, 1, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,     60, 0, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.POISON,       100, 0, false, false, false));

            // Dégâts directs aux bras (le corps rejette)
            BodyHealthAPI.damage(player, BodyPart.ARM_RIGHT, 1.5f);
            BodyHealthAPI.damage(player, BodyPart.ARM_LEFT,  1.0f);

            // Reset compteur après overdose
            persistentData.putInt(NBT_USE_COUNT, 0);

            stack.shrink(1);
            return stack;
        }

        // ── Effets normaux ────────────────────────────────────────────────

        // 1. Supprime tous les effets négatifs de blessure
        player.removeEffect(MobEffects.NAUSEA);
        player.removeEffect(MobEffects.SLOWNESS);
        player.removeEffect(MobEffects.WEAKNESS);

        // 2. Applique résistance (atténue douleur = moins de dégâts)
        player.addEffect(new MobEffectInstance(
                MobEffects.RESISTANCE, EFFECT_DURATION, 1, false, false, false));

        // 3. Marque la regen comme bloquée (PlayerTickHandler la vérifie)
        long blockedUntil = now + (EFFECT_DURATION * 50L); // ticks → ms
        persistentData.putLong(NBT_MORPHINE_ACTIVE, blockedUntil);

        // 4. Message selon le nb d'utilisations
        net.minecraft.network.chat.MutableComponent injected =
                Component.translatable("bodyhealth.msg.morphine_injected");
        if (useCount == OVERDOSE_THRESHOLD - 1)
            injected.append(Component.translatable("bodyhealth.msg.morphine_warning"));
        player.sendOverlayMessage(injected);

        stack.shrink(1);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.bodyhealth.morphine_syringe.desc1"));
        lines.accept(Component.translatable("item.bodyhealth.morphine_syringe.desc2"));
        lines.accept(Component.translatable("item.bodyhealth.morphine_syringe.desc3"));
        lines.accept(Component.translatable("item.bodyhealth.morphine_syringe.desc4"));
    }
}
