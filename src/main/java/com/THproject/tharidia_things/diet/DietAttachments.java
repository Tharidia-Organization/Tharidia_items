package com.THproject.tharidia_things.diet;

import com.THproject.tharidia_things.TharidiaThings;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registers the diet data attachment for player entities.
 */
public class DietAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TharidiaThings.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DietData>> DIET_DATA =
            ATTACHMENT_TYPES.register("diet_data", () -> AttachmentType.serializable(DietData::new).build());
}
