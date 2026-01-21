package com.THproject.tharidia_things.character;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.entity.RacePointEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Handles character creation events.
 * IMPORTANT: This handler manages the character creation flow:
 * 1. New players are teleported to character_creation dimension
 * 2. Players must select a race before being teleported back
 * 3. Players who already created their character should NEVER be sent back
 */
@EventBusSubscriber(modid = "tharidiathings")
public class CharacterEventHandler {

    private static final String THARIDIA_FEATURES_MODID = "tharidia_features";
    public static final ResourceKey<Level> CHARACTER_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("tharidiathings", "character_creation"));

    private static final int CHUNK_SPACING = 100;
    private static final int PLATFORM_RADIUS = 10;
    private static final int CLEANUP_RADIUS = 2;

    private static Constructor<?> customBorderConstructor;
    private static Method worldBorderGetMethod;
    private static Method worldBorderAddBorderMethod;
    private static Method worldBorderRemoveBorderMethod;
    private static boolean worldBorderReflectionInitialized = false;
    private static boolean worldBorderReflectionAvailable = false;

    /**
     * Calculate player's unique chunk coordinates based on their UUID.
     * Uses Math.abs() to ensure positive coordinates and avoid collisions.
     */
    public static ChunkPos getPlayerAreaChunk(UUID playerUUID) {
        int playerHash = Math.abs(playerUUID.hashCode());
        int playerChunkX = (playerHash % 1000) * CHUNK_SPACING;
        int playerChunkZ = ((playerHash / 1000) % 1000) * CHUNK_SPACING;
        return new ChunkPos(playerChunkX, playerChunkZ);
    }

    /**
     * Get the spawn position for a player in the character creation dimension
     */
    public static BlockPos getPlayerSpawnPos(UUID playerUUID) {
        ChunkPos chunk = getPlayerAreaChunk(playerUUID);
        return new BlockPos(chunk.x * 16, 100, chunk.z * 16);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Check if player was in character creation dimension
            if (player.level().dimension().equals(CHARACTER_DIMENSION)) {
                // Clean up the player's area
                cleanupPlayerArea(player.server.getLevel(CHARACTER_DIMENSION), player.getUUID());
            }
        }
    }

    /**
     * Cleans up the character creation area for a specific player.
     * This is a centralized method that can be called from multiple places.
     */
    public static void cleanupPlayerArea(ServerLevel characterLevel, UUID playerUUID) {
        if (characterLevel == null) {
            TharidiaThings.LOGGER.warn("Cannot cleanup player area - character level is null");
            return;
        }

        ChunkPos playerChunk = getPlayerAreaChunk(playerUUID);
        BlockPos center = new BlockPos(playerChunk.x * 16, 100, playerChunk.z * 16);

        TharidiaThings.LOGGER.info("Cleaning up character creation area for player at chunk ({}, {})",
                playerChunk.x, playerChunk.z);

        // Remove entities in the player's area using proper AABB around the actual center
        AABB cleanupArea = new AABB(
                center.getX() - 50, center.getY() - 50, center.getZ() - 50,
                center.getX() + 50, center.getY() + 50, center.getZ() + 50
        );

        characterLevel.getEntitiesOfClass(RacePointEntity.class, cleanupArea)
                .forEach(entity -> {
                    TharidiaThings.LOGGER.debug("Removing RacePointEntity at {}", entity.position());
                    entity.discard();
                });

        // Clear blocks in the player's area
        for (int x = -CLEANUP_RADIUS; x <= CLEANUP_RADIUS; x++) {
            for (int z = -CLEANUP_RADIUS; z <= CLEANUP_RADIUS; z++) {
                ChunkPos chunkPos = new ChunkPos(playerChunk.x + x, playerChunk.z + z);
                if (characterLevel.hasChunk(chunkPos.x, chunkPos.z)) {
                    // Reset all blocks to air (limited Y range for performance)
                    for (int bx = 0; bx < 16; bx++) {
                        for (int bz = 0; bz < 16; bz++) {
                            for (int by = 80; by < 120; by++) {
                                BlockPos blockPos = new BlockPos(
                                        chunkPos.getMinBlockX() + bx, by, chunkPos.getMinBlockZ() + bz);
                                if (!characterLevel.isEmptyBlock(blockPos)) {
                                    characterLevel.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.server == null) {
                TharidiaThings.LOGGER.warn("Server is null during login event for player {}",
                        player.getName().getString());
                return;
            }

            CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);

            // CRITICAL CHECK: Only teleport if character has NOT been created
            if (characterData != null && !characterData.hasCreatedCharacter()) {
                TharidiaThings.LOGGER.info("Player {} needs to create character - scheduling teleport",
                        player.getName().getString());

                // Delay teleportation to ensure dimension and player are fully initialized
                player.server.execute(() -> {
                    player.server.execute(() -> {
                        // Double check player is still valid and connected
                        if (!player.isRemoved() && player.connection != null) {
                            // Re-check character status in case it changed
                            CharacterData currentData = player.getData(CharacterAttachments.CHARACTER_DATA);
                            if (currentData != null && !currentData.hasCreatedCharacter()) {
                                teleportToCharacterDimension(player);
                            }
                        }
                    });
                });
            } else {
                TharidiaThings.LOGGER.debug("Player {} already has character - skipping teleport",
                        player.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);

            if (characterData == null) {
                TharidiaThings.LOGGER.warn("Character data is null for player {}",
                        player.getName().getString());
                return;
            }

            // If player hasn't created character and is not in character dimension, teleport them back
            if (!characterData.hasCreatedCharacter() && !event.getTo().equals(CHARACTER_DIMENSION)) {
                TharidiaThings.LOGGER.info("Player {} tried to leave character dimension without creating character",
                        player.getName().getString());
                teleportToCharacterDimension(player);
            }

            // If player is exiting character dimension and has created character, set to survival
            if (characterData.hasCreatedCharacter() &&
                    event.getFrom().equals(CHARACTER_DIMENSION) &&
                    !event.getTo().equals(CHARACTER_DIMENSION)) {
                player.server.execute(() -> {
                    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                    player.setInvulnerable(false);
                });
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);

            if (characterData == null) {
                TharidiaThings.LOGGER.warn("Character data is null for respawning player {}",
                        player.getName().getString());
                return;
            }

            // CRITICAL FIX: Only teleport to character creation if character has NOT been created
            // This was the main bug - players who already created characters were being sent back
            if (!characterData.hasCreatedCharacter()) {
                TharidiaThings.LOGGER.info("Player {} respawned but hasn't created character - teleporting to creation",
                        player.getName().getString());

                // Delay teleport to ensure player is fully respawned
                player.server.execute(() -> {
                    player.server.execute(() -> {
                        // Re-check status - might have changed
                        CharacterData currentData = player.getData(CharacterAttachments.CHARACTER_DATA);
                        if (currentData != null && !currentData.hasCreatedCharacter()) {
                            // Restore health before teleporting
                            player.setHealth(player.getMaxHealth());
                            player.getFoodData().setFoodLevel(20);
                            player.getFoodData().setSaturation(20.0f);

                            teleportToCharacterDimension(player);
                        }
                    });
                });
            } else {
                TharidiaThings.LOGGER.debug("Player {} respawned with existing character - normal respawn",
                        player.getName().getString());
            }
        }
    }

    private static void teleportToCharacterDimension(ServerPlayer player) {
        TharidiaThings.LOGGER.info("Teleporting player {} to character dimension", player.getName().getString());

        ServerLevel characterLevel = player.server.getLevel(CHARACTER_DIMENSION);

        if (characterLevel == null) {
            TharidiaThings.LOGGER.error("Character creation dimension not found! Cannot teleport player {}",
                    player.getName().getString());
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cCharacter creation dimension not found! Please contact an administrator."));

            // Set character as created to prevent infinite loop
            CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
            if (characterData != null) {
                characterData.setCharacterCreated(true);
            }

            // Teleport to overworld spawn as fallback
            ServerLevel overworld = player.server.overworld();
            if (overworld != null) {
                BlockPos spawnPos = overworld.getSharedSpawnPos();
                player.teleportTo(overworld, spawnPos.getX() + 0.5, spawnPos.getY() + 1,
                        spawnPos.getZ() + 0.5, 0, 0);
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            }
            return;
        }

        // Get player's unique area
        BlockPos spawnPos = getPlayerSpawnPos(player.getUUID());
        ChunkPos playerChunk = getPlayerAreaChunk(player.getUUID());

        TharidiaThings.LOGGER.info("Player {} area: chunk ({}, {}), spawn pos: {}",
                player.getName().getString(), playerChunk.x, playerChunk.z, spawnPos);

        // Pre-load chunks in the area to ensure they're ready
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                characterLevel.getChunk(playerChunk.x + x, playerChunk.z + z);
            }
        }

        // Find a safe Y position
        spawnPos = findSafeYPosition(characterLevel, spawnPos);

        TharidiaThings.LOGGER.info("Final spawn position after findSafeYPosition: {}", spawnPos);

        // Create the stone platform if it doesn't exist
        createStonePlatform(characterLevel, spawnPos);

        TharidiaThings.LOGGER.info("Teleporting player {} to position: {}",
                player.getName().getString(), spawnPos);

        // Create final copies for lambda
        final BlockPos finalSpawnPos = spawnPos;
        final ServerLevel finalCharacterLevel = characterLevel;

        // Teleport the player to the exact center of the platform
        player.teleportTo(characterLevel, spawnPos.getX() + 0.5, spawnPos.getY() + 1,
                spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());

        // Set game mode to adventure (delayed to ensure it applies after teleport)
        player.server.execute(() -> {
            player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
            player.setInvulnerable(true);
            player.removeAllEffects();

            // Restore full health and food
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0f);

            boolean borderCreated = createCharacterCreationBorder(player, finalSpawnPos);
            if (!borderCreated) {
                TharidiaThings.LOGGER.warn("Unable to create character border for {}; tharidia_features integration unavailable.",
                        player.getGameProfile().getName());
            }

            spawnRacePoints(finalCharacterLevel, finalSpawnPos.above(1), player.getUUID());
        });

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§eWelcome! Please create your character before proceeding."));
    }

    private static BlockPos findSafeYPosition(ServerLevel level, BlockPos pos) {
        // Ensure chunk is loaded
        level.getChunk(pos);

        // Start from y=100 and go down until we find air
        for (int y = 100; y > 50; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (level.isEmptyBlock(checkPos) && level.isEmptyBlock(checkPos.above())) {
                return checkPos;
            }
        }
        // If no safe position found, return Y=100 (platform will be created)
        return new BlockPos(pos.getX(), 100, pos.getZ());
    }

    private static void createStonePlatform(ServerLevel level, BlockPos center) {
        // Ensure chunk is loaded before placing blocks
        level.getChunk(center);

        TharidiaThings.LOGGER.info("Creating stone platform centered at: {}", center);

        int platformY = center.getY() - 1;
        int blocksPlaced = 0;

        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                BlockPos pos = new BlockPos(center.getX() + x, platformY, center.getZ() + z);

                level.getChunk(pos);

                if (level.isEmptyBlock(pos) || level.getBlockState(pos).canBeReplaced()) {
                    level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                    blocksPlaced++;
                }
            }
        }

        // Add barrier walls around the platform
        for (int x = -PLATFORM_RADIUS - 1; x <= PLATFORM_RADIUS + 1; x++) {
            for (int z = -PLATFORM_RADIUS - 1; z <= PLATFORM_RADIUS + 1; z++) {
                if (Math.abs(x) == PLATFORM_RADIUS + 1 || Math.abs(z) == PLATFORM_RADIUS + 1) {
                    for (int y = 1; y <= 4; y++) {
                        BlockPos pos = new BlockPos(center.getX() + x, platformY + y, center.getZ() + z);
                        level.getChunk(pos);
                        if (level.isEmptyBlock(pos)) {
                            level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        TharidiaThings.LOGGER.info("Platform creation complete. Centered at {}, Placed {} blocks",
                center, blocksPlaced);
    }

    /**
     * Spawns a single selection point at the center for the given player
     */
    private static void spawnRacePoints(ServerLevel level, BlockPos center, UUID playerUUID) {
        // FIXED: Use the correct center based on player's area, not a fixed position
        AABB searchArea = new AABB(
                center.getX() - 50, center.getY() - 50, center.getZ() - 50,
                center.getX() + 50, center.getY() + 50, center.getZ() + 50
        );

        // Remove existing race point entities in this player's area
        level.getEntitiesOfClass(RacePointEntity.class, searchArea)
                .forEach(entity -> {
                    TharidiaThings.LOGGER.debug("Removing existing RacePointEntity at {}", entity.position());
                    entity.discard();
                });

        // Spawn a single selection point at the center
        RacePointEntity selectionPoint = new RacePointEntity(
                level, center.getX(), center.getY(), center.getZ(),
                "scegli il tuo percorso", 0xFFFFFF);
        level.addFreshEntity(selectionPoint);

        TharidiaThings.LOGGER.info("Spawned RacePointEntity at {} for player area", center);
    }

    /**
     * Call this when player successfully creates their character.
     * This is the central method that should be called to complete character creation.
     */
    public static void completeCharacterCreation(ServerPlayer player) {
        CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);

        if (characterData == null) {
            TharidiaThings.LOGGER.error("Cannot complete character creation - CharacterData is null for {}",
                    player.getName().getString());
            return;
        }

        // Mark character as created FIRST
        characterData.setCharacterCreated(true);

        TharidiaThings.LOGGER.info("Character creation completed for player {}", player.getName().getString());

        // Remove invulnerability
        player.setInvulnerable(false);

        // Remove character creation border
        removeCharacterCreationBorder(player);

        // Clean up the player's area in the character dimension
        ServerLevel characterLevel = player.server.getLevel(CHARACTER_DIMENSION);
        if (characterLevel != null) {
            cleanupPlayerArea(characterLevel, player.getUUID());
        }

        // Teleport player back to overworld spawn
        ServerLevel overworld = player.server.overworld();
        BlockPos spawnPos = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawnPos.getX() + 0.5, spawnPos.getY() + 1,
                spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());

        // Set survival mode
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    }

    /**
     * Checks if a player has completed character creation
     */
    public static boolean hasCompletedCharacterCreation(ServerPlayer player) {
        CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
        return characterData != null && characterData.hasCreatedCharacter();
    }

    /**
     * Resets character creation status for a player (admin use)
     */
    public static void resetCharacterCreation(ServerPlayer player) {
        CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
        if (characterData != null) {
            characterData.setCharacterCreated(false);
            TharidiaThings.LOGGER.info("Character creation reset for player {}", player.getName().getString());
        }
    }

    private static boolean createCharacterCreationBorder(ServerPlayer player, BlockPos platformCenter) {
        if (!ensureWorldBorderReflection()) {
            return false;
        }

        double borderMinX = platformCenter.getX() - 11;
        double borderMinZ = platformCenter.getZ() - 11;
        double borderMaxX = platformCenter.getX() + 11;
        double borderMaxZ = platformCenter.getZ() + 11;

        String borderName = "character_creation_" + player.getUUID().toString().substring(0, 8);
        ResourceLocation dimension = CHARACTER_DIMENSION.location();

        try {
            Object data = worldBorderGetMethod.invoke(null);
            Object border = customBorderConstructor.newInstance(borderMinX, borderMinZ, borderMaxX, borderMaxZ);
            worldBorderAddBorderMethod.invoke(data, dimension, borderName, border);
            TharidiaThings.LOGGER.info("Created character creation border '{}' at ({}, {}) to ({}, {}) for player {}",
                    borderName, borderMinX, borderMinZ, borderMaxX, borderMaxZ, player.getName().getString());
            return true;
        } catch (Exception ex) {
            TharidiaThings.LOGGER.warn("Failed to create character creation border for {}",
                    player.getName().getString(), ex);
            return false;
        }
    }

    private static void removeCharacterCreationBorder(ServerPlayer player) {
        if (!ensureWorldBorderReflection()) {
            return;
        }

        String borderName = "character_creation_" + player.getUUID().toString().substring(0, 8);
        ResourceLocation dimension = CHARACTER_DIMENSION.location();

        try {
            Object data = worldBorderGetMethod.invoke(null);
            worldBorderRemoveBorderMethod.invoke(data, dimension, borderName);
            TharidiaThings.LOGGER.info("Removed character creation border '{}' for player {}",
                    borderName, player.getName().getString());
        } catch (Exception ex) {
            TharidiaThings.LOGGER.warn("Failed to remove character creation border for {}",
                    player.getName().getString(), ex);
        }
    }

    private static boolean ensureWorldBorderReflection() {
        if (worldBorderReflectionInitialized) {
            return worldBorderReflectionAvailable;
        }

        worldBorderReflectionInitialized = true;

        if (!ModList.get().isLoaded(THARIDIA_FEATURES_MODID)) {
            TharidiaThings.LOGGER.warn("Tharidia Features mod not detected; character borders will be disabled.");
            worldBorderReflectionAvailable = false;
            return false;
        }

        try {
            Class<?> dataClass = Class.forName("com.THproject.tharidia_features.worldborder.WorldBorderData");
            Class<?> borderClass = Class.forName("com.THproject.tharidia_features.worldborder.CustomBorder");
            customBorderConstructor = borderClass.getConstructor(double.class, double.class, double.class, double.class);
            worldBorderGetMethod = dataClass.getMethod("get");
            worldBorderAddBorderMethod = dataClass.getMethod("addBorder", ResourceLocation.class, String.class, borderClass);
            worldBorderRemoveBorderMethod = dataClass.getMethod("removeBorder", ResourceLocation.class, String.class);
            worldBorderReflectionAvailable = true;
            TharidiaThings.LOGGER.info("Tharidia Features integration enabled for character borders.");
        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            TharidiaThings.LOGGER.warn("Failed to initialize Tharidia Features integration; character borders disabled.", ex);
            worldBorderReflectionAvailable = false;
        }

        return worldBorderReflectionAvailable;
    }
}
