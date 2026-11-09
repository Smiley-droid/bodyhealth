package com.bodyhealth.item;

import com.bodyhealth.BodyHealthMod;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(BodyHealthMod.MOD_ID);

    public static final DeferredItem<Item> BANDAGE =
            ITEMS.registerItem("bandage", BandageItem::new);
    public static final DeferredItem<Item> MEDICAL_KIT =
            ITEMS.registerItem("medical_kit", MedicalKitItem::new);
    public static final DeferredItem<Item> MORPHINE_SYRINGE =
            ITEMS.registerItem("morphine_syringe", MorphineSyringeItem::new);
    public static final DeferredItem<Item> SURVIVAL_GUIDE =
            ITEMS.registerItem("survival_guide", SurvivalGuideItem::new);
}
