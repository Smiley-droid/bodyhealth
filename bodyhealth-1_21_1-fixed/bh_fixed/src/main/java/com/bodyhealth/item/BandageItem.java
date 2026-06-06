package com.bodyhealth.item;

import com.bodyhealth.api.BodyHealthAPI;
import com.bodyhealth.common.BodyHealthData;
import com.bodyhealth.common.BodyPart;
import com.bodyhealth.common.FractureState;
import com.bodyhealth.network.SyncBodyHealthPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class BandageItem extends Item {

    private static final int   USE_DURATION  = 60;
    private static final float HEAL_AMOUNT   = 3.0f;

    public BandageItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
    @Override public int getUseDuration(ItemStack s, LivingEntity e) { return USE_DURATION; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        BodyHealthData data = BodyHealthAPI.getData(player);
        boolean needsHeal = false;
        for (BodyPart p : BodyPart.values())
            if (data.getHealth(p) < data.getMaxHealth(p)) { needsHeal = true; break; }

        if (!needsHeal) {
            player.displayClientMessage(Component.literal("§7Vous êtes en pleine santé."), true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;

        BodyHealthData data   = BodyHealthAPI.getData(player);
        BodyPart       target = getMostInjured(data);
        if (target == null) return stack;

        FractureState before = data.getFracture(target);
        // healForced améliore la fracture d'un cran
        data.healForced(target, HEAL_AMOUNT, true);
        FractureState after  = data.getFracture(target);

        String partName = getPartName(target);
        String fractureMsg = !before.equals(after)
                ? " §7(fracture : §a" + after.getDisplayName() + "§7)" : "";

        player.displayClientMessage(
            Component.literal("§a🩹 +" + HEAL_AMOUNT/2f + " ❤ §7→ §f"
                    + partName + fractureMsg), true);

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

    private String getPartName(BodyPart p) {
        return switch(p) {
            case HEAD->"Tête"; case TORSO->"Torse";
            case ARM_RIGHT->"Bras droit"; case ARM_LEFT->"Bras gauche";
            case LEG_RIGHT->"Jambe droite"; case LEG_LEFT->"Jambe gauche";
        };
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext c, List<Component> l, TooltipFlag f) {
        l.add(Component.literal("§7Soigne §a+" + HEAL_AMOUNT/2f + " ❤ §7sur la partie la plus blessée"));
        l.add(Component.literal("§7Améliore la fracture §fd'un cran"));
        l.add(Component.literal("§7Maintenir §eclic droit §73 secondes"));
    }
}

// NOTE : le tooltip dynamique est géré dans la classe ci-dessus via appendHoverText
// Les données ClientBodyHealthData sont accessibles uniquement côté client
// donc on les lit directement dans appendHoverText avec un check dist
