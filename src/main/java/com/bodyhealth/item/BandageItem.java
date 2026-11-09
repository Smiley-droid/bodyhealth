package com.bodyhealth.item;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.network.SyncBodyHealthPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class BandageItem extends Item {

    private static final int   USE_DURATION  = 60;
    private static final float HEAL_AMOUNT   = 3.0f;

    public BandageItem(Item.Properties props) {
        super(props.stacksTo(16));
    }

    @Override public ItemUseAnimation getUseAnimation(ItemStack s) { return UseAnimations.HEAL; }
    @Override public int getUseDuration(ItemStack s, LivingEntity e) { return USE_DURATION; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BodyHealthData data = BodyHealthAPI.getData(player);
        boolean needsHeal = false;
        for (BodyPart p : BodyPart.values())
            if (data.getHealth(p) < data.getMaxHealth(p)) { needsHeal = true; break; }

        if (!needsHeal) {
            player.displayClientMessage(Component.translatable("bodyhealth.msg.full_health"), true);
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;

        BodyHealthData data   = BodyHealthAPI.getData(player);
        BodyPart       target = getMostInjured(data);
        if (target == null) return stack;

        data.heal(target, HEAL_AMOUNT);

        player.displayClientMessage(
            Component.translatable("bodyhealth.msg.bandage_healed", String.valueOf(HEAL_AMOUNT / 2f),
                target.displayName().withStyle(net.minecraft.ChatFormatting.WHITE)), true);

        PacketDistributor.sendToPlayer(player,
                new SyncBodyHealthPacket(data.serializeNBT()));
        stack.shrink(1);
        return stack;
    }

    private BodyPart getMostInjured(BodyHealthData data) {
        BodyPart worst = null; float worstPct = 1.0f;
        BodyPart[] priority = { BodyPart.TORSO, BodyPart.HEAD,
                BodyPart.ARM_RIGHT, BodyPart.ARM_LEFT,
                BodyPart.LEG_RIGHT, BodyPart.LEG_LEFT };
        for (BodyPart p : priority) {
            float pct = data.getHealthPercent(p);
            if (pct < worstPct) { worstPct = pct; worst = p; }
        }
        return worstPct >= 1.0f ? null : worst;
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext c, TooltipDisplay display, Consumer<Component> l, TooltipFlag f) {
        l.accept(Component.translatable("item.bodyhealth.bandage.desc1"));
        l.accept(Component.translatable("item.bodyhealth.bandage.desc2"));
    }
}
