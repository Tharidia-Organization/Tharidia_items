package com.tharidia.tharidia_things.event;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.block.PietroBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class RealmPlacementHandler {

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
                    
                    // Notify the player
                    if (event.getEntity() instanceof Player player) {
                        int distance = PietroBlock.getDistanceToNearestRealm(serverLevel, pos);
                        int needed = PietroBlock.MIN_DISTANCE_CHUNKS - distance;
                        
                        player.sendSystemMessage(
                            Component.literal("§cCannot place Pietro block here!")
                                .append(Component.literal("\n§7Too close to another realm. Move " + needed + " chunks away."))
                                .append(Component.literal("\n§7(Minimum distance: " + PietroBlock.MIN_DISTANCE_CHUNKS + " chunks)"))
                        );
                    }
                }
            }
        }
    }
}
