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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Block entity for hot gold placed on top of an anvil.
 * Gold is softer: more forgiving position/timing tolerances.
 */
public class HotGoldAnvilEntity extends BlockEntity implements IHotMetalAnvilEntity {

    private int hammerStrikes = 0;
    private String selectedComponent = "lama_lunga";
    private boolean finished = false;
    private boolean guiOpened = false;

    // Minigame state
    private boolean minigameActive = false;
    private float hotspotBaseX = 0.5f;
    private float hotspotBaseZ = 0.5f;
    private long minigameStartTime = 0;
    private int failureCount = 0;
    private int currentPhase = 0;
    private float phaseOffset = 0;
    private int qualityScore = 0;

    // Owner and cooling
    private UUID ownerUUID = null;
    private long placementTime = 0;

    public HotGoldAnvilEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.HOT_GOLD_ANVIL_ENTITY.get(), pos, state);
    }

    public HotGoldAnvilEntity(BlockPos pos, Level level) {
        this(pos, level.getBlockState(pos));
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (level.getGameTime() % 20 != 0) return;

        if (placementTime <= 0) return;
        long coolingTicks = Config.SMITHING_COOLING_TIME.get() * 20L;
        if (level.getGameTime() - placementTime > coolingTicks) {
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    15, 0.3, 0.3, 0.3, 0.02);
            }
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 0.5F);
            level.removeBlock(worldPosition, false);
        }
    }

    @Override
    public void onHammerStrike(Player player, float hitX, float hitZ) {
        if (finished) return;

        if (!minigameActive) {
            startMinigame();
            return;
        }

        float effectiveX = getEffectiveHotspotX();
        float effectiveZ = getEffectiveHotspotZ();
        float posDistance = (float) Math.sqrt(
            (hitX - effectiveX) * (hitX - effectiveX) +
            (hitZ - effectiveZ) * (hitZ - effectiveZ)
        );
        float hotspotSize = getEffectiveHotspotSize();
        float posTolerance = hotspotSize * getPositionToleranceMultiplier();
        boolean positionHit = posDistance <= posTolerance;

        // Check pulse over a small window to compensate for network latency
        float pulse = getMaxRecentPulse();
        boolean timingHit = pulse >= getTimingThreshold();

        if (positionHit && timingHit) {
            onPerfectHit(player, hitX, hitZ, posDistance, pulse, hotspotSize);
        } else if (positionHit) {
            onGoodHit(player, hitX, hitZ, posDistance, pulse, hotspotSize);
        } else {
            onMiss(player, hitX, hitZ);
        }
    }

    private void onPerfectHit(Player player, float hitX, float hitZ,
                               float posDistance, float pulse, float hotspotSize) {
        int quality = calculateQuality(posDistance, pulse, hotspotSize);
        qualityScore += quality;
        hammerStrikes++;
        currentPhase++;
        minigameActive = false;
        setChanged();

        if (level == null || level.isClientSide) return;

        float pitch = 0.9f + pulse * 0.5f;
        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, pitch);

        if (level instanceof ServerLevel sl) {
            double px = worldPosition.getX() + hitX;
            double py = worldPosition.getY() + 0.7;
            double pz = worldPosition.getZ() + hitZ;
            sl.sendParticles(ParticleTypes.LAVA, px, py, pz, 8, 0.1, 0.1, 0.1, 0.1);
            sl.sendParticles(ParticleTypes.FLAME, px, py, pz, 15, 0.15, 0.05, 0.15, 0.08);
        }

        if (hammerStrikes >= 4) {
            onForgeComplete(player);
        }

        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void onGoodHit(Player player, float hitX, float hitZ,
                            float posDistance, float pulse, float hotspotSize) {
        int quality = calculateQuality(posDistance, pulse, hotspotSize) / 2;
        qualityScore += quality;
        hammerStrikes++;
        currentPhase++;
        minigameActive = false;
        setChanged();

        if (level == null || level.isClientSide) return;

        float pitch = 0.8f + pulse * 0.3f;
        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, pitch);

        if (level instanceof ServerLevel sl) {
            double px = worldPosition.getX() + hitX;
            double py = worldPosition.getY() + 0.7;
            double pz = worldPosition.getZ() + hitZ;
            sl.sendParticles(ParticleTypes.FLAME, px, py, pz, 8, 0.1, 0.05, 0.1, 0.05);
        }

        if (hammerStrikes >= 4) {
            onForgeComplete(player);
        }

        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void onMiss(Player player, float hitX, float hitZ) {
        failureCount++;
        setChanged();

        if (level == null || level.isClientSide) return;

        level.playSound(null, worldPosition, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.7F, 0.5F);

        if (level instanceof ServerLevel sl) {
            double px = worldPosition.getX() + hitX;
            double py = worldPosition.getY() + 0.7;
            double pz = worldPosition.getZ() + hitZ;
            sl.sendParticles(ParticleTypes.SMOKE, px, py, pz, 8, 0.15, 0.1, 0.15, 0.03);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, px, py, pz, 3, 0.1, 0.05, 0.1, 0.02);
        }

        if (Config.SMITHING_CAN_LOSE_PIECE.get() &&
            failureCount >= Config.SMITHING_MAX_FAILURES.get()) {
            level.removeBlock(worldPosition, false);
            level.playSound(null, worldPosition, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    20, 0.3, 0.3, 0.3, 0.05);
            }
            return;
        }

        startMinigame();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void onForgeComplete(Player player) {
        finished = true;
        if (player != null) {
            TharidiaThings.LOGGER.info(player.getName().getString() + " forged a gold " + selectedComponent);
        }

        level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 1.5F);
        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.8F);

        if (level instanceof ServerLevel sl) {
            double cx = worldPosition.getX() + 0.5;
            double cy = worldPosition.getY() + 0.8;
            double cz = worldPosition.getZ() + 0.5;
            sl.sendParticles(ParticleTypes.LAVA, cx, cy, cz, 10, 0.2, 0.15, 0.2, 0.1);
            sl.sendParticles(ParticleTypes.FLAME, cx, cy, cz, 12, 0.15, 0.08, 0.15, 0.06);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy + 0.1, cz, 4, 0.1, 0.03, 0.1, 0.03);
        }
    }

    private int calculateQuality(float posDistance, float pulse, float hotspotSize) {
        float posNormalized = 1.0f - Math.min(posDistance / (hotspotSize * 2), 1.0f);
        int posScore = (int) (posNormalized * 50);
        int timeScore = (int) (pulse * 50);
        return posScore + timeScore;
    }

    // Gold is softer: more forgiving
    protected float getPositionToleranceMultiplier() {
        return 1.3f; // Gold: soft, easier positioning
    }

    protected float getTimingThreshold() {
        return 0.55f; // Pulse >= 55%
    }

    @Override public int getHammerStrikes() { return hammerStrikes; }
    @Override public String getSelectedComponent() { return selectedComponent; }

    @Override
    public void setSelectedComponent(String component) {
        this.selectedComponent = component;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override public boolean isFinished() { return finished; }
    @Override public float getProgress() { return hammerStrikes / 4.0f; }
    @Override public boolean hasGuiBeenOpened() { return guiOpened; }

    @Override
    public void setGuiOpened(boolean opened) {
        this.guiOpened = opened;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override public String getMaterialType() { return "gold"; }
    @Override public UUID getOwnerUUID() { return ownerUUID; }

    @Override
    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
        setChanged();
    }

    @Override public long getPlacementTime() { return placementTime; }

    @Override
    public void setPlacementTime(long time) {
        this.placementTime = time;
        setChanged();
    }

    @Override public int getQualityScore() { return qualityScore; }
    @Override public boolean isMinigameActive() { return minigameActive; }

    @Override
    public void startMinigame() {
        minigameActive = true;
        minigameStartTime = level.getGameTime();

        if (currentPhase < 3) {
            hotspotBaseX = 0.25f + level.random.nextFloat() * 0.5f;
            hotspotBaseZ = 0.25f + level.random.nextFloat() * 0.5f;
        } else {
            hotspotBaseX = 0.5f;
            hotspotBaseZ = 0.5f;
        }
        phaseOffset = (float) (level.random.nextFloat() * Math.PI * 2.0);

        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override public int getCurrentPhase() { return currentPhase; }
    @Override public float getPhaseOffset() { return phaseOffset; }

    @Override
    public float getEffectiveHotspotX() {
        if (level == null || !minigameActive) return hotspotBaseX;
        if (currentPhase == 0 || currentPhase >= 3) return hotspotBaseX;

        float moveRadius = currentPhase == 1 ? 0.06f : 0.10f;
        float moveSpeed = currentPhase == 1 ? 0.4f : 0.7f;
        long elapsed = level.getGameTime() - minigameStartTime;
        float angle = (float) (elapsed / 20.0 * moveSpeed * Math.PI * 2.0);
        float result = hotspotBaseX + (float) Math.cos(angle) * moveRadius;
        return Math.max(0.15f, Math.min(0.85f, result));
    }

    @Override
    public float getEffectiveHotspotZ() {
        if (level == null || !minigameActive) return hotspotBaseZ;
        if (currentPhase == 0 || currentPhase >= 3) return hotspotBaseZ;

        float moveRadius = currentPhase == 1 ? 0.05f : 0.08f;
        float moveSpeed = currentPhase == 1 ? 0.4f : 0.7f;
        long elapsed = level.getGameTime() - minigameStartTime;
        float angle = (float) (elapsed / 20.0 * moveSpeed * Math.PI * 2.0 + 1.2);
        float result = hotspotBaseZ + (float) Math.sin(angle) * moveRadius;
        return Math.max(0.15f, Math.min(0.85f, result));
    }

    @Override
    public float getEffectiveHotspotSize() {
        return switch (currentPhase) {
            case 0 -> 0.10f;
            case 1 -> 0.07f;
            case 2 -> 0.04f;
            case 3 -> 0.07f;
            default -> 0.07f;
        };
    }

    @Override
    public float getCurrentPulse() {
        if (!minigameActive || level == null) return 0;
        return calculatePulseAtTime(level.getGameTime());
    }

    private float calculatePulseAtTime(long gameTime) {
        long elapsed = gameTime - minigameStartTime;
        if (elapsed < 0) return 0;
        double elapsedSeconds = elapsed / 20.0;

        double cycleTime = getCycleTimeForPhase();
        double cycle = elapsedSeconds / cycleTime;
        double phase = (cycle % 1.0) * Math.PI * 2.0 + phaseOffset;

        double primary = Math.sin(phase);
        double secondary = Math.sin(phase * 2.3 + 0.7) * 0.25;
        double combined = (primary + secondary) / 1.25;

        return (float) ((combined + 1.0) / 2.0);
    }

    private float getMaxRecentPulse() {
        if (!minigameActive || level == null) return 0;
        float maxPulse = 0;
        long currentTime = level.getGameTime();
        for (int i = 0; i <= 3; i++) {
            float p = calculatePulseAtTime(currentTime - i);
            if (p > maxPulse) maxPulse = p;
        }
        return maxPulse;
    }

    private double getCycleTimeForPhase() {
        double maxCycleTime = Config.SMITHING_MAX_CYCLE_TIME.get();
        double minCycleTime = Config.SMITHING_MIN_CYCLE_TIME.get();

        if (currentPhase == 3) {
            if (level == null) return maxCycleTime;
            long elapsed = level.getGameTime() - minigameStartTime;
            double t = Math.min(elapsed / 160.0, 1.0);
            return maxCycleTime * 1.5 - t * (maxCycleTime * 1.5 - minCycleTime);
        }

        double cycleTime = maxCycleTime - (currentPhase * (maxCycleTime - minCycleTime) / 3.0);
        return Math.max(cycleTime, minCycleTime);
    }

    @Override public long getMinigameStartTime() { return minigameStartTime; }
    @Override public int getFailureCount() { return failureCount; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("HammerStrikes", hammerStrikes);
        tag.putString("SelectedComponent", selectedComponent);
        tag.putBoolean("Finished", finished);
        tag.putBoolean("GuiOpened", guiOpened);
        tag.putBoolean("MinigameActive", minigameActive);
        tag.putFloat("CircleX", hotspotBaseX);
        tag.putFloat("CircleZ", hotspotBaseZ);
        tag.putLong("MinigameStartTime", minigameStartTime);
        tag.putInt("FailureCount", failureCount);
        tag.putInt("CurrentPhase", currentPhase);
        tag.putFloat("PhaseOffset", phaseOffset);
        tag.putInt("QualityScore", qualityScore);
        tag.putLong("PlacementTime", placementTime);
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        hammerStrikes = tag.getInt("HammerStrikes");
        selectedComponent = tag.getString("SelectedComponent");
        finished = tag.getBoolean("Finished");
        guiOpened = tag.getBoolean("GuiOpened");
        minigameActive = tag.getBoolean("MinigameActive");
        hotspotBaseX = tag.getFloat("CircleX");
        hotspotBaseZ = tag.getFloat("CircleZ");
        minigameStartTime = tag.getLong("MinigameStartTime");
        failureCount = tag.getInt("FailureCount");
        currentPhase = tag.getInt("CurrentPhase");
        phaseOffset = tag.getFloat("PhaseOffset");
        qualityScore = tag.getInt("QualityScore");
        placementTime = tag.getLong("PlacementTime");
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
        }
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
