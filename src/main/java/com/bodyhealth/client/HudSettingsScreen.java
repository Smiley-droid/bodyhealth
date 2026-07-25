package com.bodyhealth.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HudSettingsScreen extends Screen {

    private static final int STEP = 5;
    private static final int BW   = 24;
    private static final int BH   = 20;

    public HudSettingsScreen() {
        super(Component.literal("Body Health - Reglages HUD"));
    }

    @Override
    protected void init() {
        int cx = this.width  / 2;
        int cy = this.height / 2;

        int ax = cx, ay = cy - 30;
        btn("^",  ax - BW/2,    ay - BH - 2, b -> adjY( STEP));
        btn("v",  ax - BW/2,    ay + 2,       b -> adjY(-STEP));
        btn("<",  ax - BW - 28, ay - BH/2,   b -> adjX( STEP));
        btn(">",  ax + 28,      ay - BH/2,   b -> adjX(-STEP));

        int sy = cy + 55;
        btn("-", cx - BW - 4, sy - BH/2, b -> { BodyHealthHUD.HUD_SCALE = Math.max(1, BodyHealthHUD.HUD_SCALE - 1); });
        btn("+", cx + 4,      sy - BH/2, b -> { BodyHealthHUD.HUD_SCALE = Math.min(6, BodyHealthHUD.HUD_SCALE + 1); });

        addRenderableWidget(Button.builder(Component.literal("Reinitialiser"),
            b -> { BodyHealthHUD.OFFSET_X = 5; BodyHealthHUD.OFFSET_Y = 5; BodyHealthHUD.HUD_SCALE = 3; })
            .bounds(cx - 105, cy + 90, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Fermer"), b -> onClose())
            .bounds(cx + 5, cy + 90, 100, 20).build());
    }

    private void btn(String lbl, int x, int y, Button.OnPress a) {
        addRenderableWidget(Button.builder(Component.literal(lbl), a).bounds(x, y, BW, BH).build());
    }

    private void adjX(int d) { BodyHealthHUD.OFFSET_X = Math.max(0, BodyHealthHUD.OFFSET_X + d); }
    private void adjY(int d) { BodyHealthHUD.OFFSET_Y = Math.max(0, BodyHealthHUD.OFFSET_Y + d); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        this.renderBackground(g, mx, my, dt);

        int cx = this.width / 2, cy = this.height / 2;
        int pw = 260, ph = 240;
        int px = cx - pw/2, py = cy - ph/2;

        // Fond panneau
        g.fill(px,      py,      px+pw,   py+ph,   0xDD0D0D18);
        g.fill(px,      py,      px+pw,   py+1,    0xFF5566AA);
        g.fill(px,      py+ph-1, px+pw,   py+ph,   0xFF223366);
        g.fill(px,      py,      px+1,    py+ph,   0xFF5566AA);
        g.fill(px+pw-1, py,      px+pw,   py+ph,   0xFF223366);

        // Titre
        g.drawCenteredString(font, "Body Health - Reglages HUD", cx, py + 10, 0xFFFFFFFF);

        // Position
        int ay = cy - 30;
        g.drawCenteredString(font, "Position",          cx, ay - BH - 20, 0xFFAAAAAA);
        g.drawCenteredString(font, "X: " + BodyHealthHUD.OFFSET_X + "  Y: " + BodyHealthHUD.OFFSET_Y,
                cx, ay + BH + 6, 0xFFDDDDDD);

        // Taille
        int sy = cy + 55;
        g.drawCenteredString(font, "Taille",             cx, sy - BH - 20, 0xFFAAAAAA);
        g.drawCenteredString(font, "Scale: " + BodyHealthHUD.HUD_SCALE + " (1-6)",
                cx, sy + 6, 0xFFDDDDDD);

        // Point rouge position HUD
        int dotX = Math.max(8, Math.min(this.width  - 8, this.width  - 5 - BodyHealthHUD.OFFSET_X));
        int dotY = Math.max(8, Math.min(this.height - 8, this.height - 5 - BodyHealthHUD.OFFSET_Y));
        g.fill(dotX - 4, dotY - 4, dotX + 4, dotY + 4, 0xFFFF3333);
        g.drawString(font, "< HUD", dotX - 36, dotY - 4, 0xFFFF8888, true);

        super.render(g, mx, my, dt);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case 265 -> { adjY( STEP); return true; }
            case 264 -> { adjY(-STEP); return true; }
            case 263 -> { adjX( STEP); return true; }
            case 262 -> { adjX(-STEP); return true; }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
