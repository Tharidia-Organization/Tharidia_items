package com.THproject.tharidia_things.stamina;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public record StaminaModifier(
        StaminaModifierType type,
        float value,
        boolean isPercentage,
        String source,
        int priority
) {
    public static StaminaModifier fromTag(CompoundTag tag) {
        StaminaModifierType type = StaminaModifierType.valueOf(tag.getString("Type"));
        float value = tag.getFloat("Value");
        boolean isPercentage = tag.getBoolean("IsPercentage");
        String source = tag.getString("Source");
        int priority = tag.getInt("Priority");
        return new StaminaModifier(type, value, isPercentage, source, priority);
    }

    public CompoundTag toTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name());
        tag.putFloat("Value", value);
        tag.putBoolean("IsPercentage", isPercentage);
        tag.putString("Source", source);
        tag.putInt("Priority", priority);
        return tag;
    }
}

