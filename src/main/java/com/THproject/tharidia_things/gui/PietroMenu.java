package com.THproject.tharidia_things.gui;

import com.THproject.tharidia_things.network.HierarchySyncPacket;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
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
import com.THproject.tharidia_things.gui.inventory.PlayerInventoryPanelLayout;

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
        
        // Send hierarchy data to client when menu is opened
        if (!playerInventory.player.level().isClientSide && playerInventory.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            HierarchySyncPacket hierarchyPacket =
                HierarchySyncPacket.fromPietroBlock(blockEntity);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, hierarchyPacket);
        }
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
            this.data = new SimpleContainerData(3);
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
    
    public int getTotalClaimPotatoes() {
        return data.get(2);
    }
    
    private void layoutSlots(Inventory playerInventory) {
        // GUI is now 300x350 pixels (PARCHMENT_WIDTH x PARCHMENT_HEIGHT)
        // Potato slot centered horizontally at top
        if (blockEntity != null) {
            this.addSlot(new SlotItemHandler(blockEntity.getPotatoInventory(), 0, 141, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(Items.POTATO);
                }
            });
        }
        
        // Move player inventory to shared left panel location
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
