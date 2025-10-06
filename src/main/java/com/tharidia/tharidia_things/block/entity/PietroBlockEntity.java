package com.tharidia.tharidia_things.block.entity;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.realm.RealmManager;
import mod.azure.azurelib.common.api.common.animatable.GeoBlockEntity;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.common.internal.common.util.AzureLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PietroBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = AzureLibUtil.createInstanceCache(this);

    // Animation controller
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.model.new");
    private static final int MIN_REALM_SIZE = 3;
    private static final int MAX_REALM_SIZE = 15;
    private static final int DEFAULT_REALM_SIZE = 3;
    private static final int BASE_POTATO_COST = 64; // Base cost for first expansion (1 stack)

    private int realmSize = DEFAULT_REALM_SIZE; // Size in chunks (e.g., 3 means 3x3 chunks)
    private ChunkPos centerChunk;
    private String ownerName = ""; // Name of the player who placed this block
    private int storedPotatoes = 0; // Current potatoes stored for next expansion

    public PietroBlockEntity(BlockPos pos, BlockState blockState) {
        super(TharidiaThings.PIETRO_BLOCK_ENTITY.get(), pos, blockState);
        // Calculate the chunk position where this block is located
        this.centerChunk = new ChunkPos(pos);
    }

    /**
     * Calculates the potato cost for the next expansion level
     * Formula: BASE_COST * (currentLevel - MIN_SIZE + 1)
     * Level 3->4: 64 potatoes (1 stack)
     * Level 4->5: 128 potatoes (2 stacks)
     * Level 5->6: 192 potatoes (3 stacks)
     * etc.
     */
    public int getPotatoCostForNextLevel() {
        if (realmSize >= MAX_REALM_SIZE) {
            return 0;
        }
        int levelDifference = realmSize - MIN_REALM_SIZE + 1;
        return BASE_POTATO_COST * levelDifference;
    }

    /**
     * Gets the current stored potatoes
     */
    public int getStoredPotatoes() {
        return storedPotatoes;
    }

    /**
     * Adds potatoes to the storage and attempts to expand if enough are stored
     * @param amount The amount of potatoes to add
     * @return true if expansion occurred, false otherwise
     */
    public boolean addPotatoes(int amount) {
        if (realmSize >= MAX_REALM_SIZE) {
            return false;
        }

        storedPotatoes += amount;
        int required = getPotatoCostForNextLevel();

        if (storedPotatoes >= required) {
            // Enough potatoes to expand
            storedPotatoes -= required;
            return expandRealm();
        }

        setChanged();
        return false;
    }

    /**
     * Gets the remaining potatoes needed for next expansion
     */
    public int getRemainingPotatoesNeeded() {
        if (realmSize >= MAX_REALM_SIZE) {
            return 0;
        }
        int required = getPotatoCostForNextLevel();
        return Math.max(0, required - storedPotatoes);
    }

    /**
     * Sets the owner of this realm
     */
    public void setOwner(String playerName) {
        this.ownerName = playerName;
        setChanged();
    }

    /**
     * Gets the owner name of this realm
     */
    public String getOwnerName() {
        return ownerName;
    }
    
    /**
     * Expands the realm by 1 chunk in all directions
     * @return true if expansion was successful, false if already at max size or would overlap
     */
    public boolean expandRealm() {
        if (realmSize >= MAX_REALM_SIZE) {
            return false;
        }

        // Check if expansion would cause overlap with other realms
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            if (!canExpand(serverLevel)) {
                return false;
            }
        }

        realmSize+=2;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    /**
     * Checks if this realm can expand without overlapping with other realms
     * @param serverLevel The server level
     * @return true if expansion is safe, false if it would cause overlap
     */
    private boolean canExpand(ServerLevel serverLevel) {
        // Calculate what the new size would be
        int newSize = realmSize + 2;
        int newRadius = newSize / 2;

        // Get all other realms
        List<PietroBlockEntity> allRealms = RealmManager.getRealms(serverLevel);

        for (PietroBlockEntity otherRealm : allRealms) {
            // Skip ourselves
            if (otherRealm.getBlockPos().equals(this.getBlockPos())) {
                continue;
            }

            // Calculate distance between realm centers
            int dx = Math.abs(this.centerChunk.x - otherRealm.getCenterChunk().x);
            int dz = Math.abs(this.centerChunk.z - otherRealm.getCenterChunk().z);
            int distance = Math.max(dx, dz);

            // Calculate minimum safe distance
            // Each realm extends newRadius from its center, so minimum distance is sum of radii
            int otherRadius = otherRealm.getRealmSize() / 2;
            int minSafeDistance = newRadius + otherRadius;

            if (distance < minSafeDistance) {
                return false; // Would overlap
            }
        }

        return true;
    }
    
    /**
     * Gets the current realm size in chunks
     */
    public int getRealmSize() {
        return realmSize;
    }
    
    /**
     * Gets the center chunk position of the realm
     */
    public ChunkPos getCenterChunk() {
        return centerChunk;
    }
    
    /**
     * Checks if a given block position is within the realm area
     */
    public boolean isPositionInRealm(BlockPos pos) {
        ChunkPos posChunk = new ChunkPos(pos);
        return isChunkInRealm(posChunk);
    }
    
    /**
     * Checks if a given chunk is within the realm area
     */
    public boolean isChunkInRealm(ChunkPos chunk) {
        int radius = realmSize / 2; // Radius in chunks from center
        int dx = Math.abs(chunk.x - centerChunk.x);
        int dz = Math.abs(chunk.z - centerChunk.z);
        return dx <= radius && dz <= radius;
    }
    
    /**
     * Gets the minimum chunk position (corner) of the realm
     */
    public ChunkPos getMinChunk() {
        int radius = realmSize / 2;
        return new ChunkPos(centerChunk.x - radius, centerChunk.z - radius);
    }
    
    /**
     * Gets the maximum chunk position (corner) of the realm
     */
    public ChunkPos getMaxChunk() {
        int radius = realmSize / 2;
        return new ChunkPos(centerChunk.x + radius, centerChunk.z + radius);
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RealmSize", realmSize);
        tag.putInt("CenterChunkX", centerChunk.x);
        tag.putInt("CenterChunkZ", centerChunk.z);
        tag.putString("OwnerName", ownerName);
        tag.putInt("StoredPotatoes", storedPotatoes);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        realmSize = tag.getInt("RealmSize");
        if (realmSize < MIN_REALM_SIZE) realmSize = DEFAULT_REALM_SIZE;
        if (realmSize > MAX_REALM_SIZE) realmSize = MAX_REALM_SIZE;

        int centerX = tag.getInt("CenterChunkX");
        int centerZ = tag.getInt("CenterChunkZ");
        centerChunk = new ChunkPos(centerX, centerZ);

        ownerName = tag.getString("OwnerName");
        storedPotatoes = tag.getInt("StoredPotatoes");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Register this realm when it's loaded
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            RealmManager.registerRealm(serverLevel, this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Unregister this realm when it's removed
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            RealmManager.unregisterRealm(serverLevel, this);
        }
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            return state.setAndContinue(IDLE_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
