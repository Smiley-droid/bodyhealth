package com.bodyhealth.client;

import com.bodyhealth.BodyHealthMod;
import com.bodyhealth.client.HeartRenderer.HeartType;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.common.FractureState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * HUD Body Health — coin bas-DROIT, taille réduite (SCALE ×3).
 *
 * FIX 1 — Jambe gauche alignée : les deux jambes sont maintenant placées
 *          au même Y (layout_y=19) car left-leg bbox démarre à y=4 et
 *          right-leg à y=3. On normalise en croppant right-leg depuis y=4.
 *
 * FIX 2 — Menu déplacé en bas-DROIT + SCALE réduit de 4 à 3 (corps 48×96 px).
 */
@OnlyIn(Dist.CLIENT)
public class BodyHealthHUD {

    // SCALE dynamique — modifiable via HudSettingsScreen (touche H)
    public  static       int HUD_SCALE = 3;
    private static final int SCALE     = 3; // conservé pour compatibilité interne
    private static final int CANVAS_W = 16;
    private static final int CANVAS_H = 32;
    // drawW()/H calculés dynamiquement depuis HUD_SCALE
    private static int drawW() { return CANVAS_W * HUD_SCALE; }
    private static int drawH() { return CANVAS_H * HUD_SCALE; }

    // Positions dans le canvas 16×32 (en pixels source)
    // FIX 1 — right-leg : on skip la ligne y=3 (1px fantôme) → même y=19 que left-leg
    //          On gère ça côté fichier croppé : right-leg-*-cropped.png commence à y=4
    private static final int[] PART_X  = {  4,  4, 12,  0,  8,  4 }; // HEAD TORSO ARM_R ARM_L LEG_R LEG_L
    private static final int[] PART_Y  = {  0,  8,  8,  8, 19, 19 }; // LEG_R et LEG_L même Y
    private static final int[] PART_SW = {  8,  8,  4,  4,  4,  4 }; // largeur source croppée
    private static final int[] PART_SH = {  8, 11, 11, 11, 12, 12 }; // FIX 1 — les deux jambes = 12px
    private static final String[] PART_FILE = {
        "head","body","right-hand","left-hand","right-leg","left-leg"
    };

    private static final ResourceLocation FRACTURE_ICON =
            ResourceLocation.fromNamespaceAndPath(BodyHealthMod.MOD_ID, "textures/gui/fracture_icon.png");

    private static final int ROW_H      = 11;
    private static final int LABEL_W    = 44;
    private static final int HEART_SIZE = 8;
    private static final int HEART_GAP  = 1;
    private static final int PAD        = 5;
    private static final int BODY_GAP   = 7;
    // BUG 3 FIX — Position configurable via setOffset() + masqué en créatif
    public static int OFFSET_X = 5;  // offset depuis la droite
    public static int OFFSET_Y = 5;  // offset depuis le bas
    private static final int MARGIN_RIGHT = 5;
    private static final int MARGIN_BOT   = 5;

    private static final BodyPart[] ORDER = {
        BodyPart.HEAD, BodyPart.TORSO,
        BodyPart.ARM_RIGHT, BodyPart.ARM_LEFT,
        BodyPart.LEG_RIGHT, BodyPart.LEG_LEFT
    };
    private static final String[] LABELS = {
        "Tête","Torse","Bras D","Bras G","Jambe D","Jambe G"
    };

    private static final long[]  lastDamageMs = new long[6];
    private static final float[] prevHp       = new float[6];

    @SubscribeEvent
    public void onRenderHud(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        // BUG 3 FIX — Masquer en mode créatif (comme la barre de faim vanilla)
        if (mc.player.getAbilities().instabuild) return;

        GuiGraphics gui = event.getGuiGraphics();
        LocalPlayer player = mc.player;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        updateFlash();
        HeartType hType = HeartRenderer.detectType(player);

        int maxH = 0;
        for (BodyPart p : ORDER)
            maxH = Math.max(maxH, (int) Math.ceil(ClientBodyHealthData.getMaxHealth(p) / 2f));
        int heartsW = maxH * (HEART_SIZE + HEART_GAP);

        int listH  = ORDER.length * ROW_H;
        int panelH = Math.max(drawH(), listH) + PAD * 2;
        int panelW = PAD + drawW() + BODY_GAP + LABEL_W + heartsW + 12 + PAD;

        // FIX 2 — coin bas-DROIT
        int px = sw - panelW - MARGIN_RIGHT - OFFSET_X;
        int py = sh - MARGIN_BOT - panelH - OFFSET_Y;

        gui.fill(px, py, px + panelW, py + panelH, 0xAA1A2030);
        // Bordure fine
        gui.fill(px,            py,            px + panelW, py + 1,      0xFF505870);
        gui.fill(px,            py + panelH-1, px + panelW, py + panelH, 0xFF252838);
        gui.fill(px,            py,            px + 1,      py + panelH, 0xFF505870);
        gui.fill(px + panelW-1, py,            px + panelW, py + panelH, 0xFF252838);

        int bodyDrawX = px + PAD;
        int bodyDrawY = py + (panelH - drawH()) / 2;
        renderBodyComposite(gui, bodyDrawX, bodyDrawY);

        int listX = bodyDrawX + drawW() + BODY_GAP;
        int listY = py + (panelH - listH) / 2;
        for (int i = 0; i < ORDER.length; i++)
            renderRow(gui, mc, ORDER[i], LABELS[i], listX, listY + i * ROW_H, hType, i);
    }

