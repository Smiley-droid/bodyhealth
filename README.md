# 🫀 Body Health System
### Mod Minecraft 1.21.1 — NeoForge v21.1.172

> Inspiré de RLCraft/Dissolution, chaque partie du corps possède sa propre barre de vie,
> ses effets de statut, et un système de fractures persistantes.

---

## 📋 Table des Matières

1. [Fonctionnalités](#fonctionnalités)
2. [Parties du Corps & Effets](#parties-du-corps--effets)
3. [Système de Fractures](#système-de-fractures)
4. [HUD — Interface](#hud--interface)
5. [Items de Soin](#items-de-soin)
6. [Système d'Armures](#système-darmures)
7. [Nourriture & Régénération](#nourriture--régénération)
8. [Sons](#sons)
9. [Configuration](#configuration)
10. [Commandes Admin](#commandes-admin)
11. [API Développeur](#api-développeur)
12. [Installation & Compilation](#installation--compilation)
13. [FAQ](#faq)

---

## ✨ Fonctionnalités

- **6 parties du corps** avec barre de vie indépendante (3 cœurs de départ)
- **Hitbox localisée** — les dégâts touchent la partie selon la position réelle de l'impact
- **Effets de statut uniques** selon la partie blessée
- **Système de fractures** — Foulée → Fracturée → Broyée
- **HUD custom** avec cœurs style vanilla (44 textures) + icônes os pour fractures
- **Corps pixel art** qui change d'état selon la santé globale
- **4 items de soin** : Bandage, Kit Médical, Seringue de Morphine, Guide de Survie
- **Sons de douleur** différents par partie du corps
- **Armures partielles** — chaque pièce protège sa partie du corps
- **Nourriture compatible tous mods** via FoodProperties automatique
- **Cœurs jaunes (Absorption)** attribués à une partie aléatoire
- **Totem d'immortalité** corrigé pour le système de santé
- **Commandes admin** complètes avec auto-complétion
- **API publique** pour intégration avec d'autres mods
- **Réseau optimisé** — dirty flag, sync lazy pour la regen

---

## 🩺 Parties du Corps & Effets

| Partie | Cœurs départ | Effet si blessée | Effet si critique/morte |
|--------|:-----------:|------------------|------------------------|
| 🧠 **Tête** | 3 ❤️ | Nausée 5 sec | Nausée permanente |
| 🫁 **Torse** | 3 ❤️ | — | **Mort du joueur** |
| 💪 **Bras droit** | 3 ❤️ | Mining Fatigue | Ne peut plus casser/poser de blocs |
| 🤚 **Bras gauche** | 3 ❤️ | — | Perd l'item offhand |
| 🦵 **Jambe droite** | 3 ❤️ | Slowness I | Slowness III |
| 🦵 **Jambe gauche** | 3 ❤️ | Slowness I | Slowness III |

### Hitbox Localisée

| Source de dégâts | Détection |
|------------------|-----------|
| Projectile (flèche, trident) | Position exacte de l'impact |
| Attaque mêlée | Hauteur des yeux de l'attaquant |
| Chute | Jambes (fallback logique) |
| Noyade | Tête (fallback logique) |
| Feu / Lave / Poison / Magie | Torse (fallback logique) |

---

## 🦴 Système de Fractures

Les fractures sont des états **persistants** qui s'accumulent et empirent sans soin.

| État | Déclencheur | Effets supplémentaires | Icône HUD |
|------|-------------|----------------------|-----------|
| **Foulée** | HP < 33% | Effets légers périodiques | 🦴 blanc |
| **Fracturée** | HP = 0 | Effets sévères permanents | 🦴🦴 orange |
| **Broyée** | 0 HP + déjà fracturée | Effets extrêmes | 🦴🦴🦴 rouge clignotant |

### Effets par partie et état

| Partie | Foulée | Fracturée | Broyée |
|--------|--------|-----------|--------|
| Tête | Nausée périodique | Nausée II + Cécité | Nausée + Cécité + Faiblesse permanent |
| Torse | Faiblesse I | Faiblesse II + Lenteur I | Faiblesse III + Lenteur II |
| Bras droit | Mining Fatigue I | Mining Fatigue III | Inutilisable totalement |
| Bras gauche | — | Drop offhand toutes les 10 sec | Offhand toujours vide |
| Jambes | Lenteur I | Lenteur II + Slow Falling | Lenteur IV + ne peut plus sauter |

### Guérison des fractures

| Item | Amélioration |
|------|-------------|
| 🩹 Bandage | +1 cran (Broyée → Fracturée, Fracturée → Foulée, Foulée → Saine) |
| 🏥 Kit Médical | +2 crans (seul moyen de guérir une partie Broyée efficacement) |
| Regen naturelle | Foulée → Saine si HP > 60% |

---

## 🖥️ HUD — Interface

Affiché en **bas à droite** de l'écran.

```
[Corps]  Tête     ❤❤❤░░  🦴
         Torse    ❤❤❤❤❤
         Bras D   ❤░░░░  🦴🦴
         Bras G   ❤❤❤░░  (cœurs jaunes si Absorption)
         Jambe D  ❤❤░░░  🦴
         Jambe G  ✗ mort 🦴🦴🦴
```

### Textures de cœurs (44 fichiers)

| État | Textures utilisées |
|------|--------------------|
| Normal | `full`, `half`, `container` |
| Poison | `poisoned_full`, `poisoned_half` |
| Wither | `withered_full`, `withered_half` |
| Gelé | `frozen_full`, `frozen_half` |
| Absorption | `absorbing_full`, `absorbing_half` (cœurs jaunes) |
| Hardcore | Variantes `hardcore_*` |
| Critique | Variantes `_blinking` |

### Corps pixel art (6 états)

| État | Seuil santé globale |
|------|---------------------|
| 🟢 Vert vif | > 85% |
| 🟡 Vert-jaune | 65–85% |
| 🟠 Orange-jaune | 45–65% |
| 🔴 Orange | 25–45% |
| 🔴 Rouge clignotant | > 0% |
| ⬛ Noir | 0% (mort) |

---

## 💊 Items de Soin

### 🩹 Bandage
- **Craft** : Laine blanche (×4) + Fil (×4) en damier → 4 bandages
- **Usage** : Maintenir clic droit 3 secondes
- **Effet** : +1.5 ❤ sur la partie la plus blessée + améliore fracture d'1 cran
- **Stack** : 16

### 🏥 Kit Médical
- **Craft** : 4 Bandages + 4 Lingots d'or + 1 Coffre → 1 kit
- **Usage** : Maintenir clic droit 5 secondes
- **Effet** : +3 ❤ sur TOUTES les parties + fractures améliorées de 2 crans
- **Stack** : 4 (rare)

### 💉 Seringue de Morphine
- **Craft** : 2 Vitres + Blaze Powder + Lingot de fer → 2 seringues
- **Usage** : Clic droit 1 seconde
- **Effet** : Supprime nausée/lenteur/faiblesse 30 sec + Résistance II
- **⚠️ Bloque toute régénération 30 sec**
- **☠️ Overdose** : 3 injections en 2 min → Nausée + Cécité + Poison + dégâts aux bras
- **Stack** : 8

### 📖 Guide de Survie
- **Craft** : Livre + Os → 1 guide (shapeless)
- **Usage** : Clic droit → affiche en chat l'état complet + guide
- **Stack** : 1

---

## 🛡️ Système d'Armures

| Pièce | Partie protégée |
|-------|----------------|
| Casque | Tête |
| Plastron | Torse (+ 50% pour les bras) |
| Jambières | Jambes |
| Bottes | Jambes (bonus anti-chute) |

Les mods d'armures tiers avec slots vanilla sont **détectés automatiquement**.
Pour les armures custom, utiliser `BodyArmorRegistry` (voir API).

---

## 🍖 Nourriture & Régénération

**Formule** : `(nutrition × 0.5 + saturation × 0.25) × multiplicateur_config`

Compatible avec **tous les mods de nourriture** via `FoodProperties` — Farmer's Delight,
Pam's HarvestCraft, Alex's Mobs, etc. Aucune configuration nécessaire.

| Source | Vitesse | Priorité |
|--------|---------|----------|
| 🍖 Nourriture | Lente (×0.4) | Torse d'abord, puis plus blessée |
| 💤 Regen passive | Très lente (0.02 HP/s) | Toutes les parties |
| 🧪 Potion Regen I | 0.5 HP / 2.5 sec | Partie la plus blessée |
| 🧪 Potion Regen II | 1.0 HP / 1.2 sec | Partie la plus blessée |
| 💛 Absorption | Variable | Partie **aléatoire** |
| 💉 Morphine active | **Bloquée** | — |

---

## 🔊 Sons

| Son | Déclencheur |
|-----|-------------|
| `hurt_head` | Tête touchée (pitch haut) |
| `hurt_torso` | Torse touché (pitch normal) |
| `hurt_arm` | Bras touché (pitch medium) |
| `hurt_leg` | Jambe touchée (pitch bas) |
| `fracture_crack` | Fracture apparue (os qui craque) |
| `fracture_shatter` | Partie broyée (son grave) |

---

## ⚙️ Configuration

Fichier : `config/bodyhealth-server.toml`

```toml
[general]
startingHearts = 3          # Cœurs de départ par partie

[status_effects]
enableNausea = true         # Nausée si tête blessée
enableSlowness = true       # Lenteur si jambes blessées
enableOffhandDrop = true    # Drop offhand si bras gauche à 0
enableBlockingDisable = true # Blocage interaction blocs bras droit

[armor]
enablePartialArmorSystem = true
enableCrackedOverlay = true
statusEffectThreshold = 2.0

[regeneration]
enabled = true
ratePerSecond = 0.02        # Très lente (1 cœur toutes les 50 sec)
requireFood = true          # Faim > 18 requise

[food]
foodHealMultiplier = 0.4    # Multiplicateur soin nourriture
```

---

## ⌨️ Commandes Admin

Niveau d'opérateur requis : **2**

```
/bodyhealth heal <joueur> [partie]
/bodyhealth set <joueur> <partie> <valeur>
/bodyhealth setmax <joueur> <partie> <valeur>
/bodyhealth addhearts <joueur> <nombre>
/bodyhealth fracture <joueur> <partie> <NONE|SPRAINED|BROKEN|SHATTERED>
/bodyhealth status <joueur>
/bodyhealth reset <joueur>
```

Parties valides : `head`, `torso`, `arm_right`, `arm_left`, `leg_right`, `leg_left`

---

## 🔧 API Développeur

### Dépendance Gradle

```groovy
dependencies {
    compileOnly "com.bodyhealth:bodyhealth:1.0.0"
}
```

### Exemples

```java
// Lire les HP
float hp = BodyHealthAPI.getHealth(player, BodyPart.HEAD);

// Augmenter les cœurs max
BodyHealthAPI.addMaxHealthAll(player, 4.0f); // +2 cœurs partout

// Soigner une partie
BodyHealthAPI.heal(player, BodyPart.TORSO, 3.0f);

// Enregistrer une armure custom
BodyArmorRegistry.register(MyItems.MY_HELMET, BodyPart.HEAD, 0.45f);

// Armure multi-parties
BodyArmorRegistry.registerMultiPart(MyItems.SUIT, Map.of(
    BodyPart.TORSO,     0.60f,
    BodyPart.ARM_LEFT,  0.30f,
    BodyPart.ARM_RIGHT, 0.30f
));
```

### Datapack (sans code Java)

```json
// data/bodyhealth/armor_compat/my_helmet.json
{
  "item": "mymod:special_helmet",
  "part": "HEAD",
  "reduction": 0.45
}
```

---

## 📦 Installation & Compilation

### Installation simple

1. Installer **NeoForge 21.1.172** pour Minecraft 1.21.1
2. Placer `bodyhealth-1.21.1-1.0.0.jar` dans `mods/`
3. Lancer Minecraft

### Compilation depuis les sources

**Prérequis** : Java 21, IntelliJ IDEA

```bash
# Ouvrir le dossier dans IntelliJ → Trust Project → attendre Gradle

# Tester en jeu
./gradlew runClient

# Compiler
./gradlew build
# → build/libs/bodyhealth-1.21.1-1.0.0.jar
```

---

## ❓ FAQ

**Q : La barre de vie vanilla est-elle supprimée ?**
> Oui, remplacée par le HUD custom.

**Q : Compatible multijoueur ?**
> Oui, sync serveur ↔ client via paquets réseau optimisés.

**Q : Les fractures persistent à la mort ?**
> Non, le respawn remet toutes les parties à pleine santé et efface les fractures.

**Q : Compatible avec les modpacks ?**
> Oui. Nourriture et armures de mods tiers sont détectées automatiquement.

**Q : Comment augmenter les cœurs via progression ?**
> `BodyHealthAPI.addMaxHealthAll(player, 2.0f)` ajoute 1 cœur à toutes les parties.
> Ou via commande : `/bodyhealth addhearts <joueur> 1`

---

## 📜 Licence

**MIT License** — Libre d'utilisation, modification et redistribution.

---

## 🐛 Bugs & Contributions

Ouvrir une issue sur le dépôt GitHub avec la version du mod et la liste des mods installés.
