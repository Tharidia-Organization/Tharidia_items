package com.THproject.tharidia_things.block.entity;

/**
 * Interface for hot metal anvil entities (iron, gold, etc.)
 * Allows generic handling of smithing operations
 */
public interface IHotMetalAnvilEntity {
    
    int getHammerStrikes();
    
    String getSelectedComponent();
    
    void setSelectedComponent(String component);
    
    boolean isFinished();
    
    float getProgress();
    
    boolean hasGuiBeenOpened();
    
    void setGuiOpened(boolean opened);
    
    void onHammerStrike(net.minecraft.world.entity.player.Player player);
    
    /**
     * Gets the material type of this hot metal (e.g., "iron", "gold", "copper")
     */
    String getMaterialType();
    
    // Minigame methods
    boolean isMinigameActive();
    
    void startMinigame();
    
    float getCircleX();
    
    float getCircleZ();
    
    float getCurrentRadius();
    
    float getTargetRadius();
    
    long getMinigameStartTime();
    
    int getFailureCount();
    
    boolean checkHit(float hitAccuracy);
}
