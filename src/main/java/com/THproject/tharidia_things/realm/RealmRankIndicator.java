package com.THproject.tharidia_things.realm;

import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spawns a subtle colored dust particle near players' shoulders
 * to indicate their rank in the realm they're currently in.
 *
 * - Only visible when player is inside a realm
 * - Single particle every ~3.5 seconds (70 ticks)
 * - Position: left shoulder (offset from player position)
 * - Color based on HierarchyRank
 */
public class RealmRankIndicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(RealmRankIndicator.class);

    // Particle spawn interval (70 ticks = ~3.5 seconds)
    private static final int TICK_INTERVAL = 70;

    // Particle size (0.7 = subtle)
    private static final float PARTICLE_SIZE = 0.7f;

    // Offset from player position (left shoulder area)
    private static final double OFFSET_X = -0.3;
    private static final double OFFSET_Y = 1.5;
    private static final double OFFSET_Z = 0.15;

    // Cache for realm lookups to avoid expensive searches every tick
    private static final Map<ChunkPos, PietroBlockEntity> realmCache = new ConcurrentHashMap<>();
    private static long lastCacheClear = System.currentTimeMillis();
    private static final long CACHE_CLEAR_INTERVAL = 10000; // 10 seconds

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Only run on server
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // Only check at intervals to save performance
        if (event.getEntity().tickCount % TICK_INTERVAL != 0) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        // Clear cache periodically
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheClear > CACHE_CLEAR_INTERVAL) {
            realmCache.clear();
            lastCacheClear = currentTime;
        }

        // Find if player is in a realm
        PietroBlockEntity realm = findRealmForPlayer(level, player.blockPosition());
        if (realm == null) {
            return;
        }

        // Get player's rank in this realm
        HierarchyRank rank = realm.getPlayerHierarchy(player.getUUID());
        if (rank == null) {
            // Player has no rank in this realm (not a member)
            return;
        }

        // Check if player has disabled their particle indicator
        if (realm.isParticleDisabled(player.getUUID())) {
            return;
        }

        // Spawn the particle
        spawnRankParticle(level, player, rank);
    }

    /**
     * Spawns a single colored dust particle at the player's left shoulder
     */
    private static void spawnRankParticle(ServerLevel level, ServerPlayer player, HierarchyRank rank) {
        Vector3f color = rank.getColor();

        DustParticleOptions dust = new DustParticleOptions(color, PARTICLE_SIZE);

        // Calculate position based on player's rotation
        double yaw = Math.toRadians(player.getYRot());

        // Rotate the offset based on player facing direction
        double rotatedX = OFFSET_X * Math.cos(yaw) - OFFSET_Z * Math.sin(yaw);
        double rotatedZ = OFFSET_X * Math.sin(yaw) + OFFSET_Z * Math.cos(yaw);

        double x = player.getX() + rotatedX;
        double y = player.getY() + OFFSET_Y;
        double z = player.getZ() + rotatedZ;

        // Send particle to all players who can see this player
        // Using count=1, no spread, no velocity for a subtle static particle
        level.sendParticles(dust, x, y, z, 1, 0, 0, 0, 0);
    }

    /**
     * Finds the realm that contains the given position.
     * Uses caching to improve performance.
     */
    private static PietroBlockEntity findRealmForPlayer(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);

        // Check cache first
        PietroBlockEntity cached = realmCache.get(chunkPos);
        if (cached != null) {
            // Verify it's still valid
            if (!cached.isRemoved()) {
                return cached;
            }
            realmCache.remove(chunkPos);
        }

        // Search through all realms
        List<PietroBlockEntity> allRealms = RealmManager.getRealms(level);

        for (PietroBlockEntity realm : allRealms) {
            if (realm.isPositionInRealm(pos)) {
                realmCache.put(chunkPos, realm);
                return realm;
            }
        }

        return null;
    }

    /**
     * Clears the realm cache. Call when realms are added/removed.
     */
    public static void clearCache() {
        realmCache.clear();
    }
}
