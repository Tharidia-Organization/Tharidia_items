package com.THproject.tharidia_things.block.entity;

import com.THproject.tharidia_things.Config;
import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for hot gold placed on top of an anvil.
 * Tracks hammer strikes and the selected component type.
 */
public class HotGoldAnvilEntity extends BlockEntity implements IHotMetalAnvilEntity {
    
    private int hammerStrikes = 0;
    private String selectedComponent = "lama_lunga"; // Default selection
    private boolean finished = false;
    private boolean guiOpened = false; // Track if GUI has been opened
    
    // Minigame state
    private boolean minigameActive = false;
    private float circleX = 0.5f; // Position on anvil (0-1 range)
    private float circleZ = 0.5f;
    private float targetRadius = 0.3f; // Fixed circle radius
    private long minigameStartTime = 0;
    private int failureCount = 0;
    private int currentPhase = 0; // Track which phase we're on (0-3)
    private float phaseOffset = 0; // Random starting phase for animation
    
    public HotGoldAnvilEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.HOT_GOLD_ANVIL_ENTITY.get(), pos, state);
    }
    
    // Constructor for when creating without BlockState
    public HotGoldAnvilEntity(BlockPos pos, Level level) {
        this(pos, level.getBlockState(pos));
    }
    
    public void onHammerStrike(net.minecraft.world.entity.player.Player player) {
        if (finished) {
            TharidiaThings.LOGGER.debug("Already finished");
            return;
        }
        
        // Start minigame if not already active
        if (!minigameActive) {
            startMinigame();
            return; // Don't count this hit, just start the minigame
        }
        
        // Check if player hit the target correctly
        float currentRadius = getCurrentRadius();
        float accuracy = Math.abs(currentRadius - targetRadius);
        
        if (checkHit(accuracy)) {
            // Successful hit
            hammerStrikes++;
            currentPhase++;
            minigameActive = false;
            setChanged();
            
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                
                // Spawn green particles for success
                if (level instanceof ServerLevel serverLevel) {
                    double x = worldPosition.getX() + circleX;
                    double y = worldPosition.getY() + 0.7;
                    double z = worldPosition.getZ() + circleZ;
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 10, 0.2, 0.1, 0.2, 0.05);
                }
                
                if (hammerStrikes >= 4) {
                    finished = true;
                    if (player != null) {
                        TharidiaThings.LOGGER.info(player.getName().getString() + " forged a gold piece");
                    }
                    level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.5F);
                }
                
                // Sync to client
                if (!level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        } else {
            // Failed hit
            failureCount++;
            setChanged();
            
            if (level != null && !level.isClientSide) {
                // Spawn red particles for failure
                if (level instanceof ServerLevel serverLevel) {
                    double x = worldPosition.getX() + circleX;
                    double y = worldPosition.getY() + 0.7;
                    double z = worldPosition.getZ() + circleZ;
                    serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, 5, 0.2, 0.1, 0.2, 0.05);
                }
                
                // Check if piece is destroyed
                if (Config.SMITHING_CAN_LOSE_PIECE.get() &&
                    failureCount >= Config.SMITHING_MAX_FAILURES.get()) {
                    // Destroy the piece
                    level.removeBlock(worldPosition, false);
                    level.playSound(null, worldPosition, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                    
                    // Spawn smoke particles
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, 
                            worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 
                            20, 0.3, 0.3, 0.3, 0.05);
                    }
                    return;
                }
                
                // Restart minigame for retry
                startMinigame();
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 0.8F);
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }
    
    public int getHammerStrikes() {
        return hammerStrikes;
    }
    
    public String getSelectedComponent() {
        return selectedComponent;
    }
    
    public void setSelectedComponent(String component) {
        this.selectedComponent = component;
        setChanged();
        
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    public boolean isFinished() {
        return finished;
    }
    
    public float getProgress() {
        return hammerStrikes / 4.0f;
    }
    
    public boolean hasGuiBeenOpened() {
        return guiOpened;
    }
    
    public void setGuiOpened(boolean opened) {
        this.guiOpened = opened;
        setChanged();
        
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    public String getMaterialType() {
        return "gold";
    }
    
    // Minigame implementation
    @Override
    public boolean isMinigameActive() {
        return minigameActive;
    }
    
    @Override
    public void startMinigame() {
        minigameActive = true;
        minigameStartTime = level.getGameTime(); // Use game time (ticks) instead of system time
        
        // Random position on the anvil surface
        circleX = 0.2f + level.random.nextFloat() * 0.6f;
        circleZ = 0.2f + level.random.nextFloat() * 0.6f;
        
        // Target radius between 0.1 and 0.2 (dimezzato)
        targetRadius = 0.1f + level.random.nextFloat() * 0.1f;
        
        // Random starting phase (0 to 2*PI)
        phaseOffset = (float) (level.random.nextFloat() * Math.PI * 2.0);
        
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    public float getCircleX() {
        return circleX;
    }
    
    @Override
    public float getCircleZ() {
        return circleZ;
    }
    
    @Override
    public float getCurrentRadius() {
        if (!minigameActive || level == null) return 0;
        
        long elapsedTicks = level.getGameTime() - minigameStartTime;
        double elapsedSeconds = elapsedTicks / 20.0; // Convert ticks to seconds (20 ticks = 1 second)
        
        // Get cycle time based on current phase (gets faster each phase)
        double maxCycleTime = Config.SMITHING_MAX_CYCLE_TIME.get();
        double minCycleTime = Config.SMITHING_MIN_CYCLE_TIME.get();
        double cycleTime = maxCycleTime - (currentPhase * (maxCycleTime - minCycleTime) / 4.0);
        cycleTime = Math.max(cycleTime, minCycleTime);
        
        // Calculate current radius (oscillates between ~0.002 and 0.30, quasi punto fino a cerchio grande)
        // Add phaseOffset for random starting position
        double cycle = elapsedSeconds / cycleTime;
        double phase = (cycle % 1.0) * Math.PI * 2.0 + phaseOffset;
        
        return (float) (0.151 + Math.sin(phase) * 0.149);
    }
    
    @Override
    public float getTargetRadius() {
        return targetRadius;
    }
    
    @Override
    public long getMinigameStartTime() {
        return minigameStartTime;
    }
    
    @Override
    public int getFailureCount() {
        return failureCount;
    }
    
    @Override
    public boolean checkHit(float accuracy) {
        double tolerance = Config.SMITHING_TOLERANCE.get();
        return accuracy <= tolerance * 0.3; // 0.3 is max tolerance range
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("HammerStrikes", hammerStrikes);
        tag.putString("SelectedComponent", selectedComponent);
        tag.putBoolean("Finished", finished);
        tag.putBoolean("GuiOpened", guiOpened);
        tag.putBoolean("MinigameActive", minigameActive);
        tag.putFloat("CircleX", circleX);
        tag.putFloat("CircleZ", circleZ);
        tag.putFloat("TargetRadius", targetRadius);
        tag.putLong("MinigameStartTime", minigameStartTime);
        tag.putInt("FailureCount", failureCount);
        tag.putInt("CurrentPhase", currentPhase);
        tag.putFloat("PhaseOffset", phaseOffset);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        hammerStrikes = tag.getInt("HammerStrikes");
        selectedComponent = tag.getString("SelectedComponent");
        finished = tag.getBoolean("Finished");
        guiOpened = tag.getBoolean("GuiOpened");
        minigameActive = tag.getBoolean("MinigameActive");
        circleX = tag.getFloat("CircleX");
        circleZ = tag.getFloat("CircleZ");
        targetRadius = tag.getFloat("TargetRadius");
        minigameStartTime = tag.getLong("MinigameStartTime");
        failureCount = tag.getInt("FailureCount");
        currentPhase = tag.getInt("CurrentPhase");
        phaseOffset = tag.getFloat("PhaseOffset");
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
