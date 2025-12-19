package com.THproject.tharidia_things.character;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Stores character creation data for a player
 */
public class CharacterData implements INBTSerializable<CompoundTag> {
    
    private boolean characterCreated = false;
    
    public CharacterData() {
        this.characterCreated = false;
    }
    
    /**
     * Gets whether the player has created their character
     */
    public boolean hasCreatedCharacter() {
        return characterCreated;
    }
    
    /**
     * Sets that the player has created their character
     */
    public void setCharacterCreated(boolean created) {
        this.characterCreated = created;
    }
    
    /**
     * Marks the character as created
     */
    public void markCharacterCreated() {
        this.characterCreated = true;
    }
    
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("CharacterCreated", characterCreated);
        return tag;
    }
    
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        characterCreated = tag.getBoolean("CharacterCreated");
    }
}
