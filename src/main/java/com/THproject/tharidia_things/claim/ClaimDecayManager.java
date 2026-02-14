package com.THproject.tharidia_things.claim;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.ClaimBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages automatic decay of expired claims after grace period + 14 days.
 * Removes station blocks and inventory blocks, but preserves building blocks.
 */
public class ClaimDecayManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimDecayManager.class);

    // Check interval: every 5 minutes (6000 ticks)
    private static final int CHECK_INTERVAL_TICKS = 6000;
    private static int tickCounter = 0;

    // Blocks to remove during decay (stations and containers)
    private static final Set<Block> DECAY_REMOVE_BLOCKS = new HashSet<>();

    static {
        // Stations / Workbenches
        DECAY_REMOVE_BLOCKS.add(Blocks.FURNACE);
        DECAY_REMOVE_BLOCKS.add(Blocks.BLAST_FURNACE);
        DECAY_REMOVE_BLOCKS.add(Blocks.SMOKER);
        DECAY_REMOVE_BLOCKS.add(Blocks.CRAFTING_TABLE);
        DECAY_REMOVE_BLOCKS.add(Blocks.SMITHING_TABLE);
        DECAY_REMOVE_BLOCKS.add(Blocks.FLETCHING_TABLE);
        DECAY_REMOVE_BLOCKS.add(Blocks.CARTOGRAPHY_TABLE);
        DECAY_REMOVE_BLOCKS.add(Blocks.LOOM);
        DECAY_REMOVE_BLOCKS.add(Blocks.STONECUTTER);
        DECAY_REMOVE_BLOCKS.add(Blocks.GRINDSTONE);
        DECAY_REMOVE_BLOCKS.add(Blocks.ANVIL);
        DECAY_REMOVE_BLOCKS.add(Blocks.CHIPPED_ANVIL);
        DECAY_REMOVE_BLOCKS.add(Blocks.DAMAGED_ANVIL);
        DECAY_REMOVE_BLOCKS.add(Blocks.ENCHANTING_TABLE);
        DECAY_REMOVE_BLOCKS.add(Blocks.BREWING_STAND);
        DECAY_REMOVE_BLOCKS.add(Blocks.CAULDRON);
        DECAY_REMOVE_BLOCKS.add(Blocks.WATER_CAULDRON);
        DECAY_REMOVE_BLOCKS.add(Blocks.LAVA_CAULDRON);
        DECAY_REMOVE_BLOCKS.add(Blocks.POWDER_SNOW_CAULDRON);
        DECAY_REMOVE_BLOCKS.add(Blocks.LECTERN);
        DECAY_REMOVE_BLOCKS.add(Blocks.COMPOSTER);
        DECAY_REMOVE_BLOCKS.add(Blocks.CAMPFIRE);
        DECAY_REMOVE_BLOCKS.add(Blocks.SOUL_CAMPFIRE);
        DECAY_REMOVE_BLOCKS.add(Blocks.BELL);
        DECAY_REMOVE_BLOCKS.add(Blocks.RESPAWN_ANCHOR);
        DECAY_REMOVE_BLOCKS.add(Blocks.LODESTONE);
        DECAY_REMOVE_BLOCKS.add(Blocks.BEEHIVE);
        DECAY_REMOVE_BLOCKS.add(Blocks.BEE_NEST);

        // Inventories / Containers
        DECAY_REMOVE_BLOCKS.add(Blocks.CHEST);
        DECAY_REMOVE_BLOCKS.add(Blocks.TRAPPED_CHEST);
        DECAY_REMOVE_BLOCKS.add(Blocks.ENDER_CHEST);
        DECAY_REMOVE_BLOCKS.add(Blocks.BARREL);
        DECAY_REMOVE_BLOCKS.add(Blocks.HOPPER);
        DECAY_REMOVE_BLOCKS.add(Blocks.DROPPER);
        DECAY_REMOVE_BLOCKS.add(Blocks.DISPENSER);
        DECAY_REMOVE_BLOCKS.add(Blocks.JUKEBOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.CHISELED_BOOKSHELF);
        DECAY_REMOVE_BLOCKS.add(Blocks.DECORATED_POT);
        DECAY_REMOVE_BLOCKS.add(Blocks.CRAFTER);

        // Shulker boxes (all colors)
        DECAY_REMOVE_BLOCKS.add(Blocks.SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.WHITE_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.ORANGE_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.MAGENTA_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.LIGHT_BLUE_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.YELLOW_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.LIME_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.PINK_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.GRAY_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.LIGHT_GRAY_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.CYAN_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.PURPLE_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.BLUE_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.BROWN_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.GREEN_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.RED_SHULKER_BOX);
        DECAY_REMOVE_BLOCKS.add(Blocks.BLACK_SHULKER_BOX);

        // Beds (remove as they act as spawn points)
        DECAY_REMOVE_BLOCKS.add(Blocks.WHITE_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.ORANGE_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.MAGENTA_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.LIGHT_BLUE_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.YELLOW_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.LIME_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.PINK_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.GRAY_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.LIGHT_GRAY_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.CYAN_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.PURPLE_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.BLUE_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.BROWN_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.GREEN_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.RED_BED);
        DECAY_REMOVE_BLOCKS.add(Blocks.BLACK_BED);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;

        if (tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }

        tickCounter = 0;

        // Process decay on all server levels
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            processDecayedClaims(level);
        }
    }

    /**
     * Processes all decayed claims in the given level.
     * A claim decays when: expired + 3 days grace + 14 days decay = 17 days total
     */
    private static void processDecayedClaims(ServerLevel level) {
        String dimension = level.dimension().location().toString();
        List<ClaimRegistry.ClaimData> allClaims = ClaimRegistry.getClaimsInDimension(dimension);

        List<BlockPos> claimsToDecay = new ArrayList<>();

        for (ClaimRegistry.ClaimData claimData : allClaims) {
            BlockPos claimPos = claimData.getPosition();

            // Only process if chunk is loaded
            if (!level.hasChunkAt(claimPos)) {
                continue;
            }

            BlockEntity be = level.getBlockEntity(claimPos);
            if (be instanceof ClaimBlockEntity claim) {
                if (claim.shouldDecay()) {
                    claimsToDecay.add(claimPos);
                }
            }
        }

        // Process decay for each claim
        for (BlockPos claimPos : claimsToDecay) {
            decayClaim(level, claimPos);
        }

        if (!claimsToDecay.isEmpty()) {
            LOGGER.info("Decayed {} claims in dimension {}", claimsToDecay.size(), dimension);
        }
    }

    /**
     * Performs decay on a single claim:
     * 1. Remove all station/inventory blocks in the claim's chunk
     * 2. Remove the claim block itself
     */
    private static void decayClaim(ServerLevel level, BlockPos claimPos) {
        BlockEntity be = level.getBlockEntity(claimPos);
        if (!(be instanceof ClaimBlockEntity claim)) {
            return;
        }

        String ownerName = claim.getOwnerName();
        String claimName = claim.getClaimName();

        // Get chunk boundaries
        int chunkX = claimPos.getX() >> 4;
        int chunkZ = claimPos.getZ() >> 4;

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        // Claim protection area (Y)
        int minY = claimPos.getY() - 20;
        int maxY = claimPos.getY() + 40;

        // Clamp to world bounds
        minY = Math.max(minY, level.getMinBuildHeight());
        maxY = Math.min(maxY, level.getMaxBuildHeight() - 1);

        int removedBlocks = 0;

        // Scan and remove decay blocks
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = level.getBlockState(pos).getBlock();

                    // Skip the claim block itself (we'll remove it at the end)
                    if (pos.equals(claimPos)) {
                        continue;
                    }

                    // Check if this block should be removed
                    if (shouldRemoveBlock(block)) {
                        // Drop no items - contents are lost
                        level.destroyBlock(pos, false);
                        removedBlocks++;
                    }
                }
            }
        }

        // Finally, remove the claim block
        level.destroyBlock(claimPos, false);

        LOGGER.info("Claim decayed: '{}' owned by '{}' at {} - Removed {} blocks",
            claimName, ownerName, claimPos, removedBlocks);
    }

    /**
     * Checks if a block should be removed during decay
     */
    private static boolean shouldRemoveBlock(Block block) {
        // Check against our predefined list
        if (DECAY_REMOVE_BLOCKS.contains(block)) {
            return true;
        }

        // Also check for any block that extends Container-related classes
        // This catches modded containers
        String className = block.getClass().getName().toLowerCase();
        if (className.contains("chest") ||
            className.contains("barrel") ||
            className.contains("hopper") ||
            className.contains("dispenser") ||
            className.contains("dropper") ||
            className.contains("shulker") ||
            className.contains("furnace") ||
            className.contains("anvil") ||
            className.contains("workbench") ||
            className.contains("crafting")) {
            return true;
        }

        return false;
    }

    /**
     * Manually trigger decay check for a specific claim.
     * Useful for admin commands.
     */
    public static boolean forceDecayClaim(ServerLevel level, BlockPos claimPos) {
        BlockEntity be = level.getBlockEntity(claimPos);
        if (be instanceof ClaimBlockEntity claim) {
            decayClaim(level, claimPos);
            return true;
        }
        return false;
    }

    /**
     * Gets the set of blocks that will be removed during decay.
     * Useful for displaying info to players.
     */
    public static Set<Block> getDecayRemoveBlocks() {
        return new HashSet<>(DECAY_REMOVE_BLOCKS);
    }
}
