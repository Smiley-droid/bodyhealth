package com.bodyhealth.item;

import com.bodyhealth.BodyHealthMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BodyHealthMod.MOD_ID);

    public static final Supplier<CreativeModeTab> BODY_HEALTH_TAB =
            CREATIVE_TABS.register("body_health_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.bodyhealth"))
                            .icon(() -> new ItemStack(ModItems.MEDICAL_KIT.get()))
                            .displayItems((params, output) -> {
                                output.accept(ModItems.SURVIVAL_GUIDE.get());
                                output.accept(ModItems.BANDAGE.get());
                                output.accept(ModItems.MEDICAL_KIT.get());
                                output.accept(ModItems.MORPHINE_SYRINGE.get());
                            })
                            .build()
            );
}
