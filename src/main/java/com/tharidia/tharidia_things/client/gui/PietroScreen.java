package com.tharidia.tharidia_things.client.gui;

import com.tharidia.tharidia_things.block.entity.PietroBlockEntity;
import com.tharidia.tharidia_things.gui.PietroMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PietroScreen extends AbstractContainerScreen<PietroMenu> {
    private static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "textures/gui/pietro_gui.png");

    public PietroScreen(PietroMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 250;
        this.imageHeight = 300;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 250, 300);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render title centered at the top
        int titleX = (this.imageWidth - this.font.width(this.title)) / 2;
        guiGraphics.drawString(this.font, this.title, titleX, 6, 4210752, false);
        
        // Render Pietro information
        PietroBlockEntity pietroEntity = this.menu.getBlockEntity();
        if (pietroEntity != null) {
            int yPos = 25; // Start below title
            int color = 0x404040; // Dark gray
            
            // Owner
            String owner = pietroEntity.getOwnerName();
            if (owner == null || owner.isEmpty()) {
                owner = "Unknown";
            }
            guiGraphics.drawString(this.font, "§6Proprietario: §f" + owner, 10, yPos, color, false);
            yPos += 12;
            
            // Realm size - use synced data from menu
            int size = this.menu.getRealmSize();
            guiGraphics.drawString(this.font, "§6Dimensione Regno: §f" + size + "x" + size + " chunks", 
                10, yPos, color, false);
            yPos += 12;
            
            // Potato progress - use synced data from menu
            if (size >= 15) {
                guiGraphics.drawString(this.font, "§aRegno al massimo livello!", 10, yPos, color, false);
            } else {
                int stored = this.menu.getStoredPotatoes();
                int required = pietroEntity.getPotatoCostForNextLevel();
                int remaining = required - stored;
                
                guiGraphics.drawString(this.font, "§6Patate per espansione:", 10, yPos, color, false);
                yPos += 12;
                
                guiGraphics.drawString(this.font, "§e" + stored + "§7/§e" + required + 
                    " §7(§6" + remaining + " §7necessarie)", 10, yPos, color, false);
                yPos += 12;
                
                // Progress bar
                int barWidth = 180;
                int barHeight = 8;
                int barX = 10;
                int barY = yPos;
                
                // Background (dark gray)
                guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF555555);
                
                // Progress (gold)
                if (required > 0) {
                    int fillWidth = (int)((stored / (float)required) * barWidth);
                    guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, 0xFFFFAA00);
                }
                
                // Border (black)
                guiGraphics.renderOutline(barX, barY, barWidth, barHeight, 0xFF000000);
            }
            
            yPos += 20;
            
            // Instructions
            guiGraphics.drawString(this.font, "§7Metti le patate nello slot →", 10, yPos, color, false);
            yPos += 10;
            guiGraphics.drawString(this.font, "§7per espandere il regno", 10, yPos, color, false);
        }
    }
    
    // Removed containerTick - processing moved to server side
}
