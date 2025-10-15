package com.tharidia.tharidia_things.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.tharidia.tharidia_things.block.entity.PietroBlockEntity;
import com.tharidia.tharidia_things.realm.RealmManager;
import org.slf4j.Logger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PietroBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<PietroBlock> CODEC = simpleCodec(PietroBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    // Minimum distance is 9 chunks (2 * max_radius where max_radius = 5)
    // This allows both realms to expand to 9x9 and just touch at the edges
    public static final int MIN_DISTANCE_CHUNKS = 50;

    public PietroBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Only create block entity for lower half
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new PietroBlockEntity(pos, state) : null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockpos = context.getClickedPos();
        Level level = context.getLevel();
        if (blockpos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockpos.above()).canBeReplaced(context)) {
            return this.defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        // Place upper half
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);

        // Store the player's name and UUID who placed the block
        if (!level.isClientSide && placer instanceof Player player) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PietroBlockEntity pietroBlockEntity) {
                pietroBlockEntity.setOwner(player.getName().getString(), player.getUUID());
                
                // Sync the new realm to all online players
                if (level instanceof ServerLevel serverLevel) {
                    syncNewRealmToAllPlayers(serverLevel, pietroBlockEntity);
                }
            }
        }
    }
    
    /**
     * Syncs a newly placed realm to all online players
     */
    private void syncNewRealmToAllPlayers(ServerLevel serverLevel, PietroBlockEntity realm) {
        // Create realm data for this new realm
        com.tharidia.tharidia_things.network.RealmSyncPacket.RealmData data = 
            new com.tharidia.tharidia_things.network.RealmSyncPacket.RealmData(
                realm.getBlockPos(),
                realm.getRealmSize(),
                realm.getOwnerName(),
                realm.getCenterChunk().x,
                realm.getCenterChunk().z
            );
        
        // Send incremental update to all players in the dimension
        com.tharidia.tharidia_things.network.RealmSyncPacket packet = 
            new com.tharidia.tharidia_things.network.RealmSyncPacket(java.util.List.of(data), false);
        
        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y) {
            if (half == DoubleBlockHalf.LOWER && direction == Direction.UP) {
                if (!neighborState.is(this) || neighborState.getValue(HALF) != DoubleBlockHalf.UPPER) {
                    // Check if block is protected before allowing removal
                    if (level instanceof ServerLevel serverLevel) {
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof PietroBlockEntity pietroBlockEntity) {
                            if (hasClaimsInRealm(serverLevel, pietroBlockEntity)) {
                                return state; // Keep the block, don't remove it
                            }
                        }
                    }
                    return Blocks.AIR.defaultBlockState();
                }
                return state;
            }
            if (half == DoubleBlockHalf.UPPER && direction == Direction.DOWN) {
                if (!neighborState.is(this) || neighborState.getValue(HALF) != DoubleBlockHalf.LOWER) {
                    // Check if block is protected before allowing removal
                    if (level instanceof ServerLevel serverLevel) {
                        BlockEntity blockEntity = level.getBlockEntity(pos.below());
                        if (blockEntity instanceof PietroBlockEntity pietroBlockEntity) {
                            if (hasClaimsInRealm(serverLevel, pietroBlockEntity)) {
                                return state; // Keep the block, don't remove it
                            }
                        }
                    }
                    return Blocks.AIR.defaultBlockState();
                }
                return state;
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, net.minecraft.world.level.material.FluidState fluid) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // Get the lower block position (where the block entity is)
            BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            
            // Get the block entity from the lower position
            BlockEntity blockEntity = level.getBlockEntity(lowerPos);
            
            if (blockEntity instanceof PietroBlockEntity pietroBlockEntity) {
                // Check if there are any claim blocks within the realm
                if (hasClaimsInRealm(serverLevel, pietroBlockEntity)) {
                    player.displayClientMessage(
                        Component.literal("Cannot remove Pietro block while claims exist in its realm!"),
                        true
                    );
                    // Restore both blocks on client side
                    level.sendBlockUpdated(pos, state, state, 3);
                    if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
                        level.sendBlockUpdated(pos.below(), level.getBlockState(pos.below()), level.getBlockState(pos.below()), 3);
                    } else {
                        level.sendBlockUpdated(pos.above(), level.getBlockState(pos.above()), level.getBlockState(pos.above()), 3);
                    }
                    return false; // Prevent destruction of both blocks
                }
            }
        }
        
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
    
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // Additional check when player starts destroying
            BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            BlockEntity blockEntity = level.getBlockEntity(lowerPos);
            
            if (blockEntity instanceof PietroBlockEntity pietroBlockEntity) {
                if (hasClaimsInRealm(serverLevel, pietroBlockEntity)) {
                    // Block is protected, don't send removal notification
                    if (player.isCreative()) {
                        preventDropFromBottomPart(level, pos, state, player);
                    }
                    super.playerWillDestroy(level, pos, state, player);
                    return state;
                }
                // Only notify removal if no claims exist
                notifyRealmRemoval(serverLevel, pietroBlockEntity);
            }
            
            if (player.isCreative()) {
                preventDropFromBottomPart(level, pos, state, player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }
    
    /**
     * Checks if there are any claim blocks within the realm boundaries
     * Optimized to check only loaded chunks and scan for block entities
     */
    private boolean hasClaimsInRealm(ServerLevel level, PietroBlockEntity realm) {
        ChunkPos minChunk = realm.getMinChunk();
        ChunkPos maxChunk = realm.getMaxChunk();
        
        // Iterate through all chunks in the realm
        for (int chunkX = minChunk.x; chunkX <= maxChunk.x; chunkX++) {
            for (int chunkZ = minChunk.z; chunkZ <= maxChunk.z; chunkZ++) {
                // Only check loaded chunks to avoid forcing chunk loads
                if (level.hasChunk(chunkX, chunkZ)) {
                    net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                    
                    // Check all block entities in this chunk
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be instanceof com.tharidia.tharidia_things.block.entity.ClaimBlockEntity) {
                            return true; // Found a claim block
                        }
                    }
                }
            }
        }
        
        return false; // No claim blocks found
    }
    
    /**
     * Notifies all online players that a realm has been removed
     */
    private void notifyRealmRemoval(ServerLevel serverLevel, PietroBlockEntity realm) {
        // IMPORTANT: Unregister the realm FIRST before syncing
        // This ensures it's not in the list when we send the sync
        BlockPos realmPos = realm.getBlockPos();
        RealmManager.unregisterRealm(serverLevel, realm);
        
        if (com.tharidia.tharidia_things.Config.DEBUG_REALM_SYNC.get()) {
            LOGGER.info("[DEBUG] Realm removed at {}, sending sync to all players", realmPos);
        }
        
        // Send a full sync to refresh all clients' realm lists
        // This ensures the removed realm is cleared from client memory
        java.util.List<PietroBlockEntity> remainingRealms = RealmManager.getRealms(serverLevel);
        java.util.List<com.tharidia.tharidia_things.network.RealmSyncPacket.RealmData> realmDataList = new java.util.ArrayList<>();
        
        for (PietroBlockEntity r : remainingRealms) {
            com.tharidia.tharidia_things.network.RealmSyncPacket.RealmData data = 
                new com.tharidia.tharidia_things.network.RealmSyncPacket.RealmData(
                    r.getBlockPos(),
                    r.getRealmSize(),
                    r.getOwnerName(),
                    r.getCenterChunk().x,
                    r.getCenterChunk().z
                );
            realmDataList.add(data);
        }
        
        if (com.tharidia.tharidia_things.Config.DEBUG_REALM_SYNC.get()) {
            LOGGER.info("[DEBUG] Sending sync with {} remaining realms after removal", realmDataList.size());
        }
        
        // Send full sync (clears and replaces all realm data on clients)
        com.tharidia.tharidia_things.network.RealmSyncPacket packet = 
            new com.tharidia.tharidia_things.network.RealmSyncPacket(realmDataList, true);
        
        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
            if (com.tharidia.tharidia_things.Config.DEBUG_REALM_SYNC.get()) {
                LOGGER.info("[DEBUG] Sent realm removal sync to player: {}", player.getName().getString());
            }
        }
    }

    protected static void preventDropFromBottomPart(Level level, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (half == DoubleBlockHalf.UPPER) {
            BlockPos blockpos = pos.below();
            BlockState blockstate = level.getBlockState(blockpos);
            if (blockstate.is(state.getBlock()) && blockstate.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, blockpos, Block.getId(blockstate));
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            // If upper half, redirect to lower half
            BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            BlockEntity blockEntity = level.getBlockEntity(lowerPos);
            if (blockEntity instanceof PietroBlockEntity pietroBlockEntity) {
                // Open GUI
                player.openMenu(pietroBlockEntity, buf -> buf.writeBlockPos(lowerPos));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    /**
     * Checks if a Pietro block can be placed at the given position
     * @param level The server level
     * @param pos The position to check
     * @return true if the block can be placed, false otherwise
     */
    public static boolean canPlacePietroBlock(ServerLevel level, BlockPos pos) {
        ChunkPos newChunkPos = new ChunkPos(pos);
        List<PietroBlockEntity> existingRealms = RealmManager.getRealms(level);

        for (PietroBlockEntity realm : existingRealms) {
            // Skip if checking the same block (for updates)
            if (realm.getBlockPos().equals(pos)) {
                continue;
            }

            ChunkPos realmChunkPos = realm.getCenterChunk();

            // Calculate distance in chunks (using Chebyshev distance for square realms)
            int dx = Math.abs(newChunkPos.x - realmChunkPos.x);
            int dz = Math.abs(newChunkPos.z - realmChunkPos.z);
            int distance = Math.max(dx, dz);

            if (distance < MIN_DISTANCE_CHUNKS) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the distance to the nearest Pietro block
     * @param level The server level
     * @param pos The position to check from
     * @return The distance in chunks, or -1 if no realms exist
     */
    public static int getDistanceToNearestRealm(ServerLevel level, BlockPos pos) {
        ChunkPos checkPos = new ChunkPos(pos);
        List<PietroBlockEntity> existingRealms = RealmManager.getRealms(level);

        if (existingRealms.isEmpty()) {
            return -1;
        }

        int minDistance = Integer.MAX_VALUE;

        for (PietroBlockEntity realm : existingRealms) {
            ChunkPos realmChunkPos = realm.getCenterChunk();
            int dx = Math.abs(checkPos.x - realmChunkPos.x);
            int dz = Math.abs(checkPos.z - realmChunkPos.z);
            int distance = Math.max(dx, dz);
            minDistance = Math.min(minDistance, distance);
        }

        return minDistance;
    }
}
