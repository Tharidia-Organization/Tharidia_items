package com.THproject.tharidia_things.character;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Stores character creation data for a player.
 * Uses a state machine to track progress through character creation stages.
 */
public class CharacterData implements INBTSerializable<CompoundTag> {

    /**
     * Stages of character creation. Order matters — new stages should be inserted
     * between AWAITING_RACE and COMPLETED for future expansion (e.g. AWAITING_ORIGIN).
     */
    public enum CreationStage {
        NOT_STARTED,
        AWAITING_RACE,
        // Future: AWAITING_ORIGIN,
        COMPLETED
    }

    private CreationStage stage = CreationStage.NOT_STARTED;
    private String selectedRace = null;

    public CharacterData() {
        this.stage = CreationStage.NOT_STARTED;
        this.selectedRace = null;
    }

    // --- Stage accessors ---

    public CreationStage getStage() {
        return stage;
    }

    public void setStage(CreationStage stage) {
        this.stage = stage;
    }

    // --- Race accessors ---

    public String getSelectedRace() {
        return selectedRace;
    }

    public void setSelectedRace(String race) {
        this.selectedRace = race;
    }

    // --- Backward-compatible convenience methods ---

    /**
     * Returns true only when the entire character creation flow is complete.
     * Backward-compatible with old code that checks this boolean.
     */
    public boolean hasCreatedCharacter() {
        return stage == CreationStage.COMPLETED;
    }

    /**
     * Marks the character as fully created (sets stage to COMPLETED).
     */
    public void markCharacterCreated() {
        this.stage = CreationStage.COMPLETED;
    }

    /**
     * Backward-compatible setter. true → COMPLETED, false → NOT_STARTED.
     */
    public void setCharacterCreated(boolean created) {
        this.stage = created ? CreationStage.COMPLETED : CreationStage.NOT_STARTED;
    }

    // --- NBT Serialization ---

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Stage", stage.name());
        if (selectedRace != null) {
            tag.putString("SelectedRace", selectedRace);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        // Backward compatibility: old saves only have "CharacterCreated" boolean
        if (tag.contains("CharacterCreated") && !tag.contains("Stage")) {
            boolean created = tag.getBoolean("CharacterCreated");
            this.stage = created ? CreationStage.COMPLETED : CreationStage.NOT_STARTED;
        } else if (tag.contains("Stage")) {
            try {
                this.stage = CreationStage.valueOf(tag.getString("Stage"));
            } catch (IllegalArgumentException e) {
                this.stage = CreationStage.NOT_STARTED;
            }
        }

        if (tag.contains("SelectedRace")) {
            this.selectedRace = tag.getString("SelectedRace");
        }
    }
}
