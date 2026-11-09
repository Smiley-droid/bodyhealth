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

public class MedicalKitItem extends Item {

    private static final int   USE_DURATION  = 100;
    private static final float HEAL_PER_PART = 6.0f;

    public MedicalKitItem(Item.Properties props) {
        super(props.stacksTo(4));
    }

    @Override public ItemUseAnimation getUseAnimation(ItemStack s) { return UseAnimations.HEAL; }
    @Override public int getUseDuration(ItemStack s, LivingEntity e) { return USE_DURATION; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.displayClientMessage(Component.translatable("bodyhealth.msg.kit_start"), true);
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;

        BodyHealthData data      = BodyHealthAPI.getData(player);
        float          totalHeal = 0f;

        for (BodyPart part : BodyPart.values()) {
            float missing = data.getMaxHealth(part) - data.getHealth(part);
            if (missing > 0) {
                float actual = Math.min(HEAL_PER_PART, missing);
                data.heal(part, actual);
                totalHeal += actual;
            }
        }

        player.displayClientMessage(
            Component.translatable("bodyhealth.msg.kit_healed", String.format("%.1f", totalHeal / 2f)),
            true);

        PacketDistributor.sendToPlayer(player,
                new SyncBodyHealthPacket(data.serializeNBT()));

        stack.shrink(1);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext c, TooltipDisplay display, Consumer<Component> l, TooltipFlag f) {
        l.accept(Component.translatable("item.bodyhealth.medical_kit.desc1"));
        l.accept(Component.translatable("item.bodyhealth.medical_kit.desc2"));
    }
}
