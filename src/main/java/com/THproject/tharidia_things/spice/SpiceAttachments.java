package com.THproject.tharidia_things.spice;

import com.THproject.tharidia_things.TharidiaThings;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class SpiceAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TharidiaThings.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerSpiceData>> PLAYER_SPICE_DATA =
            ATTACHMENT_TYPES.register("player_spice_data",
                    () -> AttachmentType.serializable(PlayerSpiceData::new).build());
}
