package com.THproject.tharidia_things.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Renders fatigue warning messages and HUD overlays
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class FatigueHudOverlay {
    
    private static String currentWarningMessage = null;
    private static long warningDisplayTime = 0;
    private static final long WARNING_DURATION = 5000; // Display for 5 seconds
    
    /**
     * Shows a fatigue warning message on screen
     */
    public static void showWarning(int minutesLeft) {
        if (minutesLeft == 5) {
            currentWarningMessage = "§eInizi a sentirti stanco";
        } else if (minutesLeft == 1) {
            currentWarningMessage = "§c§lSei MOLTO stanco!";
        }
        warningDisplayTime = System.currentTimeMillis();
    }
    
    /**
     * Renders the fatigue warning message on screen (center, not in chat)
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null) {
            return;
        }
        
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // Render warning message if active
        if (currentWarningMessage != null) {
            long elapsed = System.currentTimeMillis() - warningDisplayTime;
            
            if (elapsed < WARNING_DURATION) {
                // Calculate fade effect
                float alpha = 1.0f;
                if (elapsed > WARNING_DURATION - 1000) {
                    alpha = (WARNING_DURATION - elapsed) / 1000.0f;
                } else if (elapsed < 500) {
                    alpha = elapsed / 500.0f;
                }
                
                // Position at 1/3 from top of screen
                int yPos = screenHeight / 3;
                
                // Draw shadow for better visibility
                Component text = Component.literal(currentWarningMessage);
                int textWidth = mc.font.width(text);
                int xPos = (screenWidth - textWidth) / 2;
                
                // Apply alpha to color
                int color = 0xFFFFFF | ((int)(alpha * 255) << 24);
                
                // Shadow
                guiGraphics.drawString(mc.font, text, xPos + 2, yPos + 2, 0x000000 | ((int)(alpha * 128) << 24), false);
                // Main text
                guiGraphics.drawString(mc.font, text, xPos, yPos, color, false);
            } else {
                currentWarningMessage = null;
            }
        }
    }
}
