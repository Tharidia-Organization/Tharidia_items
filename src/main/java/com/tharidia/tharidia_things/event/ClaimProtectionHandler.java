package com.tharidia.tharidia_things.event;

import com.tharidia.tharidia_things.block.ClaimBlock;
import com.tharidia.tharidia_things.block.entity.ClaimBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.UUID;

public class ClaimProtectionHandler {

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
     * Finds a claim block that protects the given position
     * Protection area: entire chunk, 20 blocks below to 40 blocks above the claim block
     */
    private static ClaimBlockEntity findClaimForPosition(ServerLevel level, BlockPos pos) {
        // Get chunk coordinates
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

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
     * Checks if a player can interact with blocks in a claimed area
     */
    private static boolean canPlayerInteract(ClaimBlockEntity claim, Player player) {
        UUID claimOwner = claim.getOwnerUUID();
        return claimOwner != null && claimOwner.equals(player.getUUID());
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
