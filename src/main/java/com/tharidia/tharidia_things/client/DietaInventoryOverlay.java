package com.tharidia.tharidia_things.client;

import com.tharidia.tharidia_things.client.gui.DietaScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Renders the dieta button on the player inventory screen
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class DietaInventoryOverlay {
    
    // Button dimensions and position
    private static final int BUTTON_WIDTH = 10;
    private static final int BUTTON_HEIGHT = 10;
    private static final int BUTTON_X_OFFSET = 25;
    private static final int BUTTON_Y_OFFSET = 69; // In player render area
    
    /**
     * Renders the dieta button on inventory screens
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
        
        GuiGraphics guiGraphics = event.getGuiGraphics();
        InventoryScreen screen = (InventoryScreen) event.getScreen();
        
        // Calculate position based on inventory screen position
        int guiLeft = (screen.width - 176) / 2; // Standard inventory width is 176
        int guiTop = (screen.height - 166) / 2; // Standard inventory height is 166
        
        // Position the button in player render area
        int buttonX = guiLeft + BUTTON_X_OFFSET;
        int buttonY = guiTop + BUTTON_Y_OFFSET;
        
        // Draw the dieta button
        renderDietaButton(guiGraphics, buttonX, buttonY);
    }
    
    /**
     * Renders the dieta button
     */
    private static void renderDietaButton(GuiGraphics gui, int x, int y) {
        // Use vanilla button colors
        // Background
        gui.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, 0xFF808080);
        
        // Border
        gui.renderOutline(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 0xFFC0C0C0);
        
        // Inner border for depth
        gui.fill(x + 1, y + 1, x + BUTTON_WIDTH - 1, y + BUTTON_HEIGHT - 1, 0xFF606060);
        
        // Render chicken item icon (scaled down to fit button)
        gui.pose().pushPose();
        gui.pose().translate(x + 1, y + 1, 0);
        gui.pose().scale(0.5f, 0.5f, 1.0f);
        gui.renderItem(new ItemStack(Items.COOKED_CHICKEN), 0, 0);
        gui.pose().popPose();
    }
    
    /**
     * Handles mouse clicks to detect dieta button clicks
     */
    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        // Only handle on player inventory screen
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }
        
        // Check if it's a left click
        if (event.getButton() != 0) {
            return;
        }
        
        InventoryScreen screen = (InventoryScreen) event.getScreen();
        
        // Calculate position based on inventory screen position
        int guiLeft = (screen.width - 176) / 2;
        int guiTop = (screen.height - 166) / 2;
        
        // Button position
        int buttonX = guiLeft + BUTTON_X_OFFSET;
        int buttonY = guiTop + BUTTON_Y_OFFSET;
        
        // Check if click is within button bounds
        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();
        
        if (mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH &&
            mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT) {
            
            // Open dieta screen
            Minecraft.getInstance().setScreen(new DietaScreen(screen));
            event.setCanceled(true); // Prevent the click from propagating
        }
    }
}
