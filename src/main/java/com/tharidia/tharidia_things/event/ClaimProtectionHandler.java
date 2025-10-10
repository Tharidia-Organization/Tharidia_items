package com.tharidia.tharidia_things.event;

import com.tharidia.tharidia_things.block.ClaimBlock;
import com.tharidia.tharidia_things.block.entity.ClaimBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ClaimProtectionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimProtectionHandler.class);

    /**
     * Prevents block breaking in claimed areas
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Special handling for Pietro blocks - check if claims exist in realm
        if (level.getBlockState(pos).getBlock() instanceof com.tharidia.tharidia_things.block.PietroBlock) {
            if (isPietroBlockProtected(serverLevel, pos, player)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§cCannot remove Pietro block while claims exist in its realm!"));
            }
            return;
        }

        // Check if the position is protected by a claim
        ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
        if (claim != null && !canPlayerInteract(claim, player)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§cQuesta area è protetta da un Claim!"));
        }
    }

    /**
     * Prevents block interactions (right-click) in claimed areas
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Pietro blocks handle their own interaction logic (adding potatoes, etc)
        if (level.getBlockState(pos).getBlock() instanceof com.tharidia.tharidia_things.block.PietroBlock) {
            return;
        }

        // Check if the position is protected by a claim
        ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
        if (claim != null && !canPlayerInteract(claim, player)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§cQuesta area è protetta da un Claim!"));
        }
    }

    /**
     * Prevents left-clicking blocks in claimed areas
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Pietro blocks are handled in onBlockBreak
        if (level.getBlockState(pos).getBlock() instanceof com.tharidia.tharidia_things.block.PietroBlock) {
            return;
        }

        // Check if the position is protected by a claim
        ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
        if (claim != null && !canPlayerInteract(claim, player)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§cQuesta area è protetta da un Claim!"));
        }
    }

    /**
     * Prevents explosions from destroying blocks in claimed areas
     * Using HIGHEST priority to ensure we process this before other mods
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Get the entity that caused the explosion (if any)
        Entity source = event.getExplosion().getIndirectSourceEntity();
        if (source == null) {
            source = event.getExplosion().getDirectSourceEntity();
        }
        UUID sourceUuid = source != null ? source.getUUID() : null;

        // Get explosion center coordinates
        Vec3 explosionPos = event.getExplosion().center();
        BlockPos explosionCenter = BlockPos.containing(explosionPos);

        // Use an iterator to safely remove blocks while iterating
        Iterator<BlockPos> iterator = event.getAffectedBlocks().iterator();
        int protectedCount = 0;
        boolean isOwner = false;
        ClaimBlockEntity firstClaim = null;
        
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
            
            if (claim != null) {
                if (firstClaim == null) {
                    firstClaim = claim;
                    isOwner = sourceUuid != null && sourceUuid.equals(claim.getOwnerUUID());
                }
                
                // Only protect if explosions are NOT allowed
                if (!claim.getAllowExplosions()) {
                    iterator.remove();
                    protectedCount++;
                }
            }
        }

        // Log explosion protection information
        if (protectedCount > 0) {
            String sourceName = source != null ? source.getName().getString() : "Unknown";
            LOGGER.info("Explosion blocked in claim - Source: {}, Is Owner: {}, Center: {}, Protected Blocks: {}",
                sourceName, isOwner, explosionCenter, protectedCount);
        }
    }

    /**
     * Monitors explosion start events (handled by Detonate event)
     */
    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event) {
        // Explosion protection is handled in the Detonate event
        // This event is kept for potential future use
    }

    // TODO: PvP protection - requires finding correct NeoForge 1.21.1 damage event
    // The API for damage events has changed in this version

    /**
     * Prevents crop trampling in claimed areas
     */
    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = event.getPos();
        ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);

        if (claim != null) {
            // Always prevent trampling in claims
            event.setCanceled(true);
        }
    }

    /**
     * Prevents fire spread in claimed areas (unless allowed by claim flag)
     * Intercepts fire placement events to stop spread
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFirePlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Check if fire is being placed
        if (event.getPlacedBlock().getBlock() == Blocks.FIRE) {
            BlockPos pos = event.getPos();
            ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
            
            // If in a claim and fire spread is not allowed, cancel the placement
            if (claim != null && !claim.getAllowFireSpread()) {
                // Only block natural fire spread (no entity)
                // Allow players to place fire if they have permission
                if (event.getEntity() == null || !(event.getEntity() instanceof Player)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * Additional fire protection - removes existing fire in claims where it's not allowed
     * This catches fire that bypasses the placement event
     */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = event.getPos();
        
        // Check if this is fire trying to spread
        if (serverLevel.getBlockState(pos).getBlock() == Blocks.FIRE) {
            ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
            
            if (claim != null && !claim.getAllowFireSpread()) {
                // Remove fire block from claims
                serverLevel.removeBlock(pos, false);
            }
        }
    }

    /**
     * Prevents enderman from picking up blocks in claimed areas
     */
    @SubscribeEvent
    public static void onEndermanPickup(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Check if an enderman is breaking the block
        if (event.getPlayer() == null) {
            // No player = could be enderman or other entity
            BlockPos pos = event.getPos();
            ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);

            if (claim != null) {
                // Check nearby for enderman
                List<EnderMan> endermen = serverLevel.getEntitiesOfClass(EnderMan.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(10));
                if (!endermen.isEmpty()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * Finds a claim block that protects the given position
     * Protection area: entire chunk, 20 blocks below to 40 blocks above the claim block
     * Also checks merged claims for unified protection
     */
    public static ClaimBlockEntity findClaimForPosition(ServerLevel level, BlockPos pos) {
        // First check the current chunk
        ClaimBlockEntity claim = findClaimInChunk(level, pos);
        if (claim != null) {
            return claim;
        }
        
        // Check adjacent chunks for merged claims
        // This allows merged claims to act as one larger unified protection area
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue; // Already checked above
                
                int adjacentChunkX = chunkX + dx;
                int adjacentChunkZ = chunkZ + dz;
                
                if (level.hasChunk(adjacentChunkX, adjacentChunkZ)) {
                    ClaimBlockEntity adjacentClaim = findClaimInSpecificChunk(level, adjacentChunkX, adjacentChunkZ, pos);
                    if (adjacentClaim != null) {
                        // Check if this adjacent claim has merged claims that include the target position's chunk
                        if (isPositionInMergedClaimArea(adjacentClaim, pos)) {
                            return adjacentClaim;
                        }
                    }
                }
            }
        }

        return null;
    }
    
    /**
     * Finds a claim in the chunk containing the given position
     */
    private static ClaimBlockEntity findClaimInChunk(ServerLevel level, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return findClaimInSpecificChunk(level, chunkX, chunkZ, pos);
    }
    
    /**
     * Finds a claim in a specific chunk that protects the given position
     */
    private static ClaimBlockEntity findClaimInSpecificChunk(ServerLevel level, int chunkX, int chunkZ, BlockPos pos) {
        // Calculate chunk bounds
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        // Scan the entire chunk for claim blocks
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    if (level.getBlockState(checkPos).getBlock() instanceof ClaimBlock) {
                        BlockEntity blockEntity = level.getBlockEntity(checkPos);
                        if (blockEntity instanceof ClaimBlockEntity claimEntity) {
                            // Check if the position is within the claim's protection area
                            int minY = checkPos.getY() - 20;
                            int maxY = checkPos.getY() + 40;

                            if (pos.getY() >= minY && pos.getY() <= maxY) {
                                return claimEntity;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
    
    /**
     * Checks if a position is within a claim's merged area
     * This makes merged claims act as one unified protection zone
     */
    private static boolean isPositionInMergedClaimArea(ClaimBlockEntity claim, BlockPos pos) {
        // Check if the position's chunk is one of the merged claims
        int posChunkX = pos.getX() >> 4;
        int posChunkZ = pos.getZ() >> 4;
        
        for (BlockPos mergedPos : claim.getMergedClaims()) {
            int mergedChunkX = mergedPos.getX() >> 4;
            int mergedChunkZ = mergedPos.getZ() >> 4;
            
            if (posChunkX == mergedChunkX && posChunkZ == mergedChunkZ) {
                // Position is in a merged claim's chunk - verify Y level
                int minY = mergedPos.getY() - 20;
                int maxY = mergedPos.getY() + 40;
                
                if (pos.getY() >= minY && pos.getY() <= maxY) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Checks if a player can interact with blocks in a claimed area
     * Now includes trusted players
     */
    private static boolean canPlayerInteract(ClaimBlockEntity claim, Player player) {
        return claim.isTrusted(player.getUUID());
    }
    
    /**
     * Checks if a Pietro block is protected by having claims in its realm
     */
    private static boolean isPietroBlockProtected(ServerLevel level, BlockPos pos, Player player) {
        // Get the block state to determine which half we're dealing with
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        
        // Get the lower block position (where the block entity is)
        BlockPos lowerPos = state.getValue(com.tharidia.tharidia_things.block.PietroBlock.HALF) 
                == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER 
                ? pos.below() 
                : pos;
        
        // Get the block entity from the lower position
        BlockEntity blockEntity = level.getBlockEntity(lowerPos);
        
        if (blockEntity instanceof com.tharidia.tharidia_things.block.entity.PietroBlockEntity pietroBlockEntity) {
            // Check if there are any claim blocks within the realm
            return hasClaimsInRealm(level, pietroBlockEntity);
        }
        
        return false;
    }
    
    /**
     * Checks if there are any claim blocks within the Pietro block's realm boundaries
     */
    private static boolean hasClaimsInRealm(ServerLevel level, com.tharidia.tharidia_things.block.entity.PietroBlockEntity realm) {
        net.minecraft.world.level.ChunkPos minChunk = realm.getMinChunk();
        net.minecraft.world.level.ChunkPos maxChunk = realm.getMaxChunk();
        
        // Iterate through all chunks in the realm
        for (int chunkX = minChunk.x; chunkX <= maxChunk.x; chunkX++) {
            for (int chunkZ = minChunk.z; chunkZ <= maxChunk.z; chunkZ++) {
                // Only check loaded chunks to avoid forcing chunk loads
                if (level.hasChunk(chunkX, chunkZ)) {
                    net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                    
                    // Check all block entities in this chunk
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be instanceof ClaimBlockEntity) {
                            return true; // Found a claim block
                        }
                    }
                }
            }
        }
        
        return false; // No claim blocks found
    }
}
