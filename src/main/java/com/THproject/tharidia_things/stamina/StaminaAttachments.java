package com.THproject.tharidia_things.stamina;

import com.THproject.tharidia_things.TharidiaThings;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class StaminaAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TharidiaThings.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<StaminaData>> STAMINA_DATA =
            ATTACHMENT_TYPES.register("stamina_data", () -> AttachmentType.serializable(StaminaData::new).build());
}

