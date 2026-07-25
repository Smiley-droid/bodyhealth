package com.bodyhealth.common;

import com.bodyhealth.BodyHealthMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Data Attachment NeoForge 1.21.1
 * FIX BUG 6 : utilise le Codec défini dans BodyHealthData
 */
public class BodyHealthAttachment {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BodyHealthMod.MOD_ID);

    public static final Supplier<AttachmentType<BodyHealthData>> BODY_HEALTH =
            ATTACHMENT_TYPES.register("body_health", () ->
                    AttachmentType.builder(BodyHealthData::new)
                            .serialize(BodyHealthData.CODEC)
                            .copyOnDeath()
                            .build()
            );
}
