package com.bodyhealth.item;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.network.OpenGuidePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
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

    public SurvivalGuideItem(Item.Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // FIX — Exécution côté SERVEUR uniquement (accès aux vraies données)
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BodyHealthData data = BodyHealthAPI.getData(serverPlayer);
            // Envoyer les données au client via paquet → client affiche le guide
            PacketDistributor.sendToPlayer(serverPlayer,
                    new OpenGuidePacket(data.serializeNBT()));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Appelé côté CLIENT par OpenGuidePacket.handle().
     * Lit ClientBodyHealthData (mis à jour par le paquet juste avant).
     * Méthode static pour être accessible depuis le handler réseau.
     *
     * @OnlyIn(CLIENT) garanti par le chemin d'appel (OpenGuidePacket.handle → enqueueWork)
     */
    public static void displayGuideClient(Player player) {
        if (!com.bodyhealth.client.ClientBodyHealthData.isModEnabled()) {
            say(player, "disabled1");
            say(player, "disabled2");
            return;
        }

        boolean compact = isCompact();

        say(player, "title");
        say(player, "status");

        boolean anyShown = false;
        for (BodyPart part : BodyPart.values()) {
            float hp     = com.bodyhealth.client.ClientBodyHealthData.getHealth(part);
            float max    = com.bodyhealth.client.ClientBodyHealthData.getMaxHealth(part);
            float absorb = com.bodyhealth.client.ClientBodyHealthData.getAbsorption(part);

            // Mode compact : seulement les parties blessées ou protégées
            if (compact && hp >= max && absorb <= 0) continue;
            anyShown = true;

            String stateCol  = hp <= 0 ? "§8" : hp/max <= 0.34f ? "§c" : hp/max <= 0.65f ? "§e" : "§a";
            String absorbTag = absorb > 0 ? " §e+" + String.format("%.1f", absorb / 2f) + "❤" : "";

            player.displayClientMessage(Component.literal("  ")
                .append(Component.translatable("bodyhealth.part." + part.getId())
                        .withStyle(net.minecraft.ChatFormatting.GRAY))
                .append(Component.literal(": " + buildHpBar(hp, max) + " " + stateCol
                        + String.format("%.1f", hp) + "§7/§f" + String.format("%.1f", max)
                        + absorbTag)), false);
        }
        if (!anyShown) say(player, "all_healthy");

        if (compact) {
            say(player, "compact_hint");
            say(player, "footer");
            return;
        }

        blank(player);
        say(player, "effects.title");
        say(player, "effects.head_torso");
        say(player, "effects.arms");
        say(player, "effects.legs");
        say(player, "effects.nausea");
        say(player, "effects.arm_r");
        say(player, "effects.arm_l");
        say(player, "effects.torso");
        say(player, "effects.note1");
        say(player, "effects.note2");

        blank(player);
        say(player, "absorb.title");
        say(player, "absorb.golden");
        say(player, "absorb.notch");
        say(player, "absorb.totem");

        blank(player);
        say(player, "items.title");
        say(player, "items.bandage");
        say(player, "items.kit");
        say(player, "items.morphine");

        blank(player);
        say(player, "craft.title");
        say(player, "craft.bandage");
        say(player, "craft.kit");
        say(player, "craft.morphine");

        say(player, "footer");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void say(Player player, String key) {
        player.displayClientMessage(Component.translatable("bodyhealth.guide." + key), false);
    }

    private static void blank(Player player) {
        player.displayClientMessage(Component.literal(""), false);
    }

    private static boolean isCompact() {
        try {
            return com.bodyhealth.config.BodyHealthClientConfig.COMPACT_GUIDE.get();
        } catch (Exception e) {
            return false; // config pas encore chargée
        }
    }

    private static String buildHpBar(float hp, float max) {
        int total  = Math.min((int)(max / 2), 5);
        int filled = (int)(hp  / 2);
        StringBuilder sb = new StringBuilder("§f[");
        for (int i = 0; i < total; i++)
            sb.append(i < filled ? "§c❤" : "§8❤");
        sb.append("§f]");
        return sb.toString();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.bodyhealth.survival_guide.use"));
        lines.accept(Component.translatable("item.bodyhealth.survival_guide.desc2"));
        lines.accept(Component.translatable("item.bodyhealth.survival_guide.craft"));
    }
}
