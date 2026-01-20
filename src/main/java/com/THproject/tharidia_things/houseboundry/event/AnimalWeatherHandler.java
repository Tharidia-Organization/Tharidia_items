package com.THproject.tharidia_things.houseboundry.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.StableBlockEntity;
import com.THproject.tharidia_things.houseboundry.AnimalWellnessAttachments;
import com.THproject.tharidia_things.houseboundry.AnimalWellnessData;
import com.THproject.tharidia_things.houseboundry.config.AnimalConfigRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles weather effects on animal wellness.
 * Rain: +5 hygiene per 5 min, -10 comfort per 5 min (if no shelter)
 * Thunder: +30 stress instantly
 */
@EventBusSubscriber(modid = TharidiaThings.MODID)
public class AnimalWeatherHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimalWeatherHandler.class);

    // Check interval: 5 minutes real time = 6000 ticks
    private static final int WEATHER_CHECK_INTERVAL = 6000;

    // Weather effects
    private static final int RAIN_HYGIENE_BONUS = 5;
    private static final int RAIN_COMFORT_PENALTY = 10;
    private static final int THUNDER_STRESS = 30;

    // Cooldown for rain hygiene bonus (5 minutes in ms)
    private static final long RAIN_HYGIENE_COOLDOWN_MS = 300000L;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        long gameTick = level.getGameTime();

        // Check every 5 minutes
        if (gameTick % WEATHER_CHECK_INTERVAL != 0) {
            return;
        }

        boolean isRaining = level.isRaining();
        boolean isThundering = level.isThundering();

        if (!isRaining && !isThundering) {
            return;
        }

        // Process all loaded entities
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(level.getSharedSpawnPos()).inflate(256))) {

            if (entity instanceof Player) {
                continue;
            }

            // Check if this entity should have wellness data
            boolean isAnimal = entity instanceof Animal;
            boolean hasConfig = AnimalConfigRegistry.hasConfig(entity.getType());

            if (!isAnimal && !hasConfig) {
                continue;
            }

            // Get wellness data
            AnimalWellnessData data = entity.getData(AnimalWellnessAttachments.WELLNESS_DATA);

            // Check if entity is sheltered
            boolean hasShelter = isEntitySheltered(entity);

            // Apply rain effects
            if (isRaining) {
                // Hygiene bonus (with cooldown)
                long now = System.currentTimeMillis();
                if (now - data.getLastHygieneRainBonus() >= RAIN_HYGIENE_COOLDOWN_MS) {
                    data.addHygiene(RAIN_HYGIENE_BONUS);
                    data.setLastHygieneRainBonus(now);
                    LOGGER.debug("{} got rain hygiene bonus", EntityType.getKey(entity.getType()));
                }

                // Comfort penalty if no shelter
                if (!hasShelter) {
                    data.addComfort(-RAIN_COMFORT_PENALTY);
                    LOGGER.debug("{} lost comfort from rain (no shelter)", EntityType.getKey(entity.getType()));
                }
            }

            // Apply thunder stress (instant, no cooldown)
            if (isThundering && !hasShelter) {
                data.addStress(THUNDER_STRESS);
                LOGGER.debug("{} stressed from thunder", EntityType.getKey(entity.getType()));
            }
        }
    }

    /**
     * Checks if an entity is protected by a shelter.
     * Entity is sheltered if:
     * - Near a stable with shelter upgrade
     * - Has a solid block above (natural cover)
     */
    private static boolean isEntitySheltered(LivingEntity entity) {
        BlockPos entityPos = entity.blockPosition();

        // Check for stable with shelter upgrade in 5 block radius
        StableBlockEntity stable = findNearbyStableWithShelter(entity);
        if (stable != null) {
            return true;
        }

        // Check for natural cover (solid block above)
        for (int y = 1; y <= 3; y++) {
            BlockPos above = entityPos.above(y);
            if (entity.level().getBlockState(above).isSolidRender(entity.level(), above)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find a nearby stable block entity with shelter upgrade.
     */
    private static StableBlockEntity findNearbyStableWithShelter(LivingEntity entity) {
        BlockPos entityPos = entity.blockPosition();
        AABB searchBox = new AABB(entityPos).inflate(5);

        for (int x = (int) searchBox.minX; x <= searchBox.maxX; x++) {
            for (int y = (int) searchBox.minY; y <= searchBox.maxY; y++) {
                for (int z = (int) searchBox.minZ; z <= searchBox.maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity be = entity.level().getBlockEntity(pos);
                    if (be instanceof StableBlockEntity stable && stable.hasShelterUpgrade()) {
                        return stable;
                    }
                }
            }
        }

        return null;
    }
}
