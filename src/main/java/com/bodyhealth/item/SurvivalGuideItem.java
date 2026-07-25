package com.bodyhealth.item;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.network.OpenGuidePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 📖 Guide de Survie
 *
 * Affiche l'état complet du joueur : HP par partie, absorption active,
 * rappel des effets critiques, des items de soin et des recettes.
 *
 * use() s'exécute côté SERVEUR, lit les données via BodyHealthAPI
 * (accès direct au BodyHealthData serverside), puis envoie un OpenGuidePacket
 * au client avec le NBT des données. Le client affiche le guide via
 * displayGuideClient() qui lit depuis le snapshot ClientBodyHealthData
 * fraîchement mis à jour par le paquet.
 *
 * Si le mod est désactivé côté serveur (config enabled=false), le guide
 * affiche un message court au lieu des stats détaillées.
 *
 * Fonctionne en solo ET en multijoueur dédié.
 */
public class SurvivalGuideItem extends Item {

    public SurvivalGuideItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // FIX — Exécution côté SERVEUR uniquement (accès aux vraies données)
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BodyHealthData data = BodyHealthAPI.getData(serverPlayer);
            // Envoyer les données au client via paquet → client affiche le guide
            PacketDistributor.sendToPlayer(serverPlayer,
                    new OpenGuidePacket(data.serializeNBT()));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    /**
     * Appelé côté CLIENT par OpenGuidePacket.handle().
     * Lit ClientBodyHealthData (mis à jour par le paquet juste avant).
     * Méthode static pour être accessible depuis le handler réseau.
     *
     * @OnlyIn(CLIENT) garanti par le chemin d'appel (OpenGuidePacket.handle → enqueueWork)
     */
    public static void displayGuideClient(Player player) {
        // Si le mod est désactivé côté serveur, le guide n'a pas de sens
        if (!com.bodyhealth.client.ClientBodyHealthData.isModEnabled()) {
            player.sendSystemMessage(Component.literal(
                "§7[BodyHealth] §cLe système de santé localisée est actuellement désactivé sur ce serveur."));
            player.sendSystemMessage(Component.literal(
                "§7Vous utilisez la vie vanilla standard de Minecraft."));
            return;
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6══════ 📖 Guide de Survie ══════"));

        // ── Statut actuel ─────────────────────────────────────────────────────
        player.sendSystemMessage(Component.literal("§e▶ Votre état actuel :"));

        for (BodyPart part : BodyPart.values()) {
            float hp        = com.bodyhealth.client.ClientBodyHealthData.getHealth(part);
            float max       = com.bodyhealth.client.ClientBodyHealthData.getMaxHealth(part);
            float absorb    = com.bodyhealth.client.ClientBodyHealthData.getAbsorption(part);

            String hpBar    = buildHpBar(hp, max);
            String stateCol = hp <= 0 ? "§8" : hp/max <= 0.34f ? "§c" : hp/max <= 0.65f ? "§e" : "§a";
            String absorbTag = absorb > 0 ? " §e+" + String.format("%.1f", absorb/2f) + "❤" : "";

            player.sendSystemMessage(Component.literal(
                "  §7" + padRight(getPartName(part), 10) + ": "
                + hpBar + " " + stateCol
                + String.format("%.1f", hp) + "§7/§f" + String.format("%.1f", max)
                + absorbTag));
        }

        // ── Effets critiques par groupe (système façon SrGnis) ─────────────────
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§e▶ Effets critiques :"));
        player.sendSystemMessage(Component.literal("  §7Tête/Torse critique  §f→ §cFaiblesse"));
        player.sendSystemMessage(Component.literal("  §7Bras détruits        §f→ §cFatigue minière"));
        player.sendSystemMessage(Component.literal("  §7Jambes détruites     §f→ §cLenteur"));
        player.sendSystemMessage(Component.literal("  §7Tête critique        §f→ §cNausée (vision trouble)"));
        player.sendSystemMessage(Component.literal("  §7Bras D détruit       §f→ §cImpossible de casser/poser"));
        player.sendSystemMessage(Component.literal("  §7Bras G détruit       §f→ §cPerd l'item secondaire"));
        player.sendSystemMessage(Component.literal("  §7Torse à 0 HP         §f→ §4Mort instantanée"));
        player.sendSystemMessage(Component.literal("  §8Une partie est \"critique\" sous 1/3 de son max (tête/torse)"));
        player.sendSystemMessage(Component.literal("  §8ou \"détruite\" à 0 HP (bras/jambes)."));

        // ── Absorption ────────────────────────────────────────────────────────
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§e▶ Absorption (cœurs jaunes) :"));
        player.sendSystemMessage(Component.literal("  §7Pomme dorée    §f→ §e+2❤ §7sur la partie la + blessée"));
        player.sendSystemMessage(Component.literal("  §7Pomme de Notch §f→ §e+4❤ §7sur la partie la + blessée"));
        player.sendSystemMessage(Component.literal("  §7Totem d'immortalité §f→ §e+2❤ §7sur le torse, vous sauve une fois"));

        // ── Items de soin ─────────────────────────────────────────────────────
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§e▶ Items de soin :"));
        player.sendSystemMessage(Component.literal("  §a🩹 Bandage     §f→ +1.5 ❤ (partie la + blessée)  §8[3s]"));
        player.sendSystemMessage(Component.literal("  §a🏥 Kit Médical §f→ +3 ❤ toutes parties §8[5s]"));
        player.sendSystemMessage(Component.literal("  §b💉 Morphine    §f→ Supprime effets 30s §c(overdose ×3 !)"));

        // ── Craft rapide ──────────────────────────────────────────────────────
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§e▶ Craft rapide :"));
        player.sendSystemMessage(Component.literal("  §7Laine + Fil            §f→ §a🩹 Bandage §8(×4)"));
        player.sendSystemMessage(Component.literal("  §7Bandages + Or + Coffre §f→ §a🏥 Kit Médical"));
        player.sendSystemMessage(Component.literal("  §7Verre + Blaze + Fer    §f→ §b💉 Seringue §8(×2)"));

        player.sendSystemMessage(Component.literal("§6══════════════════════════════"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String buildHpBar(float hp, float max) {
        int total  = Math.min((int)(max / 2), 5);
        int filled = (int)(hp  / 2);
        StringBuilder sb = new StringBuilder("§f[");
        for (int i = 0; i < total; i++)
            sb.append(i < filled ? "§c❤" : "§8❤");
        sb.append("§f]");
        return sb.toString();
    }

    private static String getPartName(BodyPart part) {
        return switch (part) {
            case HEAD      -> "Tête";
            case TORSO     -> "Torse";
            case ARM_RIGHT -> "Bras D";
            case ARM_LEFT  -> "Bras G";
            case LEG_RIGHT -> "Jambe D";
            case LEG_LEFT  -> "Jambe G";
        };
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.bodyhealth.survival_guide.use"));
        lines.add(Component.translatable("item.bodyhealth.survival_guide.desc2"));
        lines.add(Component.translatable("item.bodyhealth.survival_guide.craft"));
    }
}
