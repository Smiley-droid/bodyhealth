package com.bodyhealth.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HudSettingsScreen extends Screen {

    private static final int STEP  = 5;
    private static final int BW    = 24;  // bouton flèche largeur
    private static final int BH    = 20;  // bouton flèche hauteur

    public HudSettingsScreen() {
        super(Component.literal("Body Health — Réglages HUD"));
    }

    @Override
    protected void init() {
        int cx = this.width  / 2;
        int cy = this.height / 2;

        // ── Flèches position (groupe centré) ─────────────────────────────────
        int ax = cx;      // centre X des flèches
        int ay = cy - 40; // centre Y des flèches

        // ▲ haut
        addArrow("▲", ax - BW/2, ay - BH - 2, b -> { BodyHealthHUD.OFFSET_Y += STEP; });
        // ▼ bas
        addArrow("▼", ax - BW/2, ay + 2,       b -> { BodyHealthHUD.OFFSET_Y = Math.max(0, BodyHealthHUD.OFFSET_Y - STEP); });
        // ◄ gauche (déplace vers la gauche = augmente offset droite)
        addArrow("◄", ax - BW - 26, ay - BH/2, b -> { BodyHealthHUD.OFFSET_X += STEP; });
        // ► droite
        addArrow("►", ax + 26,      ay - BH/2, b -> { BodyHealthHUD.OFFSET_X = Math.max(0, BodyHealthHUD.OFFSET_X - STEP); });

        // ── Boutons taille ────────────────────────────────────────────────────
        int sy = cy + 40;
        addArrow("−", cx - BW - 4, sy - BH/2, b -> { BodyHealthHUD.HUD_SCALE = Math.max(1, BodyHealthHUD.HUD_SCALE - 1); });
        addArrow("+", cx + 4,      sy - BH/2, b -> { BodyHealthHUD.HUD_SCALE = Math.min(6, BodyHealthHUD.HUD_SCALE + 1); });

        // ── Réinitialiser ─────────────────────────────────────────────────────
        this.addRenderableWidget(Button.builder(
                Component.literal("Réinitialiser"),
                b -> { BodyHealthHUD.OFFSET_X = 5; BodyHealthHUD.OFFSET_Y = 5; BodyHealthHUD.HUD_SCALE = 3; })
                .bounds(cx - 105, cy + 80, 100, 20).build());

        // ── Fermer ────────────────────────────────────────────────────────────
        this.addRenderableWidget(Button.builder(
                Component.literal("Fermer"),
                b -> this.onClose())
                .bounds(cx + 5, cy + 80, 100, 20).build());
    }

    private void addArrow(String label, int x, int y, Button.OnPress action) {
        this.addRenderableWidget(
            Button.builder(Component.literal(label), action)
                  .bounds(x, y, BW, BH)
                  .build());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        // Fond
        this.renderBackground(gui, mouseX, mouseY, delta);

        int cx = this.width  / 2;
        int cy = this.height / 2;

        // ── Fond panneau ──────────────────────────────────────────────────────
        int pw = 260, ph = 230;
        int px = cx - pw/2, py = cy - ph/2;
        gui.fill(px, py, px+pw, py+ph, 0xCC101018);
        gui.fill(px,    py,    px+pw, py+1,    0xFF5A5A8A); // bord haut
        gui.fill(px,    py+ph-1, px+pw, py+ph, 0xFF252535); // bord bas
        gui.fill(px,    py,    px+1,   py+ph,  0xFF5A5A8A); // bord gauche
        gui.fill(px+pw-1, py,  px+pw, py+ph,  0xFF252535); // bord droit

        // ── Titre ─────────────────────────────────────────────────────────────
        gui.drawCenteredString(this.font, "Body Health — Réglages HUD",
                cx, py + 10, 0xFFFFFFFF);

        // ── Section Position ──────────────────────────────────────────────────
        int ay = cy - 40;
        gui.drawCenteredString(this.font, "Position", cx, ay - BH - 18, 0xFFAAAAAA);
        gui.drawCenteredString(this.font,
                "X: " + BodyHealthHUD.OFFSET_X + "   Y: " + BodyHealthHUD.OFFSET_Y,
                cx, ay + BH + 6, 0xFFDDDDDD);

        // ── Section Taille ────────────────────────────────────────────────────
        int sy = cy + 40;
        gui.drawCenteredString(this.font, "Taille", cx, sy - BH - 16, 0xFFAAAAAA);
        gui.drawCenteredString(this.font,
                "Scale: " + BodyHealthHUD.HUD_SCALE + "  (1 - 6)",
                cx, sy + 6, 0xFFDDDDDD);

        // ── Aperçu — point rouge position HUD ────────────────────────────────
        int dotX = this.width  - 5 - BodyHealthHUD.OFFSET_X;
        int dotY = this.height - 5 - BodyHealthHUD.OFFSET_Y;
        // Clamp dans l'écran
        dotX = Math.max(10, Math.min(dotX, this.width  - 10));
        dotY = Math.max(10, Math.min(dotY, this.height - 10));
        gui.fill(dotX - 4, dotY - 4, dotX + 4, dotY + 4, 0xFFFF3333);
        gui.drawString(this.font, "HUD", dotX - 22, dotY - 4, 0xFFFF8888, true);

        super.render(gui, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case 265 -> { BodyHealthHUD.OFFSET_Y += STEP;                                               return true; } // Up
            case 264 -> { BodyHealthHUD.OFFSET_Y = Math.max(0, BodyHealthHUD.OFFSET_Y - STEP);         return true; } // Down
            case 263 -> { BodyHealthHUD.OFFSET_X += STEP;                                               return true; } // Left
            case 262 -> { BodyHealthHUD.OFFSET_X = Math.max(0, BodyHealthHUD.OFFSET_X - STEP);         return true; } // Right
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
