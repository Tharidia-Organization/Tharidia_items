package com.tharidia.tharidia_things.block;

import com.mojang.serialization.MapCodec;
import com.tharidia.tharidia_things.block.entity.ClaimBlockEntity;
import com.tharidia.tharidia_things.realm.RealmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ClaimBlock extends BaseEntityBlock {
    public static final MapCodec<ClaimBlock> CODEC = simpleCodec(ClaimBlock::new);
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public ClaimBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        // Check if we're on the server side
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // Check if the position is within a realm
            com.tharidia.tharidia_things.block.entity.PietroBlockEntity realm = 
                RealmManager.getRealmAt(serverLevel, pos);
            
            if (realm == null) {
                // Not in a realm, prevent placement
                if (player != null) {
                    player.sendSystemMessage(Component.literal("§cIl Claim può essere piazzato solo in un Regno!"));
                }
                return null;
            }

            // Check if there's already a claim block in this chunk
            ClaimBlockEntity existingClaim = getClaimInChunk(serverLevel, pos);
            if (existingClaim != null) {
                // Check if it's not owned by another player
                if (player != null && !player.getUUID().equals(existingClaim.getOwnerUUID())) {
                    player.sendSystemMessage(Component.literal("§cQuesta chunk è già occupata da un altro claim!"));
                    return null;
                } else {
                    player.sendSystemMessage(Component.literal("§cPuò essere piazzato solo un Claim per chunk!"));
                    return null;
                }
            }
            
            // Check for adjacent player claims (for expansion)
            if (player != null) {
                ClaimBlockEntity adjacentClaim = findAdjacentPlayerClaim(serverLevel, pos, player);
                
                if (adjacentClaim != null) {
                    // This is an expansion - allow it and increase expansion level
                    if (adjacentClaim.getExpansionLevel() >= 3) {
                        player.sendSystemMessage(Component.literal("§cIl claim adiacente ha già raggiunto il livello massimo di espansione (3)!"));
                        return null;
                    }
                    // Expansion will be handled in setPlacedBy
                } else {
                    // Not adjacent to existing claim - check player's total claim count (max 4)
                    // Ops/admins can place unlimited claims
                    if (!player.hasPermissions(2)) { // Permission level 2 = operator
                        int playerClaimCount = getPlayerTotalClaims(serverLevel, player);
                        if (playerClaimCount >= 4) {
                            player.sendSystemMessage(Component.literal("§cHai raggiunto il limite massimo di 4 claims!"));
                            return null;
                        }
                    }
                    
                    // Check if player already has a claim in this realm
                    if (hasPlayerClaimInRealm(serverLevel, realm, player)) {
                        player.sendSystemMessage(Component.literal("§cPer espandere il claim, piazzalo adiacente al tuo claim esistente!"));
                        return null;
                    }
                }
            }
        }

        return this.defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        // Set the owner UUID when the block is placed
        if (!level.isClientSide && placer instanceof Player player && level instanceof ServerLevel serverLevel) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ClaimBlockEntity claimEntity) {
                claimEntity.setOwnerUUID(player.getUUID());

                // Check if this is an expansion (adjacent to another claim)
                ClaimBlockEntity adjacentClaim = findAdjacentPlayerClaim(serverLevel, pos, player);
                if (adjacentClaim != null) {
                    // This is an expansion - inherit ALL settings from the adjacent claim
                    copyClaimSettings(claimEntity, adjacentClaim);

                    player.sendSystemMessage(Component.literal("§aClaim espanso!"));
                    player.sendSystemMessage(Component.literal("§eImpostazioni ereditate dal claim principale"));
                } else {
                    // New claim - set default 1-hour rental period
                    long currentTime = System.currentTimeMillis();
                    long oneHour = 60 * 60 * 1000L; // 1 hour in milliseconds
                    claimEntity.setRented(true, 0, 0.0); // 0 days, 0 cost (free initial hour)
                    claimEntity.setExpirationTime(currentTime + oneHour);

                    player.sendSystemMessage(Component.literal("§aClaim creato: §f" + claimEntity.getClaimName()));
                    player.sendSystemMessage(Component.literal("§eScade tra: §f1 ora"));
                }

                // Update expansion level based on total claims owned
                updateExpansionLevelBasedOnClaimCount(claimEntity, serverLevel, player);
            }
        }
    }

    /**
     * Checks if the player already has a claim block in the given realm
     */
    private boolean hasPlayerClaimInRealm(ServerLevel level, com.tharidia.tharidia_things.block.entity.PietroBlockEntity realm, Player player) {
        // Get the realm bounds
        net.minecraft.world.level.ChunkPos minChunk = realm.getMinChunk();
        net.minecraft.world.level.ChunkPos maxChunk = realm.getMaxChunk();
        
        // Scan each chunk in the realm
        for (int chunkX = minChunk.x; chunkX <= maxChunk.x; chunkX++) {
            for (int chunkZ = minChunk.z; chunkZ <= maxChunk.z; chunkZ++) {
                net.minecraft.world.level.ChunkPos chunkPos = new net.minecraft.world.level.ChunkPos(chunkX, chunkZ);
                
                // Only check loaded chunks for performance
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                
                // Scan all block entities in the chunk
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof ClaimBlockEntity claimEntity) {
                        // Check if this claim belongs to the player
                        if (player.getUUID().equals(claimEntity.getOwnerUUID())) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if there's already a claim block in the chunk at the given position
     * Returns the claim entity if found, null otherwise
     */
    private ClaimBlockEntity getClaimInChunk(ServerLevel level, BlockPos pos) {
        // Get chunk coordinates
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        // Only check loaded chunks
        if (!level.hasChunk(chunkX, chunkZ)) {
            return null;
        }
        
        net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        
        // Check block entities in chunk
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof ClaimBlockEntity claimEntity) {
                return claimEntity;
            }
        }

        return null;
    }
    
    /**
     * Finds an adjacent claim owned by the player
     * Checks chunks in all 4 cardinal directions
     */
    private ClaimBlockEntity findAdjacentPlayerClaim(ServerLevel level, BlockPos pos, Player player) {
        // Get current chunk coordinates
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        
        // Check all 4 adjacent chunks
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        for (int[] dir : directions) {
            int adjacentChunkX = chunkX + dir[0];
            int adjacentChunkZ = chunkZ + dir[1];
            
            if (!level.hasChunk(adjacentChunkX, adjacentChunkZ)) {
                continue;
            }
            
            net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(adjacentChunkX, adjacentChunkZ);
            
            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                if (blockEntity instanceof ClaimBlockEntity claimEntity) {
                    if (player.getUUID().equals(claimEntity.getOwnerUUID())) {
                        return claimEntity;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Copies all settings from the source claim to the target claim
     * Used when placing an expansion claim adjacent to an existing claim
     */
    private void copyClaimSettings(ClaimBlockEntity targetClaim, ClaimBlockEntity sourceClaim) {
        // Copy expiration settings
        targetClaim.setRented(sourceClaim.isRented(), sourceClaim.getRentalDays(), sourceClaim.getRentalCost());
        targetClaim.setExpirationTime(sourceClaim.getExpirationTime());

        // Copy trusted players
        targetClaim.getTrustedPlayers().clear();
        targetClaim.getTrustedPlayers().addAll(sourceClaim.getTrustedPlayers());

        // Copy claim flags
        targetClaim.setAllowExplosions(sourceClaim.getAllowExplosions());
        targetClaim.setAllowPvP(sourceClaim.getAllowPvP());
        targetClaim.setAllowMobSpawning(sourceClaim.getAllowMobSpawning());
        targetClaim.setAllowFireSpread(sourceClaim.getAllowFireSpread());

        // Note: expansion level and protection radius will be set by updateExpansionLevelBasedOnClaimCount
    }

    /**
     * Updates the expansion level and protection radius based on the total number of claims owned by the player
     * 1 claim -> expansion level 0, 2 claims -> expansion level 1, 3 claims -> expansion level 2, 4 claims -> expansion level 3
     */
    private void updateExpansionLevelBasedOnClaimCount(ClaimBlockEntity claimEntity, ServerLevel level, Player player) {
        int totalClaims = getPlayerTotalClaims(level, player);

        // Calculate expansion level: 1 claim = level 0, 4 claims = level 3
        int expansionLevel = Math.min(totalClaims - 1, 3); // Max level 3
        claimEntity.setExpansionLevel(expansionLevel);

        // Show updated info to player
        int protectionRadius = claimEntity.getProtectionRadius();
        player.sendSystemMessage(Component.literal("§6Livello di espansione: §f" + expansionLevel + "/3"));
        player.sendSystemMessage(Component.literal("§eRaggio di protezione: §f" + protectionRadius + " blocchi"));
        player.sendSystemMessage(Component.literal("§7Claims totali: §f" + totalClaims + "/4"));
    }

    /**
     * Gets the total number of claims owned by a player across all realms.
     */
    private int getPlayerTotalClaims(ServerLevel level, Player player) {
        return com.tharidia.tharidia_things.claim.ClaimRegistry.getPlayerClaims(player.getUUID()).size();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClaimBlockEntity(pos, state);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ClaimBlockEntity claimEntity) {
                player.openMenu(claimEntity, buf -> {
                    buf.writeBlockPos(pos);
                    // Send owner name to client
                    String ownerName = claimEntity.getClaimName();
                    buf.writeUtf(ownerName != null ? ownerName : "Unknown's Claim");
                });
                return net.minecraft.world.InteractionResult.CONSUME;
            }
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
