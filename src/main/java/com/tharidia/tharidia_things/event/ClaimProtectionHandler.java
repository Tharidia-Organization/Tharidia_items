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
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.UUID;

@EventBusSubscriber
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
}
