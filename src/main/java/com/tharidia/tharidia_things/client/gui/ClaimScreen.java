package com.tharidia.tharidia_things.client.gui;

import com.tharidia.tharidia_things.block.entity.ClaimBlockEntity;
import com.tharidia.tharidia_things.gui.ClaimMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ClaimScreen extends AbstractContainerScreen<ClaimMenu> {
    private static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "textures/gui/claim_gui.png");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ClaimScreen(ClaimMenu menu, Inventory playerInventory, Component title) {
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
        
        // Render claim information using synced data from menu
        int yPos = 65; // Start below the slot
        int color = 0x404040; // Dark gray
        
        // Get owner name from claim name
        String claimName = this.menu.getOwnerName();
        String ownerName = claimName.replace("'s Claim", "");
        
        // Render claim info using synced data
        guiGraphics.drawString(this.font, "§6Owner: §f" + ownerName, 10, yPos, color, false);
        yPos += 12;
        
        // Use synced protection radius
        int protectionRadius = this.menu.getProtectionRadius();
        guiGraphics.drawString(this.font, "§6Protection Radius: §f" + protectionRadius + " blocks", 
            10, yPos, color, false);
        yPos += 12;
        
        // Render expiration info - use synced data from menu
        long expirationTime = this.menu.getExpirationTime();
        boolean isRented = this.menu.isRented();
        
        if (isRented && expirationTime > 0) {
            long currentTime = System.currentTimeMillis();
            long timeLeft = expirationTime - currentTime;
            
            if (timeLeft <= 0) {
                guiGraphics.drawString(this.font, "§cStatus: EXPIRED", 10, yPos, color, false);
            } else {
                // Calculate time components
                long totalMinutes = timeLeft / (60 * 1000);
                long hours = totalMinutes / 60;
                long minutes = totalMinutes % 60;
                long seconds = (timeLeft / 1000) % 60;
                
                if (hours > 0) {
                    guiGraphics.drawString(this.font, "§6Tempo rimanente: §f" + hours + "h " + minutes + "m", 
                        10, yPos, color, false);
                } else if (minutes > 0) {
                    guiGraphics.drawString(this.font, "§6Tempo rimanente: §f" + minutes + "m " + seconds + "s", 
                        10, yPos, color, false);
                } else {
                    guiGraphics.drawString(this.font, "§cTempo rimanente: §f" + seconds + "s", 
                        10, yPos, color, false);
                }
                yPos += 12;
                
                String expiresDate = DATE_FORMAT.format(new Date(expirationTime));
                guiGraphics.drawString(this.font, "§6Scade: §f" + expiresDate, 10, yPos, color, false);
            }
        } else {
            guiGraphics.drawString(this.font, "§6Status: §aPermanent", 10, yPos, color, false);
        }
    }
}
