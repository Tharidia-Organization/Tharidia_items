package com.THproject.tharidia_things.poison;

import java.util.function.Supplier;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class PoisonAttachments implements INBTSerializable<CompoundTag> {

    // Handle screen effects on client
    private float softProgress = 0.0f;
    private float hardProgress = 0.0f;

    private boolean isSoftPoisoned = false;
    private boolean isHardPoisoned = false;
    private long softPoisonTime = -1L;
    private long hardPoisonTime = -1L;

    public void setSoftPoisoned() {
        this.isSoftPoisoned = true;
        this.softPoisonTime = System.currentTimeMillis();
    }

    public void setHardPoisoned() {
        this.isHardPoisoned = true;
        this.hardPoisonTime = System.currentTimeMillis();
    }

    public void removeSoftPoison() {
        this.isSoftPoisoned = false;
        this.softPoisonTime = -1L;
    }

    public void removeHardPoison() {
        this.isHardPoisoned = false;
        this.hardPoisonTime = -1L;
    }

    public boolean isSoftPoisoned() {
        return this.isSoftPoisoned;
    }

    public boolean isHardPoisoned() {
        return this.isHardPoisoned;
    }

    public void setSoftProgress(float progress) {
        this.softProgress = progress;
    }

    public void setHardProgress(float progress) {
        this.hardProgress = progress;
    }

    public long getSoftPoisonTime() {
        return this.softPoisonTime;
    }

    public long getHardPoisonTime() {
        return this.hardPoisonTime;
    }

    public float getSoftProgress() {
        return this.softProgress;
    }

    public float getHardProgress() {
        return this.hardProgress;
    }

    public void copyFrom(PoisonAttachments other) {
        this.softProgress = other.softProgress;
        this.hardProgress = other.hardProgress;
        this.isSoftPoisoned = other.isSoftPoisoned;
        this.isHardPoisoned = other.isHardPoisoned;
        this.softPoisonTime = other.softPoisonTime;
        this.hardPoisonTime = other.hardPoisonTime;
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("softProgress", this.softProgress);
        nbt.putFloat("hardProgress", this.hardProgress);
        nbt.putBoolean("isSoftPoisoned", this.isSoftPoisoned);
        nbt.putBoolean("isHardPoisoned", this.isHardPoisoned);
        nbt.putLong("softPoisonTime", this.softPoisonTime);
        nbt.putLong("hardPoisonTime", this.hardPoisonTime);
        return nbt;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        this.softProgress = nbt.getFloat("softProgress");
        this.hardProgress = nbt.getFloat("hardProgress");
        this.isSoftPoisoned = nbt.getBoolean("isSoftPoisoned");
        this.isHardPoisoned = nbt.getBoolean("isHardPoisoned");
        this.softPoisonTime = nbt.getLong("softPoisonTime");
        this.hardPoisonTime = nbt.getLong("hardPoisonTime");
    }

    public static final Supplier<AttachmentType<PoisonAttachments>> POISON = TharidiaThings.ATTACHMENT_TYPES
            .register("poison", () -> AttachmentType.serializable(PoisonAttachments::new).build());

    public static void register() {
    }
}
