package com.tharidia.tharidia_things.client;

import com.tharidia.tharidia_things.client.gui.TradeRequestScreen;
import com.tharidia.tharidia_things.gui.TradeMenu;
import com.tharidia.tharidia_things.network.TradeCompletePacket;
import com.tharidia.tharidia_things.network.TradeRequestPacket;
import com.tharidia.tharidia_things.network.TradeSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Handles client-side trade packet processing
 */
public class TradeClientHandler {

    public static void handleTradeRequest(TradeRequestPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            minecraft.setScreen(new TradeRequestScreen(packet.requesterId(), packet.requesterName()));
        });
    }

    public static void handleTradeComplete(TradeCompletePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            // Reset camera
            TradeCameraHandler.resetCamera();
            
            // Close any open trade screen
            if (minecraft.screen != null) {
                minecraft.setScreen(null);
            }
        });
    }
    
    public static void handleTradeSync(TradeSyncPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null) {
                AbstractContainerMenu menu = minecraft.player.containerMenu;
                if (menu instanceof TradeMenu tradeMenu) {
                    // Update other player's confirmation status
                    tradeMenu.setOtherPlayerConfirmed(packet.otherPlayerConfirmed());
                    tradeMenu.setOtherPlayerFinalConfirmed(packet.otherPlayerFinalConfirmed());
                    
                    // Update tax info
                    tradeMenu.setTaxInfo(packet.taxRate(), packet.taxAmount());
                    
                    // Update other player's items
                    for (int i = 0; i < packet.otherPlayerItems().size() && i < 6; i++) {
                        ItemStack stack = packet.otherPlayerItems().get(i);
                        tradeMenu.getOtherPlayerOffer().setItem(i, stack.copy());
                    }
                    
                    // Clear remaining slots if fewer items
                    for (int i = packet.otherPlayerItems().size(); i < 6; i++) {
                        tradeMenu.getOtherPlayerOffer().setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        });
    }
}
