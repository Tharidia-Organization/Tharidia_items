package com.THproject.tharidia_things.fatigue;

import com.THproject.tharidia_things.TharidiaThings;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registers the fatigue data attachment type
 */
public class FatigueAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = 
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TharidiaThings.MODID);
    
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FatigueData>> FATIGUE_DATA = 
        ATTACHMENT_TYPES.register("fatigue_data", () -> AttachmentType.serializable(FatigueData::new).build());
}
