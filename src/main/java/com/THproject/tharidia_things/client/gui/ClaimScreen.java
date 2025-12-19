package com.THproject.tharidia_things.client.gui;

import com.THproject.tharidia_things.client.gui.components.PlayerInventoryPanelRenderer;
import com.THproject.tharidia_things.client.gui.medieval.MedievalGuiRenderer;
import com.THproject.tharidia_things.gui.ClaimMenu;
import com.THproject.tharidia_things.gui.inventory.PlayerInventoryPanelLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Medieval-styled claim screen with parchment background and ornate decorations
 */
public class ClaimScreen extends AbstractContainerScreen<ClaimMenu> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    // Medieval styling dimensions
    private static final int PARCHMENT_WIDTH = 280;
    private static final int PARCHMENT_HEIGHT = 320;
    private static final int BORDER_WIDTH = 10;
    
    public ClaimScreen(ClaimMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, Component.literal("Pergamena del Dominio"));
        this.imageWidth = PARCHMENT_WIDTH;
        this.imageHeight = PARCHMENT_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Render medieval parchment background
        MedievalGuiRenderer.renderParchmentBackground(guiGraphics, x, y, this.imageWidth, this.imageHeight);
        
        // Render ornate border
        MedievalGuiRenderer.renderOrnateBorder(guiGraphics, x, y, this.imageWidth, this.imageHeight, 
                MedievalGuiRenderer.DEEP_CRIMSON);
        
        // Render decorative seal in top center
        renderRoyalSeal(guiGraphics, x + this.imageWidth / 2 - 15, y + 25);

        // Render shared player inventory panel on the left
        int inventoryBgX = this.leftPos + PlayerInventoryPanelLayout.PANEL_OFFSET_X;
        int inventoryBgY = this.topPos + PlayerInventoryPanelLayout.PANEL_OFFSET_Y;
        int slotStartX = this.leftPos + PlayerInventoryPanelLayout.SLOT_OFFSET_X;
        int slotStartY = this.topPos + PlayerInventoryPanelLayout.SLOT_OFFSET_Y;
        PlayerInventoryPanelRenderer.renderPanel(guiGraphics, inventoryBgX, inventoryBgY, slotStartX, slotStartY);
    }
    
    /**
     * Renders a decorative royal seal
     */
    private void renderRoyalSeal(GuiGraphics gui, int x, int y) {
        // Outer circle
        gui.fill(x - 1, y - 1, x + 31, y + 31, MedievalGuiRenderer.BRONZE);
        gui.fill(x, y, x + 30, y + 30, MedievalGuiRenderer.ROYAL_GOLD);
        
        // Inner circle
        gui.fill(x + 5, y + 5, x + 25, y + 25, MedievalGuiRenderer.DEEP_CRIMSON);
        
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int originX = 0;
        int originY = 0;

        // Render medieval title relative to the GUI origin (already translated by AbstractContainerScreen)
        MedievalGuiRenderer.renderMedievalTitle(guiGraphics, "Dominio Feudale",
                originX + BORDER_WIDTH, originY + 60, this.imageWidth - BORDER_WIDTH * 2);

        // Render claim information with medieval styling
        renderClaimInfo(guiGraphics, originX, originY);

        // Render decorative elements
        renderDecorativeElements(guiGraphics, originX, originY);
    }
    
    /**
     * Renders claim information with medieval styling
     */
    private void renderClaimInfo(GuiGraphics gui, int x, int y) {
        int yPos = y + 90;
        int textX = x + BORDER_WIDTH + 20;
        
        // Get owner name from claim name
        String claimName = this.menu.getOwnerName();
        String ownerName = claimName.replace("'s Claim", "");
        
        // Owner information with medieval styling
        renderMedievalTextLine(gui, "§6§lSignore del Dominio:", textX, yPos, MedievalGuiRenderer.BROWN_INK);
        yPos += 16;
        renderMedievalTextLine(gui, "§f" + ownerName, textX + 20, yPos, MedievalGuiRenderer.BLACK_INK);
        yPos += 24;
        
        // Render divider
        MedievalGuiRenderer.renderMedievalDivider(gui, textX, yPos, this.imageWidth - BORDER_WIDTH * 2 - 40);
        yPos += 16;
        
        // Status information
        long expirationTime = this.menu.getExpirationTime();
        boolean isRented = this.menu.isRented();
        
        renderMedievalTextLine(gui, "§6§lStato del Dominio:", textX, yPos, MedievalGuiRenderer.BROWN_INK);
        yPos += 16;
        
        if (isRented && expirationTime > 0) {
            long currentTime = System.currentTimeMillis();
            long timeLeft = expirationTime - currentTime;
            
            if (timeLeft <= 0) {
                renderMedievalTextLine(gui, "§c§lSCADUTO", textX + 20, yPos, MedievalGuiRenderer.DEEP_CRIMSON);
            } else {
                // Calculate time components
                long totalSeconds = timeLeft / 1000;
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;
                
                // Time remaining with medieval styling
                renderMedievalTextLine(gui, "§6Tempo Restante:", textX + 20, yPos, MedievalGuiRenderer.BROWN_INK);
                yPos += 16;
                
                String timeText = String.format("§e§l%dh %dm %ds", hours, minutes, seconds);
                renderMedievalTextLine(gui, timeText, textX + 40, yPos, MedievalGuiRenderer.ROYAL_GOLD);
                yPos += 20;
                
                String expiresDate = DATE_FORMAT.format(new Date(expirationTime));
                renderMedievalTextLine(gui, "§6Scade il:", textX + 20, yPos, MedievalGuiRenderer.BROWN_INK);
                yPos += 16;
                renderMedievalTextLine(gui, "§f" + expiresDate, textX + 40, yPos, MedievalGuiRenderer.BLACK_INK);
            }
        } else {
            renderMedievalTextLine(gui, "§a§lDOMINIO PERPETUO", textX + 20, yPos, MedievalGuiRenderer.PURPLE_REGAL);
        }
        
        // Protection radius
        yPos += 24;
        MedievalGuiRenderer.renderMedievalDivider(gui, textX, yPos, this.imageWidth - BORDER_WIDTH * 2 - 40);
        yPos += 16;
        
        int protectionRadius = this.menu.getProtectionRadius();
        renderMedievalTextLine(gui, "§6§lRaggio di Protezione:", textX, yPos, MedievalGuiRenderer.BROWN_INK);
        yPos += 16;
        renderMedievalTextLine(gui, "§e§l" + protectionRadius + " blocchi", textX + 20, yPos, MedievalGuiRenderer.ROYAL_GOLD);
    }
    
    /**
     * Renders text with medieval shadow effect
     */
    private void renderMedievalTextLine(GuiGraphics gui, String text, int x, int y, int color) {
        gui.drawString(Minecraft.getInstance().font, text, x, y, color);
    }
    
    /**
     * Renders decorative elements around the GUI
     */
    private void renderDecorativeElements(GuiGraphics gui, int x, int y) {
        // Corner decorations
        renderCornerDecoration(gui, x + 5, y + 5, true);
        renderCornerDecoration(gui, x + this.imageWidth - 25, y + 5, false);
        renderCornerDecoration(gui, x + 5, y + this.imageHeight - 25, true);
        renderCornerDecoration(gui, x + this.imageWidth - 25, y + this.imageHeight - 25, false);
        
        // Side decorations
        for (int i = 0; i < 3; i++) {
            int decorY = y + 150 + (i * 40);
            gui.drawString(Minecraft.getInstance().font, "❦", x + 15, decorY, MedievalGuiRenderer.BRONZE);
            gui.drawString(Minecraft.getInstance().font, "❦", x + this.imageWidth - 25, decorY, MedievalGuiRenderer.BRONZE);
        }
    }
    
    /**
     * Renders corner decoration
     */
    private void renderCornerDecoration(GuiGraphics gui, int x, int y, boolean topLeft) {
        // Simple but elegant corner decoration
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                if ((i == 0 || i == 19) && j < 15) {
                    gui.fill(x + i, y + j, x + i + 1, y + j + 1, MedievalGuiRenderer.BRONZE);
                }
                if ((j == 0 || j == 19) && i < 15) {
                    gui.fill(x + i, y + j, x + i + 1, y + j + 1, MedievalGuiRenderer.BRONZE);
                }
            }
        }
        
        // Central gem
        gui.fill(x + 7, y + 7, x + 13, y + 13, MedievalGuiRenderer.DEEP_CRIMSON);
        gui.fill(x + 8, y + 8, x + 12, y + 12, MedievalGuiRenderer.ROYAL_GOLD);
    }
}
