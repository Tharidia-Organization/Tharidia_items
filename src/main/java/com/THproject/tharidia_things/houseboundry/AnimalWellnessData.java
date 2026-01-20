package com.THproject.tharidia_things.houseboundry;

import com.THproject.tharidia_things.houseboundry.config.AnimalConfigRegistry;
import com.THproject.tharidia_things.houseboundry.config.AnimalProductionConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Stores wellness data for an animal entity.
 * Attached via NeoForge attachment system to any LivingEntity.
 */
public class AnimalWellnessData implements INBTSerializable<CompoundTag> {

    // Wellness stats (0-100)
    private int comfort;
    private int stress;
    private int hygiene;

    // Lifecycle tracking
    private LifecyclePhase phase;
    private long birthTimestamp;              // Unix ms - when animal was born/spawned
    private long productiveStartTimestamp;    // Unix ms - when animal became productive
    private long lastProductionTimestamp;     // Unix ms - last time animal produced

    // Disease tracking
    private long diseaseStartTimestamp;       // Unix ms - 0 if not diseased

    // Interaction cooldowns
    private long lastBrushTimestamp;          // Unix ms - for brush cooldown
    private long lastHygieneRainBonus;        // Unix ms - prevent rain hygiene spam

    // Breeding
    private boolean hasBred;                  // One-time breeding flag

    // Entity type for config lookup (set when attachment is first accessed)
    private ResourceLocation entityTypeId;

    /**
     * Default constructor - initializes with default values
     */
    public AnimalWellnessData() {
        this.comfort = 50;
        this.stress = 30;
        this.hygiene = 100;
        this.phase = LifecyclePhase.BABY;
        this.birthTimestamp = System.currentTimeMillis();
        this.productiveStartTimestamp = 0;
        this.lastProductionTimestamp = 0;
        this.diseaseStartTimestamp = 0;
        this.lastBrushTimestamp = 0;
        this.lastHygieneRainBonus = 0;
        this.hasBred = false;
        this.entityTypeId = null;
    }

    // ==================== WELLNESS STATS ====================

    public int getComfort() {
        return comfort;
    }

    public void setComfort(int comfort) {
        this.comfort = Math.max(0, Math.min(100, comfort));
    }

    public void addComfort(int amount) {
        setComfort(this.comfort + amount);
    }

    public int getStress() {
        return stress;
    }

    public void setStress(int stress) {
        this.stress = Math.max(0, Math.min(100, stress));
    }

    public void addStress(int amount) {
        setStress(this.stress + amount);
    }

    public int getHygiene() {
        return hygiene;
    }

    public void setHygiene(int hygiene) {
        this.hygiene = Math.max(0, Math.min(100, hygiene));
    }

    public void addHygiene(int amount) {
        setHygiene(this.hygiene + amount);
    }

    // ==================== LIFECYCLE ====================

    public LifecyclePhase getPhase() {
        return phase;
    }

    public void setPhase(LifecyclePhase phase) {
        this.phase = phase;
    }

    public long getBirthTimestamp() {
        return birthTimestamp;
    }

    public void setBirthTimestamp(long birthTimestamp) {
        this.birthTimestamp = birthTimestamp;
    }

    public long getProductiveStartTimestamp() {
        return productiveStartTimestamp;
    }

    public void setProductiveStartTimestamp(long productiveStartTimestamp) {
        this.productiveStartTimestamp = productiveStartTimestamp;
    }

    public long getLastProductionTimestamp() {
        return lastProductionTimestamp;
    }

    public void setLastProductionTimestamp(long lastProductionTimestamp) {
        this.lastProductionTimestamp = lastProductionTimestamp;
    }

