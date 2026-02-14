package com.THproject.tharidia_things.compoundTag;

import java.util.function.Supplier;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
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
        ListTag listtag = new ListTag();
        for (int i = 0; i < this.getContainerSize(); ++i) {
            ItemStack itemstack = this.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.putByte("Slot", (byte) i);
                listtag.add(itemstack.save(provider, compoundtag));
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", listtag);
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (nbt.contains("Items")) {
            ListTag listtag = nbt.getList("Items", 10);
            this.clearContent();
            for (int i = 0; i < listtag.size(); ++i) {
                CompoundTag compoundtag = listtag.getCompound(i);
                int j = compoundtag.getByte("Slot") & 255;
                if (j >= 0 && j < this.getContainerSize()) {
                    this.setItem(j, ItemStack.parse(provider, compoundtag).orElse(ItemStack.EMPTY));
                }
            }
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