    private void renderBodyComposite(GuiGraphics gui, int ox, int oy) {
        for (int i = 0; i < ORDER.length; i++) {
            float hp    = ClientBodyHealthData.getHealth(ORDER[i]);
            float maxHp = ClientBodyHealthData.getMaxHealth(ORDER[i]);
            boolean flash = (System.currentTimeMillis() - lastDamageMs[i]) < 400;
            float hpForColor = flash ? Math.min(hp, maxHp * 0.33f - 0.01f) : hp;
            String color = colorFor(hpForColor, maxHp);

            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(BodyHealthMod.MOD_ID,
                "textures/gui/body_parts/" + PART_FILE[i] + "-" + color + "-cropped.png");

            int dx = ox + PART_X[i] * HUD_SCALE;
            int dy = oy + PART_Y[i] * HUD_SCALE;
            int dw = PART_SW[i] * HUD_SCALE;
            int dh = PART_SH[i] * HUD_SCALE;
            gui.blit(tex, dx, dy, dw, dh, 0, 0, PART_SW[i], PART_SH[i], PART_SW[i], PART_SH[i]);
        }
    }

    private void renderRow(GuiGraphics gui, Minecraft mc, BodyPart part,
                            String label, int x, int y, HeartType hType, int index) {
        float hp    = ClientBodyHealthData.getHealth(part);
        float maxHp = ClientBodyHealthData.getMaxHealth(part);
        boolean dead = ClientBodyHealthData.isDead(part);
        boolean crit = ClientBodyHealthData.isCritical(part);
        FractureState fr = ClientBodyHealthData.getFracture(part);
        boolean flash = (System.currentTimeMillis() - lastDamageMs[index]) < 400;
        boolean blink = crit && (System.currentTimeMillis() / 400) % 2 == 0;

        int labelColor;
        if (dead)                               labelColor = 0xFF555555;
        else if (flash)                         labelColor = 0xFFFF3333;
        else if (fr == FractureState.SHATTERED) labelColor = 0xFFCC2200;
        else if (fr == FractureState.BROKEN)    labelColor = 0xFFFF5500;
        else if (fr == FractureState.SPRAINED)  labelColor = 0xFFFFAA00;
        else if (crit) labelColor = blink ? 0xFFFF4400 : 0xFFFF8800;
        else           labelColor = 0xFFDDDDDD;

        gui.drawString(mc.font, label, x, y + 1, labelColor, false);

        int heartsX = x + LABEL_W;
        float absorption = ClientBodyHealthData.getAbsorption(part);
        HeartRenderer.renderHearts(gui, heartsX, y,
                dead ? 0 : hp, maxHp, absorption, crit, hType, Integer.MAX_VALUE);

        if (fr != FractureState.NONE) {
            int n = (int) Math.ceil(maxHp / 2f);
            renderFractureIcon(gui, heartsX + n * (HEART_SIZE + HEART_GAP) + 2, y + 1, fr);
        }
    }

    private String colorFor(float hp, float maxHp) {
        if (maxHp <= 0 || hp <= 0) return "black";
        float pct = hp / maxHp;
        if (pct <= 0.34f) return "red";
        if (pct <= 0.65f) return "orange";
        return "green";
    }

    private void renderFractureIcon(GuiGraphics gui, int x, int y, FractureState fr) {
        boolean blink = fr == FractureState.SHATTERED && (System.currentTimeMillis() / 300) % 2 == 0;
        int c = switch (fr) {
            case SPRAINED  -> 0xFFFFFFFF;
            case BROKEN    -> 0xFFFF8800;
            case SHATTERED -> blink ? 0xFFFF2200 : 0xFF880000;
            default        -> 0xFFFFFFFF;
        };
        gui.setColor(((c>>16)&0xFF)/255f, ((c>>8)&0xFF)/255f, (c&0xFF)/255f, 1f);
        gui.blit(FRACTURE_ICON, x, y, 9, 9, 0, 0, 9, 9, 9, 9);
        gui.setColor(1f, 1f, 1f, 1f);
    }

    private void updateFlash() {
        for (int i = 0; i < ORDER.length; i++) {
            float cur = ClientBodyHealthData.getHealth(ORDER[i]);
            if (cur < prevHp[i] - 0.01f) lastDamageMs[i] = System.currentTimeMillis();
            prevHp[i] = cur;
        }
    }
}
