package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.config.FatigueConfig;
import com.THproject.tharidia_things.fatigue.FatigueAttachments;
import com.THproject.tharidia_things.fatigue.FatigueData;
import com.THproject.tharidia_things.network.FatigueSyncPacket;
import com.THproject.tharidia_things.network.FatigueWarningPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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
    
    // Track which players have been warned to avoid spam
    private static final Map<UUID, Boolean> warningsSent5Min = new HashMap<>();
    private static final Map<UUID, Boolean> warningsSent1Min = new HashMap<>();
    
    // Track players who are resting (to prevent time skip)
    private static final Map<UUID, Boolean> playersResting = new HashMap<>();
    
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
        
        FatigueData data = player.getData(FatigueAttachments.FATIGUE_DATA);
        
        // CRITICAL: Check if player is sleeping BEFORE batching
        // Bed rest must be tracked every single tick for accurate rest time
        if (player.isSleeping()) {
            handleBedRest(player, data);
            return;
        }
        
        // LARGE SERVER OPTIMIZATION: Process non-sleeping players in staggered batches
        // Each player is processed every PLAYER_BATCH_SIZE ticks based on their UUID
        // This distributes the load evenly: 150 players / 5 = 30 players per tick
        // EXCEPTION: Players who are resting (but not sleeping) need to be processed every tick
        boolean isResting = playersResting.getOrDefault(player.getUUID(), false);
        
        if (!isResting) {
            int playerBatchSize = FatigueConfig.getPlayerBatchSize();
            int playerBatch = Math.abs(player.getUUID().hashCode() % playerBatchSize);
            if (player.tickCount % playerBatchSize != playerBatch) {
                return; // Not this player's turn this tick
            }
        }
        
        // Process player (either resting or their batched turn)
        {
            // Check if player was resting but is no longer sleeping
            if (playersResting.getOrDefault(player.getUUID(), false) && !data.hasRestedEnough()) {
                // Player woke up but hasn't rested enough
                BlockPos bedPos = data.getLastBedPosition();
                
                if (bedPos != null) {
                    double distanceToBed = player.distanceToSqr(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5);
                    double maxDistance = 3.0; // Must stay within 3 blocks of bed
                    
                    if (distanceToBed <= maxDistance * maxDistance) {
                        // Player is still near the bed - continue rest tracking
                        // Treat as "resting in bed area" - same speed as sleeping
                        data.incrementBedRestTicks();
                        
                        // Check if player has now rested enough
                        if (data.hasRestedEnough()) {
                            LOGGER.info("{} finished resting while near bed!", player.getName().getString());
                            data.fullyRestore();
                            playersResting.remove(player.getUUID());
                            data.setLastBedPosition(null);
                            
                            if (player instanceof ServerPlayer serverPlayer) {
                                PacketDistributor.sendToPlayer(serverPlayer, new FatigueSyncPacket(data.getFatigueTicks()));
                                serverPlayer.displayClientMessage(
                                    Component.translatable("message.tharidiathings.fully_rested"),
                                    true
                                );
                            }
                        }
                        // No messages to avoid spam - player can check their fatigue bar
                    } else {
                        // Player moved away from bed - stop rest
                        LOGGER.warn("{} moved too far from bed! Resetting rest progress", 
                            player.getName().getString());
                        playersResting.remove(player.getUUID());
                        data.resetBedRestTicks();
                        data.setLastBedPosition(null);
                    }
                } else {
                    // No bed position - can't track rest
                    playersResting.remove(player.getUUID());
                    data.resetBedRestTicks();
                }
            }
            
            // Reset bed rest ticks if not sleeping and not marked as resting
            if (!playersResting.getOrDefault(player.getUUID(), false) && 
                player.tickCount % 20 == 0 && data.getBedRestTicks() > 0) {
                data.resetBedRestTicks();
            }
        }
        
        // Check for movement and decrease fatigue (configurable interval to reduce overhead)
        if (player.tickCount % FatigueConfig.getMovementCheckInterval() == 0) {
            handleMovement(player, data);
        }
        
        // Check for bed proximity recovery (configurable interval to avoid expensive block scanning)
        if (player.tickCount % FatigueConfig.getBedCheckInterval() == 0) {
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
     * This is called EVERY TICK when player is sleeping for accurate rest tracking
     */
    private static void handleBedRest(Player player, FatigueData data) {
        // Mark player as resting (done every tick to ensure it's always set)
        boolean wasAlreadyResting = playersResting.getOrDefault(player.getUUID(), false);
        playersResting.put(player.getUUID(), true);
        
        if (!wasAlreadyResting) {
            LOGGER.info("Player {} started resting in bed", player.getName().getString());
        }
        
        // Store bed position
        BlockPos sleepPos = player.getSleepingPos().orElse(null);
        if (sleepPos != null) {
            data.setLastBedPosition(sleepPos);
        }
        
        // Increment bed rest ticks (counts how long they've been in bed)
        data.incrementBedRestTicks();
        
        // Log progress every 5 seconds to reduce spam
        if (data.getBedRestTicks() % 100 == 0) {
            int seconds = data.getBedRestTicks() / 20;
            int requiredSeconds = FatigueConfig.getBedRestTime() / 20;
            LOGGER.info("{} resting: {}/{} seconds ({} remaining)", 
                player.getName().getString(), seconds, requiredSeconds, requiredSeconds - seconds);
        }
        
        // Check if player has rested enough
        if (data.hasRestedEnough()) {
            // Player has rested for the required time - fully restore
            LOGGER.info("{} has rested enough! Fully restoring energy", player.getName().getString());
            data.fullyRestore();
            playersResting.remove(player.getUUID());
            data.setLastBedPosition(null); // Clear bed position
            
            // Sync immediately after full restore
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new FatigueSyncPacket(data.getFatigueTicks()));
                serverPlayer.displayClientMessage(
                    Component.translatable("message.tharidiathings.fully_rested"),
                    true
                );
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
     * Players recover fatigue when near a bed, even when standing still
     */
    private static void handleBedProximity(Player player, FatigueData data) {
        // Don't recover if already at max
        // NOTE: Removed the "|| player.isSleeping()" check because sleeping players should recover in handleBedRest instead
        if (data.getFatigueTicks() >= FatigueConfig.getMaxFatigueTicks()) {
            data.resetProximityTicks();
            return;
        }
        
        // Skip proximity recovery if player is sleeping (use bed rest instead)
        if (player.isSleeping()) {
            data.resetProximityTicks();
            return;
        }
        
        // Always check for bed/campfire proximity (removed cache to ensure consistency)
        // The check runs every bedCheckInterval ticks
        boolean nearRestPoint = isNearRestPoint(player);
        
        if (nearRestPoint) {
            // Increment by bedCheckInterval since this is only called every bedCheckInterval ticks
            int bedCheckInterval = FatigueConfig.getBedCheckInterval();
            for (int i = 0; i < bedCheckInterval; i++) {
                data.incrementProximityTicks();
            }
            
            // Recover fatigue at configured interval
            if (data.getProximityTicks() >= FatigueConfig.getProximityRecoveryInterval()) {
                data.increaseFatigue(FatigueConfig.getProximityRecoveryAmount());
                data.resetProximityTicks();
                
                LOGGER.info("{} recovered {} seconds of fatigue (proximity)", 
                    player.getName().getString(), FatigueConfig.getProximityRecoveryAmount() / 20);
                
                // Sync to client immediately after recovery
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new FatigueSyncPacket(data.getFatigueTicks()));
                }
            }
        } else {
            data.resetProximityTicks();
        }
    }
    
    /**
     * Checks if player is within range of a bed or campfire
     * Optimized to check only horizontally and limited vertical range
     * since rest points are typically on the ground
     */
    private static boolean isNearRestPoint(Player player) {
        BlockPos playerPos = player.blockPosition();
        double bedRange = FatigueConfig.getBedProximityRange();
        int range = (int) Math.ceil(bedRange);
        double rangeSquared = bedRange * bedRange;
        
        // Optimize by checking only ±5 blocks vertically (rest points are usually on same level)
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
                        Block block = state.getBlock();
                        
                        // Check for bed or campfire (lit or unlit)
                        if (block instanceof BedBlock || block instanceof CampfireBlock) {
                            return true; // Early exit as soon as rest point is found
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
        int threshold5Min = FatigueConfig.getWarningThreshold5MinTicks();
        int threshold1Min = FatigueConfig.getWarningThreshold1MinTicks();
        
        // Reset warnings when fatigue is restored above thresholds
        if (currentTicks > threshold5Min) {
            warningsSent5Min.remove(playerUUID);
            warningsSent1Min.remove(playerUUID);
        } else if (currentTicks > threshold1Min) {
            warningsSent1Min.remove(playerUUID);
        }
        
        // Send 5-minute warning
        if (currentTicks <= threshold5Min && currentTicks > threshold1Min) {
            if (!warningsSent5Min.getOrDefault(playerUUID, false)) {
                PacketDistributor.sendToPlayer(player, new FatigueWarningPacket(FatigueConfig.getWarningThreshold5MinTicks() / (60 * 20)));
                warningsSent5Min.put(playerUUID, true);
            }
        }
        
        // Send 1-minute warning
        if (currentTicks <= threshold1Min && currentTicks > 0) {
            if (!warningsSent1Min.getOrDefault(playerUUID, false)) {
                PacketDistributor.sendToPlayer(player, new FatigueWarningPacket(FatigueConfig.getWarningThreshold1MinTicks() / (60 * 20)));
                warningsSent1Min.put(playerUUID, true);
            }
        }
    }
    
    /**
     * Applies exhaustion effects (slowness and nausea)
     */
    private static void applyExhaustionEffects(Player player) {
        int duration = FatigueConfig.getExhaustionEffectDuration();
        int slownessLevel = FatigueConfig.getExhaustionSlownessLevel();
        
        // Apply configurable slowness effect
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, slownessLevel, false, false, false));
        
        // Apply nausea if configured
        if (FatigueConfig.shouldApplyNauseaEffect()) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0, false, false, false));
        }
    }
    
    /**
     * Removes exhaustion effects
     */
    private static void removeExhaustionEffects(Player player) {
        // We don't need to manually remove effects as they will expire
        // The short duration (40 ticks) ensures they disappear quickly when fatigue is restored
    }
    
    /**
     * Handle when player wakes up
     * If they haven't rested enough, they must stay near the bed to continue resting
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        
        // Server-side only
        if (player.level().isClientSide) {
            return;
        }
        
        FatigueData data = player.getData(FatigueAttachments.FATIGUE_DATA);
        
        // If player wakes up, check if they were resting
        if (playersResting.getOrDefault(player.getUUID(), false)) {
            if (data.hasRestedEnough()) {
                // Player has rested enough, allow wake up
                playersResting.remove(player.getUUID());
                data.setLastBedPosition(null);
                LOGGER.info("{} fully rested and woke up naturally (rested for {}/{} ticks)", 
                    player.getName().getString(), data.getBedRestTicks(), FatigueConfig.getBedRestTime());
            } else {
                // Player woke up early (probably due to SleepFinishedTimeEvent)
                int remaining = (FatigueConfig.getBedRestTime() - data.getBedRestTicks()) / 20;
                LOGGER.info("{} woke up early! Must stay near bed for {} more seconds",
                    player.getName().getString(), remaining);
                
                // Keep resting flag - they must stay near bed
                // The tick handler will continue rest tracking if they stay close
                // No message to avoid spam
            }
        } else {
            // Player wasn't marked as resting - reset their bed rest ticks
            data.resetBedRestTicks();
            data.setLastBedPosition(null);
        }
    }
    
    /**
     * Handle bed interaction for daytime fatigue recovery
     * We intercept the bed click BEFORE vanilla handles it, so we can bypass the daytime check
     * Vanilla BedBlock.use() checks time BEFORE calling startSleepInBed(), so CanPlayerSleepEvent
     * never fires during daytime. We must cancel the vanilla interaction and call startSleepInBed ourselves.
     *
     * IMPORTANT: This must handle BOTH client and server side:
     * - Client side: Cancel event to prevent vanilla showing "You can only sleep at night"
     * - Server side: Actually start the sleep
     */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onBedInteract(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        BlockState state = player.level().getBlockState(pos);

        // Check if the block is a bed
        if (!(state.getBlock() instanceof BedBlock)) {
            return;
        }

        // Check if it's daytime using VANILLA values (24000 ticks/day, day ends at 12541)
        // Using vanilla values ensures consistency between client and server
        long dayTime = player.level().getDayTime() % 24000L;
        boolean isDaytime = dayTime >= 0 && dayTime < 12541;

        if (player.level().isClientSide) {
            // CLIENT SIDE: Cancel during daytime to prevent vanilla "You can only sleep at night" message
            // We cancel unconditionally during daytime - the server will check fatigue and decide
            if (isDaytime) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        } else {
            // SERVER SIDE: Check fatigue and start sleeping if appropriate
            FatigueData data = player.getData(FatigueAttachments.FATIGUE_DATA);
            boolean isFatigued = data.getFatigueTicks() < FatigueConfig.getMaxFatigueTicks();

            LOGGER.info("Server bed interaction: isDaytime={}, isFatigued={}, fatigue={}/{}",
                isDaytime, isFatigued, data.getFatigueTicks(), FatigueConfig.getMaxFatigueTicks());

            if (isDaytime) {
                // Cancel vanilla interaction
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);

                if (isFatigued && player instanceof ServerPlayer serverPlayer) {
                    // Store bed position for rest tracking
                    data.setLastBedPosition(pos);

                    // Manually start sleeping - this will fire CanPlayerSleepEvent where we allow it
                    Player.BedSleepingProblem problem = serverPlayer.startSleepInBed(pos).left().orElse(null);

                    if (problem == null) {
                        LOGGER.info("Player {} started daytime rest for fatigue recovery",
                            player.getName().getString());
                    } else {
                        LOGGER.info("Player {} couldn't start daytime rest: {}",
                            player.getName().getString(), problem);
                        serverPlayer.displayClientMessage(
                            Component.translatable("block.minecraft.bed." + problem.name().toLowerCase()),
                            true
                        );
                    }
                } else if (player instanceof ServerPlayer serverPlayer) {
                    // Not fatigued - show vanilla "not possible now" message
                    serverPlayer.displayClientMessage(
                        Component.translatable("block.minecraft.bed.not_possible_now"),
                        true
                    );
                }
            } else if (isFatigued) {
                // Nighttime with fatigue - store bed position, let vanilla handle the rest
                data.setLastBedPosition(pos);
                LOGGER.info("Player {} interacting with bed at night while fatigued",
                    player.getName().getString());
            }
        }
    }

    /**
     * Allow players to sleep during daytime when fatigued
     * This overrides the vanilla "You can only sleep at night" check
     */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        Player player = event.getEntity();

        // Server-side only
        if (player.level().isClientSide) {
            return;
        }

        FatigueData data = player.getData(FatigueAttachments.FATIGUE_DATA);

        // Check if player is fatigued (not at max)
        if (data.getFatigueTicks() < FatigueConfig.getMaxFatigueTicks()) {
            // Check if it's daytime
            long dayTime = player.level().getDayTime() % FatigueConfig.getDayCycleLength();
            boolean isDaytime = dayTime >= 0 && dayTime < FatigueConfig.getDayEndTime();

            if (isDaytime) {
                // Allow daytime sleep for fatigue recovery
                event.setProblem(null); // Clear the "not possible now" problem

                // Mark player as resting
                playersResting.put(player.getUUID(), true);

                LOGGER.info("Player {} allowed to sleep during daytime for fatigue recovery",
                    player.getName().getString());

                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                        Component.translatable("message.tharidiathings.resting_for_fatigue"),
                        true
                    );
                }
            }
        }
    }

    /**
     * Keep players in bed during daytime rest until they're fully rested or manually get up
     * This prevents automatic wake-up during daytime
     */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onCanContinueSleeping(CanContinueSleepingEvent event) {
        // CanContinueSleepingEvent returns LivingEntity, need to check if it's a Player
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Server-side only
        if (player.level().isClientSide) {
            return;
        }

        // Check if player is marked as resting
        if (!playersResting.getOrDefault(player.getUUID(), false)) {
            return;
        }

        // Check if it's daytime
        long dayTime = player.level().getDayTime() % FatigueConfig.getDayCycleLength();
        boolean isDaytime = dayTime >= 0 && dayTime < FatigueConfig.getDayEndTime();

        if (isDaytime) {
            // During daytime rest, allow player to continue sleeping
            // They can only exit by pressing the leave bed key
            event.setContinueSleeping(true);
        }

        // If player has fully rested, allow them to continue until they manually wake
        // The handleBedRest method will notify them when they're fully rested
    }
    
    /**
     * Prevent time skip when players are resting for fatigue
     * CRITICAL: This must check ALL players, including those who just entered bed
     */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onSleepFinished(net.neoforged.neoforge.event.level.SleepFinishedTimeEvent event) {
        // Check if any players are resting OR fatigued and sleeping
        int restingCount = 0;
        StringBuilder restingPlayers = new StringBuilder();
        
        for (Player player : event.getLevel().players()) {
            boolean isResting = playersResting.getOrDefault(player.getUUID(), false);
            
            // Also check if player is sleeping and fatigued (might not be marked yet)
            if (!isResting && player.isSleeping()) {
                FatigueData data = player.getData(FatigueAttachments.FATIGUE_DATA);
                if (data.getFatigueTicks() < FatigueConfig.getMaxFatigueTicks()) {
                    isResting = true;
                    playersResting.put(player.getUUID(), true); // Mark them now
                    LOGGER.info("Player {} was sleeping while fatigued but not marked - marking now", 
                        player.getName().getString());
                }
            }
            
            if (isResting) {
                restingCount++;
                if (restingPlayers.length() > 0) restingPlayers.append(", ");
                restingPlayers.append(player.getName().getString());
            }
        }
        
        if (restingCount > 0) {
            // At least one player is still resting - prevent time skip
            LOGGER.info("BLOCKING TIME SKIP - {} player(s) are resting: {}", restingCount, restingPlayers);
            event.setTimeAddition(0);
        } else {
            LOGGER.info("No resting players - allowing time skip");
        }
    }
    
    /**
     * Clean up warnings and resting state when player logs out
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerUUID = event.getEntity().getUUID();
        warningsSent5Min.remove(playerUUID);
        warningsSent1Min.remove(playerUUID);
        playersResting.remove(playerUUID);
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
