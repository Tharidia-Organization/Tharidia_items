package com.tharidia.tharidia_things.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Modal screen for dieta that overlays the inventory
 */
public class DietaScreen extends Screen {
    private final InventoryScreen parentScreen;
    
    // Overlay dimensions
    private static final int OVERLAY_WIDTH = 176;
    private static final int OVERLAY_HEIGHT = 83;
    
    public DietaScreen(InventoryScreen parentScreen) {
        super(Component.literal("Dieta"));
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        // Initialize parent screen with same dimensions
        if (parentScreen != null) {
            parentScreen.init(this.minecraft, this.width, this.height);
        }
    }
    
    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // Calculate positions
        int guiLeft = (this.width - 176) / 2;
        int guiTop = (this.height - 166) / 2;
        
        // Render the parent inventory screen in the background
        // Pass fake mouse position to prevent hover effects in overlay area
        if (parentScreen != null) {
            int renderMouseX = mouseX;
            int renderMouseY = mouseY;
            
            // If mouse is in overlay area, move it outside to prevent hover effects
            if (mouseX >= guiLeft && mouseX <= guiLeft + OVERLAY_WIDTH &&
                mouseY >= guiTop && mouseY <= guiTop + OVERLAY_HEIGHT) {
                renderMouseX = -1;
                renderMouseY = -1;
            }
            
            parentScreen.render(gui, renderMouseX, renderMouseY, partialTick);
        }
        
        // Draw overlay AFTER parent screen renders
        // Use pose stack to ensure we render on top
        gui.pose().pushPose();
        gui.pose().translate(0, 0, 400); // Move to front
        
        // Draw completely opaque background to cover everything underneath
        gui.fill(guiLeft, guiTop, guiLeft + OVERLAY_WIDTH, guiTop + OVERLAY_HEIGHT, 0xFFC6C6C6);
        
        // Draw 3D border effect like vanilla GUI
        // Top and left highlight
        gui.fill(guiLeft, guiTop, guiLeft + OVERLAY_WIDTH, guiTop + 1, 0xFFFFFFFF);
        gui.fill(guiLeft, guiTop, guiLeft + 1, guiTop + OVERLAY_HEIGHT, 0xFFFFFFFF);
        // Bottom and right shadow
        gui.fill(guiLeft, guiTop + OVERLAY_HEIGHT - 1, guiLeft + OVERLAY_WIDTH, guiTop + OVERLAY_HEIGHT, 0xFF555555);
        gui.fill(guiLeft + OVERLAY_WIDTH - 1, guiTop, guiLeft + OVERLAY_WIDTH, guiTop + OVERLAY_HEIGHT, 0xFF555555);
        
        // Draw the chicken leg button with item icon
        int buttonX = guiLeft + 25;
        int buttonY = guiTop + 69;
        int buttonWidth = 10;
        int buttonHeight = 10;
        
