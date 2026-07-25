package com.bodyhealth.command;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.network.BodyHealthSyncManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class BodyHealthCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bodyhealth")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("heal")
                    .then(Commands.argument("player", EntityArgument.players())
                        .executes(ctx -> healAll(ctx.getSource(),
                                EntityArgument.getPlayers(ctx, "player")))
                        .then(Commands.argument("part", StringArgumentType.word())
                            .suggests((ctx, b) -> suggestParts(b))
                            .executes(ctx -> healPart(ctx.getSource(),
                                    EntityArgument.getPlayers(ctx, "player"),
                                    StringArgumentType.getString(ctx, "part"))))))

                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.players())
                        .then(Commands.argument("part", StringArgumentType.word())
                            .suggests((ctx, b) -> suggestParts(b))
                            .then(Commands.argument("value", FloatArgumentType.floatArg(0))
                                .executes(ctx -> setHealth(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "player"),
                                        StringArgumentType.getString(ctx, "part"),
                                        FloatArgumentType.getFloat(ctx, "value")))))))

                .then(Commands.literal("setmax")
                    .then(Commands.argument("player", EntityArgument.players())
                        // Variante avec partie spécifique
                        .then(Commands.argument("part", StringArgumentType.word())
                            .suggests((ctx, b) -> suggestParts(b))
                            .then(Commands.argument("value", FloatArgumentType.floatArg(2))
                                .executes(ctx -> setMaxHealth(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "player"),
                                        StringArgumentType.getString(ctx, "part"),
                                        FloatArgumentType.getFloat(ctx, "value")))))
                        // Variante "all" : toutes les parties en une commande
                        // ex: /bodyhealth setmax @a all 8
                        .then(Commands.literal("all")
                            .then(Commands.argument("value", FloatArgumentType.floatArg(2))
                                .executes(ctx -> setMaxHealthAll(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "player"),
                                        FloatArgumentType.getFloat(ctx, "value")))))))

                .then(Commands.literal("addhearts")
                    .then(Commands.argument("player", EntityArgument.players())
                        .then(Commands.argument("hearts", FloatArgumentType.floatArg(0))
                            .executes(ctx -> addHearts(ctx.getSource(),
                                    EntityArgument.getPlayers(ctx, "player"),
                                    FloatArgumentType.getFloat(ctx, "hearts"))))))

                .then(Commands.literal("status")
                    .then(Commands.argument("player", EntityArgument.players())
                        .executes(ctx -> showStatus(ctx.getSource(),
                                EntityArgument.getPlayers(ctx, "player")))))

                .then(Commands.literal("reset")
                    .then(Commands.argument("player", EntityArgument.players())
                        .executes(ctx -> reset(ctx.getSource(),
                                EntityArgument.getPlayers(ctx, "player")))))
        );
    }

    // ── Commandes ─────────────────────────────────────────────────────────────

    private static int healAll(CommandSourceStack src, Collection<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            BodyHealthAPI.fullHeal(p);
            src.sendSuccess(() -> Component.literal(
                "§a[BodyHealth] §f" + p.getName().getString() + " soigné entièrement"), true);
        }
        return players.size();
    }

    private static int healPart(CommandSourceStack src,
                                 Collection<ServerPlayer> players, String partStr) {
        BodyPart part = parsePart(partStr);
        if (part == null) {
            src.sendFailure(Component.literal("§cPartie inconnue : " + partStr));
            return 0;
        }
        for (ServerPlayer p : players) {
            // heal() clampe déjà au max en interne — pas besoin de calculer le manque
            BodyHealthAPI.heal(p, part, Float.MAX_VALUE / 2f);
        }
        src.sendSuccess(() -> Component.literal("§a[BodyHealth] §f" + partStr + " soigné à fond"), true);
        return players.size();
    }

    private static int setHealth(CommandSourceStack src, Collection<ServerPlayer> players,
                                  String partStr, float value) {
        BodyPart part = parsePart(partStr);
        if (part == null) { src.sendFailure(Component.literal("§cPartie inconnue")); return 0; }
        for (ServerPlayer p : players) BodyHealthAPI.setHealth(p, part, value);
        src.sendSuccess(() -> Component.literal(
            "§a[BodyHealth] §f" + partStr + " → " + value + " HP"), true);
        return players.size();
    }

    private static int setMaxHealthAll(CommandSourceStack src, Collection<ServerPlayer> players,
                                        float value) {
        for (ServerPlayer p : players) {
            BodyHealthData data = BodyHealthAPI.getData(p);
            for (BodyPart part : BodyPart.values())
                data.setMaxHealth(part, value);
            BodyHealthSyncManager.markDirtyAndFlush(p, data); // 1 sync par joueur, pas 6
        }
        src.sendSuccess(() -> Component.literal(
            "§a[BodyHealth] §fMax toutes parties → " + value + " HP (" + (value/2f) + " ❤)"), true);
        return players.size();
    }

    private static int setMaxHealth(CommandSourceStack src, Collection<ServerPlayer> players,
                                     String partStr, float value) {
        BodyPart part = parsePart(partStr);
        if (part == null) { src.sendFailure(Component.literal("§cPartie inconnue")); return 0; }
        for (ServerPlayer p : players) BodyHealthAPI.setMaxHealth(p, part, value);
        src.sendSuccess(() -> Component.literal(
            "§a[BodyHealth] §fMax " + partStr + " → " + value), true);
        return players.size();
    }

    private static int addHearts(CommandSourceStack src, Collection<ServerPlayer> players, float hearts) {
        for (ServerPlayer p : players) BodyHealthAPI.addMaxHealthAll(p, hearts * 2f);
        src.sendSuccess(() -> Component.literal(
            "§a[BodyHealth] §f+" + hearts + " ❤ partout"), true);
        return players.size();
    }

    private static int showStatus(CommandSourceStack src, Collection<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            BodyHealthData data = BodyHealthAPI.getData(p);
            src.sendSuccess(() -> Component.literal(
                "§e[BodyHealth] §f" + p.getName().getString()), false);
            for (BodyPart part : BodyPart.values()) {
                float hp     = data.getHealth(part);
                float max    = data.getMaxHealth(part);
                float absorp = data.getAbsorption(part);
                boolean dead = data.isDead(part);
                boolean crit = data.isCritical(part);

                String stateTag = dead ? " §4[DÉTRUITE]"
                                 : crit ? " §c[CRITIQUE]"
                                 : "";
                String absorpTag = absorp > 0 ? " §e+" + absorp + " abs" : "";

                src.sendSuccess(() -> Component.literal(
                    "  §7" + part.getId() + ": §f" + hp + "/" + max
                    + absorpTag + stateTag), false);
            }
        }
        return players.size();
    }

    private static int reset(CommandSourceStack src, Collection<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            BodyHealthData data = BodyHealthAPI.getData(p);
            for (BodyPart part : BodyPart.values()) {
                data.setHealth(part, part.getDefaultMaxHealth());
                data.setMaxHealth(part, part.getDefaultMaxHealth());
                data.setAbsorption(part, 0f);
            }
            p.setAbsorptionAmount(0f); // clear aussi la valeur vanilla affichée
            BodyHealthSyncManager.markDirtyAndFlush(p, data);
        }
        src.sendSuccess(() -> Component.literal("§a[BodyHealth] §fRéinitialisé"), true);
        return players.size();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static BodyPart parsePart(String s) {
        for (BodyPart p : BodyPart.values())
            if (p.getId().equalsIgnoreCase(s) || p.name().equalsIgnoreCase(s)) return p;
        return null;
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
            suggestParts(SuggestionsBuilder b) {
        for (BodyPart p : BodyPart.values()) b.suggest(p.getId());
        return b.buildFuture();
    }
}
