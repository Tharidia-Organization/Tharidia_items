package com.tharidia.tharidia_things.gui;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.block.entity.PietroBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class PietroMenu extends AbstractContainerMenu {
    private final PietroBlockEntity blockEntity;
    private final BlockPos pos;
    private final ContainerData data;

    // Constructor for server-side
    public PietroMenu(int containerId, Inventory playerInventory, PietroBlockEntity blockEntity) {
        super(TharidiaThings.PIETRO_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.pos = blockEntity.getBlockPos();
        this.data = blockEntity.getContainerData();
        
        layoutSlots(playerInventory);
        addDataSlots(this.data);
    }

    // Constructor for client-side (receives data from server)
    public PietroMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(TharidiaThings.PIETRO_MENU.get(), containerId);
        this.pos = extraData.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof PietroBlockEntity pietroEntity) {
            this.blockEntity = pietroEntity;
            this.data = pietroEntity.getContainerData();
        } else {
            this.blockEntity = null;
            this.data = new SimpleContainerData(2);
        }
        
        layoutSlots(playerInventory);
        addDataSlots(this.data);
    }
    
    public int getRealmSize() {
        return data.get(0);
    }
    
    public int getStoredPotatoes() {
        return data.get(1);
    }
    
    private void layoutSlots(Inventory playerInventory) {
        // GUI is 250x300 pixels
        // Potato slot positioned next to info text (right side)
        if (blockEntity != null) {
            this.addSlot(new SlotItemHandler(blockEntity.getPotatoInventory(), 0, 200, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(Items.POTATO);
                }
            });
        }
        
        // Add player inventory slots at the bottom
        int invStartX = 44;
        int invStartY = 184;
        
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
            
            // If shift-clicking from the pietro slot (index 0)
            if (index == 0) {
                // Try to move to player inventory
                if (!this.moveItemStackTo(slotStack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // If shift-clicking from player inventory
            else {
                // Only move potatoes to pietro slot
                if (slotStack.is(Items.POTATO)) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
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

    public PietroBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public BlockPos getBlockPos() {
        return pos;
    }
}
