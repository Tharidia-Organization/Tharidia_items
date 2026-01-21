package com.THproject.tharidia_things.houseboundry.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.StableBlockEntity;
import com.THproject.tharidia_things.houseboundry.AnimalState;
import com.THproject.tharidia_things.houseboundry.AnimalWellnessAttachments;
import com.THproject.tharidia_things.houseboundry.AnimalWellnessData;
import com.THproject.tharidia_things.houseboundry.LifecyclePhase;
import com.THproject.tharidia_things.houseboundry.config.AnimalConfigRegistry;
import com.THproject.tharidia_things.houseboundry.config.AnimalProductionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main tick handler for the Animal Wellness (Houseboundry) system.
 * Handles stat decay, lifecycle transitions, production, and disease.
 * Optimized for large servers with entity batching.
 */
@EventBusSubscriber(modid = TharidiaThings.MODID)
public class AnimalWellnessHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimalWellnessHandler.class);

    // Processing intervals (in ticks)
    private static final int STAT_DECAY_INTERVAL = 1200;      // 1 minute real time
    private static final int LIFECYCLE_CHECK_INTERVAL = 600;  // 30 seconds
    private static final int PRODUCTION_CHECK_INTERVAL = 200; // 10 seconds
    private static final int DISEASE_CHECK_INTERVAL = 24000;  // 1 MC day

    // Batch size for entity processing
    private static final int ENTITY_BATCH_SIZE = 20;

    /**
     * Initialize wellness data when an animal joins the world.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        // Only process animals (or any LivingEntity that's not a player)
        if (!(entity instanceof LivingEntity living) || entity instanceof Player) {
            return;
        }

        // Only process on server side
        if (entity.level().isClientSide) {
            return;
        }

        // Check if this is an animal or has a config
        boolean isAnimal = entity instanceof Animal;
        boolean hasConfig = AnimalConfigRegistry.hasConfig(entity.getType());

        if (!isAnimal && !hasConfig) {
            return; // Not an animal and no config - skip
        }

        // Get or create wellness data
        AnimalWellnessData data = living.getData(AnimalWellnessAttachments.WELLNESS_DATA);

        // Initialize entity type if not set
        if (data.getEntityTypeId() == null) {
            data.setEntityType(entity.getType());

            // If this is a baby mob, set appropriate phase
            if (entity instanceof AgeableMob ageable && ageable.isBaby()) {
                data.setPhase(LifecyclePhase.BABY);
                data.setBirthTimestamp(System.currentTimeMillis());
            } else {
                // Adult - start as productive
                data.setPhase(LifecyclePhase.PRODUCTIVE);
                data.setBirthTimestamp(System.currentTimeMillis() - 3600000L); // 1 hour ago
                data.setProductiveStartTimestamp(System.currentTimeMillis());
            }

            LOGGER.debug("Initialized wellness data for {} ({})",
                EntityType.getKey(entity.getType()), data.getPhase());
        }
    }

    /**
     * Main tick handler for animal wellness.
     * Uses batching to distribute load across ticks.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();

        // Only process LivingEntities that are not players
        if (!(entity instanceof LivingEntity living) || entity instanceof Player) {
            return;
        }

        // Server-side only
        if (entity.level().isClientSide) {
            return;
        }

        // Check if this entity should have wellness data
        boolean isAnimal = entity instanceof Animal;
        boolean hasConfig = AnimalConfigRegistry.hasConfig(entity.getType());

        if (!isAnimal && !hasConfig) {
            return;
        }

        // Batching: stagger entity processing based on entity ID
        int entityBatch = Math.abs(entity.getId() % ENTITY_BATCH_SIZE);
        long gameTick = entity.level().getGameTime();

        // Get wellness data
        AnimalWellnessData data = living.getData(AnimalWellnessAttachments.WELLNESS_DATA);

        // Initialize entity type if needed
        if (data.getEntityTypeId() == null) {
            data.setEntityType(entity.getType());
        }

        // Lifecycle check (every 30 seconds, batched)
        if ((gameTick + entityBatch) % LIFECYCLE_CHECK_INTERVAL == 0) {
            boolean changed = data.updatePhase();
            if (changed) {
                LOGGER.debug("{} transitioned to {} phase",
                    EntityType.getKey(entity.getType()), data.getPhase());
            }
        }

        // Stat decay (every 1 minute, batched)
        if ((gameTick + entityBatch) % STAT_DECAY_INTERVAL == 0) {
            processStatDecay(living, data);
        }

        // Production check (every 10 seconds, batched)
        if ((gameTick + entityBatch) % PRODUCTION_CHECK_INTERVAL == 0) {
            processProduction(living, data);
        }

        // Disease progression and chance (every MC day, batched)
        if ((gameTick + entityBatch) % DISEASE_CHECK_INTERVAL == 0) {
            processDiseaseCheck(living, data);
        }

        // Disease death check (every 10 seconds if diseased)
        if (data.isDiseased() && gameTick % 200 == 0) {
            processDiseaseProgression(living, data);
        }
    }

    /**
     * Process stat decay based on environmental factors.
     */
    private static void processStatDecay(LivingEntity entity, AnimalWellnessData data) {
        Level level = entity.level();

        // Base comfort decay: -0.5/hour = -0.5 per call (called every minute)
        float comfortDecay = 0.5f;

        // Check for nearby stable with bedding
        StableBlockEntity stable = findNearbyStable(entity);
        if (stable != null) {
            int freshness = stable.getBeddingFreshness();
            if (freshness >= 70) {
                comfortDecay *= 0.5f; // 50% reduced decay
            } else if (freshness >= 40) {
                // Normal decay
            } else if (freshness >= 10) {
                comfortDecay *= 1.25f; // 25% increased decay
            } else {
                comfortDecay *= 1.5f; // 50% increased decay
            }

            // Hygiene based on manure level
            int manure = stable.getManureAmount();
            float hygieneDecay = 0;
            if (manure > 90) {
                hygieneDecay = 4f; // per hour = 4 per call
            } else if (manure > 60) {
                hygieneDecay = 2f;
            } else if (manure > 30) {
                hygieneDecay = 1f;
            }
            data.addHygiene(-(int) hygieneDecay);
        }

        // Disease doubles comfort decay
        if (data.isDiseased()) {
            comfortDecay *= 2.0f;
        }

        data.addComfort(-(int) Math.ceil(comfortDecay));

        // Stress natural decay (only if comfort > 50)
        if (data.getComfort() > 50 && !data.isDiseased()) {
            data.addStress(-1); // -1 per hour
        }

        // Disease stress increase
        if (data.isDiseased()) {
            data.addStress(5); // +5 per hour
        }
    }

    /**
     * Process production if animal is ready.
     */
    private static void processProduction(LivingEntity entity, AnimalWellnessData data) {
        if (!data.shouldProduce()) {
            return;
        }

        AnimalProductionConfig config = AnimalConfigRegistry.getConfig(data.getEntityTypeId()).orElse(null);
        if (config == null || config.production() == null) {
            return;
        }

        // Produce items
        for (AnimalProductionConfig.ProductItem product : config.production().products()) {
            var item = BuiltInRegistries.ITEM.get(product.item());
            if (item != null) {
                ItemStack stack = new ItemStack(item, product.count());
                entity.spawnAtLocation(stack);
                LOGGER.debug("{} produced {}", EntityType.getKey(entity.getType()), product.item());
            }
        }

        data.setLastProductionTimestamp(System.currentTimeMillis());
    }

    /**
     * Check for disease chance based on hygiene.
     */
    private static void processDiseaseCheck(LivingEntity entity, AnimalWellnessData data) {
        if (data.isDiseased()) {
            return; // Already diseased
        }

        int hygiene = data.getHygiene();
        float chance = 0;

        if (hygiene < 20) {
            chance = 0.15f;
        } else if (hygiene < 40) {
            chance = 0.08f;
        } else if (hygiene < 60) {
            chance = 0.02f;
        }

        // Outbreak event (independent, 2% daily)
        chance += 0.02f;

        if (entity.level().random.nextFloat() < chance) {
            data.contractDisease();
            LOGGER.debug("{} contracted disease (hygiene: {})",
                EntityType.getKey(entity.getType()), hygiene);
            // TODO: Spawn green particles
        }
    }

    /**
     * Process disease progression (death after 120 minutes).
     */
    private static void processDiseaseProgression(LivingEntity entity, AnimalWellnessData data) {
        int minutes = data.getDiseaseDurationMinutes();

        // Warning at 100 minutes
        if (minutes >= 100 && minutes < 102) {
            LOGGER.debug("{} disease critical! ({} minutes)",
                EntityType.getKey(entity.getType()), minutes);
            // TODO: Red particles and urgent sound
        }

        // Death at 120 minutes
        if (minutes >= 120) {
            LOGGER.info("{} died from disease", EntityType.getKey(entity.getType()));
            entity.kill();
        }
    }

    /**
     * Find a nearby stable block entity.
     */
    private static StableBlockEntity findNearbyStable(LivingEntity entity) {
        Level level = entity.level();
        BlockPos entityPos = entity.blockPosition();

        // Search in a 5 block radius for stable blocks
        AABB searchBox = new AABB(entityPos).inflate(5);

        for (int x = (int) searchBox.minX; x <= searchBox.maxX; x++) {
            for (int y = (int) searchBox.minY; y <= searchBox.maxY; y++) {
                for (int z = (int) searchBox.minZ; z <= searchBox.maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof StableBlockEntity stable) {
                        return stable;
                    }
                }
            }
        }

        return null;
    }
}
