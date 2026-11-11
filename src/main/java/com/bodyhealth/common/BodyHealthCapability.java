package com.bodyhealth.common;

import com.bodyhealth.BodyHealthMod;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Données Body Health attachées à chaque joueur (capacité Forge).
 * Équivalent de l'attachement NeoForge : sauvegardées dans le NBT du joueur et recopiées au respawn.
 */
public class BodyHealthCapability {

    public static final Capability<BodyHealthData> BODY_HEALTH =
            CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(BodyHealthMod.MOD_ID, "body_health");

    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player)
            event.addCapability(ID, new Provider());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        try {
            event.getOriginal().getCapability(BODY_HEALTH).ifPresent(old ->
                event.getEntity().getCapability(BODY_HEALTH).ifPresent(neu ->
                    neu.deserializeNBT(old.serializeNBT())));
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    private static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final BodyHealthData data = new BodyHealthData();
        private final LazyOptional<BodyHealthData> optional = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return BODY_HEALTH.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
            data.deserializeNBT(tag);
        }
    }
}
