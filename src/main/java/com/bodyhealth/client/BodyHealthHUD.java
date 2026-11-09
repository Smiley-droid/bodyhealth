package com.bodyhealth.client;

import com.bodyhealth.BodyHealthMod;
import com.bodyhealth.client.HeartRenderer.HeartType;
import com.bodyhealth.common.BodyPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Overlay Body Health (coin bas-droit). Trois modes (HudSettings.mode) :
 *   FULL   — silhouette + noms + coeurs
 *   HEARTS — noms + coeurs
 *   TEXT   — noms + valeurs de vie
 * Position, taille (HudSettings.scale, 3 = taille d'origine) et mode sont
 * réglables dans le menu H et sauvegardés.
 */
public class BodyHealthHUD {

    // Silhouette : canvas 16x32 dessiné à l'échelle fixe 3 (l'overlay entier est ensuite mis à l'échelle)
    private static final int FIG_SCALE = 3;
    private static final int FIG_W = 16 * FIG_SCALE;
    private static final int FIG_H = 32 * FIG_SCALE;

    private static final int[] PART_X  = {  4,  4, 12,  0,  8,  4 }; // HEAD TORSO ARM_R ARM_L LEG_R LEG_L
    private static final int[] PART_Y  = {  0,  8,  8,  8, 19, 19 };
    private static final int[] PART_SW = {  8,  8,  4,  4,  4,  4 };
    private static final int[] PART_SH = {  8, 11, 11, 11, 12, 12 };
    private static final String[] PART_FILE = {
        "head", "body", "right-hand", "left-hand", "right-leg", "left-leg"
    };

    private static final int ROW_H      = 11;
    private static final int HEART_SIZE = 8;
    private static final int HEART_GAP  = 1;
    private static final int PAD        = 5;
    private static final int BODY_GAP   = 7;
    private static final int LABEL_GAP  = 6;

    public static final int MARGIN_RIGHT = 5;
    public static final int MARGIN_BOT   = 5;

    // Rectangle affiché à l'écran lors du dernier rendu (utilisé par le menu pour le glisser-déposer)
    public static volatile int  lastX, lastY, lastW, lastH;
    public static volatile long lastRenderMs;

    private static final BodyPart[] ORDER = {
        BodyPart.HEAD, BodyPart.TORSO,
        BodyPart.ARM_RIGHT, BodyPart.ARM_LEFT,
        BodyPart.LEG_RIGHT, BodyPart.LEG_LEFT
    };

    private static final long[]  lastDamageMs = new long[6];
    private static final float[] prevHp       = new float[6];

    @SubscribeEvent
    public void onRenderHud(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.player.getAbilities().instabuild) return;          // comme la barre de faim
        if (!ClientBodyHealthData.isModEnabled()) return;         // mod désactivé côté serveur

        GuiGraphicsExtractor gui  = event.getGuiGraphics();
        Font        font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        updateFlash();
        HeartType hType = HeartRenderer.detectType(mc.player);

        HudSettings.Mode mode = HudSettings.mode;
        boolean figure = mode == HudSettings.Mode.FULL;

        // ── Dimensions (en unités "logiques", avant mise à l'échelle) ──────────
        Component[] names = new Component[ORDER.length];
        int labelW = 0;
        for (int i = 0; i < ORDER.length; i++) {
            names[i] = ORDER[i].displayName();
            labelW = Math.max(labelW, font.width(names[i]));
        }
        labelW += LABEL_GAP;

        int contentW = 0;
        if (mode == HudSettings.Mode.TEXT) {
            for (BodyPart p : ORDER)
                contentW = Math.max(contentW, font.width(mainText(p) + absorbText(p)));
        } else {
            int maxHearts = 0;
            for (BodyPart p : ORDER)
                maxHearts = Math.max(maxHearts, (int) Math.ceil(ClientBodyHealthData.getMaxHealth(p) / 2f));
            contentW = maxHearts * (HEART_SIZE + HEART_GAP) + 12;
        }

        int listH  = ORDER.length * ROW_H;
        int panelH = Math.max(figure ? FIG_H : 0, listH) + PAD * 2;
        int panelW = PAD + (figure ? FIG_W + BODY_GAP : 0) + labelW + contentW + PAD;

        // ── Position à l'écran ─────────────────────────────────────────────────
        float k = HudSettings.scale / 3f;
        int shownW = Math.round(panelW * k);
        int shownH = Math.round(panelH * k);
        int px = sw - shownW - MARGIN_RIGHT - HudSettings.offsetX;
        int py = sh - shownH - MARGIN_BOT   - HudSettings.offsetY;
        px = Math.max(0, Math.min(px, sw - shownW));
        py = Math.max(0, Math.min(py, sh - shownH));

        lastX = px; lastY = py; lastW = shownW; lastH = shownH;
        lastRenderMs = System.currentTimeMillis();

        gui.pose().pushMatrix();
        gui.pose().translate((float) px, (float) py);
        gui.pose().scale(k, k);

        // Fond + bordure
        gui.fill(0, 0, panelW, panelH, 0xAA1A2030);
        gui.fill(0,          0,          panelW, 1,      0xFF505870);
        gui.fill(0,          panelH - 1, panelW, panelH, 0xFF252838);
        gui.fill(0,          0,          1,      panelH, 0xFF505870);
        gui.fill(panelW - 1, 0,          panelW, panelH, 0xFF252838);

        int listX = PAD;
        if (figure) {
            renderBodyComposite(gui, PAD, (panelH - FIG_H) / 2);
            listX = PAD + FIG_W + BODY_GAP;
        }
        int listY = (panelH - listH) / 2;
        for (int i = 0; i < ORDER.length; i++)
            renderRow(gui, font, ORDER[i], names[i], listX, listY + i * ROW_H, labelW, hType, i, mode);

        gui.pose().popMatrix();
    }

    private void renderBodyComposite(GuiGraphicsExtractor gui, int ox, int oy) {
        for (int i = 0; i < ORDER.length; i++) {
            float hp    = ClientBodyHealthData.getHealth(ORDER[i]);
            float maxHp = ClientBodyHealthData.getMaxHealth(ORDER[i]);
            boolean flash = (System.currentTimeMillis() - lastDamageMs[i]) < 400;
            float hpForColor = flash ? Math.min(hp, maxHp * 0.33f - 0.01f) : hp;
            String color = colorFor(hpForColor, maxHp);

            Identifier tex = Identifier.fromNamespaceAndPath(BodyHealthMod.MOD_ID,
                "textures/gui/body_parts/" + PART_FILE[i] + "-" + color + "-cropped.png");

            int dx = ox + PART_X[i] * FIG_SCALE;
            int dy = oy + PART_Y[i] * FIG_SCALE;
            int dw = PART_SW[i] * FIG_SCALE;
            int dh = PART_SH[i] * FIG_SCALE;
            gui.blit(RenderPipelines.GUI_TEXTURED, tex, dx, dy, 0f, 0f, dw, dh, PART_SW[i], PART_SH[i], PART_SW[i], PART_SH[i]);
        }
    }

    private void renderRow(GuiGraphicsExtractor gui, Font font, BodyPart part, Component label,
                           int x, int y, int labelW, HeartType hType, int index,
                           HudSettings.Mode mode) {
        float hp    = ClientBodyHealthData.getHealth(part);
        float maxHp = ClientBodyHealthData.getMaxHealth(part);
        boolean dead  = ClientBodyHealthData.isDead(part);
        boolean crit  = ClientBodyHealthData.isCritical(part);
        boolean flash = (System.currentTimeMillis() - lastDamageMs[index]) < 400;
        boolean blink = crit && (System.currentTimeMillis() / 400) % 2 == 0;

        int labelColor;
        if (dead)       labelColor = 0xFF555555;
        else if (flash) labelColor = 0xFFFF3333;
        else if (crit)  labelColor = blink ? 0xFFFF4400 : 0xFFFF8800;
        else            labelColor = 0xFFDDDDDD;

        gui.text(font, label, x, y + 1, labelColor, false);

        int cx = x + labelW;
        if (mode == HudSettings.Mode.TEXT) {
            float pct = maxHp > 0 ? (dead ? 0f : hp) / maxHp : 0f;
            int valueColor;
            if (dead)               valueColor = 0xFF555555;
            else if (flash)         valueColor = 0xFFFF3333;
            else if (crit)          valueColor = blink ? 0xFFFF4400 : 0xFFFF8800;
            else if (pct <= 0.34f)  valueColor = 0xFFFF5555;
            else if (pct <= 0.65f)  valueColor = 0xFFFFAA00;
            else                    valueColor = 0xFF55FF55;

            String main = mainText(part);
            gui.text(font, main, cx, y + 1, valueColor, false);
            String abs = absorbText(part);
            if (!abs.isEmpty())
                gui.text(font, abs, cx + font.width(main), y + 1, 0xFFFFD84A, false);
        } else {
            HeartRenderer.renderHearts(gui, cx, y, dead ? 0 : hp, maxHp,
                    ClientBodyHealthData.getAbsorption(part), crit, hType, Integer.MAX_VALUE);
        }
    }

    private static String mainText(BodyPart p) {
        float hp  = ClientBodyHealthData.isDead(p) ? 0f : ClientBodyHealthData.getHealth(p);
        float max = ClientBodyHealthData.getMaxHealth(p);
        return String.format("%.1f/%.1f", hp, max);
    }

    private static String absorbText(BodyPart p) {
        float a = ClientBodyHealthData.getAbsorption(p);
        return a > 0 ? " +" + String.format("%.1f", a) : "";
    }

    private String colorFor(float hp, float maxHp) {
        if (maxHp <= 0 || hp <= 0) return "black";
        float pct = hp / maxHp;
        if (pct <= 0.34f) return "red";
        if (pct <= 0.65f) return "orange";
        return "green";
    }

    private void updateFlash() {
        for (int i = 0; i < ORDER.length; i++) {
            float cur = ClientBodyHealthData.getHealth(ORDER[i]);
            if (cur < prevHp[i] - 0.01f) lastDamageMs[i] = System.currentTimeMillis();
            prevHp[i] = cur;
        }
    }
}
