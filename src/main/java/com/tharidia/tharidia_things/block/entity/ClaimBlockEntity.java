package com.tharidia.tharidia_things.block.entity;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.claim.ClaimRegistry;
import com.tharidia.tharidia_things.gui.ClaimMenu;
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
            this.claimCountWhenPlaced = com.tharidia.tharidia_things.claim.ClaimRegistry.getPlayerClaimCountPersistent(ownerUUID, serverLevel);
        }

        // Automatically set claim name to owner's name
        if (level instanceof ServerLevel serverLevel) {
            net.minecraft.server.level.ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
            if (player != null) {
                this.claimName = player.getName().getString() + "'s Claim";
            }
        }

        setChanged();

        // Register in claim registry
        if (level instanceof ServerLevel serverLevel) {
            ClaimRegistry.registerClaim(serverLevel, worldPosition, this);
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
            int currentClaimCount = com.tharidia.tharidia_things.claim.ClaimRegistry.getPlayerClaimCount(ownerUUID);

            // If registry is empty (server restart), use persistent count
            if (currentClaimCount == 0) {
                currentClaimCount = com.tharidia.tharidia_things.claim.ClaimRegistry.getPlayerClaimCountPersistent(ownerUUID, serverLevel);
            }
            
            // If still no count, fall back to stored count from when this claim was placed
            int effectiveClaimCount = currentClaimCount > 0 ? currentClaimCount : claimCountWhenPlaced;

            return 8 + (effectiveClaimCount * 8); // 8, 16, 24, 32 block radius based on total claims
        }
        return 8; // Default to base radius if no owner or level
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
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
