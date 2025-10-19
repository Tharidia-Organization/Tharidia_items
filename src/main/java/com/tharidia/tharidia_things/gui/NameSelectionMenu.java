package com.tharidia.tharidia_things.gui;

import com.tharidia.tharidia_things.TharidiaThings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the name selection GUI that opens on first join
 */
public class NameSelectionMenu extends AbstractContainerMenu {

    // Constructor for server-side
    public NameSelectionMenu(int containerId, Inventory playerInventory) {
        super(TharidiaThings.NAME_SELECTION_MENU.get(), containerId);
    }

    // Constructor for client-side (receives data from server)
    public NameSelectionMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(TharidiaThings.NAME_SELECTION_MENU.get(), containerId);
    }

    @Override
    public boolean stillValid(Player player) {
        // Always valid - player can't close this menu until they choose a name
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No slots in this menu
        return ItemStack.EMPTY;
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        
        // Check if player still needs to choose a name when closing the menu
        if (!player.level().isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            try {
                // Use reflection to check NameService (server-side only dependency)
                Class<?> nameServiceClass = Class.forName("com.tharidia.tharidia_tweaks.name.NameService");
                java.lang.reflect.Method needsToChooseNameMethod = nameServiceClass.getMethod("needsToChooseName", net.minecraft.server.level.ServerPlayer.class);
                
                boolean needsName = (boolean) needsToChooseNameMethod.invoke(null, serverPlayer);
                
                if (needsName) {
                    // Player tried to close the menu without choosing a name - reopen it
                    player.level().getServer().execute(() -> {
                        serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                            (id, inv, p) -> new NameSelectionMenu(id, inv),
                            net.minecraft.network.chat.Component.translatable("gui.tharidiathings.name_selection")
                        ));
                    });
                }
            } catch (Exception e) {
                TharidiaThings.LOGGER.error("Error checking name selection status", e);
            }
        }
    }
}
