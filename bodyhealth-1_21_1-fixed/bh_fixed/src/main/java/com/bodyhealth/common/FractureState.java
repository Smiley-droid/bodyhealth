package com.bodyhealth.common;

/**
 * État de fracture d'une partie du corps.
 *
 * NONE     → partie saine
 * SPRAINED → foulée (0-33% HP restant) — effets légers
 * BROKEN   → fracturée (0 HP) — effets sévères
 * SHATTERED→ broyée (0 HP + déjà brisée non soignée) — effets extrêmes
 *
 * Progression :
 *   Saine → Foulée (hp < 33%) → Brisée (hp = 0) → Broyée (brisée + re-blessée)
 *
 * Régression (soin) :
 *   Broyée → Brisée (kit médical uniquement)
 *   Brisée → Foulée (bandage ou kit)
 *   Foulée → Saine (regen naturelle suffit)
 */
public enum FractureState {
    NONE(0),
    SPRAINED(1),   // foulée
    BROKEN(2),     // fracturée
    SHATTERED(3);  // broyée

    private final int level;

    FractureState(int level) { this.level = level; }

    public int getLevel() { return level; }

    public boolean isAtLeast(FractureState other) {
        return this.level >= other.level;
    }

    public FractureState worsen() {
        return switch (this) {
            case NONE     -> SPRAINED;
            case SPRAINED -> BROKEN;
            case BROKEN   -> SHATTERED;
            case SHATTERED-> SHATTERED; // déjà au max
        };
    }

    public FractureState improve() {
        return switch (this) {
            case SHATTERED -> BROKEN;
            case BROKEN    -> SPRAINED;
            case SPRAINED  -> NONE;
            case NONE      -> NONE;
        };
    }

    public static FractureState fromLevel(int level) {
        for (FractureState s : values()) if (s.level == level) return s;
        return NONE;
    }

    /** Nom affiché dans le HUD / messages */
    public String getDisplayName() {
        return switch (this) {
            case NONE      -> "";
            case SPRAINED  -> "Foulée";
            case BROKEN    -> "Fracturée";
            case SHATTERED -> "Broyée";
        };
    }

    /** Couleur du label dans le HUD */
    public int getColor() {
        return switch (this) {
            case NONE      -> 0xFFDDDDDD;
            case SPRAINED  -> 0xFFFFAA00;
            case BROKEN    -> 0xFFFF4400;
            case SHATTERED -> 0xFFAA0000;
        };
    }
}
