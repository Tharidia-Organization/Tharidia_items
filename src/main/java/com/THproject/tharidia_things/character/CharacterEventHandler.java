package com.THproject.tharidia_things.character;

import com.THproject.tharidia_things.Config;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.entity.RacePointEntity;
import com.THproject.tharidia_things.network.RequestNamePacket;
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
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Handles the character creation flow:
 * 1. New players (NOT_STARTED) first choose a name (if needed)
 * 2. After name is set, players are teleported to character_creation dimension (AWAITING_RACE)
 * 3. Players select a race, then teleport back to overworld (COMPLETED)
 */
@EventBusSubscriber(modid = "tharidiathings")
public class CharacterEventHandler {

    private static final String THARIDIA_FEATURES_MODID = "tharidia_features";
    public static final ResourceKey<Level> CHARACTER_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("tharidiathings", "character_creation"));

    private static final int CHUNK_SPACING = 100;
    private static final int PLATFORM_RADIUS = 10;
    // Must cover barrier walls at ±(PLATFORM_RADIUS+1) = ±11 blocks from center
    private static final int CLEANUP_RADIUS = 3;

    private static Constructor<?> customBorderConstructor;
    private static Method worldBorderGetMethod;
    private static Method worldBorderAddBorderMethod;
    private static Method worldBorderRemoveBorderMethod;
    private static boolean worldBorderReflectionInitialized = false;
    private static boolean worldBorderReflectionAvailable = false;

    /**
     * Calculate player's unique chunk coordinates based on their UUID.
     * Uses separate halves of the UUID for X and Z to minimize collisions.
     */
    public static ChunkPos getPlayerAreaChunk(UUID playerUUID) {
        // Use separate UUID halves for X and Z — avoids Math.abs(MIN_VALUE) bug
        // and expands the space from 1M to 100M unique positions
        long msb = playerUUID.getMostSignificantBits();
        long lsb = playerUUID.getLeastSignificantBits();
        int playerChunkX = (int) ((msb & 0x7FFFFFFFL) % 10000) * CHUNK_SPACING;
        int playerChunkZ = (int) ((lsb & 0x7FFFFFFFL) % 10000) * CHUNK_SPACING;
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
            if (player.level().dimension().equals(CHARACTER_DIMENSION)) {
                cleanupPlayerArea(player.server.getLevel(CHARACTER_DIMENSION), player.getUUID());
            }
        }
    }

    /**
     * Cleans up the character creation area for a specific player.
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

        // Remove entities in the player's area
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

    /**
     * Main login handler — orchestrates the character creation flow based on stage.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.server == null) return;

        // Skip character creation on lobby servers
        if (Config.IS_LOBBY_SERVER.get()) {
            TharidiaThings.LOGGER.debug("Lobby server - skipping character creation for {}",
                    player.getName().getString());
            return;
        }

        CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
        if (characterData == null) return;

        CharacterData.CreationStage stage = characterData.getStage();

        switch (stage) {
            case NOT_STARTED -> {
                TharidiaThings.LOGGER.info("Player {} stage NOT_STARTED - checking if name needed",
                        player.getName().getString());

                // Check if player needs to choose a name first
                boolean needsName = RequestNamePacket.checkIfPlayerNeedsName(player);
                if (needsName) {
                    // Send name request — do NOT teleport yet.
                    // Teleport will happen after name is confirmed (in SubmitNamePacket handler).
                    PacketDistributor.sendToPlayer(player, new RequestNamePacket(true));
                } else {
                    // Name not needed → advance to AWAITING_RACE and teleport
                    characterData.setStage(CharacterData.CreationStage.AWAITING_RACE);
                    // Delay teleport to let the player fully initialize
                    player.server.execute(() -> {
                        if (!player.isRemoved() && player.connection != null) {
                            CharacterData cd = player.getData(CharacterAttachments.CHARACTER_DATA);
                            if (cd != null && cd.getStage() == CharacterData.CreationStage.AWAITING_RACE) {
                                teleportToCharacterDimension(player);
                            }
                        }
                    });
                }
            }
            case AWAITING_RACE -> {
                // Player was mid-creation and re-logged — re-teleport
                TharidiaThings.LOGGER.info("Player {} stage AWAITING_RACE - re-teleporting to chargen",
                        player.getName().getString());
                player.server.execute(() -> {
                    if (!player.isRemoved() && player.connection != null) {
                        teleportToCharacterDimension(player);
                    }
                });
            }
            case COMPLETED -> {
                TharidiaThings.LOGGER.debug("Player {} already has character - skipping",
                        player.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
        if (characterData == null) return;

        // Only enforce dimension lock for players actively in race selection (AWAITING_RACE)
        // NOT_STARTED players stay in overworld waiting for name prompt
        if (characterData.getStage() == CharacterData.CreationStage.AWAITING_RACE
                && !event.getTo().equals(CHARACTER_DIMENSION)) {
            TharidiaThings.LOGGER.info("Player {} tried to leave character dimension during race selection",
                    player.getName().getString());
            teleportToCharacterDimension(player);
        }

        // If player is exiting character dimension after completing, set to survival
        if (characterData.hasCreatedCharacter()
                && event.getFrom().equals(CHARACTER_DIMENSION)
                && !event.getTo().equals(CHARACTER_DIMENSION)) {
            player.server.execute(() -> {
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.setInvulnerable(false);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
        if (characterData == null) return;

        // Only re-teleport players who are in AWAITING_RACE stage
        if (characterData.getStage() == CharacterData.CreationStage.AWAITING_RACE) {
            TharidiaThings.LOGGER.info("Player {} respawned during race selection - re-teleporting",
                    player.getName().getString());

            player.server.execute(() -> {
                if (!player.isRemoved() && player.connection != null) {
                    CharacterData cd = player.getData(CharacterAttachments.CHARACTER_DATA);
                    if (cd != null && cd.getStage() == CharacterData.CreationStage.AWAITING_RACE) {
                        player.setHealth(player.getMaxHealth());
                        player.getFoodData().setFoodLevel(20);
                        player.getFoodData().setSaturation(20.0f);
                        teleportToCharacterDimension(player);
                    }
                }
            });
        }
    }

    /**
     * Teleports a player to their unique area in the character creation dimension.
     * Sets up the platform, barrier walls, race point entity, and game mode.
     * Public so it can be called from SubmitNamePacket and CharacterCommands.
     */
    public static void teleportToCharacterDimension(ServerPlayer player) {
        TharidiaThings.LOGGER.info("Teleporting player {} to character dimension", player.getName().getString());

        ServerLevel characterLevel = player.server.getLevel(CHARACTER_DIMENSION);

        if (characterLevel == null) {
            TharidiaThings.LOGGER.error("Character creation dimension not found! Cannot teleport player {}",
                    player.getName().getString());
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7cDimensione di creazione personaggio non trovata! Contatta un amministratore."));
            // Do NOT mark as created — leave state as-is so it can retry next login
            return;
        }

        BlockPos spawnPos = getPlayerSpawnPos(player.getUUID());
        ChunkPos playerChunk = getPlayerAreaChunk(player.getUUID());

        TharidiaThings.LOGGER.info("Player {} area: chunk ({}, {}), spawn pos: {}",
                player.getName().getString(), playerChunk.x, playerChunk.z, spawnPos);

        // Pre-load chunks
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                characterLevel.getChunk(playerChunk.x + x, playerChunk.z + z);
            }
        }

        spawnPos = findSafeYPosition(characterLevel, spawnPos);
        createStonePlatform(characterLevel, spawnPos);

        final BlockPos finalSpawnPos = spawnPos;
        final ServerLevel finalCharacterLevel = characterLevel;

        player.teleportTo(characterLevel, spawnPos.getX() + 0.5, spawnPos.getY() + 1,
                spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());

        // Set up game mode and spawn entities after teleport
        player.server.execute(() -> {
            player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
            player.setInvulnerable(true);
            player.removeAllEffects();
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
                "\u00a7eBenvenuto! Crea il tuo personaggio prima di procedere."));
    }

    private static BlockPos findSafeYPosition(ServerLevel level, BlockPos pos) {
        level.getChunk(pos);

        for (int y = 100; y > 50; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (level.isEmptyBlock(checkPos) && level.isEmptyBlock(checkPos.above())) {
                return checkPos;
            }
        }
        return new BlockPos(pos.getX(), 100, pos.getZ());
    }

    private static void createStonePlatform(ServerLevel level, BlockPos center) {
        level.getChunk(center);

        int platformY = center.getY() - 1;

        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                BlockPos pos = new BlockPos(center.getX() + x, platformY, center.getZ() + z);
                level.getChunk(pos);
                if (level.isEmptyBlock(pos) || level.getBlockState(pos).canBeReplaced()) {
                    level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
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
    }

    private static void spawnRacePoints(ServerLevel level, BlockPos center, UUID playerUUID) {
        AABB searchArea = new AABB(
                center.getX() - 50, center.getY() - 50, center.getZ() - 50,
                center.getX() + 50, center.getY() + 50, center.getZ() + 50
        );

        level.getEntitiesOfClass(RacePointEntity.class, searchArea)
                .forEach(entity -> {
                    TharidiaThings.LOGGER.debug("Removing existing RacePointEntity at {}", entity.position());
                    entity.discard();
                });

        RacePointEntity selectionPoint = new RacePointEntity(
                level, center.getX(), center.getY(), center.getZ(),
                "scegli il tuo percorso", 0xFFFFFF);
        level.addFreshEntity(selectionPoint);

        TharidiaThings.LOGGER.info("Spawned RacePointEntity at {} for player area", center);
    }

    /**
     * Completes character creation. Called after race selection.
     */
    public static void completeCharacterCreation(ServerPlayer player) {
        CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);

        if (characterData == null) {
            TharidiaThings.LOGGER.error("Cannot complete character creation - CharacterData is null for {}",
                    player.getName().getString());
            return;
        }

        // Mark character as created
        characterData.setStage(CharacterData.CreationStage.COMPLETED);

        TharidiaThings.LOGGER.info("Character creation completed for player {} (race: {})",
                player.getName().getString(), characterData.getSelectedRace());

        player.setInvulnerable(false);
        removeCharacterCreationBorder(player);

        ServerLevel characterLevel = player.server.getLevel(CHARACTER_DIMENSION);
        if (characterLevel != null) {
            cleanupPlayerArea(characterLevel, player.getUUID());
        }

        // Teleport player back to overworld spawn
        ServerLevel overworld = player.server.overworld();
        BlockPos spawnPos = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawnPos.getX() + 0.5, spawnPos.getY() + 1,
                spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());

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
            characterData.setStage(CharacterData.CreationStage.NOT_STARTED);
            characterData.setSelectedRace(null);
            TharidiaThings.LOGGER.info("Character creation reset for player {}", player.getName().getString());
        }
    }

    // --- World Border reflection (unchanged pattern, cached) ---

    private static boolean createCharacterCreationBorder(ServerPlayer player, BlockPos platformCenter) {
        if (!ensureWorldBorderReflection()) return false;

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
            TharidiaThings.LOGGER.info("Created character creation border '{}' for player {}",
                    borderName, player.getName().getString());
            return true;
        } catch (Exception ex) {
            TharidiaThings.LOGGER.warn("Failed to create character creation border for {}",
                    player.getName().getString(), ex);
            return false;
        }
    }

    private static void removeCharacterCreationBorder(ServerPlayer player) {
        if (!ensureWorldBorderReflection()) return;

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
