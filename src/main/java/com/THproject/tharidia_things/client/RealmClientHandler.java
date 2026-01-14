package com.THproject.tharidia_things.client;

import com.mojang.logging.LogUtils;
import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import com.THproject.tharidia_things.realm.RealmManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

public class RealmClientHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean wasInRealm = false;
    private static boolean isInRealm = false;
    private static String currentRealmOwner = "";
    private static long lastLogTime = 0;
    private static final long LOG_INTERVAL = 5000; // Log every 5 seconds

    /**
     * Gets the current realm status for rendering
     */
    public static boolean isPlayerInRealm() {
        return isInRealm;
    }

    /**
     * Gets the current realm owner name
     */
    public static String getCurrentRealmOwner() {
        return currentRealmOwner;
    }
    
    @SubscribeEvent
    public static void onClientPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof LocalPlayer player) {
            Minecraft mc = Minecraft.getInstance();
            
            // Check if we're in single player or on a server
            if (mc.level != null && !mc.level.isClientSide) {
                return; // Skip if somehow called on server side
            }
            
            // We need to check realm status
            // Note: On client side, we need to track this differently
            // For now, we'll use a simple position check
            // In a full implementation, you'd want to sync this data from server
            
            checkRealmStatus(player);
        }
    }
    
    private static void checkRealmStatus(LocalPlayer player) {
        // Update the realm status
        wasInRealm = isInRealm;

        // For client-side, we need to check against server data
        // This is a simplified check - in production you'd sync this from server
        Minecraft mc = Minecraft.getInstance();
        PietroBlockEntity currentRealm = null;

        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            // Single player - we can access server data
            ServerLevel serverLevel = mc.getSingleplayerServer().getLevel(player.level().dimension());
            if (serverLevel != null) {
                // Check if player is in any realm's OUTER LAYER (not just main realm)
                for (PietroBlockEntity realm : RealmManager.getRealms(serverLevel)) {
                    if (realm.isPositionInRealm(player.blockPosition()) || realm.isPositionInOuterLayer(player.blockPosition())) {
                        currentRealm = realm;
                        isInRealm = true;
                        currentRealmOwner = realm.getOwnerName();
                        break;
                    }
                }
                if (currentRealm == null) {
                    isInRealm = false;
                }
            } else {
                isInRealm = false;
            }
        } else {
            // Multiplayer - check against synced realm data
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLogTime > LOG_INTERVAL) {
                LOGGER.info("Checking realm status in multiplayer. Synced realms: {}, Player chunk: ({}, {})", 
                    ClientPacketHandler.syncedRealms.size(), 
                    player.chunkPosition().x, 
                    player.chunkPosition().z);
                lastLogTime = currentTime;
            }
            
            isInRealm = false;
            currentRealmOwner = "";
            
            // Check if player is in any synced realm
            for (PietroBlockEntity realm : ClientPacketHandler.syncedRealms) {
                if (isPlayerInRealmBounds(player, realm)) {
                    isInRealm = true;
                    currentRealm = realm;
                    currentRealmOwner = realm.getOwnerName();
                    if (currentTime - lastLogTime > LOG_INTERVAL) {
                        LOGGER.info("Player is in realm owned by: {}", currentRealmOwner);
                    }
                    break;
                }
            }
        }

        // Check if status changed
        if (isInRealm && !wasInRealm) {
            // Just entered realm
            if (!currentRealmOwner.isEmpty()) {
                player.displayClientMessage(Component.literal("§6Sei nel regno di " + currentRealmOwner), false);
            } else {
                player.displayClientMessage(Component.literal("§6Sei nel regno"), false);
            }
        } else if (!isInRealm && wasInRealm) {
            // Just left realm
            player.displayClientMessage(Component.literal("§7Hai lasciato il regno."), false);
            currentRealmOwner = "";
        }
    }
    
    /**
     * Checks if a player is within the bounds of a realm (including outer layer)
     * Now checks the OUTER LAYER (6 chunks beyond the main realm) instead of inner realm
     */
    private static boolean isPlayerInRealmBounds(LocalPlayer player, PietroBlockEntity realm) {
        if (realm == null || realm.getCenterChunk() == null) {
            return false;
        }
        
        // Get player chunk position
        int playerChunkX = player.chunkPosition().x;
        int playerChunkZ = player.chunkPosition().z;
        
        // Get outer layer bounds (6 chunks beyond the main realm)
        net.minecraft.world.level.ChunkPos minChunk = realm.getOuterLayerMinChunk();
        net.minecraft.world.level.ChunkPos maxChunk = realm.getOuterLayerMaxChunk();
        
        // Check if player is within outer layer bounds
        return playerChunkX >= minChunk.x && playerChunkX <= maxChunk.x &&
               playerChunkZ >= minChunk.z && playerChunkZ <= maxChunk.z;
    }
    
    /**
     * Resets the realm status (useful for dimension changes)
     */
    public static void reset() {
        wasInRealm = false;
        isInRealm = false;
    }
}
