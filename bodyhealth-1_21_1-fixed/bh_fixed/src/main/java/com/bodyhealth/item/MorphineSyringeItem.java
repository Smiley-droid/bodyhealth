package com.bodyhealth.item;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyPart;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
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

    public MorphineSyringeItem() {
        super(new Item.Properties().stacksTo(8));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;

        long now = System.currentTimeMillis();

        // ── Vérification overdose ─────────────────────────────────────────
        var persistentData = player.getPersistentData();
        int  useCount  = persistentData.getInt(NBT_USE_COUNT);
        long firstUse  = persistentData.getLong(NBT_FIRST_USE);

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
            player.displayClientMessage(
                Component.literal("§4💉 OVERDOSE ! §cTrop de morphine..."), true);

            // Nausée forcée 10 sec + confusion
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,    200, 1, false, false, false));
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
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.WEAKNESS);

        // 2. Applique résistance (atténue douleur = moins de dégâts)
        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION, 1, false, false, false));

        // 3. Marque la regen comme bloquée (PlayerTickHandler la vérifie)
        long blockedUntil = now + (EFFECT_DURATION * 50L); // ticks → ms
        persistentData.putLong(NBT_MORPHINE_ACTIVE, blockedUntil);

        // 4. Message selon le nb d'utilisations
        String warning = useCount == OVERDOSE_THRESHOLD - 1
                ? " §c⚠ Dernière dose sûre !"
                : "";
        player.displayClientMessage(
            Component.literal("§b💉 Morphine injectée. §7Effets supprimés 30s." + warning),
            true);

        stack.shrink(1);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("§7Supprime §fnausée, lenteur, faiblesse §730 sec"));
        lines.add(Component.literal("§7Donne §bRésistance II §730 sec"));
        lines.add(Component.literal("§c⚠ Bloque toute régénération pendant 30 sec"));
        lines.add(Component.literal("§4☠ 3 injections en 2 min → Overdose !"));
    }
}
