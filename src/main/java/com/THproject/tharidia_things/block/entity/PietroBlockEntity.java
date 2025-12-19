package com.THproject.tharidia_things.block.entity;

import com.THproject.tharidia_things.block.PietroBlock;
import com.THproject.tharidia_things.config.CropProtectionConfig;
import com.THproject.tharidia_things.gui.PietroMenu;
import com.THproject.tharidia_things.network.HierarchySyncPacket;
import com.THproject.tharidia_things.network.RealmSyncPacket;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.claim.ClaimRegistry;
import com.THproject.tharidia_things.realm.HierarchyRank;
import com.THproject.tharidia_things.realm.RealmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PietroBlockEntity extends BlockEntity implements MenuProvider {

    private static final int MIN_REALM_SIZE = 3;
    private static final int MAX_REALM_SIZE = 15;
    private static final int DEFAULT_REALM_SIZE = 3;
    private static final int OUTER_LAYER_OFFSET = 6; // 6 chunks outer layer around the realm
    private static final int BASE_POTATO_COST = 64; // Base cost for first expansion (1 stack)

    private int realmSize = DEFAULT_REALM_SIZE; // Size in chunks (e.g., 3 means 3x3 chunks)
    public ChunkPos centerChunk;
    private String ownerName = ""; // Name of the player who placed this block
    private UUID ownerUUID; // UUID of the realm owner
    private int storedPotatoes = 0; // Current potatoes stored for next expansion
    private int totalClaimPotatoes = 0; // Total potatoes received from claims in this realm
    
    // Player hierarchy system - maps player UUID to their hierarchy rank
    private Map<UUID, HierarchyRank> playerHierarchy = new HashMap<>();
    
    // Potato inventory (1 slot for potatoes)
    private final ItemStackHandler potatoInventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // Process potatoes when contents change (server-side only)
            if (level != null && !level.isClientSide) {
                processPotatoes();
            }
        }
    };

    // Container data for syncing to client
    private final net.minecraft.world.inventory.ContainerData containerData = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> realmSize;
                case 1 -> storedPotatoes;
                case 2 -> totalClaimPotatoes;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> realmSize = value;
                case 1 -> storedPotatoes = value;
                case 2 -> totalClaimPotatoes = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public PietroBlockEntity(BlockPos pos, BlockState blockState) {
        super(TharidiaThings.PIETRO_BLOCK_ENTITY.get(), pos, blockState);
        // Calculate the chunk position where this block is located
        this.centerChunk = new ChunkPos(pos);
    }
    
    public ItemStackHandler getPotatoInventory() {
        return potatoInventory;
    }
    
    public net.minecraft.world.inventory.ContainerData getContainerData() {
        return containerData;
    }
    
    /**
     * Process potatoes from the inventory slot
     * Called each tick when GUI is open
     */
    public void processPotatoes() {
        ItemStack stack = potatoInventory.getStackInSlot(0);
        
        if (!stack.isEmpty() && stack.is(net.minecraft.world.item.Items.POTATO)) {
            int amount = stack.getCount();
            
            // Add potatoes and check for expansion
            boolean expanded = addPotatoes(amount);
            
            // Remove potatoes from slot
            potatoInventory.setStackInSlot(0, ItemStack.EMPTY);
            
            // Notify player through GUI update
            setChanged();
            
            if (expanded && level instanceof ServerLevel serverLevel) {
                // Broadcast expansion to nearby players
                for (Player nearbyPlayer : level.players()) {
                    if (nearbyPlayer.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 256) {
                        nearbyPlayer.sendSystemMessage(Component.literal("§aRegno espanso a " +
                            getRealmSize() + "x" + getRealmSize() + " chunks!"));
                    }
                }
            }
        }
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
    public void setOwner(String playerName, UUID playerUUID) {
        this.ownerName = playerName;
        this.ownerUUID = playerUUID;
        setChanged();
    }

    /**
     * Gets the owner name of this realm
     */
    public String getOwnerName() {
        return ownerName;
    }
    
    /**
     * Gets the owner UUID of this realm
     */
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    /**
     * Gets all players who have claims in this realm
     * Returns a set of UUIDs
     */
    public Set<UUID> getPlayersWithClaimsInRealm() {
        Set<UUID> players = new HashSet<>();
        
        if (level == null || level.isClientSide) {
            // On client, return cached hierarchy players
            return new HashSet<>(playerHierarchy.keySet());
        }
        
        if (level instanceof ServerLevel serverLevel) {
            String dimension = serverLevel.dimension().location().toString();
            List<ClaimRegistry.ClaimData> allClaims = ClaimRegistry.getClaimsInDimension(dimension);
            
            // Check each claim to see if it's in this realm
            for (ClaimRegistry.ClaimData claimData : allClaims) {
                if (isPositionInRealm(claimData.getPosition())) {
                    players.add(claimData.getOwnerUUID());
                }
            }
        }
        
        return players;
    }
    
    /**
     * Updates the player hierarchy when a claim is placed/removed in the realm
     */
    public void updatePlayerHierarchy() {
        if (level == null || level.isClientSide) {
            return;
        }
        
        Set<UUID> currentPlayers = getPlayersWithClaimsInRealm();
        
        // Add new players with default rank
        for (UUID playerUUID : currentPlayers) {
            if (!playerUUID.equals(ownerUUID) && !playerHierarchy.containsKey(playerUUID)) {
                playerHierarchy.put(playerUUID, HierarchyRank.COLONO);
                TharidiaThings.LOGGER.info("Added player {} to realm hierarchy with rank COLONO", playerUUID);
            }
        }
        
        // Remove players who no longer have claims
        Set<UUID> toRemove = new HashSet<>();
        for (UUID uuid : playerHierarchy.keySet()) {
            if (!currentPlayers.contains(uuid) && !uuid.equals(ownerUUID)) {
                toRemove.add(uuid);
            }
        }
        
        for (UUID uuid : toRemove) {
            playerHierarchy.remove(uuid);
            TharidiaThings.LOGGER.info("Removed player {} from realm hierarchy (no longer has claims)", uuid);
        }
        
        setChanged();
        
        // Sync to all players who might have the GUI open
        if (level instanceof ServerLevel serverLevel) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Sets the hierarchy rank for a player
     * Only the realm owner should be able to do this
     */
    public void setPlayerHierarchy(UUID playerUUID, HierarchyRank rank) {
        if (playerUUID.equals(ownerUUID)) {
            return; // Owner's rank cannot be changed
        }
        
        playerHierarchy.put(playerUUID, rank);
        setChanged();
        
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            
            // Send hierarchy update to all nearby players
            HierarchySyncPacket packet =
                HierarchySyncPacket.fromPietroBlock(this);
            
            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 256) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
                }
            }
            
            TharidiaThings.LOGGER.info("Updated hierarchy for player {} to rank {}", playerUUID, rank.getDisplayName());
        }
    }
    
    /**
     * Gets the hierarchy rank for a player
     */
    public HierarchyRank getPlayerHierarchy(UUID playerUUID) {
        if (playerUUID.equals(ownerUUID)) {
            return HierarchyRank.LORD; // Owner is always Lord
        }
        return playerHierarchy.getOrDefault(playerUUID, HierarchyRank.COLONO);
    }
    
    /**
     * Gets all player hierarchies
     */
    public Map<UUID, HierarchyRank> getAllPlayerHierarchies() {
        return new HashMap<>(playerHierarchy);
    }
    
    /**
     * Adds potato payment from a claim to this realm's total
     */
    public void addClaimPotatoPayment(int amount) {
        totalClaimPotatoes += amount;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        TharidiaThings.LOGGER.info("Realm at {} received {} potatoes from claim. Total: {}", 
            getBlockPos(), amount, totalClaimPotatoes);
    }
    
    public int getTotalClaimPotatoes() {
        return totalClaimPotatoes;
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

        int oldSize = realmSize;
        int oldLevel = calculateRealmLevel(oldSize);
        
        realmSize+=2;
        int newLevel = calculateRealmLevel(realmSize);
        
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            
            // Update block state if level changed
            if (newLevel > oldLevel) {
                net.minecraft.world.level.block.Block block = level.getBlockState(getBlockPos()).getBlock();
                if (block instanceof PietroBlock pietroBlock) {
                    pietroBlock.updateBlockStateLevel(level, getBlockPos(), realmSize);
                    pietroBlock.playUpgradeEffects(level, getBlockPos(), oldLevel, newLevel);
                }
            }
            
            // Sync the updated realm data to all nearby players
            if (level instanceof ServerLevel serverLevel) {
                syncRealmToNearbyPlayers(serverLevel);
            }
        }
        return true;
    }
    
    /**
     * Calculates the visual level from realm size
     */
    private int calculateRealmLevel(int realmSize) {
        if (realmSize == 3) return 0;
        if (realmSize == 5) return 1;
        if (realmSize == 7 || realmSize == 9) return 2;
        if (realmSize == 11 || realmSize == 13) return 3;
        if (realmSize == 15) return 4;
        return 0; // fallback
    }
    
    /**
     * Updates the block state to match the current realm size
     */
    private void updateBlockStateFromSize() {
        if (level != null && !level.isClientSide) {
            net.minecraft.world.level.block.Block block = level.getBlockState(getBlockPos()).getBlock();
            if (block instanceof PietroBlock pietroBlock) {
                pietroBlock.updateBlockStateLevel(level, getBlockPos(), realmSize);
            }
        }
    }
    
    /**
     * Syncs this realm's data to all nearby players
     */
    private void syncRealmToNearbyPlayers(ServerLevel serverLevel) {
        // Create realm data for this realm
        RealmSyncPacket.RealmData data =
            new RealmSyncPacket.RealmData(
                getBlockPos(),
                getRealmSize(),
                getOwnerName(),
                getCenterChunk().x,
                getCenterChunk().z
            );
        
        // Send to all players in the dimension
        RealmSyncPacket packet =
            new RealmSyncPacket(java.util.List.of(data));
        
        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
        }
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
     * Checks if a given block position is within the outer layer area
     * The outer layer is always 6 chunks beyond the main realm border
     */
    public boolean isPositionInOuterLayer(BlockPos pos) {
        ChunkPos posChunk = new ChunkPos(pos);
        return isChunkInOuterLayer(posChunk);
    }
    
    /**
     * Checks if a given chunk is within the outer layer area
     * The outer layer expands with the realm, always maintaining 6 chunks from the realm border
     */
    public boolean isChunkInOuterLayer(ChunkPos chunk) {
        // Check if chunk is outside main realm but within outer layer
        if (isChunkInRealm(chunk)) {
            return false; // It's in the main realm, not outer layer
        }
        
        int outerRadius = (realmSize / 2) + OUTER_LAYER_OFFSET;
        int dx = Math.abs(chunk.x - centerChunk.x);
        int dz = Math.abs(chunk.z - centerChunk.z);
        
        return dx <= outerRadius && dz <= outerRadius;
    }
    
    /**
     * Checks if a block position is a crop or farmland that needs protection in the outer layer
     */
    public boolean isProtectedCrop(BlockPos pos, net.minecraft.world.level.Level level) {
        if (!isPositionInOuterLayer(pos)) {
            return false;
        }
        
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        net.minecraft.world.level.block.Block block = state.getBlock();
        
        // Check if the block is a protected crop or farmland using the config
        return CropProtectionConfig.isProtectedCrop(block) ||
               CropProtectionConfig.isProtectedFarmland(block);
    }
    
    /**
     * Gets the minimum chunk position (corner) of the outer layer
     */
    public ChunkPos getOuterLayerMinChunk() {
        int outerRadius = (realmSize / 2) + OUTER_LAYER_OFFSET;
        return new ChunkPos(centerChunk.x - outerRadius, centerChunk.z - outerRadius);
    }
    
    /**
     * Gets the maximum chunk position (corner) of the outer layer
     */
    public ChunkPos getOuterLayerMaxChunk() {
        int outerRadius = (realmSize / 2) + OUTER_LAYER_OFFSET;
        return new ChunkPos(centerChunk.x + outerRadius, centerChunk.z + outerRadius);
    }
    
    /**
     * Gets all realms in the given server level
     */
    public static List<PietroBlockEntity> getRealms(ServerLevel level) {
        return RealmManager.getRealms(level);
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
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        tag.putInt("StoredPotatoes", storedPotatoes);
        tag.putInt("TotalClaimPotatoes", totalClaimPotatoes);
        tag.put("PotatoInventory", potatoInventory.serializeNBT(registries));
        
        // Save player hierarchy
        CompoundTag hierarchyTag = new CompoundTag();
        int i = 0;
        for (Map.Entry<UUID, HierarchyRank> entry : playerHierarchy.entrySet()) {
            hierarchyTag.putUUID("Player" + i, entry.getKey());
            hierarchyTag.putInt("Rank" + i, entry.getValue().getLevel());
            i++;
        }
        hierarchyTag.putInt("Count", i);
        tag.put("PlayerHierarchy", hierarchyTag);
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
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
        }
        storedPotatoes = tag.getInt("StoredPotatoes");
        totalClaimPotatoes = tag.getInt("TotalClaimPotatoes");
        if (tag.contains("PotatoInventory")) {
            potatoInventory.deserializeNBT(registries, tag.getCompound("PotatoInventory"));
        }
        
        // Load player hierarchy
        if (tag.contains("PlayerHierarchy")) {
            CompoundTag hierarchyTag = tag.getCompound("PlayerHierarchy");
            int count = hierarchyTag.getInt("Count");
            playerHierarchy.clear();
            for (int i = 0; i < count; i++) {
                if (hierarchyTag.hasUUID("Player" + i)) {
                    UUID playerUUID = hierarchyTag.getUUID("Player" + i);
                    int rankLevel = hierarchyTag.getInt("Rank" + i);
                    playerHierarchy.put(playerUUID, HierarchyRank.fromLevel(rankLevel));
                }
            }
        }
        
        // Update block state after loading (will be applied in onLoad)
        // Don't do it here as level might not be ready yet
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.tharidiathings.pietro");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PietroMenu(containerId, playerInventory, this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Register this realm when it's loaded
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            RealmManager.registerRealm(serverLevel, this);
            
            // Update block state to match current realm size
            updateBlockStateFromSize();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Unregister this realm when it's removed
        // NOTE: This might be called after notifyRealmRemoval already unregistered it
        // RealmManager.unregisterRealm handles duplicate unregistration safely
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
}
