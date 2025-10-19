package com.tharidia.tharidia_things.fatigue;

import com.tharidia.tharidia_things.config.FatigueConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Stores fatigue data for a player
 */
public class FatigueData implements INBTSerializable<CompoundTag> {
    
    private int fatigueTicks; // Current fatigue level (starts at MAX_FATIGUE_TICKS)
    private int bedRestTicks; // Ticks spent resting in bed
    private int proximityTicks; // Ticks spent near a bed
    private double lastPosX, lastPosY, lastPosZ; // Last position to detect movement
    
    // Time-based tracking for accurate fatigue decrease
    private long lastMovementTime; // Last time player moved (milliseconds)
    private boolean wasMoving; // Was player moving in last check
    
    // Cache for bed proximity check (optimization)
    private transient Boolean cachedNearBed = null;
    private transient double cachedCheckPosX = Double.MAX_VALUE;
    private transient double cachedCheckPosZ = Double.MAX_VALUE;
    
    // Last bed position for forcing player back to bed
    private BlockPos lastBedPosition = null;
    
    public FatigueData() {
        this.fatigueTicks = FatigueConfig.getMaxFatigueTicks();
        this.bedRestTicks = 0;
        this.proximityTicks = 0;
        this.lastPosX = 0;
        this.lastPosY = 0;
        this.lastPosZ = 0;
        this.lastMovementTime = System.currentTimeMillis();
        this.wasMoving = false;
    }
    
    /**
     * Gets the current fatigue level in ticks
     */
    public int getFatigueTicks() {
        return fatigueTicks;
    }
    
    /**
     * Sets the fatigue level
     */
    public void setFatigueTicks(int ticks) {
        this.fatigueTicks = Math.max(0, Math.min(FatigueConfig.getMaxFatigueTicks(), ticks));
    }
    
    /**
     * Decreases fatigue by one tick (called when player is moving)
     */
    public void decreaseFatigue() {
        if (fatigueTicks > 0) {
            fatigueTicks--;
        }
    }
    
    /**
     * Decreases fatigue based on real time elapsed (in milliseconds)
     * This is more accurate than tick-based decrement, especially with player batching
     */
    public void decreaseFatigueByTime(long elapsedMillis) {
        if (fatigueTicks > 0) {
            // Convert milliseconds to ticks (1 tick = 50ms at 20 TPS)
            int ticksToDecrease = (int) (elapsedMillis / 50);
            fatigueTicks = Math.max(0, fatigueTicks - ticksToDecrease);
        }
    }
    
    /**
     * Gets the last movement time
     */
    public long getLastMovementTime() {
        return lastMovementTime;
    }
    
    /**
     * Updates the last movement time to current time
     */
    public void updateLastMovementTime() {
        this.lastMovementTime = System.currentTimeMillis();
    }
    
    /**
     * Sets the last movement time
     */
    public void setLastMovementTime(long time) {
        this.lastMovementTime = time;
    }
    
    /**
     * Gets whether player was moving in last check
     */
    public boolean wasMoving() {
        return wasMoving;
    }
    
    /**
     * Sets whether player is currently moving
     */
    public void setMoving(boolean moving) {
        this.wasMoving = moving;
    }
    
    /**
     * Increases fatigue (recovery)
     */
    public void increaseFatigue(int amount) {
        fatigueTicks = Math.min(FatigueConfig.getMaxFatigueTicks(), fatigueTicks + amount);
    }
    
    /**
     * Checks if the player is exhausted
     */
    public boolean isExhausted() {
        return fatigueTicks <= 0;
    }
    
    /**
     * Gets the fatigue percentage (0.0 to 1.0)
     */
    public float getFatiguePercentage() {
        return (float) fatigueTicks / FatigueConfig.getMaxFatigueTicks();
    }
    
    /**
     * Gets bed rest ticks
     */
    public int getBedRestTicks() {
        return bedRestTicks;
    }
    
    /**
     * Increments bed rest ticks
     */
    public void incrementBedRestTicks() {
        bedRestTicks++;
    }
    
    /**
     * Resets bed rest ticks
     */
    public void resetBedRestTicks() {
        bedRestTicks = 0;
    }
    
