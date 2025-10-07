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

            // Check if the player already has a claim in this realm
            if (player != null && hasPlayerClaimInRealm(serverLevel, realm, player)) {
                player.sendSystemMessage(Component.literal("§cPuoi piazzare solo un Claim per Regno!"));
                return null;
            }

            // Check if there's already a claim block in this chunk
            if (hasClaimInChunk(serverLevel, pos)) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal("§cPuò essere piazzato solo un Claim per chunk!"));
                }
                return null;
            }
        }

        return this.defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        
        // Set the owner UUID when the block is placed
        if (!level.isClientSide && placer instanceof Player player) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ClaimBlockEntity claimEntity) {
                claimEntity.setOwnerUUID(player.getUUID());
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
     */
    private boolean hasClaimInChunk(ServerLevel level, BlockPos pos) {
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
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClaimBlockEntity(pos, state);
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
