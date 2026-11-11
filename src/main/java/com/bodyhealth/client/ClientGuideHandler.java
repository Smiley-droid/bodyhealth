package com.bodyhealth.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Logique client du Guide de Survie — isolée dans SON PROPRE fichier .class.
 *
 * POURQUOI CE FICHIER EXISTE :
 * Les lambdas Java sont compilées comme méthodes synthétiques DANS la classe
 * qui les contient. Donc même avec un FQCN (net.minecraft.client.Minecraft),
 * si OpenGuidePacket.handle() contenait un lambda utilisant Minecraft/LocalPlayer,
 * le bytecode de OpenGuidePacket.class contiendrait une référence constante à
 * LocalPlayer — et FML scanne CHAQUE classe chargée pour ces références,
 * peu importe si le code s'exécute réellement (RuntimeDistCleaner).
 *
 * En isolant ce code dans CE fichier séparé, OpenGuidePacket.class ne contient
 * plus aucune référence à Minecraft/LocalPlayer — seulement un appel
 * invokestatic vers ClientGuideHandler, une classe à nous. Ce fichier-ci n'est
 * chargé par la JVM que lorsque handle() est réellement APPELÉ, ce qui
 * n'arrive jamais sur un serveur dédié (playToClient = le serveur envoie,
 * jamais ne reçoit/exécute ce handler).
 */
@OnlyIn(Dist.CLIENT)
public class ClientGuideHandler {

    public static void handle(CompoundTag healthData) {
        ClientBodyHealthData.update(healthData);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            com.bodyhealth.item.SurvivalGuideItem.displayGuideClient(mc.player);
        }
    }
}
