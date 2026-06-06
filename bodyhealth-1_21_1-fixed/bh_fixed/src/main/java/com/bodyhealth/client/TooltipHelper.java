package com.bodyhealth.client;

import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.common.FractureState;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Helper pour les tooltips dynamiques des items de soin.
 * Lit ClientBodyHealthData pour afficher les vraies valeurs en temps réel.
 *
 * Appelé uniquement côté client (dans appendHoverText).
 */
public class TooltipHelper {

    /**
     * Tooltip dynamique pour le Bandage.
     * Affiche quelle partie sera soignée et de combien.
     */
    public static void addBandageTooltip(List<Component> lines, float healAmount) {
        BodyPart target  = getMostInjured();
        if (target == null) {
            lines.add(Component.literal("§7Vous êtes en §apleine santé"));
            return;
        }

        float currentHp = ClientBodyHealthData.getHealth(target);
        float maxHp     = ClientBodyHealthData.getMaxHealth(target);
        float pct       = ClientBodyHealthData.getHealthPercent(target);
        FractureState fr = ClientBodyHealthData.getFracture(target);

        String partName  = getPartName(target);
        float  afterHeal = Math.min(currentHp + healAmount, maxHp);

        lines.add(Component.literal(
            "§7Cible : §f" + partName +
            " §7(" + String.format("%.0f", pct * 100) + "% HP)"));
        lines.add(Component.literal(
            "§7Soin : §a" + String.format("%.1f", currentHp/2f) +
            " §7→ §a" + String.format("%.1f", afterHeal/2f) + " ❤"));

        if (fr != FractureState.NONE) {
            FractureState improved = fr.improve();
            lines.add(Component.literal(
                "§7Fracture : §c" + fr.getDisplayName() +
                " §7→ §a" + (improved == FractureState.NONE
                        ? "Guérie" : improved.getDisplayName())));
        }
    }

    /**
     * Tooltip dynamique pour le Kit Médical.
     * Affiche le soin total sur toutes les parties.
     */
    public static void addMedicalKitTooltip(List<Component> lines, float healPerPart) {
        float totalMissing = 0;
        int   partsToHeal  = 0;
        int   fracturesCount = 0;

        for (BodyPart part : BodyPart.values()) {
            float missing = ClientBodyHealthData.getMaxHealth(part)
                          - ClientBodyHealthData.getHealth(part);
            if (missing > 0) {
                totalMissing += Math.min(healPerPart, missing);
                partsToHeal++;
            }
            if (ClientBodyHealthData.getFracture(part) != FractureState.NONE)
                fracturesCount++;
        }

        if (partsToHeal == 0) {
            lines.add(Component.literal("§7Vous êtes en §apleine santé"));
            return;
        }

        lines.add(Component.literal(
            "§7Soin total : §a+" + String.format("%.1f", totalMissing / 2f) +
            " ❤ §7sur §f" + partsToHeal + " partie(s)"));

        if (fracturesCount > 0) {
            lines.add(Component.literal(
                "§7Fractures améliorées : §f" + fracturesCount));
        }
    }

    private static BodyPart getMostInjured() {
        BodyPart worst    = null;
        float    worstPct = 1.0f;
        BodyPart[] priority = {
            BodyPart.TORSO, BodyPart.HEAD,
            BodyPart.ARM_RIGHT, BodyPart.ARM_LEFT,
            BodyPart.LEG_RIGHT, BodyPart.LEG_LEFT
        };
        for (BodyPart part : priority) {
            float pct = ClientBodyHealthData.getHealthPercent(part);
            if (pct < worstPct) { worstPct = pct; worst = part; }
        }
        return worstPct >= 1.0f ? null : worst;
    }

    private static String getPartName(BodyPart part) {
        return switch (part) {
            case HEAD -> "Tête"; case TORSO -> "Torse";
            case ARM_RIGHT -> "Bras droit"; case ARM_LEFT -> "Bras gauche";
            case LEG_RIGHT -> "Jambe droite"; case LEG_LEFT -> "Jambe gauche";
        };
    }
}
