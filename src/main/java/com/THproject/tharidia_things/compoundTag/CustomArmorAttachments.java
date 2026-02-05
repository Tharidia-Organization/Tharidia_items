package com.THproject.tharidia_things.compoundTag;

import java.util.function.Supplier;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class CustomArmorAttachments extends SimpleContainer implements INBTSerializable<CompoundTag> {

    public CustomArmorAttachments() {
        super(4); // 4 Custom Slots
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        ListTag tag = this.createTag(provider);
        nbt.put("Items", tag);
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (nbt.contains("Items")) {
            this.fromTag(nbt.getList("Items", 10), provider);
        }
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.ATTACHMENT_TYPES, TharidiaThings.MODID);

    public static final Supplier<AttachmentType<CustomArmorAttachments>> CUSTOM_ARMOR_DATA = ATTACHMENT_TYPES
            .register(
                    "custom_armor_data",
                    () -> AttachmentType.serializable(CustomArmorAttachments::new).build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
