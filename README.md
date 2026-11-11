# 🫀 Body Health System

**NeoForge 1.21.11 · v12.7.1**

Replaces Minecraft's single health bar with a localized injury system: every body part has its own HP, its own critical state and its own consequences.

## Features

- **6 independent body parts** — head, torso, both arms, both legs
- **Hybrid hit detection** — melee and projectile hits are resolved from the impact position; falls hit the legs, falling anvils/blocks hit the head, explosions are random, drowning/hunger/fire hit the torso
- **Critical effects** (inspired by [SrGnis/Body-Health-System](https://github.com/SrGnis/Body-Health-System)):
  - Head or torso critical → Weakness
  - Arms destroyed → Mining Fatigue
  - Legs destroyed → Slowness
  - Head critical → Nausea
  - Right arm destroyed → cannot break/place blocks
  - Left arm destroyed → drops the offhand item
  - Torso at 0 HP → death
- **Absorption** — golden apples and the Totem of Undying give yellow hearts on a specific body part
- **Configurable HUD** — three display modes (figure + hearts, names + hearts, names + health numbers); move it by dragging, resize it, all from the in-game menu (default key `H`, rebindable in Controls). Settings are saved in `config/bodyhealth-hud.json`
- **Persistent data** — stored in the player NBT *and* mirrored to `world/bodyhealth/<UUID>.json`
- **Full off switch** — `enabled = false` hands everything back to vanilla (health bar, food, potions, golden apples, totem)
- **Translations** — English and French are complete; item tooltips are translated in 19 languages (missing texts fall back to English)

## Medical items

| Item | Recipe | Effect |
|---|---|---|
| Bandage | Wool + String | +1.5 ❤ on the most injured part (3 s) |
| Medical Kit | Bandages + Chest + Gold | +3 ❤ on every part (5 s) |
| Morphine Syringe | Glass + Blaze Powder + Iron | Suppresses effects for 30 s — 3 injections in 2 min = overdose |
| Survival Guide | Book + Bone | Right-click to see your status and a quick reference |

## Configuration

**Server** — `bodyhealth-server.toml` (per world)

```toml
[general]
    enabled = true               # false = full vanilla behaviour
    actionBarMessages = true     # warnings above the hotbar when a part is critical/destroyed
    startingHearts = 3           # hearts per body part for new players (1-20)

[status_effects]
    enableNausea = true
    enableSlowness = true
    enableOffhandDrop = true
    enableBlockingDisable = true

[armor]
    enablePartialArmorSystem = true
    enableCrackedOverlay = true
    statusEffectThreshold = 2.0

[regeneration]
    enabled = true
    ratePerSecond = 0.02
    requireFood = true

[food]
    foodHealMultiplier = 0.4
```

**Client** — `bodyhealth-client.toml`

```toml
[text]
    compactGuide = false   # true = the Survival Guide only lists injured parts
```

To reduce on-screen text: set `compactGuide = true` (client) and/or `actionBarMessages = false` (server).

## Commands (operators)

```
/bodyhealth heal <players> [part]
/bodyhealth set <players> <part> <hp>
/bodyhealth setmax <players> <part|all> <hp>
/bodyhealth addhearts <players> <hearts>
/bodyhealth status <players>
/bodyhealth reset <players>
```

## Developer API

```java
float hp      = BodyHealthAPI.getHealth(player, BodyPart.HEAD);
boolean dead  = BodyHealthAPI.isDead(player, BodyPart.TORSO);

BodyHealthAPI.heal(player, BodyPart.HEAD, 4f);
BodyHealthAPI.fullHeal(player);
BodyHealthAPI.addMaxHealthAll(player, 4f);

// Every player — online now, or offline (applied at their next login)
BodyHealthAPI.applyToAll(data -> data.addMaxHealthAll(4f));
```

## Requirements

- Minecraft 1.21.11 (Java 21)
- NeoForge 21.11.48+

## Credits

- The Medical Kit texture and the critical-effect design come from [SrGnis/Body-Health-System](https://github.com/SrGnis/Body-Health-System) (MIT licence).
