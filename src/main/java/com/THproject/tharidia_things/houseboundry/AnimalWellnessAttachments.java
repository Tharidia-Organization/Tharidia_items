package com.THproject.tharidia_things.houseboundry;

import com.THproject.tharidia_things.TharidiaThings;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registers the animal wellness data attachment type.
 * This attachment can be applied to any LivingEntity (animals, modded creatures, etc.)
 */
public class AnimalWellnessAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TharidiaThings.MODID);

    /**
     * The wellness data attachment for animals.
     * Contains comfort, stress, hygiene stats and lifecycle information.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AnimalWellnessData>> WELLNESS_DATA =
        ATTACHMENT_TYPES.register("animal_wellness_data",
            () -> AttachmentType.serializable(AnimalWellnessData::new).build());
}
