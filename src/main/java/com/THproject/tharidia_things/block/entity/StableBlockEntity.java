package com.THproject.tharidia_things.block.entity;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.stable.StableConfig;
import com.THproject.tharidia_things.stable.StableConfigLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StableBlockEntity extends BlockEntity {

    // Helper method to get config
    private static StableConfig cfg() {
        return StableConfigLoader.getConfig();
    }

    private final List<AnimalData> animals = new ArrayList<>();
    private int waterTicks = 0; // Time remaining for water
    private int foodAmount = 0; // Amount of food in feeder (0-64)
    private int foodConsumptionTicks = 0; // Ticks until next food consumption
    private int feedUses = 0; // Number of times animal feed has been used (0-5)
    private int manureAmount = 0; // Current manure level (0-100)
    private int manureProductionTicks = 0; // Ticks until next manure production

    // Houseboundry bedding system
    private int beddingFreshness = 0; // 0-100, 0 means no bedding
    private boolean hasShelterUpgrade = false; // Permanent shelter upgrade
    private long lastBeddingDecayMs = 0; // Timestamp for bedding decay (real-time)

    // Houseboundry wellness system - REAL-TIME TIMESTAMPS (no tick counters!)
    // This allows stats to update correctly even when chunk is unloaded
    private long lastWellnessUpdateMs = 0;      // Last wellness stat update (every 60000 ms = 1 min real)
    private long lastDiseaseCheckMs = 0;        // Last disease chance check (every 3600000 ms = 1 hour real)
    private long lastRandomEventCheckMs = 0;   // Last random event check (every 1200000 ms = 20 min real, ~1 MC day)
    private boolean wasRainingLastTick = false; // Track rain start for hygiene boost
    private boolean wasThunderingLastTick = false; // Track thunder for stress trigger
    
    // Animal wellness states (Houseboundry)
    public enum AnimalState {
        GOLD,      // Excellent condition - bonus production
        OK,        // Normal condition
        LOW,       // Poor condition - reduced production
        CRITICAL   // Very poor - no production, risk of death
    }

    public static class AnimalData {
        public EntityType<?> entityType;
        public boolean isBaby;
        public int growthTicks; // Time until animal grows up
        public int feedCount; // How much food has been given (for breeding)
        public int eggCount; // Current eggs available for collection
        public int totalEggsProduced; // Total eggs produced in lifetime (legacy, no longer kills)
        public int eggProductionTicks;
        // Milk production (Houseboundry)
        public int milkProductionTicks; // Ticks until milk is ready
        public boolean milkReady; // Whether milk can be collected

        // Houseboundry Wellness Stats
        public int comfort = 50;           // 0-100, default 50
        public int stress = 30;            // 0-100, default 30 (higher = worse)
        public int hygiene = 100;          // 0-100, default 100

        // Disease System
        public boolean diseased = false;
        public long diseaseStartTimestamp = 0;  // Unix ms, 0 if not diseased

        // Interaction cooldowns
        public long lastBrushTimestamp = 0;     // For 60s brush cooldown

        // Breeding
        public boolean hasBred = false;         // One-time breeding flag (per Houseboundry schema)

        // Weather tracking (to avoid multiple triggers per weather event)
        public boolean thunderTriggeredThisStorm = false;
        public boolean rainHygieneApplied = false;

        public AnimalData(EntityType<?> entityType) {
            this.entityType = entityType;
            this.isBaby = true;
            this.growthTicks = 0;
            this.feedCount = 0;
            this.eggCount = 0;
            this.totalEggsProduced = 0;
            this.eggProductionTicks = 0;
            this.milkProductionTicks = 0;
            this.milkReady = false;
            // Wellness defaults
            this.comfort = 50;
            this.stress = 30;
            this.hygiene = 100;
            this.diseased = false;
            this.diseaseStartTimestamp = 0;
            this.lastBrushTimestamp = 0;
            this.hasBred = false;
            this.thunderTriggeredThisStorm = false;
            this.rainHygieneApplied = false;
        }

        public AnimalData() {
            this(EntityType.PIG);
        }

        /**
         * Calculates the animal's current wellness state based on Houseboundry schema.
         * GOLD: comfort >= 70, stress <= 20, hygiene >= 60, not diseased
         * CRITICAL: diseased, comfort < 20, or stress >= 70
         * LOW: comfort < 40 or stress >= 50
         * OK: everything else
         */
        public AnimalState calculateState() {
            if (diseased || comfort < 20 || stress >= 70) return AnimalState.CRITICAL;
            if (comfort < 40 || stress >= 50) return AnimalState.LOW;
            if (comfort >= 70 && stress <= 20 && hygiene >= 60) return AnimalState.GOLD;
            return AnimalState.OK;
        }

        /**
         * Gets production multiplier based on state.
         * GOLD: 0.7 (faster production)
         * OK: 1.0 (normal)
         * LOW: 1.3 (slower)
         * CRITICAL: -1 (no production)
         */
        public float getProductionMultiplier() {
            return switch (calculateState()) {
                case GOLD -> 0.7f;
                case OK -> 1.0f;
                case LOW -> 1.3f;
                case CRITICAL -> -1f; // No production
            };
        }

        /**
         * Gets growth multiplier based on state.
         * GOLD: 0.75 (25% faster)
         * OK: 1.0 (normal)
         * LOW: 1.25 (25% slower)
         * CRITICAL: -1 (no growth)
         */
        public float getGrowthMultiplier() {
            return switch (calculateState()) {
                case GOLD -> 0.75f;
                case OK -> 1.0f;
                case LOW -> 1.25f;
                case CRITICAL -> -1f; // No growth
            };
        }

        public void save(CompoundTag tag) {
            tag.putString("EntityType", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
            tag.putBoolean("IsBaby", isBaby);
            tag.putInt("GrowthTicks", growthTicks);
            tag.putInt("FeedCount", feedCount);
            tag.putInt("EggCount", eggCount);
            tag.putInt("TotalEggsProduced", totalEggsProduced);
            tag.putInt("EggProductionTicks", eggProductionTicks);
            tag.putInt("MilkProductionTicks", milkProductionTicks);
            tag.putBoolean("MilkReady", milkReady);
            // Houseboundry Wellness
            tag.putInt("Comfort", comfort);
            tag.putInt("Stress", stress);
            tag.putInt("Hygiene", hygiene);
            tag.putBoolean("Diseased", diseased);
            tag.putLong("DiseaseStartTimestamp", diseaseStartTimestamp);
            tag.putLong("LastBrushTimestamp", lastBrushTimestamp);
            tag.putBoolean("HasBred", hasBred);
            tag.putBoolean("ThunderTriggeredThisStorm", thunderTriggeredThisStorm);
            tag.putBoolean("RainHygieneApplied", rainHygieneApplied);
        }

        public void load(CompoundTag tag) {
            ResourceLocation entityId = ResourceLocation.parse(tag.getString("EntityType"));
            entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);
            isBaby = tag.getBoolean("IsBaby");
            growthTicks = tag.getInt("GrowthTicks");
            feedCount = tag.getInt("FeedCount");
            eggCount = tag.getInt("EggCount");
            totalEggsProduced = tag.getInt("TotalEggsProduced");
            eggProductionTicks = tag.getInt("EggProductionTicks");
            milkProductionTicks = tag.contains("MilkProductionTicks") ? tag.getInt("MilkProductionTicks") : 0;
            milkReady = tag.contains("MilkReady") && tag.getBoolean("MilkReady");
            // Houseboundry Wellness (with defaults for old saves)
            comfort = tag.contains("Comfort") ? tag.getInt("Comfort") : 50;
            stress = tag.contains("Stress") ? tag.getInt("Stress") : 30;
            hygiene = tag.contains("Hygiene") ? tag.getInt("Hygiene") : 100;
            diseased = tag.contains("Diseased") && tag.getBoolean("Diseased");
            diseaseStartTimestamp = tag.contains("DiseaseStartTimestamp") ? tag.getLong("DiseaseStartTimestamp") : 0;
            lastBrushTimestamp = tag.contains("LastBrushTimestamp") ? tag.getLong("LastBrushTimestamp") : 0;
            hasBred = tag.contains("HasBred") && tag.getBoolean("HasBred");
            thunderTriggeredThisStorm = tag.contains("ThunderTriggeredThisStorm") && tag.getBoolean("ThunderTriggeredThisStorm");
            rainHygieneApplied = tag.contains("RainHygieneApplied") && tag.getBoolean("RainHygieneApplied");
        }
    }
    
    public StableBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.STABLE_BLOCK_ENTITY.get(), pos, state);
    }
    
    public boolean hasAnimal() {
        return !animals.isEmpty();
    }
    
    public List<AnimalData> getAnimals() {
        return animals;
    }
    
    public EntityType<?> getAnimalType() {
        return animals.isEmpty() ? null : animals.get(0).entityType;
    }
    
    public boolean isBaby() {
        return animals.isEmpty() ? false : animals.get(0).isBaby;
    }
    
    public int getEggCount() {
        return animals.isEmpty() ? 0 : animals.get(0).eggCount;
    }

    /**
     * Returns the total egg count from all chickens in the stable
     */
    public int getTotalEggCount() {
        int total = 0;
        for (AnimalData animal : animals) {
            if (!animal.isBaby && animal.eggCount > 0) {
                total += animal.eggCount;
            }
        }
        return total;
    }

    /**
     * Checks if there is an adult animal capable of producing milk.
     * This includes vanilla cows, goats, mooshrooms, and modded animals.
     */
    public boolean hasMilkProducingAnimal() {
        for (AnimalData animal : animals) {
            if (!animal.isBaby && isMilkProducingType(animal.entityType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an entity type can produce milk.
     * Supports vanilla animals (cow, goat, mooshroom) and modded animals.
     */
    private static boolean isMilkProducingType(EntityType<?> entityType) {
        // Vanilla milk-producing animals
        if (entityType == EntityType.COW ||
            entityType == EntityType.GOAT ||
            entityType == EntityType.MOOSHROOM) {
            return true;
        }

        // Check for modded animals by entity type ID
        // Common modded milk-producing animals often have "cow", "goat", or "milk" in their ID
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId != null) {
            String path = entityId.getPath().toLowerCase();
            if (path.contains("cow") || path.contains("goat") || path.contains("milk")) {
                return true;
            }
        }

        return false;
    }
    
    public boolean hasWater() {
        return waterTicks > 0;
    }
    
    public boolean canRefillWater() {
        return true;  // Always allow water refill, even when not empty
    }
    
    public void refillWater() {
        waterTicks = cfg().waterDurationTicks();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
    
    public int getFoodAmount() {
        return foodAmount;
    }

    /**
     * Returns the water level as a percentage (0.0 to 1.0)
     */
    public float getWaterLevel() {
        return (float) waterTicks / cfg().waterDurationTicks();
    }

    /**
     * Returns the food level as a percentage (0.0 to 1.0)
     */
    public float getFoodLevel() {
        return (float) foodAmount / cfg().maxFoodItems();
    }

    public int getManureAmount() {
        return manureAmount;
    }

    public boolean canCollectManure() {
        return manureAmount >= cfg().manureCollectAmount();
    }

    public void collectManure(Player player) {
        int collectAmount = cfg().manureCollectAmount();
        if (manureAmount >= collectAmount) {
            manureAmount -= collectAmount;
            player.addItem(new ItemStack(TharidiaThings.MANURE.get(), 1));

            // Houseboundry: Clean stall gives -10 stress to all animals
            for (AnimalData animal : animals) {
                animal.stress = Math.max(0, animal.stress - 10);
            }

            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                level.playSound(null, worldPosition, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
        }
    }

    // ==================== HOUSEBOUNDRY BEDDING ====================

    /**
     * Gets the current bedding freshness level.
     * @return 0-100, where 0 means no bedding and 100 is fresh bedding
     */
    public int getBeddingFreshness() {
        return beddingFreshness;
    }

    /**
     * Sets the bedding freshness level.
     * @param freshness value 0-100
     */
    public void setBeddingFreshness(int freshness) {
        this.beddingFreshness = Math.max(0, Math.min(100, freshness));
        this.lastBeddingDecayMs = System.currentTimeMillis(); // Reset decay timestamp when adding bedding
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Checks if stable has bedding.
     */
    public boolean hasBedding() {
        return beddingFreshness > 0;
    }

    /**
     * Removes bedding from stable, returns true if there was bedding to remove.
     */
    public boolean removeBedding() {
        if (beddingFreshness > 0) {
            beddingFreshness = 0;
            lastBeddingDecayMs = 0; // Reset timestamp since no bedding
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if stable has shelter upgrade installed.
     */
    public boolean hasShelterUpgrade() {
        return hasShelterUpgrade;
    }

    /**
     * Attempts to cure a diseased animal with honey bottle.
     * 60% success rate per Houseboundry schema.
     * @return true if an animal was cured, false if cure failed or no diseased animals
     */
    public boolean tryHoneyCure() {
        if (level == null) return false;

        for (AnimalData animal : animals) {
            if (animal.diseased) {
                if (level.random.nextFloat() < 0.60f) { // 60% success
                    animal.diseased = false;
                    animal.diseaseStartTimestamp = 0;
                    setChanged();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    level.playSound(null, worldPosition, SoundEvents.PLAYER_BURP, SoundSource.BLOCKS, 1.0F, 1.2F);
                    TharidiaThings.LOGGER.info("[STABLE] Honey cure successful!");
                    return true;
                } else {
                    TharidiaThings.LOGGER.info("[STABLE] Honey cure failed (40% chance)");
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Checks if any animal in the stable is diseased.
     */
    public boolean hasDiseasedAnimal() {
        for (AnimalData animal : animals) {
            if (animal.diseased) return true;
        }
        return false;
    }

    /**
     * Applies brush effect to an animal.
     * +12 comfort, -3 stress per Houseboundry schema.
     * 60 second cooldown per animal.
     * @return true if brushing was applied, false if on cooldown or no animals
     */
    public boolean applyBrush() {
        if (animals.isEmpty()) return false;

        long currentTime = System.currentTimeMillis();

        // Find an animal that can be brushed (not on cooldown)
        for (AnimalData animal : animals) {
            if (currentTime - animal.lastBrushTimestamp >= 60000) { // 60 second cooldown
                animal.comfort = Math.min(100, animal.comfort + 12);
                animal.stress = Math.max(0, animal.stress - 3);
                animal.lastBrushTimestamp = currentTime;
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    level.playSound(null, worldPosition, SoundEvents.HORSE_SADDLE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return true;
            }
        }

        return false; // All animals on cooldown
    }

    /**
     * Gets the time in seconds until an animal can be brushed again.
     * Returns 0 if an animal is ready to brush.
     */
    public int getBrushCooldownSeconds() {
        if (animals.isEmpty()) return 0;

        long currentTime = System.currentTimeMillis();
        int minCooldown = Integer.MAX_VALUE;

        for (AnimalData animal : animals) {
            long elapsed = currentTime - animal.lastBrushTimestamp;
            int remaining = (int) Math.max(0, (60000 - elapsed) / 1000);
            minCooldown = Math.min(minCooldown, remaining);
        }

        return minCooldown == Integer.MAX_VALUE ? 0 : minCooldown;
    }

    /**
     * Installs permanent shelter upgrade. Returns true if installed successfully.
     */
    public boolean installShelterUpgrade() {
        if (!hasShelterUpgrade) {
            hasShelterUpgrade = true;
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
            }
            return true;
        }
        return false;
    }

    public boolean canAddAnimalFeed() {
        return feedUses < cfg().feedUsesRequired();
    }

    public void addAnimalFeed() {
        if (feedUses < cfg().feedUsesRequired()) {
            feedUses++;
            if (feedUses >= cfg().feedUsesRequired()) {
                // Fill feeder completely when uses reached
                foodAmount = cfg().maxFoodItems();
                feedUses = 0; // Reset uses
            }
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                level.playSound(null, worldPosition, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }
    
    public boolean placeAnimal(EntityType<?> entityType) {
        if (animals.size() >= cfg().maxAnimals() - 1) { // -1 to leave room for breeding
            return false;
        }
        
        if (!animals.isEmpty() && !animals.get(0).entityType.equals(entityType)) {
            return false;
        }
        
        animals.add(new AnimalData(entityType));
        
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.playSound(null, worldPosition, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return true;
    }
    
    public boolean canFeed(ItemStack stack) {
        if (animals.size() != 2) {
            return false;
        }

        EntityType<?> entityType = animals.get(0).entityType;

        // Check breeding eligibility for both animals
        for (AnimalData animal : animals) {
            // Must be adult
            if (animal.isBaby) {
                return false;
            }
            // Houseboundry: Cannot breed if already bred once
            if (animal.hasBred) {
                return false;
            }
            // Houseboundry: Must be in OK or GOLD state
            AnimalState state = animal.calculateState();
            if (state == AnimalState.LOW || state == AnimalState.CRITICAL) {
                return false;
            }
        }

        // Check if correct food type and not fully fed
        int feedRequired = cfg().feedRequiredForBreeding();
        if (entityType == EntityType.COW && stack.is(Items.WHEAT)) {
            for (AnimalData animal : animals) {
                if (animal.feedCount < feedRequired) {
                    return true;
                }
            }
        } else if (entityType == EntityType.CHICKEN && stack.is(Items.WHEAT_SEEDS)) {
            for (AnimalData animal : animals) {
                if (animal.feedCount < feedRequired) {
                    return true;
                }
            }
        }

        return false;
    }
    
    public void feed(ItemStack stack) {
        if (!canFeed(stack)) {
            return;
        }
        
        // Feed the first animal that needs food
        for (AnimalData animal : animals) {
            if (!animal.isBaby && animal.feedCount < cfg().feedRequiredForBreeding()) {
                animal.feedCount++;
                
                if (level != null) {
                    level.playSound(null, worldPosition, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.5F, 1.0F);
                }
                
                // Check if both animals are fully fed for breeding
                checkBreeding();
                
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
                break;
            }
        }
    }
    
    private void checkBreeding() {
        if (animals.size() != 2 || animals.size() >= cfg().maxAnimals()) {
            return;
        }

        // Houseboundry: Check all breeding requirements
        int feedRequired = cfg().feedRequiredForBreeding();
        boolean canBreed = true;
        for (AnimalData animal : animals) {
            // Must be adult, fully fed, not already bred, and in OK or GOLD state
            if (animal.isBaby || animal.feedCount < feedRequired) {
                canBreed = false;
                break;
            }
            // Houseboundry: Animals can only breed once
            if (animal.hasBred) {
                canBreed = false;
                break;
            }
            // Houseboundry: Must be in OK or GOLD state (not LOW or CRITICAL)
            AnimalState state = animal.calculateState();
            if (state == AnimalState.LOW || state == AnimalState.CRITICAL) {
                canBreed = false;
                break;
            }
        }

        if (canBreed) {
            // Reset feed counts and mark as bred
            for (AnimalData animal : animals) {
                animal.feedCount = 0;
                animal.hasBred = true; // Houseboundry: One-time breeding
            }

            // Create baby
            animals.add(new AnimalData(animals.get(0).entityType));

            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 2.0F);
            }
        }
    }
    
    public boolean canCollectMilk() {
        for (AnimalData animal : animals) {
            if (!animal.isBaby && animal.milkReady && isMilkProducingType(animal.entityType)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Collects milk from the first available milk-producing animal.
     * The animal is NOT killed - it will produce more milk after a cooldown period.
     */
    public void collectMilk(Player player) {
        if (!canCollectMilk()) {
            return;
        }

        for (AnimalData animal : animals) {
            if (!animal.isBaby && animal.milkReady && isMilkProducingType(animal.entityType)) {
                // Reset milk production - animal will produce again after cooldown
                animal.milkReady = false;
                animal.milkProductionTicks = 0;

                if (level != null) {
                    level.playSound(null, worldPosition, SoundEvents.COW_MILK, SoundSource.BLOCKS, 1.0F, 1.0F);
                    // Give milk bucket to player
                    dropLoot(player, new ItemStack(Items.MILK_BUCKET, 1));
                }

                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
                break;
            }
        }
    }
    
    public boolean canCollectEggs() {
        if (animals.isEmpty() || animals.get(0).entityType != EntityType.CHICKEN) {
            return false;
        }
        
        for (AnimalData animal : animals) {
            if (!animal.isBaby && animal.eggCount > 0) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Collects eggs from all chickens that have eggs available.
     * Chickens are NOT killed - they continue living and producing eggs.
     * After reaching maxEggsPerChicken lifetime total, they simply stop producing (BARREN phase).
     */
    public void collectEggs(Player player) {
        if (!canCollectEggs()) {
            return;
        }

        int totalCollected = 0;
        for (AnimalData animal : animals) {
            if (!animal.isBaby && animal.eggCount > 0 && animal.entityType == EntityType.CHICKEN) {
                totalCollected += animal.eggCount;
                animal.eggCount = 0;
                animal.eggProductionTicks = 0;
            }
        }

        if (totalCollected > 0 && level != null) {
            level.playSound(null, worldPosition, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.addItem(new ItemStack(Items.EGG, totalCollected));
        }

        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Checks if there are any animals that can be slaughtered.
     */
    public boolean canSlaughter() {
        return !animals.isEmpty();
    }

    /**
     * Slaughters the first adult animal (or baby if no adults).
     * This is an EXPLICIT action - animals are only killed when the player chooses to slaughter.
     * Returns drops based on animal type.
     */
    public void slaughterAnimal(Player player) {
        if (animals.isEmpty()) {
            return;
        }

        // Prefer slaughtering adults, especially non-productive ones
        AnimalData toSlaughter = null;
        int slaughterIndex = -1;

        // First pass: look for barren/non-productive adults
        for (int i = 0; i < animals.size(); i++) {
            AnimalData animal = animals.get(i);
            if (!animal.isBaby) {
                // For chickens, prefer barren ones (reached max eggs)
                if (animal.entityType == EntityType.CHICKEN && animal.totalEggsProduced >= cfg().maxEggsPerChicken()) {
                    toSlaughter = animal;
                    slaughterIndex = i;
                    break;
                }
                // For other animals, just pick first adult
                if (toSlaughter == null) {
                    toSlaughter = animal;
                    slaughterIndex = i;
                }
            }
        }

        // If no adults found, slaughter a baby (not ideal but allowed)
        if (toSlaughter == null) {
            toSlaughter = animals.get(0);
            slaughterIndex = 0;
        }

        // Drop loot based on animal type
        dropSlaughterLoot(player, toSlaughter);

        // Remove the animal
        animals.remove(slaughterIndex);

        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Drops loot for a slaughtered animal based on its type.
     */
    private void dropSlaughterLoot(Player player, AnimalData animal) {
        boolean isAdult = !animal.isBaby;
        EntityType<?> type = animal.entityType;

        if (type == EntityType.COW || type == EntityType.MOOSHROOM) {
            dropLoot(player, new ItemStack(Items.BEEF, isAdult ? 2 : 1));
            if (isAdult) {
                dropLoot(player, new ItemStack(Items.LEATHER, 2));
            }
        } else if (type == EntityType.CHICKEN) {
            dropLoot(player, new ItemStack(Items.CHICKEN, 1));
            if (isAdult) {
                dropLoot(player, new ItemStack(Items.FEATHER, 2));
            }
        } else if (type == EntityType.PIG) {
            dropLoot(player, new ItemStack(Items.PORKCHOP, isAdult ? 2 : 1));
        } else if (type == EntityType.SHEEP) {
            dropLoot(player, new ItemStack(Items.MUTTON, isAdult ? 2 : 1));
            if (isAdult) {
                dropLoot(player, new ItemStack(Items.WHITE_WOOL, 1));
            }
        } else if (type == EntityType.GOAT) {
            // Goats don't drop specific meat in vanilla, give generic leather
            if (isAdult) {
                dropLoot(player, new ItemStack(Items.LEATHER, 1));
            }
        } else {
            // Generic fallback for modded animals
            if (isAdult) {
                dropLoot(player, new ItemStack(Items.LEATHER, 1));
            }
        }
    }

    private void dropLoot(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, StableBlockEntity entity) {
        boolean changed = false;
        StableConfig config = cfg();

        // Handle water consumption only if there are animals
        if (!entity.animals.isEmpty() && entity.waterTicks > 0) {
            entity.waterTicks--;
            if (entity.waterTicks == 0) {
                changed = true;
            }
        }

        // Handle food consumption only if there are animals
        if (!entity.animals.isEmpty() && entity.foodAmount > 0) {
            entity.foodConsumptionTicks++;
            if (entity.foodConsumptionTicks >= config.foodConsumptionRateTicks()) {
                entity.foodAmount--;
                entity.foodConsumptionTicks = 0;
                changed = true;
            }
        }

        // Handle manure production for each animal
        if (!entity.animals.isEmpty() && entity.manureAmount < config.maxManure()) {
            entity.manureProductionTicks++;
            int babyCount = 0;
            int adultCount = 0;
            for (AnimalData animal : entity.animals) {
                if (animal.isBaby) babyCount++;
                else adultCount++;
            }
            int effectiveRate = adultCount > 0 ? config.adultManureRateTicks() : config.babyManureRateTicks();
            if (entity.manureProductionTicks >= effectiveRate) {
                int manureProduced = adultCount + (babyCount > 0 ? 1 : 0);
                entity.manureAmount = Math.min(config.maxManure(), entity.manureAmount + manureProduced);
                entity.manureProductionTicks = 0;
                changed = true;
            }
        }

        // Handle bedding freshness decay (even without animals) - TIMESTAMP BASED
        // Uses config.beddingDecayIntervalTicks() converted to milliseconds
        // Default: 72000 ticks = 3600 seconds = 3600000 ms (1 hour real-time per freshness point)
        if (entity.beddingFreshness > 0) {
            long now = System.currentTimeMillis();
            long elapsedMs = now - entity.lastBeddingDecayMs;
            // Convert ticks to milliseconds: ticks / 20 ticks/sec * 1000 ms/sec = ticks * 50
            long decayIntervalMs = (long) config.beddingDecayIntervalTicks() * 50L;

            if (elapsedMs >= decayIntervalMs) {
                int decayAmount = (int) (elapsedMs / decayIntervalMs);
                entity.beddingFreshness = Math.max(0, entity.beddingFreshness - decayAmount);
                entity.lastBeddingDecayMs = now;
                changed = true;
            }
        }

        if (entity.animals.isEmpty()) {
            if (changed) {
                entity.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }
            return;
        }

        // ==================== HOUSEBOUNDRY WELLNESS SYSTEM ====================

        // Weather effects tracking
        boolean isRaining = level.isRaining();
        boolean isThundering = level.isThundering();

        // Check for rain START (hygiene boost - once per rain event)
        if (isRaining && !entity.wasRainingLastTick) {
            for (AnimalData animal : entity.animals) {
                if (!animal.rainHygieneApplied) {
                    animal.hygiene = Math.min(100, animal.hygiene + 15);
                    animal.rainHygieneApplied = true;
                    changed = true;
                }
            }
        }

        // Reset rain hygiene flag when rain stops
        if (!isRaining && entity.wasRainingLastTick) {
            for (AnimalData animal : entity.animals) {
                animal.rainHygieneApplied = false;
            }
        }

        // Thunder effects (instant, once per storm)
        // Stress: +15 without shelter, +8 with shelter
        // Comfort: -5 without shelter, -2 with shelter
        if (isThundering) {
            for (AnimalData animal : entity.animals) {
                if (!animal.thunderTriggeredThisStorm) {
                    int stressIncrease = entity.hasShelterUpgrade ? 8 : 15;
                    int comfortDrop = entity.hasShelterUpgrade ? 2 : 5;
                    animal.stress = Math.min(100, animal.stress + stressIncrease);
                    animal.comfort = Math.max(0, animal.comfort - comfortDrop);
                    animal.thunderTriggeredThisStorm = true;
                    changed = true;
                }
            }
        }

        // Reset thunder flag when storm ends
        if (!isThundering && entity.wasThunderingLastTick) {
            for (AnimalData animal : entity.animals) {
                animal.thunderTriggeredThisStorm = false;
            }
        }

        entity.wasRainingLastTick = isRaining;
        entity.wasThunderingLastTick = isThundering;

        // Wellness stat decay/gain (timestamp-based, every 60000ms = 1 minute real-time)
        long now = System.currentTimeMillis();
        long wellnessElapsedMs = now - entity.lastWellnessUpdateMs;

        // Process wellness updates based on real time elapsed (even if multiple intervals passed while unloaded)
        if (wellnessElapsedMs >= 60000) {
            int minutesPassed = (int) (wellnessElapsedMs / 60000);
            entity.lastWellnessUpdateMs = now;
            changed = true;

            for (AnimalData animal : entity.animals) {
                // === COMFORT DECAY ===
                // Base: -0.5/hour
                float comfortDecayPerHour = 0.5f;

                // Rain without shelter: -2/hour additional
                if (isRaining && !entity.hasShelterUpgrade) {
                    comfortDecayPerHour += 2.0f;
                }

                // Diseased: decay x2
                if (animal.diseased) {
                    comfortDecayPerHour *= 2.0f;
                }

                // Bedding effects on comfort decay
                if (entity.beddingFreshness >= 70) {
                    comfortDecayPerHour *= 0.5f; // Fresh bedding halves decay
                } else if (entity.beddingFreshness >= 10 && entity.beddingFreshness < 40) {
                    comfortDecayPerHour *= 1.25f;
                } else if (entity.beddingFreshness < 10 && entity.beddingFreshness > 0) {
                    comfortDecayPerHour *= 1.5f;
                }

                // Apply comfort decay based on actual minutes passed
                float comfortLoss = (comfortDecayPerHour / 60.0f) * minutesPassed;
                animal.comfort = Math.max(0, Math.min(100, animal.comfort - (int) Math.ceil(comfortLoss)));

                // === STRESS ===
                float stressChangePerHour = 0;

                // Diseased: +5/hour stress
                if (animal.diseased) {
                    stressChangePerHour += 5.0f;
                }

                // Shelter reduces stress: -2/hour
                if (entity.hasShelterUpgrade) {
                    stressChangePerHour -= 2.0f;
                }

                // Comfort > 50 reduces stress naturally: -1/hour
                if (animal.comfort > 50) {
                    stressChangePerHour -= 1.0f;
                }

                // Apply stress change based on actual minutes passed
                float stressChange = (stressChangePerHour / 60.0f) * minutesPassed;
                animal.stress = Math.max(0, Math.min(100, animal.stress + (int) Math.round(stressChange)));

                // === HYGIENE (based on manure level) ===
                float hygieneDecayPerHour = 0;

                if (entity.manureAmount > 30 && entity.manureAmount <= 60) {
                    hygieneDecayPerHour = 1.0f; // -1/hour
                } else if (entity.manureAmount > 60 && entity.manureAmount <= 90) {
                    hygieneDecayPerHour = 2.0f; // -2/hour
                } else if (entity.manureAmount > 90) {
                    hygieneDecayPerHour = 4.0f; // -4/hour
                }

                // Apply hygiene decay based on actual minutes passed
                float hygieneLoss = (hygieneDecayPerHour / 60.0f) * minutesPassed;
                animal.hygiene = Math.max(0, Math.min(100, animal.hygiene - (int) Math.ceil(hygieneLoss)));
            }
        }

        // Disease chance check (timestamp-based, every 3600000ms = 1 hour real-time)
        long diseaseElapsedMs = now - entity.lastDiseaseCheckMs;
        if (diseaseElapsedMs >= 3600000) {
            int hoursPassed = (int) (diseaseElapsedMs / 3600000);
            entity.lastDiseaseCheckMs = now;

            for (AnimalData animal : entity.animals) {
                if (!animal.diseased) {
                    // Disease chance based on hygiene (check once per hour passed)
                    for (int h = 0; h < hoursPassed; h++) {
                        float diseaseChance = 0;
                        if (animal.hygiene >= 60) {
                            diseaseChance = 0; // 0%
                        } else if (animal.hygiene >= 40) {
                            diseaseChance = 0.02f; // 2%
                        } else if (animal.hygiene >= 20) {
                            diseaseChance = 0.08f; // 8%
                        } else {
                            diseaseChance = 0.15f; // 15%
                        }

                        if (diseaseChance > 0 && level.random.nextFloat() < diseaseChance) {
                            animal.diseased = true;
                            animal.diseaseStartTimestamp = now;
                            changed = true;
                            TharidiaThings.LOGGER.info("[STABLE] Animal became diseased! Hygiene was: {}", animal.hygiene);
                            break; // Animal is now diseased, stop checking
                        }
                    }
                }
            }
        }

        // Disease progression check (uses 'now' timestamp declared earlier)
        List<AnimalData> toRemove = new ArrayList<>();
        for (AnimalData animal : entity.animals) {
            if (animal.diseased && animal.diseaseStartTimestamp > 0) {
                long diseaseMinutes = (now - animal.diseaseStartTimestamp) / 60000;

                // After 120 minutes (2 hours real time) - animal dies
                if (diseaseMinutes >= 120) {
                    toRemove.add(animal);
                    TharidiaThings.LOGGER.info("[STABLE] Animal died from disease after {} minutes", diseaseMinutes);
                }
            }

            // Natural cure: hygiene > 80 for 2 days (48 hours real = 2880000 ms)
            // Simplified: if hygiene is > 80 and disease has been present < 30 min, 1% chance per minute to cure
            if (animal.diseased && animal.hygiene > 80) {
                long diseaseMinutes = (now - animal.diseaseStartTimestamp) / 60000;
                if (diseaseMinutes < 30 && level.random.nextFloat() < 0.01f) {
                    animal.diseased = false;
                    animal.diseaseStartTimestamp = 0;
                    changed = true;
                    TharidiaThings.LOGGER.info("[STABLE] Animal naturally cured from disease!");
                }
            }
        }

        // Remove dead animals
        for (AnimalData dead : toRemove) {
            entity.animals.remove(dead);
            changed = true;
            level.playSound(null, pos, SoundEvents.WOLF_DEATH, SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        // Random events check (timestamp-based, every 1200000ms = 20 minutes real-time = ~1 MC day)
        long randomEventElapsedMs = now - entity.lastRandomEventCheckMs;
        if (randomEventElapsedMs >= 1200000) {
            int daysPassed = (int) (randomEventElapsedMs / 1200000);
            entity.lastRandomEventCheckMs = now;

            // Process random events for each "day" passed
            for (int d = 0; d < daysPassed; d++) {
                float eventRoll = level.random.nextFloat();

                // 5% chance: "Content Animals" - +20 comfort to all
                if (eventRoll < 0.05f) {
                    for (AnimalData animal : entity.animals) {
                        animal.comfort = Math.min(100, animal.comfort + 20);
                    }
                    changed = true;
                    TharidiaThings.LOGGER.info("[STABLE] Random event: Content Animals! +20 comfort to all");
                }
                // 8% chance: "Restless Night" - -15 comfort (skip if bedding > 70)
                else if (eventRoll < 0.13f) { // 0.05 + 0.08
                    if (entity.beddingFreshness <= 70) {
                        for (AnimalData animal : entity.animals) {
                            animal.comfort = Math.max(0, animal.comfort - 15);
                        }
                        changed = true;
                        TharidiaThings.LOGGER.info("[STABLE] Random event: Restless Night! -15 comfort to all");
                    } else {
                        TharidiaThings.LOGGER.info("[STABLE] Random event: Restless Night blocked by fresh bedding!");
                    }
                }

                // 2% chance: "Outbreak Event" - one random healthy animal gets diseased (independent of hygiene)
                float outbreakRoll = level.random.nextFloat();
                if (outbreakRoll < 0.02f) {
                    // Find healthy animals
                    List<AnimalData> healthyAnimals = new ArrayList<>();
                    for (AnimalData animal : entity.animals) {
                        if (!animal.diseased) {
                            healthyAnimals.add(animal);
                        }
                    }
                    if (!healthyAnimals.isEmpty()) {
                        AnimalData victim = healthyAnimals.get(level.random.nextInt(healthyAnimals.size()));
                        victim.diseased = true;
                        victim.diseaseStartTimestamp = now;
                        changed = true;
                        TharidiaThings.LOGGER.info("[STABLE] Random event: OUTBREAK! One animal became diseased randomly");
                    }
                }
            }
        }

        // ==================== END HOUSEBOUNDRY WELLNESS ====================

        // Only allow growth and production if there is water and food
        boolean hasWater = entity.waterTicks > 0;
        boolean hasFood = entity.foodAmount > 0;

        for (AnimalData animal : entity.animals) {
            AnimalState animalState = animal.calculateState();
            float growthMult = animal.getGrowthMultiplier();
            float productionMult = animal.getProductionMultiplier();

            // Handle growth for baby animals (only with water and food, and not CRITICAL)
            if (animal.isBaby && hasWater && hasFood && growthMult > 0) {
                animal.growthTicks++;
                int effectiveGrowthTime = (int) (config.growthTimeTicks() * growthMult);
                if (animal.growthTicks >= effectiveGrowthTime) {
                    animal.isBaby = false;
                    animal.growthTicks = 0;
                    changed = true;
                    level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5F, 1.5F);
                }
            }

            // Handle egg production (only with water, food, not CRITICAL, not BARREN)
            if (animal.entityType == EntityType.CHICKEN && !animal.isBaby
                    && animal.totalEggsProduced < config.maxEggsPerChicken()
                    && hasWater && hasFood && productionMult > 0) {
                animal.eggProductionTicks++;
                int effectiveEggTime = (int) (config.eggProductionTimeTicks() * productionMult);
                if (animal.eggProductionTicks >= effectiveEggTime) {
                    animal.eggCount++;
                    animal.totalEggsProduced++;
                    animal.eggProductionTicks = 0;
                    changed = true;
                    level.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 0.8F, 1.0F);
                }
            }

            // Handle milk production (only with water, food, not CRITICAL)
            if (isMilkProducingType(animal.entityType) && !animal.isBaby && !animal.milkReady
                    && hasWater && hasFood && productionMult > 0) {
                animal.milkProductionTicks++;
                int effectiveMilkTime = (int) (config.milkProductionTimeTicks() * productionMult);
                if (animal.milkProductionTicks >= effectiveMilkTime) {
                    animal.milkReady = true;
                    animal.milkProductionTicks = 0;
                    changed = true;
                    level.playSound(null, pos, SoundEvents.COW_AMBIENT, SoundSource.BLOCKS, 0.5F, 1.2F);
                }
            }
        }

        if (changed) {
            entity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        ListTag animalList = new ListTag();
        for (AnimalData animal : animals) {
            CompoundTag animalTag = new CompoundTag();
            animal.save(animalTag);
            animalList.add(animalTag);
        }
        tag.put("Animals", animalList);
        tag.putInt("WaterTicks", waterTicks);
        tag.putInt("FoodAmount", foodAmount);
        tag.putInt("FoodConsumptionTicks", foodConsumptionTicks);
        tag.putInt("FeedUses", feedUses);
        tag.putInt("ManureAmount", manureAmount);
        tag.putInt("ManureProductionTicks", manureProductionTicks);
        // Houseboundry
        tag.putInt("BeddingFreshness", beddingFreshness);
        tag.putBoolean("HasShelterUpgrade", hasShelterUpgrade);
        tag.putLong("LastBeddingDecayMs", lastBeddingDecayMs);
        // Houseboundry wellness timestamps (real-time based)
        tag.putLong("LastWellnessUpdateMs", lastWellnessUpdateMs);
        tag.putLong("LastDiseaseCheckMs", lastDiseaseCheckMs);
        tag.putLong("LastRandomEventCheckMs", lastRandomEventCheckMs);
        tag.putBoolean("WasRainingLastTick", wasRainingLastTick);
        tag.putBoolean("WasThunderingLastTick", wasThunderingLastTick);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        animals.clear();
        ListTag animalList = tag.getList("Animals", Tag.TAG_COMPOUND);
        for (int i = 0; i < animalList.size(); i++) {
            CompoundTag animalTag = animalList.getCompound(i);
            AnimalData animal = new AnimalData();
            animal.load(animalTag);
            animals.add(animal);
        }
        waterTicks = tag.getInt("WaterTicks");
        foodAmount = tag.getInt("FoodAmount");
        foodConsumptionTicks = tag.getInt("FoodConsumptionTicks");
        feedUses = tag.getInt("FeedUses");
        manureAmount = tag.getInt("ManureAmount");
        manureProductionTicks = tag.getInt("ManureProductionTicks");
        // Houseboundry
        beddingFreshness = tag.contains("BeddingFreshness") ? tag.getInt("BeddingFreshness") : 0;
        hasShelterUpgrade = tag.contains("HasShelterUpgrade") && tag.getBoolean("HasShelterUpgrade");
        lastBeddingDecayMs = tag.contains("LastBeddingDecayMs") ? tag.getLong("LastBeddingDecayMs") : System.currentTimeMillis();
        // Houseboundry wellness timestamps (real-time based)
        long now = System.currentTimeMillis();
        lastWellnessUpdateMs = tag.contains("LastWellnessUpdateMs") ? tag.getLong("LastWellnessUpdateMs") : now;
        lastDiseaseCheckMs = tag.contains("LastDiseaseCheckMs") ? tag.getLong("LastDiseaseCheckMs") : now;
        lastRandomEventCheckMs = tag.contains("LastRandomEventCheckMs") ? tag.getLong("LastRandomEventCheckMs") : now;
        wasRainingLastTick = tag.contains("WasRainingLastTick") && tag.getBoolean("WasRainingLastTick");
        wasThunderingLastTick = tag.contains("WasThunderingLastTick") && tag.getBoolean("WasThunderingLastTick");
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
