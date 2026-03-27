package com.THproject.tharidia_things.compoundTag;

import java.util.function.Supplier;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class CookAttachments implements INBTSerializable<CompoundTag> {
    public static final int MAX_PROGRESS = 100;
    public static final int MAX_LEVEL = 5;
    
    private int progress = 0;
    private int level = 1;

    public void incrementProgress() {
        this.progress = Math.min(this.progress + 1, MAX_PROGRESS);
    }

    public void setProgress(int value) {
        this.progress = Math.clamp(value, 0, MAX_PROGRESS);
    }

    public int getProgress() {
        return this.progress;
    }

    public boolean levelUp() {
        if (this.level == MAX_LEVEL) return false;
        if (this.progress < MAX_PROGRESS) return false;
        
        this.level = Math.min(this.level + 1, MAX_LEVEL);
        this.progress = 0;
        return true;
    }

    public void setLevel(int value) {
        this.level = Math.clamp(value, 1, MAX_LEVEL);
    }

    public int getLevel() {
        return this.level;
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("progress", this.progress);
        nbt.putInt("level", this.level);
        return nbt;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        this.progress = nbt.getInt("progress");
        this.level = nbt.getInt("level");
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.ATTACHMENT_TYPES, TharidiaThings.MODID);

    public static final Supplier<AttachmentType<CookAttachments>> COOK_DATA = ATTACHMENT_TYPES
            .register("cook_data", () -> AttachmentType.serializable(CookAttachments::new).build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
