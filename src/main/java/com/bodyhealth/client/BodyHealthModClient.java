package com.bodyhealth.client;

import com.bodyhealth.BodyHealthMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Point d'entrée CLIENT. Cette classe n'est jamais chargée sur un serveur dédié
 * (@Mod avec dist = CLIENT), donc elle peut utiliser librement le code client.
 */
@Mod(value = BodyHealthMod.MOD_ID, dist = Dist.CLIENT)
public class BodyHealthModClient {
    public BodyHealthModClient(IEventBus modEventBus) {
        ClientInit.init(modEventBus);
    }
}