    /**
     * Updates the lifecycle phase based on real time elapsed.
     * Should be called periodically (e.g., every tick or minute).
     *
     * @return true if phase changed
     */
    public boolean updatePhase() {
        if (entityTypeId == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        AnimalProductionConfig config = AnimalConfigRegistry.getConfig(entityTypeId).orElse(null);

        // Use defaults if no config
        float babyHours = config != null ? config.lifecycle().babyDurationHours() : 1.0f;
        float productiveDays = config != null ? config.lifecycle().productiveDurationDays() : 10.0f;

        long babyDurationMs = (long) (babyHours * 3600000L);
        long productiveDurationMs = (long) (productiveDays * 86400000L);

        LifecyclePhase oldPhase = this.phase;

        // Check if still baby
        long age = now - birthTimestamp;
        if (age < babyDurationMs) {
            this.phase = LifecyclePhase.BABY;
            return oldPhase != this.phase;
        }

        // Transition to productive if not already
        if (productiveStartTimestamp == 0) {
            productiveStartTimestamp = now;
            lastProductionTimestamp = now; // Reset production timer
        }

        // Check if still productive
        long productiveAge = now - productiveStartTimestamp;
        if (productiveAge < productiveDurationMs) {
            this.phase = LifecyclePhase.PRODUCTIVE;
            return oldPhase != this.phase;
        }

        // Barren
        this.phase = LifecyclePhase.BARREN;
        return oldPhase != this.phase;
    }

    /**
     * Gets the remaining productive days.
     *
     * @return days remaining, or -1 if not productive
     */
    public float getRemainingProductiveDays() {
        if (phase != LifecyclePhase.PRODUCTIVE || productiveStartTimestamp == 0 || entityTypeId == null) {
            return -1;
        }

        AnimalProductionConfig config = AnimalConfigRegistry.getConfig(entityTypeId).orElse(null);
        float productiveDays = config != null ? config.lifecycle().productiveDurationDays() : 10.0f;
        long productiveDurationMs = (long) (productiveDays * 86400000L);

        long now = System.currentTimeMillis();
        long elapsed = now - productiveStartTimestamp;
        long remaining = productiveDurationMs - elapsed;

        return remaining > 0 ? remaining / 86400000.0f : 0;
    }

    // ==================== DISEASE ====================

    public boolean isDiseased() {
        return diseaseStartTimestamp > 0;
    }

    public long getDiseaseStartTimestamp() {
        return diseaseStartTimestamp;
    }

    public void setDiseaseStartTimestamp(long diseaseStartTimestamp) {
        this.diseaseStartTimestamp = diseaseStartTimestamp;
    }

    public void contractDisease() {
        if (!isDiseased()) {
            this.diseaseStartTimestamp = System.currentTimeMillis();
        }
    }

    public void cureDisease() {
        this.diseaseStartTimestamp = 0;
    }

    /**
     * Gets disease duration in minutes.
     *
     * @return minutes since disease started, or 0 if not diseased
     */
    public int getDiseaseDurationMinutes() {
        if (!isDiseased()) {
            return 0;
        }
        long now = System.currentTimeMillis();
        return (int) ((now - diseaseStartTimestamp) / 60000L);
    }

    // ==================== INTERACTIONS ====================

    public long getLastBrushTimestamp() {
        return lastBrushTimestamp;
    }

    public void setLastBrushTimestamp(long lastBrushTimestamp) {
        this.lastBrushTimestamp = lastBrushTimestamp;
    }

    /**
     * Checks if brush is on cooldown.
     *
     * @param cooldownMs cooldown duration in milliseconds
     * @return true if still on cooldown
     */
    public boolean isBrushOnCooldown(long cooldownMs) {
        return System.currentTimeMillis() - lastBrushTimestamp < cooldownMs;
    }

    public long getLastHygieneRainBonus() {
        return lastHygieneRainBonus;
    }

    public void setLastHygieneRainBonus(long lastHygieneRainBonus) {
        this.lastHygieneRainBonus = lastHygieneRainBonus;
    }

    // ==================== BREEDING ====================

    public boolean hasBred() {
        return hasBred;
    }

    public void setHasBred(boolean hasBred) {
        this.hasBred = hasBred;
    }

    /**
     * Checks if animal can breed based on Houseboundry rules.
     *
     * @return true if can breed
     */
    public boolean canBreed() {
        if (hasBred) {
            return false; // One-time breeding
        }
        if (phase != LifecyclePhase.PRODUCTIVE) {
            return false; // Must be productive
        }
        AnimalState state = AnimalState.calculateState(this);
        return state == AnimalState.GOLD || state == AnimalState.OK; // Must be OK or GOLD
    }

    // ==================== ENTITY TYPE ====================

    public ResourceLocation getEntityTypeId() {
        return entityTypeId;
    }

    public void setEntityTypeId(ResourceLocation entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    public void setEntityType(EntityType<?> entityType) {
        this.entityTypeId = EntityType.getKey(entityType);
    }

    // ==================== STATE CALCULATION ====================

    /**
     * Calculates and returns the current animal state.
     */
    public AnimalState getState() {
        return AnimalState.calculateState(this);
    }

    /**
     * Gets the adjusted production interval in milliseconds.
     *
     * @return interval in ms, or Long.MAX_VALUE if cannot produce
     */
    public long getAdjustedProductionInterval() {
        if (entityTypeId == null || phase != LifecyclePhase.PRODUCTIVE) {
            return Long.MAX_VALUE;
        }

        AnimalProductionConfig config = AnimalConfigRegistry.getConfig(entityTypeId).orElse(null);
        if (config == null || config.production() == null) {
            return Long.MAX_VALUE;
        }

        long baseIntervalMs = (long) (config.production().intervalHours() * 3600000L);

        AnimalState state = getState();
        double multiplier = state.getProductionMultiplier();

        if (multiplier < 0) {
            return Long.MAX_VALUE; // CRITICAL state - no production
        }

        return (long) (baseIntervalMs * multiplier);
    }

    /**
     * Checks if it's time to produce.
     *
     * @return true if production should occur
     */
    public boolean shouldProduce() {
        if (phase != LifecyclePhase.PRODUCTIVE || isDiseased()) {
            return false;
        }

        AnimalState state = getState();
        if (state == AnimalState.CRITICAL) {
            return false;
        }

        long interval = getAdjustedProductionInterval();
        if (interval == Long.MAX_VALUE) {
            return false;
        }

        long now = System.currentTimeMillis();
        return now - lastProductionTimestamp >= interval;
    }

    // ==================== NBT SERIALIZATION ====================

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        // Wellness stats
        tag.putInt("Comfort", comfort);
        tag.putInt("Stress", stress);
        tag.putInt("Hygiene", hygiene);

        // Lifecycle
        tag.putString("Phase", phase.name());
        tag.putLong("BirthTimestamp", birthTimestamp);
        tag.putLong("ProductiveStartTimestamp", productiveStartTimestamp);
        tag.putLong("LastProductionTimestamp", lastProductionTimestamp);

        // Disease
        tag.putLong("DiseaseStartTimestamp", diseaseStartTimestamp);

        // Interactions
        tag.putLong("LastBrushTimestamp", lastBrushTimestamp);
        tag.putLong("LastHygieneRainBonus", lastHygieneRainBonus);

        // Breeding
        tag.putBoolean("HasBred", hasBred);

        // Entity type
        if (entityTypeId != null) {
            tag.putString("EntityTypeId", entityTypeId.toString());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        // Wellness stats
        comfort = tag.contains("Comfort") ? tag.getInt("Comfort") : 50;
        stress = tag.contains("Stress") ? tag.getInt("Stress") : 30;
        hygiene = tag.contains("Hygiene") ? tag.getInt("Hygiene") : 100;

        // Lifecycle
        phase = tag.contains("Phase") ? LifecyclePhase.fromName(tag.getString("Phase")) : LifecyclePhase.BABY;
        birthTimestamp = tag.contains("BirthTimestamp") ? tag.getLong("BirthTimestamp") : System.currentTimeMillis();
        productiveStartTimestamp = tag.contains("ProductiveStartTimestamp") ? tag.getLong("ProductiveStartTimestamp") : 0;
        lastProductionTimestamp = tag.contains("LastProductionTimestamp") ? tag.getLong("LastProductionTimestamp") : 0;

        // Disease
        diseaseStartTimestamp = tag.contains("DiseaseStartTimestamp") ? tag.getLong("DiseaseStartTimestamp") : 0;

        // Interactions
        lastBrushTimestamp = tag.contains("LastBrushTimestamp") ? tag.getLong("LastBrushTimestamp") : 0;
        lastHygieneRainBonus = tag.contains("LastHygieneRainBonus") ? tag.getLong("LastHygieneRainBonus") : 0;

        // Breeding
        hasBred = tag.contains("HasBred") && tag.getBoolean("HasBred");

        // Entity type
        if (tag.contains("EntityTypeId")) {
            entityTypeId = ResourceLocation.tryParse(tag.getString("EntityTypeId"));
        }
    }
}
