package com.tharidia.tharidia_things.gui; // You might want to move this to an 'inventory' package

import com.tharidia.tharidia_things.TharidiaThings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class BattleInviteMenu extends AbstractContainerMenu {
    public final Component inviterUUID;
    public final Component inviterName;

    /**
     * A new "base" constructor that does the real work.
     * We make it private or protected.
     */
    private BattleInviteMenu(int containerId, Inventory playerInventory, Component inviterUUID, Component inviterName) {
        super(TharidiaThings.BATTLE_INVITE_MENU.get(), containerId);
        this.inviterUUID = inviterUUID;
        this.inviterName = inviterName;
    }

    /**
     * SERVER-SIDE constructor.
     * This just calls the base constructor with a "default" name.
     */
    public BattleInviteMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, Component.literal("UNKNOWN"), Component.literal("UNKNOWN"));
    }

    /**
     * CLIENT-SIDE constructor.
     * This now reads the buffer *first*, then calls the base constructor.
     */
    public BattleInviteMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        // Read the name from the buffer FIRST
        this(containerId, playerInventory, Component.literal(buffer.readUtf()), Component.literal(buffer.readUtf()));
    }

    public Component getInviterUUID() {
        return inviterUUID;
    }

    public Component getInviterName() {
        return inviterName;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player arg0, int arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'quickMoveStack'");
    }
}