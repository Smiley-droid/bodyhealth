# 🫀 Body Health System

**NeoForge 1.21.1 · v12.5.1**

Replaces Minecraft's vanilla health bar with a full localized injury system — every body part has its own HP pool, its own critical threshold, and its own consequences.

---

## ✨ Features

- **6 independent body parts** — Head, Torso, Right Arm, Left Arm, Right Leg, Left Leg
- **Hybrid hitbox system** — melee/projectile hits resolved by impact position; fall damage goes to legs, anvils to head, explosions randomized
- **Critical effects** (inspired by SrGnis Body-Health-System):
  - Head/Torso critical → Weakness
  - Arms destroyed → Mining Fatigue
  - Legs destroyed → Slowness
  - Head critical → Nausea
  - Right arm destroyed → Cannot break/place blocks
  - Left arm destroyed → Drops offhand item
  - Torso at 0 HP → Instant death
- **Absorption hearts** on specific body parts (golden apples, Notch apple, Totem)
- **Custom pixel-art HUD** (bottom-right, configurable with H key)
- **Double save** — NeoForge NBT attachment + JSON per player (`world/bodyhealth/<UUID>.json`)
- **Server enable/disable** — set `enabled = false` in config to fully restore vanilla behavior
- **19 languages** — EN, FR, DE, ES, PT, IT, NL, RU, UK, PL, CS, ZH, JA, KO, TR, AR

---

## 💊 Medical Items

| Item | Recipe | Effect |
|---|---|---|
| 🩹 Bandage | Wool + String | +1.5 ❤ on most injured part (3s) |
| 🏥 Medical Kit | Bandages + Chest + Gold | +3 ❤ on every part (5s) |
| 💉 Morphine Syringe | Glass + Blaze + Iron | Suppresses effects 30s ⚠️ Overdose ×3 |
| 📖 Survival Guide | Book + Bone | Right-click to view your current status |

---

## ⚙️ Server Config

Located in `world/serverconfig/bodyhealth-server.toml` after first launch.

```toml
[general]
    # Disable to fully restore vanilla health (no HUD, no body parts, no effects)
    enabled = true
    # Starting hearts per body part (default: 3 hearts = 6 HP)
    startingHearts = 3
    # Enable passive regeneration
    regenEnabled = true
    regenRate = 0.5
    regenRequireFood = true
```

---

## 🧰 Admin Commands

```
/bodyhealth heal <player> [part]          Heal a part or all parts
/bodyhealth set <player> <part> <hp>      Set HP directly
/bodyhealth setmax <player> <part|all> <value>  Set max HP
/bodyhealth addhearts <player> <n>        Add n hearts everywhere
/bodyhealth status <player>               Show full health status
/bodyhealth reset <player>                Reset to defaults
```

---

## 👨‍💻 Developer API

```java
// Read
float hp   = BodyHealthAPI.getHealth(player, BodyPart.HEAD);
boolean dead = BodyHealthAPI.isDead(player, BodyPart.TORSO);

// Write
BodyHealthAPI.heal(player, BodyPart.HEAD, 4f);
BodyHealthAPI.fullHeal(player);
BodyHealthAPI.addMaxHealthAll(player, 4f);

// All players (connected + offline)
BodyHealthAPI.applyToAll(data -> data.addMaxHealthAll(4f));
```

---

## 📋 Requirements

- Minecraft **1.21.1**
- NeoForge **21.1.172+**

---

## 📄 License

MIT — feel free to use in modpacks.
