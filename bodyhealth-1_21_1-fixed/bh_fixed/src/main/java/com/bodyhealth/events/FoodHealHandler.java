package com.bodyhealth.events;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.config.BodyHealthConfig;
import com.bodyhealth.network.SyncBodyHealthPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Gestion de la nourriture → soin des parties du corps.
 *
 * ══════════════════════════════════════════════════════════
 * COMPATIBILITÉ MODS TIERS — AUTOMATIQUE
 * ══════════════════════════════════════════════════════════
 * NeoForge expose FoodProperties sur TOUS les items de nourriture,
 * qu'ils viennent de Farmer's Delight, Pam's HarvestCraft, Alex's Mobs,
 * ou n'importe quel autre mod.
 *
 * La formule s'adapte automatiquement aux valeurs de chaque item :
 *
 *   totalHeal = (nutrition × 0.5 + saturation × 0.25) × multiplicateur_config
 *
 * Exemples avec nourriture vanilla et mods :
 *
 *   Vanilla :
 *     Pain         (5 / 0.6) → 2.65 × 0.4 = 1.06 HP
 *     Steak cuit   (8 / 0.8) → 4.20 × 0.4 = 1.68 HP
 *     Carotte dorée(6 / 1.2) → 3.30 × 0.4 = 1.32 HP
 *
 *   Farmer's Delight (exemple) :
 *     Pot-au-feu   (12 / 1.2)→ 6.30 × 0.4 = 2.52 HP  ← automatique
 *     Gâteau moelleux(8/0.9) → 4.23 × 0.4 = 1.69 HP  ← automatique
 *
 *   Alex's Mobs (exemple) :
 *     Viande rare  (10 / 1.0)→ 5.25 × 0.4 = 2.10 HP  ← automatique
 *
 * Aucune configuration nécessaire pour les mods tiers.
 * Le multiplicateur global (config) s'applique à tout.
 *
 * ══════════════════════════════════════════════════════════
 * PRIORITÉ DE SOIN
 * ══════════════════════════════════════════════════════════
 * 1. Torse en premier si blessé (partie vitale)
 * 2. Partie la plus blessée en % ensuite
 * 3. Cascade si une partie déborde sur la suivante
 */
public class FoodHealHandler {

    @SubscribeEvent
    public void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem();
        FoodProperties food = stack.getFoodProperties(player);
        if (food == null) return;

        // ── Formule adaptive — fonctionne avec TOUS les mods de nourriture ──
        float nutrition  = food.nutrition();
        float saturation = food.saturation();
        float base       = (nutrition * 0.5f) + (saturation * 0.25f);
        float multiplier = BodyHealthConfig.FOOD_HEAL_MULTIPLIER.get().floatValue();
        float totalHeal  = base * multiplier;

        if (totalHeal <= 0) return;

        BodyHealthData data     = BodyHealthAPI.getData(player);
        BodyPart mainHealed     = distributeHealing(data, totalHeal);

        // Message action bar avec les vraies valeurs
        if (mainHealed != null) {
            String hpStr = String.format("%.1f", totalHeal / 2f);
            player.displayClientMessage(
                Component.literal("§a❤ +" + hpStr + " ❤ §7→ §f" + getPartName(mainHealed)),
                true);
        } else {
            player.displayClientMessage(
                Component.literal("§7Vous êtes en pleine santé."), true);
        }

        PacketDistributor.sendToPlayer(player,
                new SyncBodyHealthPacket(data.serializeNBT()));
    }

    /**
     * Distribue les HP sur les parties blessées par priorité.
     * Cascade : si une partie est remplie, le surplus va à la suivante.
     */
    private BodyPart distributeHealing(BodyHealthData data, float totalHeal) {
        float remaining = totalHeal;
        BodyPart first  = null;
        int guard       = 0;

        while (remaining > 0.01f && guard++ < 20) {
            BodyPart target = getMostInjured(data);
            if (target == null) break;
            if (first == null) first = target;

            float missing = data.getMaxHealth(target) - data.getHealth(target);
            float toHeal  = Math.min(remaining, missing);
            data.heal(target, toHeal);
            remaining -= toHeal;
        }
        return first;
    }

    /**
     * Retourne la partie la plus blessée.
     * Torse prioritaire à égalité (partie vitale).
     */
    private BodyPart getMostInjured(BodyHealthData data) {
        BodyPart worst    = null;
        float    worstPct = 1.0f;

        // Ordre de priorité : torse d'abord
        BodyPart[] priority = {
            BodyPart.TORSO, BodyPart.HEAD,
            BodyPart.ARM_RIGHT, BodyPart.ARM_LEFT,
            BodyPart.LEG_RIGHT, BodyPart.LEG_LEFT
        };

        for (BodyPart part : priority) {
            float pct = data.getHealthPercent(part);
            if (pct < worstPct) { worstPct = pct; worst = part; }
        }
        return worstPct >= 1.0f ? null : worst;
    }

    private String getPartName(BodyPart part) {
        return switch (part) {
            case HEAD      -> "Tête";
            case TORSO     -> "Torse";
            case ARM_RIGHT -> "Bras droit";
            case ARM_LEFT  -> "Bras gauche";
            case LEG_RIGHT -> "Jambe droite";
            case LEG_LEFT  -> "Jambe gauche";
        };
    }
}
