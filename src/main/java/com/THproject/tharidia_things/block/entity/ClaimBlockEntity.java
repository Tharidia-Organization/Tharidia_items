package com.THproject.tharidia_things.block.entity;

import com.THproject.tharidia_things.integration.GodEyeIntegration;
import com.THproject.tharidia_things.realm.RealmManager;
import com.THproject.tharidia_things.util.PlayerNameHelper;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.claim.ClaimRegistry;
import com.THproject.tharidia_things.gui.ClaimMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ClaimBlockEntity extends BlockEntity implements MenuProvider {
    private UUID ownerUUID;
    private String ownerName = ""; // Chosen name from NameService
    private Set<UUID> trustedPlayers = new HashSet<>();
    private String claimName = "";
    private long creationTime;
    private long expirationTime = -1; // -1 means no expiration
    private boolean isRented = false;
    private int rentalDays = 0;
    private double rentalCost = 0.0;
    private List<BlockPos> mergedClaims = new ArrayList<>();
    // Removed expansionLevel field - no longer stored per claim
    private int claimCountWhenPlaced = 0; // Stores claim count when this claim was placed
    private BlockPos linkedRealmPos = null; // Position of the Pietro block (realm) this claim belongs to
    private int totalPotatoesPaid = 0; // Total potatoes paid to this claim over time
    
    // Claim flags
    private boolean allowExplosions = false;
    private boolean allowPvP = false;
    private boolean allowMobSpawning = false;
    private boolean allowFireSpread = false;
    
    // Single slot inventory
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // Process potato payment when items are added
            processPotatoPayment(slot);
        }
        
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Only accept potatoes as payment
            return stack.is(net.minecraft.world.item.Items.POTATO);
        }
    };
    
    // Container data for syncing to client
    private final net.minecraft.world.inventory.ContainerData containerData = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (expirationTime >> 32); // High 32 bits
                case 1 -> (int) (expirationTime & 0xFFFFFFFF); // Low 32 bits
                case 2 -> isRented ? 1 : 0;
                case 3 -> getProtectionRadius(); // Calculated from claim count, not stored
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> expirationTime = (expirationTime & 0xFFFFFFFFL) | ((long) value << 32);
                case 1 -> expirationTime = (expirationTime & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                case 2 -> isRented = value == 1;
                case 3 -> {} // Protection radius is calculated, no need to set
            }
        }

        @Override
        public int getCount() {
            return 4; // Reduced from 5 since we removed expansion level
        }
    };

    public ClaimBlockEntity(BlockPos pos, BlockState blockState) {
        super(TharidiaThings.CLAIM_BLOCK_ENTITY.get(), pos, blockState);
    }
    
    public ItemStackHandler getInventory() {
        return inventory;
    }
    
    public net.minecraft.world.inventory.ContainerData getContainerData() {
        return containerData;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
        this.creationTime = System.currentTimeMillis();

        // Store current claim count for this player when claim is placed
        // Use persistent count to ensure accuracy even after server restart
        if (level instanceof ServerLevel serverLevel) {
            this.claimCountWhenPlaced = ClaimRegistry.getPlayerClaimCountPersistent(ownerUUID, serverLevel);
        }

        // Automatically set claim name and owner name using chosen name from NameService
        if (level instanceof ServerLevel serverLevel) {
            net.minecraft.server.level.ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
            if (player != null) {
                this.ownerName = PlayerNameHelper.getChosenName(player);
                this.claimName = this.ownerName + "'s Claim";
            } else {
                // Player is offline, try to get from NameService storage
                this.ownerName = PlayerNameHelper.getChosenNameByUUID(ownerUUID, serverLevel.getServer());
                this.claimName = this.ownerName + "'s Claim";
            }
        }

        setChanged();

        // Register in claim registry
        if (level instanceof ServerLevel serverLevel) {
            ClaimRegistry.registerClaim(serverLevel, worldPosition, this);
            // Try to link to a nearby realm
            findAndLinkToRealm(serverLevel);
        }
    }

    public Set<UUID> getTrustedPlayers() {
        return trustedPlayers;
    }

    public void addTrustedPlayer(UUID playerUUID) {
        trustedPlayers.add(playerUUID);
        setChanged();
    }

    public void removeTrustedPlayer(UUID playerUUID) {
        trustedPlayers.remove(playerUUID);
        setChanged();
    }

    public boolean isTrusted(UUID playerUUID) {
        return ownerUUID != null && (ownerUUID.equals(playerUUID) || trustedPlayers.contains(playerUUID));
    }

    public String getClaimName() {
        return claimName;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
        setChanged();
    }

    public void setClaimName(String name) {
        this.claimName = name;
        setChanged();
        
        // Update claim registry
        if (level instanceof ServerLevel serverLevel) {
            ClaimRegistry.updateClaimName(serverLevel, worldPosition, name);
        }
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
        setChanged();
        
        // Update persistent storage
        if (level instanceof ServerLevel serverLevel) {
            ClaimRegistry.updateClaimExpirationTime(serverLevel, worldPosition, expirationTime);
            
            // Sync to GodEye database
            if (ownerUUID != null) {
                GodEyeIntegration.syncPlayerClaims(ownerUUID, serverLevel);
            }
        }
    }

    public boolean isExpired() {
        return expirationTime > 0 && System.currentTimeMillis() > expirationTime;
    }

    public boolean isRented() {
        return isRented;
    }

    public void setRented(boolean rented, int days, double cost) {
        this.isRented = rented;
        this.rentalDays = days;
        this.rentalCost = cost;
        if (rented && days > 0) {
            this.expirationTime = System.currentTimeMillis() + (days * 24L * 60L * 60L * 1000L);
        }
        setChanged();
    }

    public int getRentalDays() {
        return rentalDays;
    }

    public double getRentalCost() {
        return rentalCost;
    }

    public List<BlockPos> getMergedClaims() {
        return mergedClaims;
    }

    public void addMergedClaim(BlockPos pos) {
        if (mergedClaims.size() < 3) { // Max 3 additional claims (4 total)
            mergedClaims.add(pos);
            setChanged();
        }
    }

    public void removeMergedClaim(BlockPos pos) {
        mergedClaims.remove(pos);
        setChanged();
    }

    // Removed getExpansionLevel() and setExpansionLevel() methods - expansion level no longer stored per claim
    
    public boolean getAllowExplosions() {
        return allowExplosions;
    }

    public void setAllowExplosions(boolean allow) {
        this.allowExplosions = allow;
        setChanged();
    }

    public boolean getAllowPvP() {
        return allowPvP;
    }

    public void setAllowPvP(boolean allow) {
        this.allowPvP = allow;
        setChanged();
    }

    public boolean getAllowMobSpawning() {
        return allowMobSpawning;
    }

    public void setAllowMobSpawning(boolean allow) {
        this.allowMobSpawning = allow;
        setChanged();
    }

    public boolean getAllowFireSpread() {
        return allowFireSpread;
    }

    public void setAllowFireSpread(boolean allow) {
        this.allowFireSpread = allow;
        setChanged();
    }

    public int getProtectionRadius() {
        // Calculate from claim count instead of stored expansion level
        if (level instanceof ServerLevel serverLevel && ownerUUID != null) {
            // Try to get current claim count from registry first
            int currentClaimCount = ClaimRegistry.getPlayerClaimCount(ownerUUID);

            // If registry is empty (server restart), use persistent count
            if (currentClaimCount == 0) {
                currentClaimCount = ClaimRegistry.getPlayerClaimCountPersistent(ownerUUID, serverLevel);
            }
            
            // If still no count, fall back to stored count from when this claim was placed
            int effectiveClaimCount = currentClaimCount > 0 ? currentClaimCount : claimCountWhenPlaced;

            return 8 + (effectiveClaimCount * 8); // 8, 16, 24, 32 block radius based on total claims
        }
        return 8; // Default to base radius if no owner or level
    }
    
    /**
     * Process potato payment to extend claim time
     * 1 potato = 1 hour of additional time
     * Also syncs time to all merged claims and adjacent claims of the same owner
     */
    private void processPotatoPayment(int slot) {
        // Only process on server side
        if (level == null || level.isClientSide) {
            return;
        }
        
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty() || !stack.is(net.minecraft.world.item.Items.POTATO)) {
            return;
        }
        
        // Calculate time to add (1 potato = 1 hour in milliseconds)
        int potatoCount = stack.getCount();
        long hoursInMillis = potatoCount * 60L * 60L * 1000L; // hours to milliseconds
        
        // If claim doesn't have expiration time yet, start from current time
        long newExpirationTime;
        if (expirationTime <= 0 || expirationTime < System.currentTimeMillis()) {
            newExpirationTime = System.currentTimeMillis() + hoursInMillis;
            isRented = true;
        } else {
            // Add time to existing expiration
            newExpirationTime = expirationTime + hoursInMillis;
        }
        
        // Use setExpirationTime to update persistent storage and sync to GodEye database
        setExpirationTime(newExpirationTime);
        
        // Track total potatoes paid
        totalPotatoesPaid += potatoCount;
        
        // Log the payment
        TharidiaThings.LOGGER.info("Claim at {} received {} potatoes, added {} hours. New expiration: {}. Total paid: {}", 
            worldPosition, potatoCount, potatoCount, new java.util.Date(expirationTime), totalPotatoesPaid);
        
        // Notify linked realm about potato payment
        if (linkedRealmPos != null) {
            notifyRealmOfPayment((ServerLevel) level, potatoCount);
        }
        
        // Sync time to all connected claims (merged + adjacent)
        syncTimeToConnectedClaims((ServerLevel) level, expirationTime);
        
        // Consume the potatoes
        inventory.setStackInSlot(slot, ItemStack.EMPTY);
        
        setChanged();
        
        // Sync to client
        if (level instanceof ServerLevel) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Syncs expiration time to all connected claims (merged + adjacent same-owner claims)
     */
    private void syncTimeToConnectedClaims(ServerLevel level, long newExpirationTime) {
        if (ownerUUID == null) {
            return;
        }
        
        List<ClaimBlockEntity> connectedClaims = new ArrayList<>();
        
        // Add explicitly merged claims
        for (BlockPos mergedPos : mergedClaims) {
            BlockEntity be = level.getBlockEntity(mergedPos);
            if (be instanceof ClaimBlockEntity mergedClaim && ownerUUID.equals(mergedClaim.getOwnerUUID())) {
                connectedClaims.add(mergedClaim);
            }
        }
        
        // Find adjacent claims (claims in neighboring chunks with same owner)
        connectedClaims.addAll(findAdjacentClaims(level));
        
        // Update expiration time for all connected claims
        for (ClaimBlockEntity claim : connectedClaims) {
            claim.isRented = true;
            // Use setExpirationTime to update persistent storage and sync to GodEye database
            claim.setExpirationTime(newExpirationTime);
            level.sendBlockUpdated(claim.getBlockPos(), claim.getBlockState(), claim.getBlockState(), 3);
            
            TharidiaThings.LOGGER.info("Synced time to connected claim at {}", claim.getBlockPos());
        }
    }
    
    /**
     * Finds all adjacent claims (in neighboring chunks) owned by the same player
     */
    private List<ClaimBlockEntity> findAdjacentClaims(ServerLevel level) {
        List<ClaimBlockEntity> adjacentClaims = new ArrayList<>();
        
        if (ownerUUID == null) {
            return adjacentClaims;
        }
        
        int chunkX = worldPosition.getX() >> 4;
        int chunkZ = worldPosition.getZ() >> 4;
        
        // Check all 8 adjacent chunks plus diagonals
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue; // Skip current chunk
                
                int adjacentChunkX = chunkX + dx;
                int adjacentChunkZ = chunkZ + dz;
                
                if (level.hasChunk(adjacentChunkX, adjacentChunkZ)) {
                    net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(adjacentChunkX, adjacentChunkZ);
                    
                    // Check all block entities in the adjacent chunk
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be instanceof ClaimBlockEntity adjacentClaim) {
                            // Only include claims with the same owner
                            if (ownerUUID.equals(adjacentClaim.getOwnerUUID()) && 
                                !adjacentClaim.getBlockPos().equals(worldPosition)) {
                                adjacentClaims.add(adjacentClaim);
                            }
                        }
                    }
                }
            }
        }
        
        return adjacentClaims;
    }
    
    /**
     * Notifies the linked realm about potato payment
     */
    private void notifyRealmOfPayment(ServerLevel level, int potatoCount) {
        if (linkedRealmPos == null) {
            return;
        }
        
        BlockEntity be = level.getBlockEntity(linkedRealmPos);
        if (be instanceof PietroBlockEntity pietroBlock) {
            pietroBlock.addClaimPotatoPayment(potatoCount);
            TharidiaThings.LOGGER.info("Notified realm at {} of {} potato payment from claim at {}", 
                linkedRealmPos, potatoCount, worldPosition);
        }
    }
    
    /**
     * Attempts to find and link to a nearby Pietro block (realm)
     * Called when claim is placed
     * Uses optimized RealmManager instead of brute-force block scanning
     */
    public void findAndLinkToRealm(ServerLevel level) {
        // Use RealmManager for optimized lookup - O(n) where n = number of realms, not blocks
        PietroBlockEntity realm =
            RealmManager.getRealmAt(level, worldPosition);
        
        if (realm != null) {
            linkedRealmPos = realm.getBlockPos();
            setChanged();
            TharidiaThings.LOGGER.info("Claim at {} linked to realm at {} (owner: {})", 
                worldPosition, linkedRealmPos, realm.getOwnerName());
            
            // Update the realm's player hierarchy
            realm.updatePlayerHierarchy();
        } else {
            TharidiaThings.LOGGER.info("Claim at {} is not within any realm boundaries", worldPosition);
        }
    }
    
    public BlockPos getLinkedRealmPos() {
        return linkedRealmPos;
    }
    
    public void setLinkedRealmPos(BlockPos pos) {
        this.linkedRealmPos = pos;
        setChanged();
    }
    
    public int getTotalPotatoesPaid() {
        return totalPotatoesPaid;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        tag.putString("OwnerName", ownerName);
        tag.put("Inventory", inventory.serializeNBT(registries));
        
        // Save trusted players
        CompoundTag trustedTag = new CompoundTag();
        int i = 0;
        for (UUID uuid : trustedPlayers) {
            trustedTag.putUUID("Player" + i, uuid);
            i++;
        }
        trustedTag.putInt("Count", i);
        tag.put("TrustedPlayers", trustedTag);
        
        // Save claim properties
        tag.putString("ClaimName", claimName);
        tag.putLong("CreationTime", creationTime);
        tag.putLong("ExpirationTime", expirationTime);
        tag.putBoolean("IsRented", isRented);
        tag.putInt("RentalDays", rentalDays);
        tag.putDouble("RentalCost", rentalCost);
        // Removed expansionLevel from save - no longer stored per claim
        tag.putInt("ClaimCountWhenPlaced", claimCountWhenPlaced);
        tag.putInt("TotalPotatoesPaid", totalPotatoesPaid);
        if (linkedRealmPos != null) {
            tag.putLong("LinkedRealmPos", linkedRealmPos.asLong());
        }
        
        // Save flags
        tag.putBoolean("AllowExplosions", allowExplosions);
        tag.putBoolean("AllowPvP", allowPvP);
        tag.putBoolean("AllowMobSpawning", allowMobSpawning);
        tag.putBoolean("AllowFireSpread", allowFireSpread);
        
        // Save merged claims
        CompoundTag mergedTag = new CompoundTag();
        for (int j = 0; j < mergedClaims.size(); j++) {
            BlockPos pos = mergedClaims.get(j);
            mergedTag.putLong("Pos" + j, pos.asLong());
        }
        mergedTag.putInt("MergedCount", mergedClaims.size());
        tag.put("MergedClaims", mergedTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("OwnerName")) {
            ownerName = tag.getString("OwnerName");
        } else if (ownerUUID != null && level instanceof ServerLevel serverLevel) {
            // Migrate old claims without ownerName
            ownerName = PlayerNameHelper.getChosenNameByUUID(ownerUUID, serverLevel.getServer());
        }
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        
        // Load trusted players
        if (tag.contains("TrustedPlayers")) {
            CompoundTag trustedTag = tag.getCompound("TrustedPlayers");
            int count = trustedTag.getInt("Count");
            trustedPlayers.clear();
            for (int i = 0; i < count; i++) {
                if (trustedTag.hasUUID("Player" + i)) {
                    trustedPlayers.add(trustedTag.getUUID("Player" + i));
                }
            }
        }
        
        // Load claim properties
        claimName = tag.getString("ClaimName");
        creationTime = tag.getLong("CreationTime");
        expirationTime = tag.getLong("ExpirationTime");
        isRented = tag.getBoolean("IsRented");
        rentalDays = tag.getInt("RentalDays");
        rentalCost = tag.getDouble("RentalCost");
        claimCountWhenPlaced = tag.getInt("ClaimCountWhenPlaced");
        totalPotatoesPaid = tag.getInt("TotalPotatoesPaid");
        if (tag.contains("LinkedRealmPos")) {
            linkedRealmPos = BlockPos.of(tag.getLong("LinkedRealmPos"));
        }
        
        // Load flags
        allowExplosions = tag.getBoolean("AllowExplosions");
        allowPvP = tag.getBoolean("AllowPvP");
        allowMobSpawning = tag.getBoolean("AllowMobSpawning");
        allowFireSpread = tag.getBoolean("AllowFireSpread");
        
        // Load merged claims
        if (tag.contains("MergedClaims")) {
            CompoundTag mergedTag = tag.getCompound("MergedClaims");
            int count = mergedTag.getInt("MergedCount");
            mergedClaims.clear();
            for (int j = 0; j < count; j++) {
                long posLong = mergedTag.getLong("Pos" + j);
                mergedClaims.add(BlockPos.of(posLong));
            }
        }
        
        // Register in claim registry after loading
        if (level instanceof ServerLevel serverLevel && ownerUUID != null) {
            ClaimRegistry.registerClaim(serverLevel, worldPosition, this);
        }
    }
    
    @Override
    public void setRemoved() {
        super.setRemoved();
        
        // Unregister from claim registry
        if (level instanceof ServerLevel serverLevel) {
            ClaimRegistry.unregisterClaim(serverLevel, worldPosition);
            
            // Update linked realm's hierarchy when claim is removed
            // CRITICAL: Only do this if the chunk is already loaded to avoid deadlock during unload
            if (linkedRealmPos != null) {
                net.minecraft.world.level.ChunkPos realmChunkPos = new net.minecraft.world.level.ChunkPos(linkedRealmPos);
                
                // Check if the realm's chunk is loaded WITHOUT forcing chunk load
                if (serverLevel.hasChunk(realmChunkPos.x, realmChunkPos.z)) {
                    // Safe to access - chunk is already loaded
                    BlockEntity be = serverLevel.getBlockEntity(linkedRealmPos);
                    if (be instanceof PietroBlockEntity realm) {
                        realm.updatePlayerHierarchy();
                    }
                }
                // If chunk isn't loaded, skip the update - realm will recalculate on next access
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.tharidiathings.claim");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ClaimMenu(containerId, playerInventory, this);
    }
}