    /**
     * Checks if the player has rested enough in bed
     */
    public boolean hasRestedEnough() {
        return bedRestTicks >= FatigueConfig.getBedRestTime();
    }
    
    /**
     * Gets proximity ticks
     */
    public int getProximityTicks() {
        return proximityTicks;
    }
    
    /**
     * Increments proximity ticks
     */
    public void incrementProximityTicks() {
        proximityTicks++;
    }
    
    /**
     * Resets proximity ticks
     */
    public void resetProximityTicks() {
        proximityTicks = 0;
    }
    
    /**
     * Updates last position
     */
    public void updateLastPosition(double x, double y, double z) {
        this.lastPosX = x;
        this.lastPosY = y;
        this.lastPosZ = z;
    }
    
    /**
     * Gets cached bed proximity result if position hasn't changed significantly
     * Returns null if cache is invalid
     */
    public Boolean getCachedBedProximity(double x, double z) {
        // Invalidate cache if player moved more than 5 blocks horizontally
        if (cachedNearBed != null) {
            double dx = x - cachedCheckPosX;
            double dz = z - cachedCheckPosZ;
            double distSquared = dx * dx + dz * dz;
            if (distSquared > 25.0) { // 5 blocks squared
                cachedNearBed = null;
            }
        }
        return cachedNearBed;
    }
    
    /**
     * Caches bed proximity result
     */
    public void setCachedBedProximity(double x, double z, boolean nearBed) {
        this.cachedCheckPosX = x;
        this.cachedCheckPosZ = z;
        this.cachedNearBed = nearBed;
    }
    
    /**
     * Checks if the player has moved from last position
     */
    public boolean hasMoved(double x, double y, double z) {
        double dx = x - lastPosX;
        double dy = y - lastPosY;
        double dz = z - lastPosZ;
        double distSquared = dx * dx + dy * dy + dz * dz;
        return distSquared > 0.001; // Small threshold to avoid floating point errors
    }
    
    /**
     * Gets the last bed position
     */
    public BlockPos getLastBedPosition() {
        return lastBedPosition;
    }
    
    /**
     * Sets the last bed position
     */
    public void setLastBedPosition(BlockPos pos) {
        this.lastBedPosition = pos;
    }
    
    /**
     * Fully restores fatigue
     */
    public void fullyRestore() {
        fatigueTicks = FatigueConfig.getMaxFatigueTicks();
        bedRestTicks = 0;
        proximityTicks = 0;
        cachedNearBed = null; // Invalidate cache
        lastBedPosition = null; // Clear bed position
    }
    
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("FatigueTicks", fatigueTicks);
        tag.putInt("BedRestTicks", bedRestTicks);
        tag.putInt("ProximityTicks", proximityTicks);
        tag.putDouble("LastPosX", lastPosX);
        tag.putDouble("LastPosY", lastPosY);
        tag.putDouble("LastPosZ", lastPosZ);
        tag.putLong("LastMovementTime", lastMovementTime);
        tag.putBoolean("WasMoving", wasMoving);
        
        // Save last bed position if present
        if (lastBedPosition != null) {
            tag.putInt("BedPosX", lastBedPosition.getX());
            tag.putInt("BedPosY", lastBedPosition.getY());
            tag.putInt("BedPosZ", lastBedPosition.getZ());
        }
        
        return tag;
    }
    
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        fatigueTicks = tag.getInt("FatigueTicks");
        bedRestTicks = tag.getInt("BedRestTicks");
        proximityTicks = tag.getInt("ProximityTicks");
        lastPosX = tag.getDouble("LastPosX");
        lastPosY = tag.getDouble("LastPosY");
        lastPosZ = tag.getDouble("LastPosZ");
        lastMovementTime = tag.contains("LastMovementTime") ? tag.getLong("LastMovementTime") : System.currentTimeMillis();
        wasMoving = tag.contains("WasMoving") ? tag.getBoolean("WasMoving") : false;
        
        // Load last bed position if present
        if (tag.contains("BedPosX") && tag.contains("BedPosY") && tag.contains("BedPosZ")) {
            lastBedPosition = new BlockPos(
                tag.getInt("BedPosX"),
                tag.getInt("BedPosY"),
                tag.getInt("BedPosZ")
            );
        } else {
            lastBedPosition = null;
        }
    }
}
