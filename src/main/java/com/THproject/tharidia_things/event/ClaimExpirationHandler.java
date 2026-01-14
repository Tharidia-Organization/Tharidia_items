package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.block.entity.ClaimBlockEntity;
import com.THproject.tharidia_things.claim.ClaimRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ClaimExpirationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimExpirationHandler.class);
    
    // Check every 60 seconds (1200 ticks) - reduced from 20s for better performance
    // Expiration checks don't need to be super frequent
    private static final int CHECK_INTERVAL = 1200;
    private static int tickCounter = 0;
    
    // Warning times in milliseconds
    private static final long ONE_HOUR = 60 * 60 * 1000L;
    private static final long TWELVE_HOURS = 12 * 60 * 60 * 1000L;
    private static final long ONE_DAY = 24 * 60 * 60 * 1000L;
    
    // Track which claims have been warned
    private static final Map<BlockPos, Set<WarningLevel>> warnedClaims = new HashMap<>();
    
    private enum WarningLevel {
        ONE_DAY,
        TWELVE_HOURS,
        ONE_HOUR
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        
        // Only check every CHECK_INTERVAL ticks
        tickCounter++;
        if (tickCounter % CHECK_INTERVAL != 0) {
            return;
        }

        // Check all loaded worlds
        for (ServerLevel level : server.getAllLevels()) {
            checkExpiredClaims(level, server);
        }
    }

    private static void checkExpiredClaims(ServerLevel level, MinecraftServer server) {
        List<BlockPos> claimsToRemove = new ArrayList<>();
        List<ClaimWarning> warnings = new ArrayList<>();
        
        // OPTIMIZED: Use claim registry for O(1) lookup instead of scanning all chunks
        String dimension = level.dimension().location().toString();
        List<ClaimRegistry.ClaimData> dimensionClaims =
            ClaimRegistry.getClaimsInDimension(dimension);
        
        // Early exit if no claims
        if (dimensionClaims.isEmpty()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        for (ClaimRegistry.ClaimData claimData : dimensionClaims) {
            BlockPos claimPos = claimData.getPosition();
            
            // OPTIMIZATION: Only load block entity if chunk is loaded
            if (!level.hasChunkAt(claimPos)) {
                continue;
            }
            
            BlockEntity blockEntity = level.getBlockEntity(claimPos);
            
            if (blockEntity instanceof ClaimBlockEntity claim) {
                if (!claim.isRented()) {
                    continue;
                }
                
                long expirationTime = claim.getExpirationTime();
                
                if (expirationTime <= 0) {
                    continue; // No expiration set
                }
                
                long timeLeft = expirationTime - currentTime;
                
                // Check if expired
                if (timeLeft <= 0) {
                    claimsToRemove.add(claimPos);
                    LOGGER.info("Claim at {} has expired and will be removed", claimPos);
                } else {
                    // Check for warnings
                    checkAndQueueWarning(claim, claimPos, timeLeft, warnings);
                }
            }
        }
        
        // Send warnings to online owners
        for (ClaimWarning warning : warnings) {
            ServerPlayer owner = server.getPlayerList().getPlayer(warning.ownerUUID);
            if (owner != null) {
                sendExpirationWarning(owner, warning.claimPos, warning.timeLeft, warning.claimName);
            }
        }
        
        // Remove expired claims
        for (BlockPos pos : claimsToRemove) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ClaimBlockEntity claim) {
                UUID ownerUUID = claim.getOwnerUUID();
                String claimName = claim.getClaimName();
                
                // Notify owner if online
                ServerPlayer owner = server.getPlayerList().getPlayer(ownerUUID);
                if (owner != null) {
                    owner.sendSystemMessage(Component.literal("§c§l[!] Your claim '" + 
                        (claimName.isEmpty() ? "Unnamed" : claimName) + "' has expired and been removed!"));
                }
                
                // Remove the claim block
                level.destroyBlock(pos, true);
                warnedClaims.remove(pos);
                
                LOGGER.info("Removed expired claim at {} owned by {}", pos, ownerUUID);
            }
        }
    }

    private static void checkAndQueueWarning(ClaimBlockEntity claim, BlockPos claimPos, 
                                            long timeLeft, List<ClaimWarning> warnings) {
        Set<WarningLevel> warned = warnedClaims.computeIfAbsent(claimPos, k -> new HashSet<>());
        
        // Check for 24h warning
        if (timeLeft <= ONE_DAY && timeLeft > TWELVE_HOURS && !warned.contains(WarningLevel.ONE_DAY)) {
            warnings.add(new ClaimWarning(claim.getOwnerUUID(), claimPos, timeLeft, claim.getClaimName()));
            warned.add(WarningLevel.ONE_DAY);
        }
        // Check for 12h warning
        else if (timeLeft <= TWELVE_HOURS && timeLeft > ONE_HOUR && !warned.contains(WarningLevel.TWELVE_HOURS)) {
            warnings.add(new ClaimWarning(claim.getOwnerUUID(), claimPos, timeLeft, claim.getClaimName()));
            warned.add(WarningLevel.TWELVE_HOURS);
        }
        // Check for 1h warning
        else if (timeLeft <= ONE_HOUR && !warned.contains(WarningLevel.ONE_HOUR)) {
            warnings.add(new ClaimWarning(claim.getOwnerUUID(), claimPos, timeLeft, claim.getClaimName()));
            warned.add(WarningLevel.ONE_HOUR);
        }
    }

    private static void sendExpirationWarning(ServerPlayer player, BlockPos claimPos, 
                                             long timeLeft, String claimName) {
        long hours = timeLeft / (60 * 60 * 1000);
        long minutes = (timeLeft % (60 * 60 * 1000)) / (60 * 1000);
        
        String name = claimName.isEmpty() ? "Unnamed claim" : claimName;
        String timeStr;
        
        if (hours > 0) {
            timeStr = hours + " hour" + (hours > 1 ? "s" : "");
        } else {
            timeStr = minutes + " minute" + (minutes > 1 ? "s" : "");
        }
        
        player.sendSystemMessage(Component.literal("§e§l[!] Warning: Your claim '" + name + 
            "' at " + claimPos.toShortString() + " will expire in " + timeStr + "!"));
        player.sendSystemMessage(Component.literal("§7Use /claim info while in the claim to check details"));
    }

    private static class ClaimWarning {
        UUID ownerUUID;
        BlockPos claimPos;
        long timeLeft;
        String claimName;
        
        ClaimWarning(UUID ownerUUID, BlockPos claimPos, long timeLeft, String claimName) {
            this.ownerUUID = ownerUUID;
            this.claimPos = claimPos;
            this.timeLeft = timeLeft;
            this.claimName = claimName;
        }
    }
}
