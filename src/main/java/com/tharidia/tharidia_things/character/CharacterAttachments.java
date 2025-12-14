package com.tharidia.tharidia_things.character;

import com.tharidia.tharidia_things.TharidiaThings;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registers the character creation data attachment type
 */
public class CharacterAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = 
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TharidiaThings.MODID);
    
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CharacterData>> CHARACTER_DATA = 
        ATTACHMENT_TYPES.register("character_data", () -> AttachmentType.serializable(CharacterData::new).build());
}
