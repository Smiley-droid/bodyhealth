package com.bodyhealth.item;

import com.bodyhealth.BodyHealthMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BodyHealthMod.MOD_ID);

    public static final RegistryObject<Item> BANDAGE          = ITEMS.register("bandage", BandageItem::new);
    public static final RegistryObject<Item> MEDICAL_KIT      = ITEMS.register("medical_kit", MedicalKitItem::new);
    public static final RegistryObject<Item> MORPHINE_SYRINGE = ITEMS.register("morphine_syringe", MorphineSyringeItem::new);
    public static final RegistryObject<Item> SURVIVAL_GUIDE   = ITEMS.register("survival_guide", SurvivalGuideItem::new);
}
