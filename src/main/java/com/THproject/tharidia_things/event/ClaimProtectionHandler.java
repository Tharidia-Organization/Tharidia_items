package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.block.PietroBlock;
import com.THproject.tharidia_things.block.entity.ClaimBlockEntity;
import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import com.THproject.tharidia_things.claim.ClaimRegistry;
import com.THproject.tharidia_things.realm.RealmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ClaimProtectionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimProtectionHandler.class);
    
    // Cache for outer layer realm lookups (prevents repeated searches for same chunk)
    private static final java.util.Map<net.minecraft.world.level.ChunkPos, PietroBlockEntity> outerLayerCache = 
        new java.util.concurrent.ConcurrentHashMap<>();
    private static long lastCacheClear = System.currentTimeMillis();
    private static final long CACHE_CLEAR_INTERVAL = 5000; // Clear cache every 5 seconds
    
    // Cache for claim lookups (prevents repeated registry searches)
    private static final java.util.Map<BlockPos, ClaimBlockEntity> claimCache = 
        new java.util.concurrent.ConcurrentHashMap<>();
    private static long lastClaimCacheClear = System.currentTimeMillis();
    private static final long CLAIM_CACHE_CLEAR_INTERVAL = 10000; // Clear every 10 seconds

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

        // Special handling for Pietro blocks - check if claims exist in realm
        if (level.getBlockState(pos).getBlock() instanceof PietroBlock) {
            if (isPietroBlockProtected(serverLevel, pos, player)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.translatable("message.tharidiathings.claim.pietro_protected"));
            }
            return;
        }

        // Check for outer layer crop protection
        PietroBlockEntity realm = findRealmForPosition(serverLevel, pos);
        if (realm != null && realm.isPositionInOuterLayer(pos)) {
            // Check if it's a protected crop in the outer layer
            if (realm.isProtectedCrop(pos, level)) {
                // Only allow breaking crops if the player has a claim in the realm
                if (!hasClaimInRealm(player.getUUID(), realm, serverLevel)) {
                    event.setCanceled(true);
                    player.sendSystemMessage(Component.translatable("message.tharidiathings.claim.crop_requires_claim"));
                    return;
                }
            }
            // Allow breaking other blocks in the outer layer
            return;
        }

        // Check if the position is protected by a claim
        ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
        if (claim != null && claim.isProtectionActive() && !canPlayerInteract(claim, player)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.translatable("message.tharidiathings.claim.area_protected"));
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

        // Pietro blocks handle their own interaction logic (adding potatoes, etc)
        if (level.getBlockState(pos).getBlock() instanceof PietroBlock) {
            return;
        }

        // Check for outer layer crop protection
        PietroBlockEntity realm = findRealmForPosition(serverLevel, pos);
        if (realm != null && realm.isPositionInOuterLayer(pos)) {
            // Check if it's a protected crop in the outer layer
            if (realm.isProtectedCrop(pos, level)) {
                // Only allow interacting with crops if the player has a claim in the realm
                if (!hasClaimInRealm(player.getUUID(), realm, serverLevel)) {
                    event.setCanceled(true);
                    player.sendSystemMessage(Component.translatable("message.tharidiathings.claim.crop_requires_claim"));
                    return;
                }
            }
            // Allow interacting with other blocks in the outer layer
            return;
        }

        // Check if the position is protected by a claim
        ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
        if (claim != null && claim.isProtectionActive() && !canPlayerInteract(claim, player)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.translatable("message.tharidiathings.claim.area_protected"));
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

        // Pietro blocks are handled in onBlockBreak
        if (level.getBlockState(pos).getBlock() instanceof PietroBlock) {
            return;
        }

        // Check if the position is protected by a claim
        ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
        if (claim != null && claim.isProtectionActive() && !canPlayerInteract(claim, player)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.translatable("message.tharidiathings.claim.area_protected"));
        }
    }

    /**
     * Prevents explosions from destroying blocks in claimed areas
     * Using HIGHEST priority to ensure we process this before other mods
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Get the entity that caused the explosion (if any)
        Entity source = event.getExplosion().getIndirectSourceEntity();
        if (source == null) {
            source = event.getExplosion().getDirectSourceEntity();
        }
        UUID sourceUuid = source != null ? source.getUUID() : null;

        // Get explosion center coordinates
        Vec3 explosionPos = event.getExplosion().center();
        BlockPos explosionCenter = BlockPos.containing(explosionPos);

        // Use an iterator to safely remove blocks while iterating
        Iterator<BlockPos> iterator = event.getAffectedBlocks().iterator();
        int protectedCount = 0;
        boolean isOwner = false;
        ClaimBlockEntity firstClaim = null;
        
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
            
            if (claim != null && claim.isProtectionActive()) {
                if (firstClaim == null) {
                    firstClaim = claim;
                    isOwner = sourceUuid != null && sourceUuid.equals(claim.getOwnerUUID());
                }

                // Only protect if explosions are NOT allowed
                if (!claim.getAllowExplosions()) {
                    iterator.remove();
                    protectedCount++;
                }
            }
        }

        // Log explosion protection information
        if (protectedCount > 0) {
            String sourceName = source != null ? source.getName().getString() : "Unknown";
            LOGGER.info("Explosion blocked in claim - Source: {}, Is Owner: {}, Center: {}, Protected Blocks: {}",
                sourceName, isOwner, explosionCenter, protectedCount);
        }
    }

    /**
     * Monitors explosion start events (handled by Detonate event)
     */
    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event) {
        // Explosion protection is handled in the Detonate event
        // This event is kept for potential future use
    }

    /**
     * Prevents PvP attacks in claimed areas
     * PvP is ALWAYS disabled in claims, regardless of trust status
     * Uses NORMAL priority to run after battle logic checks
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // If event is already cancelled (e.g., by battle logic), don't process
        if (event.isCanceled()) {
            return;
        }
        
        Player attacker = event.getEntity();
        
        // Only check player vs player attacks
        if (!(event.getTarget() instanceof Player target)) {
            return;
        }
        
        // Check if either player is in a claimed area with active protection
        ClaimBlockEntity attackerClaim = findClaimForPosition(serverLevel, attacker.blockPosition());
        ClaimBlockEntity targetClaim = findClaimForPosition(serverLevel, target.blockPosition());

        // Block PvP if either player is in a claim with active protection
        boolean attackerProtected = attackerClaim != null && attackerClaim.isProtectionActive();
        boolean targetProtected = targetClaim != null && targetClaim.isProtectionActive();

        if (attackerProtected || targetProtected) {
            event.setCanceled(true);
            attacker.sendSystemMessage(Component.translatable("message.tharidiathings.claim.pvp_disabled"));
            
            LOGGER.info("PvP attack blocked in claim - Attacker: {} (in claim: {}), Target: {} (in claim: {})", 
                attacker.getName().getString(), 
                attackerClaim != null,
                target.getName().getString(),
                targetClaim != null);
        }
    }

    /**
     * Prevents crop trampling in claimed areas
     */
    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = event.getPos();
        
        // Check for outer layer protection first
        PietroBlockEntity realm = findRealmForPosition(serverLevel, pos);
        if (realm != null && realm.isPositionInOuterLayer(pos)) {
            // Check if trampling entity is a player
            if (event.getEntity() instanceof Player player) {
                // Only allow trampling if the player has a claim in the realm
                if (!hasClaimInRealm(player.getUUID(), realm, serverLevel)) {
                    event.setCanceled(true);
                    return;
                }
            } else {
                // Always prevent non-player trampling in outer layer
                event.setCanceled(true);
                return;
            }
        }
        
        ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);

        if (claim != null && claim.isProtectionActive()) {
            // Always prevent trampling in claims with active protection
            event.setCanceled(true);
        }
    }

    /**
     * Prevents fire spread in claimed areas (unless allowed by claim flag)
     * Intercepts fire placement events to stop spread
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFirePlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Check if fire is being placed
        if (event.getPlacedBlock().getBlock() == Blocks.FIRE) {
            BlockPos pos = event.getPos();
            ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
            
            // If in a claim with active protection and fire spread is not allowed, cancel the placement
            if (claim != null && claim.isProtectionActive() && !claim.getAllowFireSpread()) {
                // Only block natural fire spread (no entity)
                // Allow players to place fire if they have permission
                if (event.getEntity() == null || !(event.getEntity() instanceof Player)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * Additional fire protection - removes existing fire in claims where it's not allowed
     * This catches fire that bypasses the placement event
     */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = event.getPos();
        
        // Check if this is fire trying to spread
        if (serverLevel.getBlockState(pos).getBlock() == Blocks.FIRE) {
            ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
            
            if (claim != null && claim.isProtectionActive() && !claim.getAllowFireSpread()) {
                // Remove fire block from claims with active protection
                serverLevel.removeBlock(pos, false);
            }
        }
    }

    // ==================== NEW PROTECTION HANDLERS ====================

    /**
     * CRITICAL FIX: Prevents block placement in claimed areas by non-trusted players
     * This fixes the edge placement exploit where players could place blocks inside claims
     * by clicking on blocks outside the claim boundary
     *
     * Uses HIGHEST priority to run before other mods
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Skip fire - handled by onFirePlace with different logic
        if (event.getPlacedBlock().getBlock() == Blocks.FIRE) {
            return;
        }

        BlockPos placedPos = event.getPos();
        Entity placer = event.getEntity();

        // Check if the PLACED position is in a claim (not the clicked position!)
        ClaimBlockEntity claim = findClaimForPosition(serverLevel, placedPos);

        if (claim != null && claim.isProtectionActive()) {
            // If placed by a player, check trust
            if (placer instanceof Player player) {
                if (!canPlayerInteract(claim, player)) {
                    event.setCanceled(true);
                    player.sendSystemMessage(Component.translatable("message.tharidiathings.claim.no_place_blocks"));
                    LOGGER.debug("Block placement blocked for {} at {} (not trusted)",
                        player.getName().getString(), placedPos);
                }
            } else {
                // Non-player placement (dispenser, etc.) - always block in claims
                event.setCanceled(true);
                LOGGER.debug("Non-player block placement blocked at {} in claim", placedPos);
            }
        }

        // Also check outer layer realm protection for crops
        PietroBlockEntity realm = findRealmForPosition(serverLevel, placedPos);
        if (realm != null && realm.isPositionInOuterLayer(placedPos)) {
            if (placer instanceof Player player) {
                if (!hasClaimInRealm(player.getUUID(), realm, serverLevel)) {
                    // Only block if placing on farmland or crop-related
                    if (isCropRelatedBlock(event.getPlacedBlock().getBlock())) {
                        event.setCanceled(true);
                        player.sendSystemMessage(Component.translatable("message.tharidiathings.claim.plant_requires_claim"));
                    }
                }
            }
        }
    }

    /**
     * CRITICAL FIX: Prevents fluids from destroying blocks in claimed areas
     * This protects crops and other blocks from water/lava flow
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = event.getPos();

        // Check if position is in a claim with active protection
        ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
        if (claim != null && claim.isProtectionActive()) {
            // Always prevent fluid destruction in claims with active protection
            event.setCanceled(true);
            LOGGER.debug("Fluid block destruction prevented at {} in claim", pos);
            return;
        }

        // Also protect outer layer crops
        PietroBlockEntity realm = findRealmForPosition(serverLevel, pos);
        if (realm != null && realm.isPositionInOuterLayer(pos)) {
            // Prevent fluid from destroying anything in outer layer
            event.setCanceled(true);
            LOGGER.debug("Fluid block destruction prevented at {} in realm outer layer", pos);
        }
    }

    /**
     * Prevents pistons from pushing blocks INTO claimed areas
     * This stops exploits using pistons to grief claims
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPistonPre(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pistonPos = event.getPos();
        Direction direction = event.getDirection();

        // Get the piston structure to know what blocks will be moved
        PistonStructureResolver resolver = new PistonStructureResolver(
            serverLevel, pistonPos, direction, event.getPistonMoveType().isExtend
        );

        if (!resolver.resolve()) {
            return; // Piston can't move anything
        }

        // Check each block that will be pushed
        for (BlockPos movedPos : resolver.getToPush()) {
            BlockPos destinationPos = movedPos.relative(direction);

            // Check if source is in a claim
            ClaimBlockEntity sourceClaim = findClaimForPosition(serverLevel, movedPos);
            // Check if destination is in a claim
            ClaimBlockEntity destClaim = findClaimForPosition(serverLevel, destinationPos);

            // Check protection status for both claims
            boolean sourceProtected = sourceClaim != null && sourceClaim.isProtectionActive();
            boolean destProtected = destClaim != null && destClaim.isProtectionActive();

            // Case 1: Pushing block OUT of a protected claim (griefing by pulling)
            if (sourceProtected && !destProtected) {
                event.setCanceled(true);
                LOGGER.debug("Piston blocked: would push block out of claim from {} to {}",
                    movedPos, destinationPos);
                return;
            }

            // Case 2: Pushing block INTO a protected claim (griefing by pushing)
            if (!sourceProtected && destProtected) {
                event.setCanceled(true);
                LOGGER.debug("Piston blocked: would push block into claim from {} to {}",
                    movedPos, destinationPos);
                return;
            }

            // Case 3: Moving between different protected claims (not allowed)
            if (sourceProtected && destProtected &&
                !sourceClaim.getBlockPos().equals(destClaim.getBlockPos())) {
                event.setCanceled(true);
                LOGGER.debug("Piston blocked: would move block between different claims");
                return;
            }
        }

        // Check blocks that will be destroyed
        for (BlockPos destroyedPos : resolver.getToDestroy()) {
            ClaimBlockEntity claim = findClaimForPosition(serverLevel, destroyedPos);
            if (claim != null && claim.isProtectionActive()) {
                event.setCanceled(true);
                LOGGER.debug("Piston blocked: would destroy block in claim at {}", destroyedPos);
                return;
            }
        }
    }

    // NOTE: Mob spawn control via allowMobSpawning flag is not implemented yet
    // The flag exists in ClaimBlockEntity but requires a compatible spawn event
    // This can be added later when the correct NeoForge event is identified

    /**
     * Helper method to check if a block is crop-related
     */
    private static boolean isCropRelatedBlock(net.minecraft.world.level.block.Block block) {
        return block instanceof net.minecraft.world.level.block.CropBlock ||
               block instanceof net.minecraft.world.level.block.StemBlock ||
               block instanceof net.minecraft.world.level.block.AttachedStemBlock ||
               block == Blocks.PUMPKIN ||
               block == Blocks.MELON ||
               block == Blocks.SUGAR_CANE ||
               block == Blocks.CACTUS ||
               block == Blocks.BAMBOO ||
               block == Blocks.SWEET_BERRY_BUSH;
    }

    /**
     * Helper method to check if a block is a container
     */
    private static boolean isContainerBlock(net.minecraft.world.level.block.Block block) {
        return block instanceof net.minecraft.world.level.block.ChestBlock ||
               block instanceof net.minecraft.world.level.block.BarrelBlock ||
               block instanceof net.minecraft.world.level.block.ShulkerBoxBlock ||
               block instanceof net.minecraft.world.level.block.HopperBlock ||
               block instanceof net.minecraft.world.level.block.DispenserBlock ||
               block instanceof net.minecraft.world.level.block.DropperBlock ||
               block instanceof net.minecraft.world.level.block.AbstractFurnaceBlock ||
               block instanceof net.minecraft.world.level.block.BrewingStandBlock ||
               block instanceof net.minecraft.world.level.block.BeaconBlock ||
               block == Blocks.ENDER_CHEST ||
               block == Blocks.JUKEBOX ||
               block == Blocks.CHISELED_BOOKSHELF ||
               block == Blocks.LECTERN;
    }

    /**
     * Helper method to check if a block is a door/gate
     */
    private static boolean isDoorBlock(net.minecraft.world.level.block.Block block) {
        return block instanceof net.minecraft.world.level.block.DoorBlock ||
               block instanceof net.minecraft.world.level.block.TrapDoorBlock ||
               block instanceof net.minecraft.world.level.block.FenceGateBlock;
    }

    /**
     * Helper method to check if a block is a switch/redstone activator
     */
    private static boolean isSwitchBlock(net.minecraft.world.level.block.Block block) {
        return block instanceof net.minecraft.world.level.block.ButtonBlock ||
               block instanceof net.minecraft.world.level.block.LeverBlock ||
               block instanceof net.minecraft.world.level.block.PressurePlateBlock ||
               block instanceof net.minecraft.world.level.block.WeightedPressurePlateBlock ||
               block instanceof net.minecraft.world.level.block.DaylightDetectorBlock ||
               block instanceof net.minecraft.world.level.block.TripWireHookBlock ||
               block instanceof net.minecraft.world.level.block.NoteBlock ||
               block instanceof net.minecraft.world.level.block.RepeaterBlock ||
               block instanceof net.minecraft.world.level.block.ComparatorBlock;
    }

    /**
     * Handles container access based on claim flag
     * If allowContainerAccess is true, non-trusted players can open containers
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onContainerAccess(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        net.minecraft.world.level.block.Block block = level.getBlockState(pos).getBlock();

        // Only handle container blocks
        if (!isContainerBlock(block)) {
            return;
        }

        ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
        if (claim == null || !claim.isProtectionActive()) {
            return;
        }

        // If player is trusted, always allow
        if (canPlayerInteract(claim, player)) {
            return;
        }

        // If containers flag is enabled, allow access
        if (claim.getAllowContainerAccess()) {
            return;
        }

        // Block access
        event.setCanceled(true);
        player.sendSystemMessage(Component.translatable("message.tharidiathings.claim.no_container_access"));
    }

    /**
     * Handles door/gate access based on claim flag
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDoorAccess(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        net.minecraft.world.level.block.Block block = level.getBlockState(pos).getBlock();

        // Only handle door blocks
        if (!isDoorBlock(block)) {
            return;
        }

        ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
        if (claim == null || !claim.isProtectionActive()) {
            return;
        }

        // If player is trusted, always allow
        if (canPlayerInteract(claim, player)) {
            return;
        }

        // If doors flag is enabled, allow access
        if (claim.getAllowDoorUse()) {
            return;
        }

        // Block access
        event.setCanceled(true);
        player.sendSystemMessage(Component.translatable("message.tharidiathings.claim.no_door_access"));
    }

    /**
     * Handles switch/lever access based on claim flag
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSwitchAccess(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        net.minecraft.world.level.block.Block block = level.getBlockState(pos).getBlock();

        // Only handle switch blocks
        if (!isSwitchBlock(block)) {
            return;
        }

        ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
        if (claim == null || !claim.isProtectionActive()) {
            return;
        }

        // If player is trusted, always allow
        if (canPlayerInteract(claim, player)) {
            return;
        }

        // If switches flag is enabled, allow access
        if (claim.getAllowSwitchUse()) {
            return;
        }

        // Block access
        event.setCanceled(true);
        player.sendSystemMessage(Component.translatable("message.tharidiathings.claim.no_switch_access"));
    }

    /**
     * Handles vehicle placement (boats, minecarts) based on claim flag
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onVehiclePlace(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Check if player is placing a boat or minecart
        net.minecraft.world.item.ItemStack heldItem = event.getItemStack();
        if (heldItem.isEmpty()) {
            return;
        }

        boolean isVehicle = heldItem.getItem() instanceof net.minecraft.world.item.BoatItem ||
                           heldItem.getItem() instanceof net.minecraft.world.item.MinecartItem;

        if (!isVehicle) {
            return;
        }

        // Check the position where vehicle will be placed
        BlockPos placePos = pos.relative(event.getFace());
        ClaimBlockEntity claim = findClaimForPosition(serverLevel, placePos);
        if (claim == null) {
            claim = findClaimForPosition(serverLevel, pos);
        }

        if (claim == null || !claim.isProtectionActive()) {
            return;
        }

        // If player is trusted, always allow
        if (canPlayerInteract(claim, player)) {
            return;
        }

        // If vehicles flag is enabled, allow placement
        if (claim.getAllowVehiclePlace()) {
            return;
        }

        // Block placement
        event.setCanceled(true);
        player.sendSystemMessage(Component.translatable("message.tharidiathings.claim.no_vehicle_place"));
    }

    /**
     * Handles animal interaction (leash, saddle, feed) based on claim flag
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAnimalInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Level level = event.getLevel();

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Check if target is an animal
        Entity target = event.getTarget();
        if (!(target instanceof net.minecraft.world.entity.animal.Animal)) {
            return;
        }

        BlockPos pos = target.blockPosition();
        ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);
        if (claim == null || !claim.isProtectionActive()) {
            return;
        }

        // If player is trusted, always allow
        if (canPlayerInteract(claim, player)) {
            return;
        }

        // If animals flag is enabled, allow interaction
        if (claim.getAllowAnimalInteract()) {
            return;
        }

        // Block interaction
        event.setCanceled(true);
        player.sendSystemMessage(Component.translatable("message.tharidiathings.claim.no_animal_interact"));
    }

    // ==================== END NEW PROTECTION HANDLERS ====================

    /**
     * Prevents enderman from picking up blocks in claimed areas
     */
    @SubscribeEvent
    public static void onEndermanPickup(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Check if an enderman is breaking the block
        if (event.getPlayer() == null) {
            // No player = could be enderman or other entity
            BlockPos pos = event.getPos();
            ClaimBlockEntity claim = findClaimForPosition(serverLevel, pos);

            if (claim != null && claim.isProtectionActive()) {
                // Check nearby for enderman
                List<EnderMan> endermen = serverLevel.getEntitiesOfClass(EnderMan.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(10));
                if (!endermen.isEmpty()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * Finds a claim block that protects the given position
     * Protection area: entire chunk, 20 blocks below to 40 blocks above the claim block
     * Also checks merged claims for unified protection
     */
    public static ClaimBlockEntity findClaimForPosition(ServerLevel level, BlockPos pos) {
        // First check the current chunk
        ClaimBlockEntity claim = findClaimInChunk(level, pos);
        if (claim != null) {
            return claim;
        }
        
        // Check adjacent chunks for merged claims
        // This allows merged claims to act as one larger unified protection area
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue; // Already checked above
                
                int adjacentChunkX = chunkX + dx;
                int adjacentChunkZ = chunkZ + dz;
                
                if (level.hasChunk(adjacentChunkX, adjacentChunkZ)) {
                    ClaimBlockEntity adjacentClaim = findClaimInSpecificChunk(level, adjacentChunkX, adjacentChunkZ, pos);
                    if (adjacentClaim != null) {
                        // Check if this adjacent claim has merged claims that include the target position's chunk
                        if (isPositionInMergedClaimArea(adjacentClaim, pos)) {
                            return adjacentClaim;
                        }
                    }
                }
            }
        }

        return null;
    }
    
    /**
     * Finds a claim in the chunk containing the given position
     */
    private static ClaimBlockEntity findClaimInChunk(ServerLevel level, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return findClaimInSpecificChunk(level, chunkX, chunkZ, pos);
    }
    
    /**
     * Finds a claim in a specific chunk that protects the given position
     * OPTIMIZED: Uses ClaimRegistry instead of scanning ~98,000 blocks per chunk
     */
    private static ClaimBlockEntity findClaimInSpecificChunk(ServerLevel level, int chunkX, int chunkZ, BlockPos pos) {
        // Clear claim cache periodically
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClaimCacheClear > CLAIM_CACHE_CLEAR_INTERVAL) {
            claimCache.clear();
            lastClaimCacheClear = currentTime;
        }
        
        // Get dimension
        String dimension = level.dimension().location().toString();
        
        // Use ClaimRegistry for O(1) lookup instead of O(n) chunk scan
        List<ClaimRegistry.ClaimData> dimensionClaims =
            ClaimRegistry.getClaimsInDimension(dimension);
        
        // Filter claims in the target chunk
        for (ClaimRegistry.ClaimData claimData : dimensionClaims) {
            BlockPos claimPos = claimData.getPosition();
            
            // Check if claim is in the target chunk
            int claimChunkX = claimPos.getX() >> 4;
            int claimChunkZ = claimPos.getZ() >> 4;
            
            if (claimChunkX == chunkX && claimChunkZ == chunkZ) {
                // Check if position is within claim's protection area (Y-axis)
                int minY = claimPos.getY() - 20;
                int maxY = claimPos.getY() + 40;
                
                if (pos.getY() >= minY && pos.getY() <= maxY) {
                    // Try cache first
                    ClaimBlockEntity cached = claimCache.get(claimPos);
                    if (cached != null) {
                        return cached;
                    }
                    
                    // Cache miss - load from world
                    BlockEntity blockEntity = level.getBlockEntity(claimPos);
                    if (blockEntity instanceof ClaimBlockEntity claimEntity) {
                        claimCache.put(claimPos, claimEntity);
                        return claimEntity;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Checks if a position is within a claim's merged area
     * This makes merged claims act as one unified protection zone
     */
    private static boolean isPositionInMergedClaimArea(ClaimBlockEntity claim, BlockPos pos) {
        // Check if the position's chunk is one of the merged claims
        int posChunkX = pos.getX() >> 4;
        int posChunkZ = pos.getZ() >> 4;
        
        for (BlockPos mergedPos : claim.getMergedClaims()) {
            int mergedChunkX = mergedPos.getX() >> 4;
            int mergedChunkZ = mergedPos.getZ() >> 4;
            
            if (posChunkX == mergedChunkX && posChunkZ == mergedChunkZ) {
                // Position is in a merged claim's chunk - verify Y level
                int minY = mergedPos.getY() - 20;
                int maxY = mergedPos.getY() + 40;
                
                if (pos.getY() >= minY && pos.getY() <= maxY) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Checks if a player can interact with blocks in a claimed area
     * Now includes trusted players
     */
    private static boolean canPlayerInteract(ClaimBlockEntity claim, Player player) {
        return claim.isTrusted(player.getUUID());
    }
    
    /**
     * Checks if a Pietro block is protected by having claims in its realm
     */
    private static boolean isPietroBlockProtected(ServerLevel level, BlockPos pos, Player player) {
        // Get the block state to determine which half we're dealing with
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        
        // Get the lower block position (where the block entity is)
        BlockPos lowerPos = state.getValue(PietroBlock.HALF)
                == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER 
                ? pos.below() 
                : pos;
        
        // Get the block entity from the lower position
        BlockEntity blockEntity = level.getBlockEntity(lowerPos);
        
        if (blockEntity instanceof PietroBlockEntity pietroBlockEntity) {
            // Check if there are any claim blocks within the realm
            return hasClaimsInRealm(level, pietroBlockEntity);
        }
        
        return false;
    }
    
    /**
     * Checks if there are any claim blocks within the Pietro block's realm boundaries
     */
    private static boolean hasClaimsInRealm(ServerLevel level, PietroBlockEntity realm) {
        net.minecraft.world.level.ChunkPos minChunk = realm.getMinChunk();
        net.minecraft.world.level.ChunkPos maxChunk = realm.getMaxChunk();
        
        // Iterate through all chunks in the realm
        for (int chunkX = minChunk.x; chunkX <= maxChunk.x; chunkX++) {
            for (int chunkZ = minChunk.z; chunkZ <= maxChunk.z; chunkZ++) {
                // Only check loaded chunks to avoid forcing chunk loads
                if (level.hasChunk(chunkX, chunkZ)) {
                    net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                    
                    // Check all block entities in this chunk
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be instanceof ClaimBlockEntity) {
                            return true; // Found a claim block
                        }
                    }
                }
            }
        }
        
        return false; // No claim blocks found
    }
    
    /**
     * Finds a realm that contains the given position's OUTER LAYER only
     * Optimized: Only checks outer layer, not inner realm (which is handled by claims)
     * Uses caching to prevent repeated lookups for the same chunk
     */
    @org.jetbrains.annotations.Nullable
    private static PietroBlockEntity findRealmForPosition(ServerLevel level, BlockPos pos) {
        // Clear cache periodically to avoid stale data
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheClear > CACHE_CLEAR_INTERVAL) {
            outerLayerCache.clear();
            lastCacheClear = currentTime;
        }
        
        // Use chunk-based lookup for better performance
        net.minecraft.world.level.ChunkPos chunkPos = new net.minecraft.world.level.ChunkPos(pos);
        
        // Check cache first
        PietroBlockEntity cached = outerLayerCache.get(chunkPos);
        if (cached != null) {
            return cached;
        }
        
        // Cache miss - search through realms
        List<PietroBlockEntity> allRealms = RealmManager.getRealms(level);
        
        for (PietroBlockEntity realm : allRealms) {
            // Only check outer layer (inner realm is protected by claims)
            if (realm.isChunkInOuterLayer(chunkPos)) {
                outerLayerCache.put(chunkPos, realm);
                return realm;
            }
        }
        return null;
    }
    
    /**
     * Checks if a player has a claim in the specified realm
     * Optimized: Uses ClaimRegistry for O(1) lookup instead of iterating through chunks
     */
    private static boolean hasClaimInRealm(UUID playerUuid, PietroBlockEntity realm, ServerLevel level) {
        // Use ClaimRegistry for efficient lookup
        List<ClaimRegistry.ClaimData> playerClaims =
            ClaimRegistry.getPlayerClaims(playerUuid);
        
        if (playerClaims.isEmpty()) {
            return false;
        }
        
        // Get dimension string for filtering
        String currentDimension = level.dimension().location().toString();
        
        // Check if any of the player's claims are in this realm
        net.minecraft.world.level.ChunkPos minChunk = realm.getMinChunk();
        net.minecraft.world.level.ChunkPos maxChunk = realm.getMaxChunk();
        
        for (ClaimRegistry.ClaimData claim : playerClaims) {
            // Skip claims from other dimensions
            if (!claim.getDimension().equals(currentDimension)) {
                continue;
            }
            
            net.minecraft.world.level.ChunkPos claimChunk = new net.minecraft.world.level.ChunkPos(claim.getPosition());
            
            // Check if claim chunk is within realm bounds
            if (claimChunk.x >= minChunk.x && claimChunk.x <= maxChunk.x &&
                claimChunk.z >= minChunk.z && claimChunk.z <= maxChunk.z) {
                return true;
            }
        }
        
        return false;
    }
}
