package com.tharidia.tharidia_things.client;

import com.tharidia.tharidia_things.client.gui.TradeRequestScreen;
import com.tharidia.tharidia_things.network.TradeCompletePacket;
import com.tharidia.tharidia_things.network.TradeRequestPacket;
import net.minecraft.client.Minecraft;

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
            // Close any open trade screen
            if (minecraft.screen != null) {
                minecraft.setScreen(null);
            }
        });
    }
}
