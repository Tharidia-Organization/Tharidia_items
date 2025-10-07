package com.tharidia.tharidia_things.client;

import com.mojang.logging.LogUtils;
import com.tharidia.tharidia_things.block.entity.PietroBlockEntity;
import com.tharidia.tharidia_things.network.ClaimOwnerSyncPacket;
import com.tharidia.tharidia_things.network.RealmSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ClientPacketHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static List<PietroBlockEntity> syncedRealms = new ArrayList<>();

    public static void handleClaimOwnerSync(ClaimOwnerSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level != null) {
                BlockEntity blockEntity = level.getBlockEntity(packet.pos());
                if (blockEntity instanceof PietroBlockEntity claimEntity) {
                    // Note: ClaimOwnerSyncPacket is for ClaimBlock, but seems misused here?
                    // Assuming it's for setting owner, but PietroBlockEntity doesn't have setOwnerUUID
                }
            }
        });
    }

    public static void handleRealmSync(RealmSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            LOGGER.info("Received RealmSyncPacket with {} realms (fullSync: {})", 
                packet.realms().size(), packet.fullSync());
            
            // If this is a full sync, clear the list first
            if (packet.fullSync()) {
                LOGGER.info("Full sync - clearing all existing realm data");
                syncedRealms.clear();
            }
            
            // Process each realm in the packet
            for (RealmSyncPacket.RealmData data : packet.realms()) {
                LOGGER.info("Syncing realm at {} with size {} and center chunk ({}, {})", 
                    data.pos(), data.realmSize(), data.centerChunkX(), data.centerChunkZ());
                
                try {
                    // Get the actual block state from the world (if available) or create a default one
                    Level level = Minecraft.getInstance().level;
                    net.minecraft.world.level.block.state.BlockState blockState = null;
                    if (level != null && level.isLoaded(data.pos())) {
                        blockState = level.getBlockState(data.pos());
                    }
                    
                    // If we can't get the actual state, use the default Pietro block state
                    if (blockState == null || !blockState.is(com.tharidia.tharidia_things.TharidiaThings.PIETRO.get())) {
                        blockState = com.tharidia.tharidia_things.TharidiaThings.PIETRO.get().defaultBlockState();
                    }
                    
                    final net.minecraft.world.level.block.state.BlockState finalBlockState = blockState;
                    
                    // Create a dummy PietroBlockEntity for rendering
                    PietroBlockEntity dummy = new PietroBlockEntity(data.pos(), finalBlockState) {
                        private final int syncedRealmSize = data.realmSize();
                        private final net.minecraft.world.level.ChunkPos syncedCenterChunk = 
                            new net.minecraft.world.level.ChunkPos(data.centerChunkX(), data.centerChunkZ());
                        
                        @Override
                        public int getRealmSize() {
                            return syncedRealmSize;
                        }

                        @Override
                        public String getOwnerName() {
                            return data.ownerName();
                        }
                        
                        @Override
                        public net.minecraft.world.level.ChunkPos getCenterChunk() {
                            return syncedCenterChunk;
                        }
                        
                        @Override
                        public net.minecraft.world.level.ChunkPos getMinChunk() {
                            int radius = syncedRealmSize / 2;
                            return new net.minecraft.world.level.ChunkPos(
                                syncedCenterChunk.x - radius, 
                                syncedCenterChunk.z - radius
                            );
                        }
                        
                        @Override
                        public net.minecraft.world.level.ChunkPos getMaxChunk() {
                            int radius = syncedRealmSize / 2;
                            return new net.minecraft.world.level.ChunkPos(
                                syncedCenterChunk.x + radius, 
                                syncedCenterChunk.z + radius
                            );
                        }
                    };
                    // Set the center chunk on the parent class as well
                    dummy.centerChunk = new net.minecraft.world.level.ChunkPos(data.centerChunkX(), data.centerChunkZ());
                    
                    if (packet.fullSync()) {
                        // For full sync, just add all realms (list was already cleared)
                        syncedRealms.add(dummy);
                        LOGGER.info("Added realm at {} with size {}", data.pos(), data.realmSize());
                    } else {
                        // For incremental update, check if this realm already exists (by position)
                        boolean updated = false;
                        for (int i = 0; i < syncedRealms.size(); i++) {
                            PietroBlockEntity existing = syncedRealms.get(i);
                            if (existing.getBlockPos().equals(data.pos())) {
                                // Update the existing realm
                                syncedRealms.set(i, dummy);
                                updated = true;
                                LOGGER.info("Updated existing realm at {} to size {}", data.pos(), data.realmSize());
                                break;
                            }
                        }
                        
                        if (!updated) {
                            // Add new realm
                            syncedRealms.add(dummy);
                            LOGGER.info("Added new synced realm at {} with size {}", data.pos(), data.realmSize());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to sync realm at {}: {}", data.pos(), e.getMessage(), e);
                }
            }
            LOGGER.info("RealmSync completed. Total synced realms: {}", syncedRealms.size());
        });
    }
}
