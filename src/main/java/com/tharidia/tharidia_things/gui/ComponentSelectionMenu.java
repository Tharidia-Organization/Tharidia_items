package com.tharidia.tharidia_things.gui;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.block.entity.HotIronAnvilEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Menu for selecting which component to smith from the hot iron
 */
public class ComponentSelectionMenu extends AbstractContainerMenu {
    
    private final BlockPos pos;
    private final ContainerLevelAccess access;
    
    public ComponentSelectionMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }
    
    public ComponentSelectionMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(TharidiaThings.COMPONENT_SELECTION_MENU.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
    }
    
    public BlockPos getPos() {
        return pos;
    }
    
    public void selectComponent(String componentId) {
        access.execute((level, blockPos) -> {
            if (level.getBlockEntity(blockPos) instanceof HotIronAnvilEntity entity) {
                entity.setSelectedComponent(componentId);
            }
        });
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, TharidiaThings.HOT_IRON_MARKER.get());
    }
}
