package com.THproject.tharidia_things.stamina;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StaminaData implements INBTSerializable<CompoundTag> {
    private float currentStamina;
    private float maxStamina;
    private boolean initialized;
    private boolean inCombat;
    private int combatTicksRemaining;
    private int regenDelayTicksRemaining;
    private final List<StaminaModifier> modifiers = new ArrayList<>();

    public StaminaData() {
        this.currentStamina = 0.0f;
        this.maxStamina = 0.0f;
        this.initialized = false;
        this.inCombat = false;
        this.combatTicksRemaining = 0;
        this.regenDelayTicksRemaining = 0;
    }

    public float getCurrentStamina() {
        return currentStamina;
    }

    public void setCurrentStamina(float currentStamina) {
        this.currentStamina = Math.max(0.0f, currentStamina);
    }

    public float getMaxStamina() {
        return maxStamina;
    }

    public void setMaxStamina(float maxStamina) {
        this.maxStamina = Math.max(0.0f, maxStamina);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isInCombat() {
        return inCombat;
    }

    public void setInCombat(boolean inCombat) {
        this.inCombat = inCombat;
    }

    public int getCombatTicksRemaining() {
        return combatTicksRemaining;
    }

    public void setCombatTicksRemaining(int combatTicksRemaining) {
        this.combatTicksRemaining = Math.max(0, combatTicksRemaining);
    }

    public int getRegenDelayTicksRemaining() {
        return regenDelayTicksRemaining;
    }

    public void setRegenDelayTicksRemaining(int regenDelayTicksRemaining) {
        this.regenDelayTicksRemaining = Math.max(0, regenDelayTicksRemaining);
    }

    public List<StaminaModifier> getModifiers() {
        return List.copyOf(modifiers);
    }

    public void addModifier(StaminaModifier modifier) {
        if (modifier == null || modifier.source() == null || modifier.source().isBlank()) {
            return;
        }
        modifiers.removeIf(existing -> Objects.equals(existing.source(), modifier.source()) && existing.type() == modifier.type());
        modifiers.add(modifier);
    }

    public void removeModifiersBySource(String source) {
        if (source == null || source.isBlank()) {
            return;
        }
        modifiers.removeIf(existing -> Objects.equals(existing.source(), source));
    }

    public void clearModifiers() {
        modifiers.clear();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("CurrentStamina", currentStamina);
        tag.putFloat("MaxStamina", maxStamina);
        tag.putBoolean("Initialized", initialized);
        tag.putBoolean("InCombat", inCombat);
        tag.putInt("CombatTicksRemaining", combatTicksRemaining);
        tag.putInt("RegenDelayTicksRemaining", regenDelayTicksRemaining);

        ListTag modifierTags = new ListTag();
        for (StaminaModifier modifier : modifiers) {
            modifierTags.add(modifier.toTag(provider));
        }
        tag.put("Modifiers", modifierTags);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        currentStamina = tag.getFloat("CurrentStamina");
        maxStamina = tag.contains("MaxStamina") ? tag.getFloat("MaxStamina") : 0.0f;
        initialized = tag.contains("Initialized") && tag.getBoolean("Initialized");
        inCombat = tag.getBoolean("InCombat");
        combatTicksRemaining = tag.getInt("CombatTicksRemaining");
        regenDelayTicksRemaining = tag.getInt("RegenDelayTicksRemaining");

        modifiers.clear();
        if (tag.contains("Modifiers", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("Modifiers", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag modifierTag = listTag.getCompound(i);
                try {
                    modifiers.add(StaminaModifier.fromTag(modifierTag));
                } catch (Exception ignored) {
                }
            }
        }
    }
}
