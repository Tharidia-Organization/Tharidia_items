package com.THproject.tharidia_things.gui;

import com.THproject.tharidia_things.util.PlayerNameHelper;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.trade.TradeManager;
import com.THproject.tharidia_things.trade.TradeSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Medieval-themed trade menu with 6 slots per player
 */
public class TradeMenu extends AbstractContainerMenu {
    private final Container playerOffer;
    private final Container otherPlayerOffer;
    private final UUID sessionId;
    private final UUID otherPlayerId;
    private final String otherPlayerName;
    private boolean playerConfirmed;
    private boolean otherPlayerConfirmed;
    private boolean playerFinalConfirmed;
    private boolean otherPlayerFinalConfirmed;
    private double taxRate;
    private int taxAmount;
    private boolean tradeCompleted = false; // Flag to prevent item duplication

    // Server-side constructor
    public TradeMenu(int containerId, Inventory playerInventory, TradeSession session, Player player) {
        super(TharidiaThings.TRADE_MENU.get(), containerId);
        this.playerOffer = new SimpleContainer(6);
        this.otherPlayerOffer = new SimpleContainer(6);
        this.sessionId = session.getSessionId();
        
        UUID playerId = player.getUUID();
        Player otherPlayer = session.getOtherPlayer(playerId);
        this.otherPlayerId = otherPlayer.getUUID();
        // Use registered name from PlayerNameHelper
        if (otherPlayer instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            this.otherPlayerName = PlayerNameHelper.getChosenName(serverPlayer);
        } else {
            this.otherPlayerName = otherPlayer.getName().getString();
        }
        
        layoutSlots(playerInventory);
    }

    // Client-side constructor
    public TradeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(TharidiaThings.TRADE_MENU.get(), containerId);
        this.playerOffer = new SimpleContainer(6);
        this.otherPlayerOffer = new SimpleContainer(6);
        this.sessionId = extraData.readUUID();
        this.otherPlayerId = extraData.readUUID();
        this.otherPlayerName = extraData.readUtf();
        
        layoutSlots(playerInventory);
    }

    private void layoutSlots(Inventory playerInventory) {
        // GUI is 230x195 pixels
        
        // Player's offer slots (left side) - 6 slots in 2 rows of 3
        int playerOfferX = 35;
        int playerOfferY = 20;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                final int slotIndex = col + row * 3;
                this.addSlot(new Slot(playerOffer, slotIndex, playerOfferX + col * 18, playerOfferY + row * 18) {
                    @Override
                    public boolean mayPickup(Player player) {
                        // Cannot modify after confirming
                        return !playerConfirmed && !playerFinalConfirmed;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        // Cannot modify after confirming
                        return !playerConfirmed && !playerFinalConfirmed;
                    }
                    
                    @Override
                    public ItemStack remove(int amount) {
                        // Block removal after confirmation
                        if (playerConfirmed || playerFinalConfirmed) {
                            return ItemStack.EMPTY;
                        }
                        return super.remove(amount);
                    }
                    
                    @Override
                    public void set(ItemStack stack) {
                        // Block setting after confirmation
                        if (playerConfirmed || playerFinalConfirmed) {
                            return;
                        }
                        super.set(stack);
                    }
                });
            }
        }

        // Other player's offer slots (right side) - 6 slots in 2 rows of 3
        int otherOfferX = 145;
        int otherOfferY = 20;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(otherPlayerOffer, col + row * 3, otherOfferX + col * 18, otherOfferY + row * 18) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return false; // Cannot take items from other player's side
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // Cannot place items on other player's side
                    }
                });
            }
        }

        // Player inventory (bottom)
        int invStartX = 35;
        int invStartY = 103; // Alzato di 5 pixel (era 108)
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, invStartX + col * 18, invStartY + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, invStartX + col * 18, invStartY + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        TradeSession session = TradeManager.getPlayerSession(player.getUUID());
        return session != null && session.getSessionId().equals(sessionId);
    }
    
    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        // Block all interactions with trade slots after confirmation
        if (slotId >= 0 && slotId < 6) { // Player offer slots
            if (playerConfirmed || playerFinalConfirmed) {
                player.sendSystemMessage(Component.literal("§cNon puoi modificare gli item dopo la conferma!"));
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Block shift-click if player has confirmed
        if (playerConfirmed || playerFinalConfirmed) {
            player.sendSystemMessage(Component.literal("§cNon puoi modificare gli item dopo la conferma!"));
            return ItemStack.EMPTY;
        }
        
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            
            // If shift-clicking from player offer slots (0-5)
            if (index < 6) {
                if (!this.moveItemStackTo(slotStack, 12, 48, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // If shift-clicking from other player's slots (6-11) - not allowed
            else if (index < 12) {
                return ItemStack.EMPTY;
            }
            // If shift-clicking from player inventory (12-47)
            else {
                if (!this.moveItemStackTo(slotStack, 0, 6, false)) {
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        
        // Only return items if trade was NOT completed successfully
        // This prevents item duplication when trade completes
        if (!player.level().isClientSide && !tradeCompleted) {
            for (int i = 0; i < 6; i++) {
                ItemStack stack = playerOffer.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    if (!player.getInventory().add(stack)) {
                        // If inventory is full, drop at player's feet
                        player.drop(stack, false);
                    }
                }
            }
        }
    }

    public Container getPlayerOffer() {
        return playerOffer;
    }

    public Container getOtherPlayerOffer() {
        return otherPlayerOffer;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getOtherPlayerId() {
        return otherPlayerId;
    }

    public String getOtherPlayerName() {
        return otherPlayerName;
    }

    public void setPlayerConfirmed(boolean confirmed) {
        this.playerConfirmed = confirmed;
    }

    public void setOtherPlayerConfirmed(boolean confirmed) {
        this.otherPlayerConfirmed = confirmed;
    }

    public boolean isPlayerConfirmed() {
        return playerConfirmed;
    }

    public boolean isOtherPlayerConfirmed() {
        return otherPlayerConfirmed;
    }

    public void setPlayerFinalConfirmed(boolean confirmed) {
        this.playerFinalConfirmed = confirmed;
    }

    public void setOtherPlayerFinalConfirmed(boolean confirmed) {
        this.otherPlayerFinalConfirmed = confirmed;
    }

    public boolean isPlayerFinalConfirmed() {
        return playerFinalConfirmed;
    }

    public boolean isOtherPlayerFinalConfirmed() {
        return otherPlayerFinalConfirmed;
    }
    
    public void setTaxInfo(double taxRate, int taxAmount) {
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
    }
    
    public double getTaxRate() {
        return taxRate;
    }
    
    public int getTaxAmount() {
        return taxAmount;
    }
    
    public void setTradeCompleted(boolean completed) {
        this.tradeCompleted = completed;
    }
}
