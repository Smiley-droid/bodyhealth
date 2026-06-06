package com.bodyhealth.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import com.bodyhealth.network.OpenGuidePacket;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Enregistrement de tous les paquets réseau du mod.
 */
public class NetworkRegistry {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Paquet serveur → client : synchronisation des HP par partie
        registrar.playToClient(
                SyncBodyHealthPacket.TYPE,
                SyncBodyHealthPacket.CODEC,
                SyncBodyHealthPacket::handle
        );

        // BUG 5 FIX — OpenGuidePacket n'était pas enregistré → guide silencieux
        registrar.playToClient(
                OpenGuidePacket.TYPE,
                OpenGuidePacket.CODEC,
                OpenGuidePacket::handle
        );
    }
}
