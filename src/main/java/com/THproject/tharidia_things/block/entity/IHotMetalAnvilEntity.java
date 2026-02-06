package com.THproject.tharidia_things.block.entity;

import java.util.UUID;

/**
 * Interface for hot metal anvil entities (iron, gold, copper).
 * Supports the position+timing smithing minigame system.
 */
public interface IHotMetalAnvilEntity {

    int getHammerStrikes();

    String getSelectedComponent();

    void setSelectedComponent(String component);

    boolean isFinished();

    float getProgress();

    boolean hasGuiBeenOpened();

    void setGuiOpened(boolean opened);

    void onHammerStrike(net.minecraft.world.entity.player.Player player, float hitX, float hitZ);

    String getMaterialType();

    UUID getOwnerUUID();

    void setOwnerUUID(UUID uuid);

    long getPlacementTime();

    void setPlacementTime(long time);

    int getQualityScore();

    // Minigame methods
    boolean isMinigameActive();

    void startMinigame();

    float getEffectiveHotspotX();

    float getEffectiveHotspotZ();

    float getEffectiveHotspotSize();

    float getCurrentPulse();

    long getMinigameStartTime();

    int getFailureCount();

    int getCurrentPhase();

    float getPhaseOffset();
}
