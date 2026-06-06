package com.bodyhealth.events;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.config.BodyHealthConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bloque les interactions si le bras droit est à 0 HP (mort uniquement).
 *
 * FIX 3 — Avant ce fix, isCritical() (≤ 2 HP sur 6 = 33%) déclenchait
 * le blocage. Un joueur avec un bras à 2/6 HP ne pouvait plus rien faire.
 * Corrigé : blocage UNIQUEMENT sur isDead() (0 HP exact).
 * À critical, on affiche juste un avertissement sans bloquer.
 */
public class BlockInteractionHandler {

    // Cooldown messages pour ne pas spammer l'action bar
    private final Map<UUID, Long> lastMsgTime = new HashMap<>();
    private static final long MSG_COOLDOWN_MS = 2000L;

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!BodyHealthConfig.ENABLE_BLOCKING_DISABLE.get()) return;

        BodyHealthData data = BodyHealthAPI.getData(player);

        if (data.isDead(BodyPart.ARM_RIGHT)) {
            event.setCanceled(true);
            sendMessage(player, "§c☠ Bras droit détruit — impossible de casser des blocs.");
        } else if (data.isCritical(BodyPart.ARM_RIGHT)) {
            // Avertissement seulement, pas de blocage
            sendMessage(player, "§e⚠ Bras droit critique — agissez rapidement !");
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!BodyHealthConfig.ENABLE_BLOCKING_DISABLE.get()) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;

        BodyHealthData data = BodyHealthAPI.getData(player);

        if (data.isDead(BodyPart.ARM_RIGHT)) {
            event.setCanceled(true);
            sendMessage(player, "§c☠ Bras droit détruit — impossible de poser des blocs.");
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!BodyHealthConfig.ENABLE_BLOCKING_DISABLE.get()) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;

        BodyHealthData data = BodyHealthAPI.getData(player);

        if (data.isDead(BodyPart.ARM_RIGHT)) {
            event.setCanceled(true);
            sendMessage(player, "§c☠ Bras droit détruit — impossible d'utiliser cet objet.");
        }
    }

    private void sendMessage(ServerPlayer player, String msg) {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUUID();
        if (now - lastMsgTime.getOrDefault(uuid, 0L) < MSG_COOLDOWN_MS) return;
        lastMsgTime.put(uuid, now);
        player.displayClientMessage(Component.literal(msg), true);
    }
}
