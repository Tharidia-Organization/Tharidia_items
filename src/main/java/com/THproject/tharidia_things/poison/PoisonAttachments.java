package com.THproject.tharidia_things.poison;

import java.util.function.Supplier;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.poison.PoisonHelper.PoisonType;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class PoisonAttachments implements INBTSerializable<CompoundTag> {
    

    // Handle screen effects on client
    private float poisonProgress = 0.0f;

    private PoisonType poisonType = PoisonType.NONE;
    private long poisonTime = -1L;

    public void setPoisoned(PoisonType type) {
        if (isPoisoned(PoisonType.SOFT) && type == PoisonType.HARD) {
            this.poisonType = type;
            this.poisonTime = System.currentTimeMillis();
        } else if (!isPoisoned()) {
            this.poisonType = type;
            this.poisonTime = System.currentTimeMillis();
        }
    }

    public PoisonType getPoisonType() {
        return this.poisonType;
    }

    public boolean isPoisoned(PoisonType type) {
        return this.poisonType == type;
    }

    public boolean isPoisoned() {
        return this.poisonType != PoisonType.NONE;
    }

    public void removePoison() {
        this.poisonType = PoisonType.NONE;
        this.poisonTime = -1L;
    }

    public void setProgress(float progress) {
        this.poisonProgress = Math.min(progress, 1.0f);
    }

    public long getPoisonTime() {
        return this.poisonTime;
    }

    public float getProgress() {
        return Math.min(this.poisonProgress, 1.0f);
    }

    public void copyFrom(PoisonAttachments other) {
        this.poisonProgress = other.getProgress();
        this.poisonTime = other.getPoisonTime();
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("PoisonProgress", this.getProgress());
        nbt.putString("PoisonType", getPoisonType().toString());
        return nbt;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        this.setProgress(nbt.getFloat("PoisonProgress"));
        this.setPoisoned(PoisonType.valueOf(nbt.getString("PoisonType")));
    }

    public static final Supplier<AttachmentType<PoisonAttachments>> POISON = TharidiaThings.ATTACHMENT_TYPES
            .register("poison", () -> AttachmentType.serializable(PoisonAttachments::new).build());

    public static void register() {
    }
}
