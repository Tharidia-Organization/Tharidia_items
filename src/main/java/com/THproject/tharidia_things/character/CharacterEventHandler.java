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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import com.THproject.tharidia_things.entity.TrebuchetEntity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Handles character creation events
 */
@EventBusSubscriber(modid = "tharidiathings")
public class CharacterEventHandler {
    
    private static final String THARIDIA_FEATURES_MODID = "tharidia_features";
    private static final ResourceKey<Level> CHARACTER_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("tharidiathings", "character_creation"));

    private static Constructor<?> customBorderConstructor;
    private static Method worldBorderGetMethod;
    private static Method worldBorderAddBorderMethod;
    private static Method worldBorderRemoveBorderMethod;
    private static boolean worldBorderReflectionInitialized = false;
    private static boolean worldBorderReflectionAvailable = false;
    
    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Check if player was in character creation dimension
            if (player.level().dimension().equals(CHARACTER_DIMENSION)) {
                // Clean up the player's area
                cleanupPlayerArea(player);
            }
        }
    }
    
    private static void cleanupPlayerArea(ServerPlayer player) {
        var characterLevel = player.server.getLevel(CHARACTER_DIMENSION);
        
        if (characterLevel != null) {
            // Calculate player's area based on UUID
            int playerHash = player.getUUID().hashCode();
            int chunkSpacing = 100;
            int playerChunkX = (playerHash % 1000) * chunkSpacing;
            int playerChunkZ = ((playerHash / 1000) % 1000) * chunkSpacing;
            
            // Clear chunks in the player's area
            int radius = 2; // Clear 5x5 chunks around center
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    ChunkPos chunkPos = new ChunkPos(playerChunkX + x, playerChunkZ + z);
                    if (characterLevel.hasChunk(chunkPos.x, chunkPos.z)) {
                        // Clear all entities
                        characterLevel.getEntities().getAll().forEach(entity -> {
                            if (entity.chunkPosition().equals(chunkPos)) {
                                entity.discard();
                            }
                        });
                        // Reset all blocks to air (limited Y range for performance)
                        for (int bx = 0; bx < 16; bx++) {
                            for (int bz = 0; bz < 16; bz++) {
                                for (int by = 80; by < 120; by++) {
                                    characterLevel.setBlock(
                                        new BlockPos(chunkPos.getMinBlockX() + bx, by, chunkPos.getMinBlockZ() + bz),
                                        Blocks.AIR.defaultBlockState(), 3
                                    );
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
            // Add null check for player data
            if (player == null || player.server == null) {
                TharidiaThings.LOGGER.warn("Player or server is null during login event");
                return;
            }
            
            CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
            
            if (characterData != null && !characterData.hasCreatedCharacter()) {
                // Delay teleportation by 2 ticks to ensure dimension and player are fully initialized
                player.server.execute(() -> {
                    player.server.execute(() -> {
                        // Double check player is still valid and connected
                        if (!player.isRemoved() && player.connection != null) {
                            teleportToCharacterDimension(player);
                        }
                    });
                });
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
            
            // If player hasn't created character and is not in character dimension, teleport them back
            if (!characterData.hasCreatedCharacter() && 
                !event.getTo().equals(CHARACTER_DIMENSION)) {
                teleportToCharacterDimension(player);
            }
            
            // If player is exiting character dimension to overworld and has created character, set to survival
            if (characterData.hasCreatedCharacter() && 
                event.getFrom().equals(CHARACTER_DIMENSION) && 
                !event.getTo().equals(CHARACTER_DIMENSION)) {
                // Set game mode to survival (delayed to ensure it applies after teleport)
                player.server.execute(() -> {
                    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                });
            }
        }
    }
    
    private static void teleportToCharacterDimension(ServerPlayer player) {
        TharidiaThings.LOGGER.info("Teleporting player {} to character dimension", player.getName().getString());
        
        ServerLevel characterLevel = player.server.getLevel(CHARACTER_DIMENSION);
        
        if (characterLevel == null) {
            TharidiaThings.LOGGER.error("Character creation dimension not found! Cannot teleport player {}", player.getName().getString());
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cCharacter creation dimension not found! Please contact an administrator."));
            
            // Set character as created to prevent infinite loop
            CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
            if (characterData != null) {
                characterData.setCharacterCreated(true);
            }
            
            // Teleport to overworld spawn as fallback
            ServerLevel overworld = player.server.overworld();
            if (overworld != null) {
                BlockPos spawnPos = overworld.getSharedSpawnPos();
                player.teleportTo(overworld, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, 0, 0);
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            }
            return;
        }
        
        // Calculate player's area based on UUID
        int playerHash = player.getUUID().hashCode();
        int chunkSpacing = 100; // Spacing between player areas in chunks
        int playerChunkX = (playerHash % 1000) * chunkSpacing;
        int playerChunkZ = ((playerHash / 1000) % 1000) * chunkSpacing;
        BlockPos initialPos = new BlockPos(playerChunkX * 16, 100, playerChunkZ * 16);
        
        TharidiaThings.LOGGER.info("Player hash: {}, chunkX: {}, chunkZ: {}, initialPos: {}", 
            playerHash, playerChunkX, playerChunkZ, initialPos);
        
        BlockPos spawnPos = initialPos;
        
        // Pre-load chunks in the area to ensure they're ready
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                characterLevel.getChunk(playerChunkX + x, playerChunkZ + z);
            }
        }
        
        // Find a safe Y position
        spawnPos = findSafeYPosition(characterLevel, spawnPos);
        
        TharidiaThings.LOGGER.info("Final spawn position after findSafeYPosition: {}", spawnPos);
        
        // Create the stone platform if it doesn't exist
        createStonePlatform(characterLevel, spawnPos);
        
        TharidiaThings.LOGGER.info("Teleporting player {} to position: {}", player.getName().getString(), spawnPos);
        
        // Create final copies for lambda
        final BlockPos finalSpawnPos = spawnPos;
        final ServerLevel finalCharacterLevel = characterLevel;
        
        // Teleport the player to the exact center of the platform
        // Platform is built around spawnPos, so we teleport to its center
        player.teleportTo(characterLevel, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, 
                         player.getYRot(), player.getXRot());
        
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
                TharidiaThings.LOGGER.warn("Unable to create character border for {}; tharidia_features integration unavailable or failed.", player.getGameProfile().getName());
            }

            spawnRacePoints(finalCharacterLevel, finalSpawnPos.above(1));
        });
        
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eWelcome! Please create your character before proceeding."));
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
        
        // Log platform creation
        TharidiaThings.LOGGER.info("Creating stone platform centered at: {}", center);
        
        // Create a 20x20 stone platform with the given center as the exact center
        // Platform extends 10 blocks in each direction from center
        int radius = 10;
        int platformY = center.getY() - 1; // Platform is one block below spawn position
        int blocksPlaced = 0;
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = new BlockPos(center.getX() + x, platformY, center.getZ() + z);
                
                // Ensure chunk is loaded for each position
                level.getChunk(pos);
                
                // Only place stone if the block is air or replaceable
                if (level.isEmptyBlock(pos) || level.getBlockState(pos).canBeReplaced()) {
                    level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                    blocksPlaced++;
                }
            }
        }
        
        // Add barrier walls around the platform
        for (int x = -radius - 1; x <= radius + 1; x++) {
            for (int z = -radius - 1; z <= radius + 1; z++) {
                if (Math.abs(x) == radius + 1 || Math.abs(z) == radius + 1) {
                    // Create vertical barrier wall
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
        
        TharidiaThings.LOGGER.info("Platform creation complete. Centered at {}, Placed {} blocks", center, blocksPlaced);
    }
    
    /**
     * Spawns a single selection point at the center
     */
    private static void spawnRacePoints(ServerLevel level, BlockPos center) {
        // Remove existing race point entities - use fixed center to catch all entities
        BlockPos fixedCenter = new BlockPos(0, 100, 0);
        level.getEntitiesOfClass(RacePointEntity.class, 
            new net.minecraft.world.phys.AABB(fixedCenter.getX() - 50, fixedCenter.getY() - 50, fixedCenter.getZ() - 50,
                                             fixedCenter.getX() + 50, fixedCenter.getY() + 50, fixedCenter.getZ() + 50))
            .forEach(entity -> {
                entity.discard();
            });
        
        // Spawn a single selection point at the center
        RacePointEntity selectionPoint = new RacePointEntity(level, center.getX(), center.getY(), center.getZ(), "scegli il tuo percorso", 0xFFFFFF);
        level.addFreshEntity(selectionPoint);
    }
    
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
            
            // If player hasn't created character, teleport them back to character creation
            if (!characterData.hasCreatedCharacter()) {
                // Delay teleport to ensure player is fully respawned
                player.server.execute(() -> {
                    player.server.execute(() -> {
                        // Restore health before teleporting
                        player.setHealth(player.getMaxHealth());
                        player.getFoodData().setFoodLevel(20);
                        player.getFoodData().setSaturation(20.0f);
                        
                        teleportToCharacterDimension(player);
                    });
                });
            }
        }
    }
    /**
     * Call this when player successfully creates their character
     */
    public static void completeCharacterCreation(ServerPlayer player) {
        CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
        characterData.setCharacterCreated(true);
        
        // Remove invulnerability
        player.setInvulnerable(false);
        
        // Remove character creation border
        removeCharacterCreationBorder(player);

        // Teleport player back to overworld spawn
        ServerLevel overworld = player.server.overworld();
        BlockPos spawnPos = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5,
                         player.getYRot(), player.getXRot());
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
            TharidiaThings.LOGGER.warn("Failed to create character creation border for {}", player.getName().getString(), ex);
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
            TharidiaThings.LOGGER.info("Removed character creation border '{}' for player {}", borderName, player.getName().getString());
        } catch (Exception ex) {
            TharidiaThings.LOGGER.warn("Failed to remove character creation border for {}", player.getName().getString(), ex);
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        
        // Check if player is riding a trebuchet
        if (player.getVehicle() instanceof TrebuchetEntity trebuchet && trebuchet.hasAmmo()) {
            trebuchet.beginFiring(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        
        // Check if player is riding a trebuchet
        if (player.getVehicle() instanceof TrebuchetEntity trebuchet) {
            // If holding ammo and trebuchet has no ammo, load it
            if (!held.isEmpty() && isAmmoItem(held) && !trebuchet.hasAmmo()) {
                trebuchet.loadAmmo(held, player);
                event.setCanceled(true); // Prevent normal item use
            }
        }
    }
    
    private static boolean isAmmoItem(ItemStack stack) {
        return stack.getItem() == net.minecraft.world.item.Items.STONE;
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
