package com.bodyhealth.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Menu de réglages de l'overlay (touche H par défaut).
 * - mode d'affichage (silhouette / noms + coeurs / noms + valeurs)
 * - position : flèches, flèches du clavier, ou glisser l'overlay à la souris
 * - taille
 * Les réglages sont sauvegardés à la fermeture (config/bodyhealth-hud.json).
 * Le panneau est à gauche pour laisser voir l'aperçu en direct en bas à droite.
 */
@OnlyIn(Dist.CLIENT)
public class HudSettingsScreen extends Screen {

    private static final int STEP = 5;
    private static final int BW   = 24;
    private static final int BH   = 20;
    private static final int PW   = 200;
    private static final int PH   = 252;

    private Button  modeButton;
    private boolean dragging;
    private double  grabX, grabY;

    public HudSettingsScreen() {
        super(Component.translatable("bodyhealth.hud.title"));
    }

    private int panelX() { return 10; }
    private int panelY() { return Math.max(6, (this.height - PH) / 2); }

    @Override
    protected void init() {
        int x  = panelX();
        int y  = panelY();
        int cx = x + PW / 2;

        // Mode d'affichage
        modeButton = addRenderableWidget(Button.builder(modeLabel(), b -> {
            HudSettings.mode = HudSettings.mode.next();
            b.setMessage(modeLabel());
        }).bounds(x + 10, y + 42, PW - 20, 20).build());

        // Croix de position
        int top = y + 90;
        btn("^", cx - BW / 2,        top,                b -> adjY( STEP));
        btn("<", cx - BW - BW / 2 - 2, top + BH + 2,     b -> adjX( STEP));
        btn(">", cx + BW / 2 + 2,    top + BH + 2,       b -> adjX(-STEP));
        btn("v", cx - BW / 2,        top + 2 * (BH + 2), b -> adjY(-STEP));

        // Taille
        int sy = y + 178;
        btn("-", cx - BW - 34, sy, b -> HudSettings.scale = Math.max(HudSettings.MIN_SCALE, HudSettings.scale - 1));
        btn("+", cx + 34,      sy, b -> HudSettings.scale = Math.min(HudSettings.MAX_SCALE, HudSettings.scale + 1));

        // Réinitialiser / Fermer
        addRenderableWidget(Button.builder(Component.translatable("bodyhealth.hud.reset"), b -> {
            HudSettings.reset();
            modeButton.setMessage(modeLabel());
        }).bounds(x + 8, y + 206, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("bodyhealth.hud.close"), b -> onClose())
            .bounds(x + PW - 98, y + 206, 90, 20).build());
    }

    private Component modeLabel() {
        return Component.translatable("bodyhealth.hud.display")
                .append(": ")
                .append(Component.translatable("bodyhealth.hud.mode." + HudSettings.mode.key));
    }

    private void btn(String label, int x, int y, Button.OnPress action) {
        addRenderableWidget(Button.builder(Component.literal(label), action).bounds(x, y, BW, BH).build());
    }

    private void adjX(int d) { HudSettings.offsetX = Math.max(0, HudSettings.offsetX + d); }
    private void adjY(int d) { HudSettings.offsetY = Math.max(0, HudSettings.offsetY + d); }

    /** Pas de flou ni d'assombrissement : l'aperçu de l'overlay reste net. */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float dt) { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        int x  = panelX();
        int y  = panelY();
        int cx = x + PW / 2;

        // Panneau
        g.fill(x,          y,          x + PW,     y + PH,     0xDD0D0D18);
        g.fill(x,          y,          x + PW,     y + 1,      0xFF5566AA);
        g.fill(x,          y + PH - 1, x + PW,     y + PH,     0xFF223366);
        g.fill(x,          y,          x + 1,      y + PH,     0xFF5566AA);
        g.fill(x + PW - 1, y,          x + PW,     y + PH,     0xFF223366);

        g.drawCenteredString(font, this.title, cx, y + 10, 0xFFFFFFFF);

        boolean visible = System.currentTimeMillis() - BodyHealthHUD.lastRenderMs < 300;
        if (!visible)
            g.drawCenteredString(font, Component.translatable("bodyhealth.hud.not_visible"), cx, y + 24, 0xFFFF8888);

        g.drawCenteredString(font, Component.translatable("bodyhealth.hud.display"),  cx, y + 32, 0xFFAAAAAA);
        g.drawCenteredString(font, Component.translatable("bodyhealth.hud.position"), cx, y + 76, 0xFFAAAAAA);
        g.drawCenteredString(font, "X: " + HudSettings.offsetX + "   Y: " + HudSettings.offsetY,
                cx, y + 160, 0xFFDDDDDD);

        g.drawCenteredString(font, Component.translatable("bodyhealth.hud.size"), cx, y + 168 - 2 + 0, 0xFFAAAAAA);
        g.drawCenteredString(font, String.valueOf(HudSettings.scale) + " / " + HudSettings.MAX_SCALE,
                cx, y + 178 + 6, 0xFFDDDDDD);

        g.drawCenteredString(font, Component.translatable("bodyhealth.hud.drag_hint"),
                cx, y + PH - 16, 0xFF888888);

        super.render(g, mx, my, dt);
    }

    // ── Glisser-déposer de l'overlay ──────────────────────────────────────────

    private boolean overHud(double mx, double my) {
        return System.currentTimeMillis() - BodyHealthHUD.lastRenderMs < 300
            && mx >= BodyHealthHUD.lastX && mx <= BodyHealthHUD.lastX + BodyHealthHUD.lastW
            && my >= BodyHealthHUD.lastY && my <= BodyHealthHUD.lastY + BodyHealthHUD.lastH;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0 && overHud(mx, my)) {
            dragging = true;
            grabX = mx - BodyHealthHUD.lastX;
            grabY = my - BodyHealthHUD.lastY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && button == 0) {
            int left = (int) Math.round(mx - grabX);
            int top  = (int) Math.round(my - grabY);
            HudSettings.offsetX = Math.max(0, this.width  - BodyHealthHUD.MARGIN_RIGHT - BodyHealthHUD.lastW - left);
            HudSettings.offsetY = Math.max(0, this.height - BodyHealthHUD.MARGIN_BOT   - BodyHealthHUD.lastH - top);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case 265 -> { adjY( STEP); return true; } // haut
            case 264 -> { adjY(-STEP); return true; } // bas
            case 263 -> { adjX( STEP); return true; } // gauche
            case 262 -> { adjX(-STEP); return true; } // droite
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void removed() {
        HudSettings.save();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
