package com.tharidia.tharidia_things.client.gui;

import com.tharidia.tharidia_things.block.entity.PietroBlockEntity;
import com.tharidia.tharidia_things.gui.PietroMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PietroScreen extends AbstractContainerScreen<PietroMenu> {
    private static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "textures/gui/pietro_gui.png");
    
    private static final int TAB_EXPANSION = 0;
    private static final int TAB_CLAIMS = 1;
    
    private int currentTab = TAB_EXPANSION;
    private Button expansionTabButton;
    private Button claimsTabButton;

    public PietroScreen(PietroMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 250;
        this.imageHeight = 300;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int tabX = this.leftPos + 10;
        int tabY = this.topPos + 15;
        int tabWidth = 70;
        int tabHeight = 20;
        
        // Expansion tab button
        expansionTabButton = Button.builder(
            Component.literal("Espansione"),
            button -> switchTab(TAB_EXPANSION)
        ).bounds(tabX, tabY, tabWidth, tabHeight).build();
        
        // Claims tab button
        claimsTabButton = Button.builder(
            Component.literal("Rivendicazioni"),
            button -> switchTab(TAB_CLAIMS)
        ).bounds(tabX + tabWidth + 5, tabY, tabWidth + 30, tabHeight).build();
        
        this.addRenderableWidget(expansionTabButton);
        this.addRenderableWidget(claimsTabButton);
        
        updateTabButtons();
    }
    
    private void switchTab(int tab) {
        currentTab = tab;
        updateTabButtons();
    }
    
    private void updateTabButtons() {
        expansionTabButton.active = currentTab != TAB_EXPANSION;
        claimsTabButton.active = currentTab != TAB_CLAIMS;
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
        
        // Render content based on current tab
        if (currentTab == TAB_EXPANSION) {
            renderExpansionTab(guiGraphics);
        } else if (currentTab == TAB_CLAIMS) {
            renderClaimsTab(guiGraphics);
        }
    }
    
    private void renderExpansionTab(GuiGraphics guiGraphics) {
        PietroBlockEntity pietroEntity = this.menu.getBlockEntity();
        if (pietroEntity != null) {
            int yPos = 45; // Start below tabs
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
    
    private void renderClaimsTab(GuiGraphics guiGraphics) {
        int yPos = 45; // Start below tabs
        int color = 0x404040; // Dark gray
        
        // Title
        guiGraphics.drawString(this.font, "§6§lRivendicazioni", 10, yPos, color, false);
        yPos += 20;
        
        // Total potatoes from claims
        int totalPotatoes = this.menu.getTotalClaimPotatoes();
        
        guiGraphics.drawString(this.font, "§6Monete totali ricevute", 10, yPos, color, false);
        guiGraphics.drawString(this.font, "§6dai tuoi territori:", 10, yPos + 10, color, false);
        yPos += 30;
        
        // Big number display
        String potatoText = String.valueOf(totalPotatoes);
        int textWidth = this.font.width(potatoText);
        int centerX = (this.imageWidth - textWidth) / 2;
        
        // Draw shadow
        guiGraphics.drawString(this.font, potatoText, centerX + 2, yPos + 2, 0x804400, false);
        // Draw main text (larger scale would require matrix transformations)
        guiGraphics.drawString(this.font, "§e§l" + potatoText, centerX, yPos, color, false);
        yPos += 20;
        
        // Potato icon/emoji
        guiGraphics.drawString(this.font, "§6🥔🥔🥔", centerX + textWidth/2 - 10, yPos, color, false);
    }
    
    // Removed containerTick - processing moved to server side
}
