package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.Config;
import com.mojang.logging.LogUtils;
import com.THproject.tharidia_things.block.PietroBlock;
import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import com.THproject.tharidia_things.network.RealmSyncPacket;
import com.THproject.tharidia_things.realm.RealmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RealmPlacementHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            Block placedBlock = event.getPlacedBlock().getBlock();
            
            // Check if a Pietro block is being placed
            if (placedBlock instanceof PietroBlock) {
                BlockPos pos = event.getPos();
                
                // Get player UUID if available
                UUID playerUUID = null;
                if (event.getEntity() instanceof Player player) {
                    playerUUID = player.getUUID();
                }
                
                // Check if placement is valid (requires player UUID for ownership check)
                if (playerUUID == null || !PietroBlock.canPlacePietroBlock(serverLevel, pos, playerUUID)) {
                    // Cancel the placement
                    event.setCanceled(true);
                    
                    if (Config.DEBUG_REALM_SYNC.get()) {
                        LOGGER.info("[DEBUG] Realm placement cancelled at {}, sending sync to clear client data", pos);
                    }
                    
                    // Notify the player
                    if (event.getEntity() instanceof Player player) {
                        // Check specific reason for cancellation
                        if (playerUUID != null && RealmManager.playerOwnsRealm(serverLevel, playerUUID)) {
                            // Player already owns a realm
                            player.sendSystemMessage(
                                Component.literal("§cCannot place Realm block!")
                                    .append(Component.literal("\n§7You already own a realm. Each player can only have one realm."))
                            );
                        } else {
                            // Too close to another realm
                            int distance = PietroBlock.getDistanceToNearestRealm(serverLevel, pos);
                            int needed = PietroBlock.MIN_DISTANCE_CHUNKS - distance;
                            
                            player.sendSystemMessage(
                                Component.literal("§cCannot place Realm block here!")
                                    .append(Component.literal("\n§7Too close to another realm. Move " + needed + " chunks away."))
                                    .append(Component.literal("\n§7(Minimum distance: " + PietroBlock.MIN_DISTANCE_CHUNKS + " chunks)"))
                            );
                        }
                        
                        // CRITICAL: Send realm removal sync to client
                        // The block might have been briefly registered before cancellation
                        syncAllRealmsToNearbyPlayers(serverLevel, pos);
                    }
                }
            }
        }
    }
    
    /**
     * Syncs all valid realms to nearby players to ensure cancelled realms are removed from client
     */
    private static void syncAllRealmsToNearbyPlayers(ServerLevel serverLevel, BlockPos pos) {
        List<RealmSyncPacket.RealmData> realmDataList = new ArrayList<>();
        List<PietroBlockEntity> allRealms = RealmManager.getRealms(serverLevel);
        
        for (PietroBlockEntity realm : allRealms) {
            RealmSyncPacket.RealmData data = new RealmSyncPacket.RealmData(
                realm.getBlockPos(),
                realm.getRealmSize(),
                realm.getOwnerName(),
                realm.getCenterChunk().x,
                realm.getCenterChunk().z
            );
            realmDataList.add(data);
        }
        
        // Send full sync (clears and replaces all realm data on clients)
        RealmSyncPacket packet = new RealmSyncPacket(realmDataList, true);
        
        // Send to all nearby players (within 256 blocks)
        for (ServerPlayer player : serverLevel.players()) {
            if (player.blockPosition().distSqr(pos) < 256 * 256) {
                PacketDistributor.sendToPlayer(player, packet);
                if (Config.DEBUG_REALM_SYNC.get()) {
                    LOGGER.info("[DEBUG] Sent realm sync to nearby player {} with {} realms", 
                        player.getName().getString(), realmDataList.size());
                }
            }
        }
    }
}
