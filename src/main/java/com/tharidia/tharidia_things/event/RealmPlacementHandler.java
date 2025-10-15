package com.tharidia.tharidia_things.event;

import com.mojang.logging.LogUtils;
import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.block.PietroBlock;
import com.tharidia.tharidia_things.block.entity.PietroBlockEntity;
import com.tharidia.tharidia_things.network.RealmSyncPacket;
import com.tharidia.tharidia_things.realm.RealmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RealmPlacementHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            Block placedBlock = event.getPlacedBlock().getBlock();
            
            // Check if a Pietro block is being placed
            if (placedBlock instanceof PietroBlock) {
                BlockPos pos = event.getPos();
                
                // Check if placement is valid
                if (!PietroBlock.canPlacePietroBlock(serverLevel, pos)) {
                    // Cancel the placement
                    event.setCanceled(true);
                    
                    if (com.tharidia.tharidia_things.Config.DEBUG_REALM_SYNC.get()) {
                        LOGGER.info("[DEBUG] Realm placement cancelled at {}, sending sync to clear client data", pos);
                    }
                    
                    // Notify the player
                    if (event.getEntity() instanceof Player player) {
                        int distance = PietroBlock.getDistanceToNearestRealm(serverLevel, pos);
                        int needed = PietroBlock.MIN_DISTANCE_CHUNKS - distance;
                        
                        player.sendSystemMessage(
                            Component.literal("§cCannot place Pietro block here!")
                                .append(Component.literal("\n§7Too close to another realm. Move " + needed + " chunks away."))
                                .append(Component.literal("\n§7(Minimum distance: " + PietroBlock.MIN_DISTANCE_CHUNKS + " chunks)"))
                        );
                        
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
                if (com.tharidia.tharidia_things.Config.DEBUG_REALM_SYNC.get()) {
                    LOGGER.info("[DEBUG] Sent realm sync to nearby player {} with {} realms", 
                        player.getName().getString(), realmDataList.size());
                }
            }
        }
    }
}
