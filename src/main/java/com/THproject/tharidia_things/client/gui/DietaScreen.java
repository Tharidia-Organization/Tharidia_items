package com.THproject.tharidia_things.client.gui;

import com.THproject.tharidia_things.client.DietaInventoryOverlay;
import com.THproject.tharidia_things.diet.DietAttachments;
import com.THproject.tharidia_things.diet.DietCategory;
import com.THproject.tharidia_things.diet.DietData;
import com.THproject.tharidia_things.diet.DietRegistry;
import com.THproject.tharidia_things.diet.DietProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Modal screen for dieta that overlays the inventory
 */
public class DietaScreen extends Screen {
    private final InventoryScreen parentScreen;
    
    // Overlay dimensions (slightly smaller than inventory area)
    private static final int OVERLAY_WIDTH = 174;
    private static final int OVERLAY_HEIGHT = 81;
    
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
        int guiLeft = (this.width - 174) / 2;
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
        // Top highlight (two-pixel thickness)
        gui.fill(guiLeft, guiTop, guiLeft + OVERLAY_WIDTH, guiTop + 1, 0xFFFFFFFF);
        gui.fill(guiLeft, guiTop + 1, guiLeft + OVERLAY_WIDTH, guiTop + 2, 0xFFFFFFFF);
        // Left highlight (two-pixel thickness)
        gui.fill(guiLeft, guiTop, guiLeft + 1, guiTop + OVERLAY_HEIGHT, 0xFFFFFFFF);
        gui.fill(guiLeft + 1, guiTop, guiLeft + 2, guiTop + OVERLAY_HEIGHT, 0xFFFFFFFF);

        // Bottom and right shadow with black outer line + two inner colored lines
        int shadowColor = 0xFF777777;
        int shadowBlack = 0xFF000000;
        
        // Bottom outer shadow
        gui.fill(guiLeft, guiTop + OVERLAY_HEIGHT, guiLeft + OVERLAY_WIDTH, guiTop + OVERLAY_HEIGHT + 1, shadowBlack);
        // Bottom inner shadow lines
        gui.fill(guiLeft, guiTop + OVERLAY_HEIGHT - 1, guiLeft + OVERLAY_WIDTH, guiTop + OVERLAY_HEIGHT, shadowColor);
        gui.fill(guiLeft, guiTop + OVERLAY_HEIGHT - 2, guiLeft + OVERLAY_WIDTH, guiTop + OVERLAY_HEIGHT - 1, shadowColor);
        
        // Right outer shadow
        gui.fill(guiLeft + OVERLAY_WIDTH, guiTop + 2, guiLeft + OVERLAY_WIDTH + 1, guiTop + OVERLAY_HEIGHT + 1, shadowBlack);
        // Right inner shadow lines
        gui.fill(guiLeft + OVERLAY_WIDTH - 1, guiTop, guiLeft + OVERLAY_WIDTH, guiTop + OVERLAY_HEIGHT, shadowColor);
        gui.fill(guiLeft + OVERLAY_WIDTH - 2, guiTop, guiLeft + OVERLAY_WIDTH - 1, guiTop + OVERLAY_HEIGHT, shadowColor);

        
        Player player = Minecraft.getInstance().player;
        DietData dietData = player != null ? player.getData(DietAttachments.DIET_DATA) : null;
        java.util.List<BarInfo> bars = buildBars(dietData);

        // Draw horizontal bars for food categories using fatigue bar style
        int barHeight = 3;
        int barWidth = 80;
        int labelX = guiLeft + 8;
        int barX = guiLeft + 55;
        int paddingTop = 8;
        int paddingBottom = 6;
        int barsStartY = guiTop + paddingTop;
        
        int barCount = bars.size();
        float usableHeight = (guiTop + OVERLAY_HEIGHT - paddingBottom - barHeight) - barsStartY;
        float spacing = barCount > 1 ? usableHeight / (barCount - 1) : 0.0f;
        
        Minecraft mcInstance = Minecraft.getInstance();
        double rawMouseX = mcInstance.mouseHandler.xpos() * this.width / mcInstance.getWindow().getScreenWidth();
        double rawMouseY = mcInstance.mouseHandler.ypos() * this.height / mcInstance.getWindow().getScreenHeight();
        boolean mouseDown = mcInstance.mouseHandler.isLeftPressed();
        DietaInventoryOverlay.renderFeatureButtons(gui, guiLeft, guiTop, rawMouseX, rawMouseY, mouseDown);
        
        for (int i = 0; i < barCount; i++) {
            int barY = Math.round(barsStartY + i * spacing);
            BarInfo bar = bars.get(i);
            renderDietBar(gui, bar.label(), labelX, barX, barY, barWidth, barHeight, bar.percentage(), bar.color());
        }
        
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

    private java.util.List<BarInfo> buildBars(DietData dietData) {
        java.util.List<BarInfo> bars = new java.util.ArrayList<>(DietCategory.COUNT);
        DietProfile maxProfile = DietRegistry.getMaxValues();

        addBar(bars, "Cereali", DietCategory.GRAIN, 0xFFDAA520, dietData, maxProfile);
        addBar(bars, "Proteine", DietCategory.PROTEIN, 0xFFCD5C5C, dietData, maxProfile);
        addBar(bars, "Verdure", DietCategory.VEGETABLE, 0xFF228B22, dietData, maxProfile);
        addBar(bars, "Frutta", DietCategory.FRUIT, 0xFFFF6347, dietData, maxProfile);
        addBar(bars, "Zuccheri", DietCategory.SUGAR, 0xFFFFB6C1, dietData, maxProfile);
        addBar(bars, "Idratazione", DietCategory.WATER, 0xFF1E90FF, dietData, maxProfile);

        return bars;
    }

    private void addBar(java.util.List<BarInfo> bars, String label, DietCategory category, int color, DietData data, DietProfile maxProfile) {
        float value = data != null ? data.get(category) : 0.0f;
        float max = maxProfile.get(category);
        float percentage = max > 0.0f ? value / max : 0.0f;
        percentage = Math.min(1.0f, Math.max(0.0f, percentage));

        bars.add(new BarInfo(label, percentage, color));
    }
    
    private record BarInfo(String label, float percentage, int color) {}
}
