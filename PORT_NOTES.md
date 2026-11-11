# Notes de portage — NeoForge 26.1 / 26.2 / 26.3

**Ce code n'a pas été compilé** (aucun accès aux dépôts Maven depuis l'environnement où il a été écrit). Les API ont été vérifiées une par une dans les sources de NeoForge (tests compilés par leur CI) et dans les guides de migration officiels. Lance `./gradlew build` puis `./gradlew runClient` et renvoie-moi les erreurs éventuelles.

## Construire
- JDK **25** (Gradle le télécharge via le toolchain), Gradle 9.2.1 (wrapper inclus).
- Par défaut le projet vise **26.3** (`gradle.properties`). Pour 26.1 ou 26.2, remplace les lignes `minecraft_version`, `minecraft_version_range` et `neo_version` par celles de `versions/26.1.properties` ou `versions/26.2.properties`. Le même code source sert aux trois ; le jar est nommé `bodyhealth-mc<version>-12.7.1.jar`.
- **26.1 et 26.2 n'ont pas été comparés ligne à ligne** avec la 26.3 : les API utilisées par le mod sont identiques dans les trois branches de NeoForge, sauf le nom du type de config serveur (`SERVER` en 26.1 et 26.2, `SYNCED` en 26.3), choisi automatiquement à l'exécution.

## Config
- Serveur, 26.3 : `config/bodyhealth-synced.toml` (surcharge par monde dans `<monde>/syncedconfig/`).
- Serveur, 26.1 et 26.2 : `<monde>/serverconfig/bodyhealth-server.toml`.
- Client : `config/bodyhealth-client.toml` et `config/bodyhealth-hud.json`.

## Ce qui a changé par rapport à la version 1.21.1

- **Build** : plugin ModDevGradle 2.0.148, Gradle 9.2.1 (fichiers repris du kit officiel). `neoforge.mods.toml` est généré depuis `src/main/templates/`.
- **Objets** : `use()` renvoie `InteractionResult`, `appendHoverText` reçoit un `TooltipDisplay` et un `Consumer`, `UseAnim` devient `ItemUseAnimation`, les `Item.Properties` viennent du registre (`registerItem`). Ajout de `assets/bodyhealth/items/*.json` (obligatoire depuis la 1.21.4).
- **Recettes** : ingrédients en simples chaînes, résultat `{ "id": ... }`.
- **Données NBT** : les getters de `CompoundTag` renvoient des `Optional`. Le format de sauvegarde du joueur est inchangé (`MapCodec.assumeMapUnsafe`).
- **Joueurs hors ligne** : `BodyHealthAPI.applyToAll` modifie le JSON `world/data/bodyhealth/<UUID>.json` du joueur, appliqué à sa prochaine connexion (le format du dossier `playerdata` a changé : il n'est plus utilisé).
- **Effets** : `NAUSEA`, `SLOWNESS`, `RESISTANCE`, `MINING_FATIGUE`.
- **Divers** : `ResourceLocation` devient `Identifier`, `kill(ServerLevel)`, `lookupOrThrow` / `getOrThrow`, permissions des commandes via `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`.
- **Client** : touche H avec `KeyMapping.Category`, écrans avec `MouseButtonEvent` / `KeyEvent`, pile 2D `pushMatrix` / `popMatrix`, `blit` et `blitSprite` avec `RenderPipelines.GUI_TEXTURED`, point d'entrée `@Mod(dist = CLIENT)`.
- `@OnlyIn` retiré : il ne retire plus rien au chargement dans ces versions.

## Points que je n'ai pas pu confirmer
- `ItemUseAnimation.BOW` : cherchée à l'exécution, repli sur `EAT` (`UseAnimations.java`).
- `mc.level.getLevelData().isHardcore()` (couleur des cœurs en hardcore).
- Le totem d'immortalité est devenu un composant de données en 1.21.2 : mon gestionnaire annule la mort avant, comme auparavant, mais je n'ai pas pu le tester.
