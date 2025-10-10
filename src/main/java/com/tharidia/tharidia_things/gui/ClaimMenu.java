package com.tharidia.tharidia_things.gui;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.block.entity.ClaimBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ClaimMenu extends AbstractContainerMenu {
    private final ClaimBlockEntity blockEntity;
    private final BlockPos pos;
    private final ContainerData data;
    private final String ownerName; // Store owner name for client

    // Constructor for server-side
    public ClaimMenu(int containerId, Inventory playerInventory, ClaimBlockEntity blockEntity) {
        super(TharidiaThings.CLAIM_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.pos = blockEntity.getBlockPos();
        this.data = blockEntity.getContainerData();
        this.ownerName = blockEntity.getClaimName() != null ? blockEntity.getClaimName() : "Unknown's Claim";
        
        layoutSlots(playerInventory);
        addDataSlots(this.data);
    }

    // Constructor for client-side (receives data from server)
    public ClaimMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(TharidiaThings.CLAIM_MENU.get(), containerId);
        this.pos = extraData.readBlockPos();
        this.ownerName = extraData.readUtf(); // Read owner name from packet
        
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof ClaimBlockEntity claimEntity) {
            this.blockEntity = claimEntity;
            this.data = claimEntity.getContainerData();
        } else {
            this.blockEntity = null;
            this.data = new SimpleContainerData(4); // Match ClaimBlockEntity's 4 slots
        }
        
        layoutSlots(playerInventory);
        addDataSlots(this.data);
    }
    
    public long getExpirationTime() {
        // Data slots are ints, so we need to reconstruct the long from two ints
        long high = ((long) data.get(0)) << 32;
        long low = data.get(1) & 0xFFFFFFFFL;
        return high | low;
    }
    
    public boolean isRented() {
        return data.get(2) == 1;
    }
    
    public int getProtectionRadius() {
        return data.get(3);
    }
    
    public String getOwnerName() {
        return ownerName; // Use the synced owner name
    }
    
    private void layoutSlots(Inventory playerInventory) {
        // GUI is 250x300 pixels
        // Center slot horizontally: (250 / 2) - 9 = 116
        if (blockEntity != null) {
            this.addSlot(new SlotItemHandler(blockEntity.getInventory(), 0, 116, 35));
        }
        
        // Add player inventory slots at the bottom
        // Center the 9-slot width (162 pixels) horizontally: (250 - 162) / 2 = 44
        int invStartX = 44;
        int invStartY = 184; // Positioned in lower area with margin
        
        // Player inventory (9-35)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, invStartX + col * 18, invStartY + row * 18));
            }
        }
        
        // Player hotbar (0-8)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, invStartX + col * 18, invStartY + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            return false;
        }
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            
            // If shift-clicking from the claim slot (index 0)
            if (index == 0) {
                // Try to move to player inventory
                if (!this.moveItemStackTo(slotStack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // If shift-clicking from player inventory
            else {
                // Try to move to claim slot
                if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slot.onTake(player, slotStack);
        }
        
        return itemstack;
    }

    public ClaimBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public BlockPos getBlockPos() {
        return pos;
    }
}
