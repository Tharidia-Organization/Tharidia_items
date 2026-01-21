package com.THproject.tharidia_things.houseboundry.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Static registry for animal production configurations.
 * Populated by AnimalConfigLoader from datapacks.
 */
public class AnimalConfigRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimalConfigRegistry.class);
    private static final Map<ResourceLocation, AnimalProductionConfig> CONFIGS = new HashMap<>();

    /**
     * Clears all registered configurations.
     * Called before reloading datapacks.
     */
    public static void clear() {
        CONFIGS.clear();
        LOGGER.debug("Cleared animal production configs");
    }

    /**
     * Registers a configuration for an entity type.
     *
     * @param config the configuration to register
     */
    public static void register(AnimalProductionConfig config) {
        if (config == null || config.entityType() == null) {
            LOGGER.warn("Attempted to register null config or config with null entity type");
            return;
        }

        CONFIGS.put(config.entityType(), config);
        LOGGER.debug("Registered animal config for {}", config.entityType());
    }

    /**
     * Gets the configuration for an entity type.
     *
     * @param entityType the entity type resource location
     * @return Optional containing the config if found
     */
    public static Optional<AnimalProductionConfig> getConfig(ResourceLocation entityType) {
        return Optional.ofNullable(CONFIGS.get(entityType));
    }

    /**
     * Gets the configuration for an entity type.
     *
     * @param entityType the entity type
     * @return Optional containing the config if found
     */
    public static Optional<AnimalProductionConfig> getConfig(EntityType<?> entityType) {
        return getConfig(EntityType.getKey(entityType));
    }

    /**
     * Checks if a configuration exists for an entity type.
     *
     * @param entityType the entity type resource location
     * @return true if config exists
     */
    public static boolean hasConfig(ResourceLocation entityType) {
        return CONFIGS.containsKey(entityType);
    }

    /**
     * Checks if a configuration exists for an entity type.
     *
     * @param entityType the entity type
     * @return true if config exists
     */
    public static boolean hasConfig(EntityType<?> entityType) {
        return hasConfig(EntityType.getKey(entityType));
    }

    /**
     * Gets the number of registered configurations.
     *
     * @return count of configs
     */
    public static int getConfigCount() {
        return CONFIGS.size();
    }

    /**
     * Gets all registered entity types.
     *
     * @return iterable of entity type resource locations
     */
    public static Iterable<ResourceLocation> getRegisteredTypes() {
        return CONFIGS.keySet();
    }
}
