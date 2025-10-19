package com.tharidia.tharidia_things.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tharidia.tharidia_things.fatigue.FatigueAttachments;
import com.tharidia.tharidia_things.fatigue.FatigueData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Renders a vertical fatigue bar on the left side of the inventory screen
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class FatigueInventoryOverlay {
    
    // Bar dimensions
    private static final int BAR_WIDTH = 3;
    private static final int BAR_HEIGHT = 100;
    private static final int BAR_X_OFFSET = 5; // Pixels from left edge of screen
    
    /**
     * Renders the fatigue bar on inventory screens
     */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        // Only render on player inventory screen
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null) {
            return;
        }
        
        FatigueData data = player.getData(FatigueAttachments.FATIGUE_DATA);
        float fatiguePercentage = data.getFatiguePercentage();
        
        GuiGraphics guiGraphics = event.getGuiGraphics();
        InventoryScreen screen = (InventoryScreen) event.getScreen();
        
        // Calculate position based on inventory screen position
        int guiLeft = (screen.width - 176) / 2; // Standard inventory width is 176
        int guiTop = (screen.height - 166) / 2; // Standard inventory height is 166
        
        // Position on the left side, vertically centered with the inventory
        int barX = guiLeft - BAR_X_OFFSET - BAR_WIDTH;
        int barY = guiTop + (166 - BAR_HEIGHT) / 2;
        
        // Draw the fatigue bar
        renderFatigueBar(guiGraphics, barX, barY, fatiguePercentage, data);
    }
    
    /**
     * Renders the vertical fatigue bar
     */
    private static void renderFatigueBar(GuiGraphics guiGraphics, int x, int y, float percentage, FatigueData data) {
        // Draw background (dark gray)
        guiGraphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xFF000000);
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF3C3C3C);
        
        // Calculate filled height (bottom to top)
        int filledHeight = (int) (BAR_HEIGHT * percentage);
        int emptyHeight = BAR_HEIGHT - filledHeight;
        
        // Determine color based on percentage
        int color;
        if (percentage >= 0.75f) {
            color = 0xFF00FF00; // Green
        } else if (percentage >= 0.50f) {
            color = 0xFFFFFF00; // Yellow
        } else if (percentage >= 0.25f) {
            color = 0xFFFFA500; // Orange
        } else if (percentage > 0.0f) {
            color = 0xFFFF4500; // Red-Orange
        } else {
            color = 0xFFFF0000; // Red (exhausted)
        }
        
        // Draw filled portion (from bottom)
        if (filledHeight > 0) {
            guiGraphics.fill(x, y + emptyHeight, x + BAR_WIDTH, y + BAR_HEIGHT, color);
        }
        
        // Add pulsing effect when low on fatigue
        if (percentage < 0.15f && percentage > 0.0f) {
            long time = System.currentTimeMillis();
            float pulse = (float) (Math.sin(time / 200.0) + 1.0) / 2.0f; // Oscillates between 0 and 1
            int alpha = (int) (pulse * 128); // Pulse alpha between 0 and 128
            int pulseColor = (color & 0x00FFFFFF) | (alpha << 24);
            
            // Draw pulsing overlay
            guiGraphics.fill(x, y + emptyHeight, x + BAR_WIDTH, y + BAR_HEIGHT, pulseColor);
        }
        
        // Draw segmentation lines (every 10% for easier reading)
        for (int i = 1; i < 10; i++) {
            int segY = y + (BAR_HEIGHT * i / 10);
            guiGraphics.fill(x, segY, x + BAR_WIDTH, segY + 1, 0xFF000000);
        }
    }
    
    /**
     * Renders tooltip when hovering over the bar
     */
    @SubscribeEvent
    public static void onScreenMouseMove(ScreenEvent.MouseDragged.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null) {
            return;
        }
        
        InventoryScreen screen = (InventoryScreen) event.getScreen();
        int guiLeft = (screen.width - 176) / 2;
        int guiTop = (screen.height - 166) / 2;
        
        int barX = guiLeft - BAR_X_OFFSET - BAR_WIDTH;
        int barY = guiTop + (166 - BAR_HEIGHT) / 2;
        
        // Check if mouse is over the bar
        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();
        
        if (mouseX >= barX - 5 && mouseX <= barX + BAR_WIDTH + 5 &&
            mouseY >= barY && mouseY <= barY + BAR_HEIGHT) {
            
            FatigueData data = player.getData(FatigueAttachments.FATIGUE_DATA);
            int minutes = data.getFatigueTicks() / (60 * 20);
            int seconds = (data.getFatigueTicks() % (60 * 20)) / 20;
            float percentage = data.getFatiguePercentage();
            
            // Prepare tooltip (will be rendered in the next render event)
            // This is a simplified version - full tooltip rendering would need more work
        }
    }
    
    /**
     * Renders detailed tooltip for the fatigue bar
     */
    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null) {
            return;
        }
        
        InventoryScreen screen = (InventoryScreen) event.getScreen();
        int guiLeft = (screen.width - 176) / 2;
        int guiTop = (screen.height - 166) / 2;
        
        int barX = guiLeft - BAR_X_OFFSET - BAR_WIDTH;
        int barY = guiTop + (166 - BAR_HEIGHT) / 2;
        
        // Get mouse position
        double mouseX = mc.mouseHandler.xpos() * screen.width / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * screen.height / mc.getWindow().getScreenHeight();
        
        // Check if mouse is over the bar
        if (mouseX >= barX - 5 && mouseX <= barX + BAR_WIDTH + 5 &&
            mouseY >= barY && mouseY <= barY + BAR_HEIGHT) {
            
            FatigueData data = player.getData(FatigueAttachments.FATIGUE_DATA);
            int minutes = data.getFatigueTicks() / (60 * 20);
            int seconds = (data.getFatigueTicks() % (60 * 20)) / 20;
            float percentage = data.getFatiguePercentage();
            
            // Render tooltip
            GuiGraphics guiGraphics = event.getGuiGraphics();
            
            String status;
            if (data.isExhausted()) {
                status = "§c§lESAUSTO";
            } else if (percentage < 0.15f) {
                status = "§c§lCRITICO";
            } else if (percentage < 0.50f) {
                status = "§6Stanco";
            } else {
                status = "§aEnergetico";
            }
            
            java.util.List<Component> tooltipLines = java.util.List.of(
                Component.literal("§6§lStanchezza"),
                Component.literal("§7Energia: §f" + (int)(percentage * 100) + "%"),
                Component.literal("§7Tempo: §f" + minutes + "m " + seconds + "s"),
                Component.literal("§7Stato: " + status)
            );
            
            guiGraphics.renderComponentTooltip(
                mc.font,
                tooltipLines,
                (int) mouseX,
                (int) mouseY
            );
        }
    }
}
