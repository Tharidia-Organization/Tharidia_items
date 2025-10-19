package com.tharidia.tharidia_things.event;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.fatigue.FatigueAttachments;
import com.tharidia.tharidia_things.fatigue.FatigueData;
import com.tharidia.tharidia_things.network.FatigueSyncPacket;
import com.tharidia.tharidia_things.network.FatigueWarningPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles player fatigue system
 * Optimized to reduce tick impact
 */
public class FatigueHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FatigueHandler.class);
    private static final ResourceLocation FATIGUE_SPEED_MODIFIER_ID = 
        ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "fatigue_speed_penalty");
    
    // Performance optimization: Check movement every 5 ticks instead of every tick
    private static final int MOVEMENT_CHECK_INTERVAL = 5;
    // Performance optimization: Check bed proximity every second instead of every tick
    private static final int BED_CHECK_INTERVAL = 20;
    
    // LARGE SERVER OPTIMIZATION: Stagger player processing to distribute load
    // Process only 1/5th of players per tick (each player processed every 5 ticks)
    private static final int PLAYER_BATCH_SIZE = 5;
    
    // Warning thresholds in minutes
    private static final int WARNING_THRESHOLD_5_MIN = 5 * 60 * 20; // 5 minutes in ticks
    private static final int WARNING_THRESHOLD_1_MIN = 1 * 60 * 20; // 1 minute in ticks
    
    // Track which players have been warned to avoid spam
    private static final Map<UUID, Boolean> warningsSent5Min = new HashMap<>();
    private static final Map<UUID, Boolean> warningsSent1Min = new HashMap<>();
    
    /**
     * Main tick handler for fatigue system
     * Optimized to run critical checks less frequently
     * LARGE SERVER: Uses player batching to distribute load across ticks
     */
    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        // Server-side only
        if (player.level().isClientSide) {
            return;
        }
        
        // LARGE SERVER OPTIMIZATION: Process players in staggered batches
        // Each player is processed every PLAYER_BATCH_SIZE ticks based on their UUID
        // This distributes the load evenly: 150 players / 5 = 30 players per tick
        int playerBatch = Math.abs(player.getUUID().hashCode() % PLAYER_BATCH_SIZE);
        if (player.tickCount % PLAYER_BATCH_SIZE != playerBatch) {
            return; // Not this player's turn this tick
        }
        
        FatigueData data = player.getData(FatigueAttachments.FATIGUE_DATA);
        
        // Check if player is sleeping in bed (every tick for accurate rest tracking)
        if (player.isSleeping()) {
            handleBedRest(player, data);
            return;
        } else {
            // Reset bed rest ticks if not sleeping (only check occasionally)
            if (player.tickCount % 20 == 0 && data.getBedRestTicks() > 0) {
                data.resetBedRestTicks();
            }
        }
        
        // Check for movement and decrease fatigue (every 5 ticks to reduce overhead)
        if (player.tickCount % MOVEMENT_CHECK_INTERVAL == 0) {
            handleMovement(player, data);
        }
        
        // Check for bed proximity recovery (every second to avoid expensive block scanning)
        if (player.tickCount % BED_CHECK_INTERVAL == 0) {
            handleBedProximity(player, data);
        }
        
        // Apply exhaustion effects (only every 20 ticks, effects last 40 ticks so this is sufficient)
        if (player.tickCount % 20 == 0) {
            if (data.isExhausted()) {
                applyExhaustionEffects(player);
            }
            // No need to remove effects as they expire automatically
        }
        
        // Sync to client every second
        if (player.tickCount % 20 == 0 && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new FatigueSyncPacket(data.getFatigueTicks()));
            
            // Check for fatigue warnings
            checkAndSendWarnings(serverPlayer, data);
        }
    }
    
    /**
     * Handles bed rest recovery
     */
    private static void handleBedRest(Player player, FatigueData data) {
        data.incrementBedRestTicks();
        
        // Check if player has rested enough
        if (data.hasRestedEnough()) {
            data.fullyRestore();
            // Sync immediately after full restore
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new FatigueSyncPacket(data.getFatigueTicks()));
            }
        }
    }
    
    /**
     * Handles movement detection and fatigue decrease
     * Uses REAL TIME instead of ticks for accurate fatigue tracking
     * This prevents issues with player batching affecting fatigue speed
     */
    private static void handleMovement(Player player, FatigueData data) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        
        long currentTime = System.currentTimeMillis();
        boolean isMoving = data.hasMoved(x, y, z);
        
        // If player is moving, calculate time-based fatigue decrease
        if (isMoving) {
            if (!data.isExhausted()) {
                // Calculate elapsed time since last check
                long elapsedMillis = currentTime - data.getLastMovementTime();
                
                // Only decrease if player was also moving in previous check
                // This prevents sudden large decreases when player starts moving
                if (data.wasMoving() && elapsedMillis > 0) {
                    data.decreaseFatigueByTime(elapsedMillis);
                }
                
                // Mark as moving for next check
                data.setMoving(true);
            }
        } else {
            // Player stopped moving
            data.setMoving(false);
        }
        
        // Update timestamp and position
        data.setLastMovementTime(currentTime);
        data.updateLastPosition(x, y, z);
    }
    
    /**
     * Handles bed proximity recovery
     * Uses cache to avoid expensive block scanning every call
     */
    private static void handleBedProximity(Player player, FatigueData data) {
        // Don't recover if already at max or if sleeping
        if (data.getFatigueTicks() >= FatigueData.MAX_FATIGUE_TICKS || player.isSleeping()) {
            data.resetProximityTicks();
            return;
        }
        
        // Check cache first to avoid expensive block scanning
        double x = player.getX();
        double z = player.getZ();
        Boolean cachedResult = data.getCachedBedProximity(x, z);
        
        boolean nearBed;
        if (cachedResult != null) {
            // Use cached result
            nearBed = cachedResult;
        } else {
            // Cache miss or invalidated - perform expensive check
            nearBed = isNearBed(player);
            data.setCachedBedProximity(x, z, nearBed);
        }
        
        if (nearBed) {
            data.incrementProximityTicks();
            
            // Recover fatigue every 10 seconds
            if (data.getProximityTicks() >= FatigueData.PROXIMITY_RECOVERY_INTERVAL) {
                data.increaseFatigue(FatigueData.PROXIMITY_RECOVERY_AMOUNT);
                data.resetProximityTicks();
            }
        } else {
            data.resetProximityTicks();
        }
    }
    
    /**
     * Checks if player is within range of a bed
     * Optimized to check only horizontally and limited vertical range
     * since beds are typically on the ground
     */
    private static boolean isNearBed(Player player) {
        BlockPos playerPos = player.blockPosition();
        int range = (int) Math.ceil(FatigueData.BED_PROXIMITY_RANGE);
        double rangeSquared = FatigueData.BED_PROXIMITY_RANGE * FatigueData.BED_PROXIMITY_RANGE;
        
        // Optimize by checking only ±5 blocks vertically (beds are usually on same level)
        int verticalRange = Math.min(5, range);
        
        // Check blocks in an optimized pattern
        // Start with closest blocks (spiral out from player)
        for (int r = 0; r <= range; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    // Skip if not on the outer edge of current radius (already checked)
                    if (r > 0 && Math.abs(x) < r && Math.abs(z) < r) {
                        continue;
                    }
                    
                    for (int y = -verticalRange; y <= verticalRange; y++) {
                        BlockPos checkPos = playerPos.offset(x, y, z);
                        
                        // Check if within actual range (sphere)
                        double distSquared = playerPos.distSqr(checkPos);
                        if (distSquared > rangeSquared) {
                            continue;
                        }
                        
                        BlockState state = player.level().getBlockState(checkPos);
                        if (state.getBlock() instanceof BedBlock) {
                            return true; // Early exit as soon as bed is found
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks and sends fatigue warnings to player
     */
    private static void checkAndSendWarnings(ServerPlayer player, FatigueData data) {
        UUID playerUUID = player.getUUID();
        int currentTicks = data.getFatigueTicks();
        
        // Reset warnings when fatigue is restored above thresholds
        if (currentTicks > WARNING_THRESHOLD_5_MIN) {
            warningsSent5Min.remove(playerUUID);
            warningsSent1Min.remove(playerUUID);
        } else if (currentTicks > WARNING_THRESHOLD_1_MIN) {
            warningsSent1Min.remove(playerUUID);
        }
        
        // Send 5-minute warning
        if (currentTicks <= WARNING_THRESHOLD_5_MIN && currentTicks > WARNING_THRESHOLD_1_MIN) {
            if (!warningsSent5Min.getOrDefault(playerUUID, false)) {
                PacketDistributor.sendToPlayer(player, new FatigueWarningPacket(5));
                warningsSent5Min.put(playerUUID, true);
            }
        }
        
        // Send 1-minute warning
        if (currentTicks <= WARNING_THRESHOLD_1_MIN && currentTicks > 0) {
            if (!warningsSent1Min.getOrDefault(playerUUID, false)) {
                PacketDistributor.sendToPlayer(player, new FatigueWarningPacket(1));
                warningsSent1Min.put(playerUUID, true);
            }
        }
    }
    
    /**
     * Applies exhaustion effects (slowness and nausea)
     */
    private static void applyExhaustionEffects(Player player) {
        // Apply extreme slowness (level 3 = -60% speed)
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3, false, false, false));
        
        // Apply slight nausea for blurred vision
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, false, false, false));
    }
    
    /**
     * Removes exhaustion effects
     */
    private static void removeExhaustionEffects(Player player) {
        // We don't need to manually remove effects as they will expire
        // The short duration (40 ticks) ensures they disappear quickly when fatigue is restored
    }
    
    /**
     * Prevent player from leaving bed until fully rested
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        
        // Server-side only
        if (player.level().isClientSide) {
            return;
        }
        
        FatigueData data = player.getData(FatigueAttachments.FATIGUE_DATA);
        
        // If player tries to leave bed before fully rested, prevent it
        if (data.getBedRestTicks() > 0 && !data.hasRestedEnough()) {
            // We can't directly cancel the wake up event, but we can force them back to sleep
            // This is handled by checking if they have enough rest
            // If not, they should stay in bed
            
            // Note: The actual prevention of leaving bed requires checking in the player tick
            // when they attempt to stop sleeping
        }
    }
    
    /**
     * Clean up warnings when player logs out
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerUUID = event.getEntity().getUUID();
        warningsSent5Min.remove(playerUUID);
        warningsSent1Min.remove(playerUUID);
    }
    
    /**
     * Initialize fatigue data when player logs in
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            FatigueData data = serverPlayer.getData(FatigueAttachments.FATIGUE_DATA);
            // Sync initial fatigue to client
            PacketDistributor.sendToPlayer(serverPlayer, new FatigueSyncPacket(data.getFatigueTicks()));
        }
    }
    
    /**
     * Clone fatigue data when player respawns
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        
        Player original = event.getOriginal();
        Player player = event.getEntity();
        
        // Copy fatigue data from old player to new player
        FatigueData oldData = original.getData(FatigueAttachments.FATIGUE_DATA);
        FatigueData newData = player.getData(FatigueAttachments.FATIGUE_DATA);
        
        newData.setFatigueTicks(oldData.getFatigueTicks());
        newData.updateLastPosition(player.getX(), player.getY(), player.getZ());
    }
}
