package com.tharidia.tharidia_things.character;

import com.tharidia.tharidia_things.character.CharacterData;
import com.tharidia.tharidia_things.character.RaceData;
import com.tharidia.tharidia_things.entity.ModEntities;
import com.tharidia.tharidia_things.entity.RacePointEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;

/**
 * Handles character creation events
 */
@EventBusSubscriber(modid = "tharidiathings")
public class CharacterEventHandler {
    
    // The character creation dimension
    private static final ResourceKey<Level> CHARACTER_DIMENSION = 
        ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("tharidiathings", "character_creation"));
    
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
            CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
            
            if (!characterData.hasCreatedCharacter()) {
                // Teleport player to character creation dimension
                teleportToCharacterDimension(player);
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
        ServerLevel characterLevel = player.server.getLevel(CHARACTER_DIMENSION);
        
        if (characterLevel == null) {
            // Dimension not found, use overworld as fallback
            characterLevel = player.server.overworld();
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cCharacter creation dimension not found! Please contact an administrator."));
        }
        
        // Teleport to the center of the dimension at y=100
        // Use UUID hash to create unique position for each player
        int playerHash = player.getUUID().hashCode();
        int chunkSpacing = 100; // Spacing between player areas in chunks
        int playerChunkX = (playerHash % 1000) * chunkSpacing;
        int playerChunkZ = ((playerHash / 1000) % 1000) * chunkSpacing;
        BlockPos spawnPos = new BlockPos(playerChunkX * 16, 100, playerChunkZ * 16);
        
        // Find a safe Y position
        spawnPos = findSafeYPosition(characterLevel, spawnPos);
        
        // Create the stone platform if it doesn't exist
        createStonePlatform(characterLevel, spawnPos);
        
        // Teleport the player
        player.teleportTo(characterLevel, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, 
                         player.getYRot(), player.getXRot());
        
        // Set game mode to adventure (delayed to ensure it applies after teleport)
        player.server.execute(() -> {
            player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
        });
        
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eWelcome! Please create your character before proceeding."));
    }
    
    private static BlockPos findSafeYPosition(ServerLevel level, BlockPos pos) {
        // Start from y=100 and go down until we find air
        for (int y = 100; y > 50; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (level.isEmptyBlock(checkPos) && level.isEmptyBlock(checkPos.above())) {
                return checkPos;
            }
        }
        return new BlockPos(pos.getX(), 100, pos.getZ());
    }
    
    private static void createStonePlatform(ServerLevel level, BlockPos center) {
        // Create a 20x20 stone platform at the player's feet level
        int radius = 10;
        int platformY = center.getY() - 1;
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = new BlockPos(center.getX() + x, platformY, center.getZ() + z);
                
                // Only place stone if the block is air or replaceable
                if (level.isEmptyBlock(pos) || level.getBlockState(pos).canBeReplaced()) {
                    level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
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
                        if (level.isEmptyBlock(pos)) {
                            level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
        
        // Spawn race points in pentacle formation
        spawnRacePoints(level, center.above(1));
    }
    
    /**
     * Spawns 5 race points in pentacle formation around the center
     */
    private static void spawnRacePoints(ServerLevel level, BlockPos center) {
        RaceData.loadRaceData();
        
        // Remove existing race point entities - use fixed center to catch all entities
        BlockPos fixedCenter = new BlockPos(0, 100, 0);
        level.getEntitiesOfClass(RacePointEntity.class, 
            new net.minecraft.world.phys.AABB(fixedCenter.getX() - 50, fixedCenter.getY() - 50, fixedCenter.getZ() - 50,
                                             fixedCenter.getX() + 50, fixedCenter.getY() + 50, fixedCenter.getZ() + 50))
            .forEach(entity -> {
                entity.discard();
            });
        
        // Race names and their colors
        String[] races = {"umano", "elfo", "nano", "dragonide", "orcho"};
        int[] colors = {0xFFFFFF, 0x55FF55, 0xFFAA55, 0xFF5555, 0x5555FF};
        
        // Calculate pentacle positions
        double radius = 8.0;
        for (int i = 0; i < 5; i++) {
            // Pentagon angles: 90°, 162°, 234°, 306°, 18° (in radians)
            double angle = Math.toRadians(90 + i * 72);
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY();
            
            RacePointEntity racePoint = new RacePointEntity(level, x, y, z, races[i], colors[i]);
            level.addFreshEntity(racePoint);
        }
    }
    
    /**
     * Call this when player successfully creates their character
     */
    public static void completeCharacterCreation(ServerPlayer player) {
        CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
        characterData.setCharacterCreated(true);
        
        // Teleport player back to overworld spawn
        ServerLevel overworld = player.server.overworld();
        BlockPos spawnPos = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5,
                         player.getYRot(), player.getXRot());
    }
}