        // Button background
        gui.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, 0xFF808080);
        gui.renderOutline(buttonX, buttonY, buttonWidth, buttonHeight, 0xFFC0C0C0);
        gui.fill(buttonX + 1, buttonY + 1, buttonX + buttonWidth - 1, buttonY + buttonHeight - 1, 0xFF606060);
        
        // Render chicken item icon (scaled down to fit button)
        gui.pose().pushPose();
        gui.pose().translate(buttonX + 1, buttonY + 1, 0);
        gui.pose().scale(0.5f, 0.5f, 1.0f);
        gui.renderItem(new ItemStack(Items.COOKED_CHICKEN), 0, 0);
        gui.pose().popPose();
        
        // Draw 5 horizontal bars for food categories using fatigue bar style
        int barHeight = 3;
        int barSpacing = 11; // Increased by 1 pixel
        int barWidth = 80;
        int labelX = guiLeft + 8;
        int barX = guiLeft + 55;
        int startY = guiTop + 6;
        
        // Bar colors for each category
        int grainColor = 0xFFDAA520;     // Gold
        int proteinColor = 0xFFCD5C5C;   // Indian Red
        int vegetableColor = 0xFF228B22; // Forest Green
        int fruitColor = 0xFFFF6347;     // Tomato
        int sugarColor = 0xFFFFB6C1;     // Light Pink
        
        // All bars at 0% by default
        float grainPercent = 0.0f;
        float proteinPercent = 0.0f;
        float vegetablePercent = 0.0f;
        float fruitPercent = 0.0f;
        float sugarPercent = 0.0f;
        
        // Render each bar with percentage
        renderDietBar(gui, "Grain", labelX, barX, startY, barWidth, barHeight, grainPercent, grainColor);
        renderDietBar(gui, "Protein", labelX, barX, startY + barSpacing, barWidth, barHeight, proteinPercent, proteinColor);
        renderDietBar(gui, "Vegetable", labelX, barX, startY + barSpacing * 2, barWidth, barHeight, vegetablePercent, vegetableColor);
        renderDietBar(gui, "Fruit", labelX, barX, startY + barSpacing * 3, barWidth, barHeight, fruitPercent, fruitColor);
        renderDietBar(gui, "Sugar", labelX, barX, startY + barSpacing * 4, barWidth, barHeight, sugarPercent, sugarColor);
        
        gui.pose().popPose();
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int guiLeft = (this.width - 176) / 2;
        int guiTop = (this.height - 166) / 2;
        
        // Check if clicking the D button to close
        int buttonX = guiLeft + 25;
        int buttonY = guiTop + 69;
        int buttonWidth = 10;
        int buttonHeight = 10;
        
        if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
            mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
            // Close and return to inventory
            Minecraft.getInstance().setScreen(parentScreen);
            return true;
        }
        
        // Check if clicking outside the overlay area (in the slots area below)
        if (mouseX < guiLeft || mouseX > guiLeft + OVERLAY_WIDTH ||
            mouseY < guiTop || mouseY > guiTop + OVERLAY_HEIGHT) {
            
            // Close and return to inventory
            Minecraft.getInstance().setScreen(parentScreen);
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC to close
        if (keyCode == 256) { // ESC key
            Minecraft.getInstance().setScreen(parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    /**
     * Renders a horizontal diet bar in fatigue bar style
     */
    private void renderDietBar(GuiGraphics gui, String label, int labelX, int barX, int y, int barWidth, int barHeight, float percentage, int fillColor) {
        // Draw label with smaller font (scale down)
        gui.pose().pushPose();
        float scale = 0.7f; // Reduce font size by ~30% (approximately 3 pixels smaller)
        gui.pose().translate(labelX, y - 1, 0);
        gui.pose().scale(scale, scale, 1.0f);
        gui.drawString(this.font, label, 0, 0, 0xFF404040, false);
        gui.pose().popPose();
        
        // Draw bar background (black border + dark gray fill)
        gui.fill(barX - 1, y - 1, barX + barWidth + 1, y + barHeight + 1, 0xFF000000);
        gui.fill(barX, y, barX + barWidth, y + barHeight, 0xFF3C3C3C);
        
        // Calculate filled width
        int filledWidth = (int) (barWidth * Math.min(1.0f, Math.max(0.0f, percentage)));
        
        // Draw filled portion
        if (filledWidth > 0) {
            gui.fill(barX, y, barX + filledWidth, y + barHeight, fillColor);
        }
        
        // Draw segmentation lines (every 10%)
        for (int i = 1; i < 10; i++) {
            int segX = barX + (barWidth * i / 10);
            gui.fill(segX, y, segX + 1, y + barHeight, 0xFF000000);
        }
        
        // Draw percentage text on the right with smaller font
        gui.pose().pushPose();
        gui.pose().translate(barX + barWidth + 4, y - 1, 0);
        gui.pose().scale(scale, scale, 1.0f);
        String percentText = (int)(percentage * 100) + "%";
        gui.drawString(this.font, percentText, 0, 0, 0xFF404040, false);
        gui.pose().popPose();
    }
}
