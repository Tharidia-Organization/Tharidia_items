package com.THproject.tharidia_things.gui;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.ClaimBlockEntity;
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
import com.THproject.tharidia_things.gui.inventory.PlayerInventoryPanelLayout;

public class ClaimMenu extends AbstractContainerMenu {
    private final ClaimBlockEntity blockEntity;
    private final BlockPos pos;
    private final ContainerData data;
    private final String ownerName; // Store owner name for client
    private static final int GUI_WIDTH = 280; // Keep in sync with ClaimScreen

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
        // GUI is 280x320 pixels (matching ClaimScreen)
        // Center slot horizontally: (GUI_WIDTH / 2) - 9 (slot is 18px wide)
        if (blockEntity != null) {
            int claimSlotX = GUI_WIDTH / 2 - 9 + 2; // slight right shift for visual alignment
            this.addSlot(new SlotItemHandler(blockEntity.getInventory(), 0, claimSlotX, 32));
        }
        
        // Align player inventory with shared left-side panel
        int invStartX = PlayerInventoryPanelLayout.SLOT_OFFSET_X;
        int invStartY = PlayerInventoryPanelLayout.SLOT_OFFSET_Y;
        
        // Player inventory (9-35)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, invStartX + col * 18, invStartY + row * 18));
            }
        }
        
        // Player hotbar (0-8)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, invStartX + col * 18, invStartY + 78));
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
