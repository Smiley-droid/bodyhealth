# Notes de portage — NeoForge 1.21.11

**Ce code n'a pas été compilé** (aucun accès aux dépôts Maven depuis l'environnement où il a été écrit). Les API ont été vérifiées une par une dans les sources de NeoForge (tests compilés par leur CI) et dans les guides de migration officiels. Lance `./gradlew build` puis `./gradlew runClient` et renvoie-moi les erreurs éventuelles.

## Construire
- JDK **21**, Gradle 9.2.1 (wrapper inclus). Visé : Minecraft 1.21.11, NeoForge 21.11.48.

## Config
- Serveur : `<monde>/serverconfig/bodyhealth-server.toml` ; client : `config/bodyhealth-client.toml` et `config/bodyhealth-hud.json`.

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

## Différences avec le port 26.x
- `GuiGraphics` (au lieu de `GuiGraphicsExtractor`), `render` / `renderBackground`, `drawString` / `drawCenteredString`.
- `BlockEvent.BreakEvent` (au lieu de `BreakBlockEvent`) et `LivingDamageEvent.Post#getNewDamage()` (au lieu de `getHealthDamage()`).

## Points que je n'ai pas pu confirmer
- `ItemUseAnimation.BOW` : cherchée à l'exécution, repli sur `EAT` (`UseAnimations.java`).
- `mc.level.getLevelData().isHardcore()` (couleur des cœurs en hardcore).
