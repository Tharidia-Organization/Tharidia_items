package com.tharidia.tharidia_things.client.gui;

import com.tharidia.tharidia_things.gui.TradeMenu;
import com.tharidia.tharidia_things.network.TradeCancelPacket;
import com.tharidia.tharidia_things.network.TradeUpdatePacket;
import com.tharidia.tharidia_things.network.TradeFinalConfirmPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Medieval-themed trade screen with two-column layout
 */
public class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "textures/gui/trade_background.png");
    private static final ResourceLocation YES_BUTTON_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "textures/gui/trade_button_yes.png");
    private static final ResourceLocation NO_BUTTON_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "textures/gui/trade_button_no.png");
    
    private Button confirmButton;
    private Button cancelButton;
    private Button finalConfirmButton;
    private Button completeTradeButton;
    private boolean localConfirmed = false;
    private boolean localFinalConfirmed = false;

    public TradeScreen(TradeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 195;
    }

    @Override
    protected void init() {
        super.init();
        
        // Position GUI centered on screen
        this.leftPos = (this.width - this.imageWidth) - 10;
        this.topPos = (this.height - this.imageHeight) - 10;
        
        int centerX = this.leftPos + this.imageWidth / 2;
        int buttonY = this.topPos + 65;
        
        // Buttons arranged horizontally
        int buttonWidth = 48;
        int buttonHeight = 18;
        int buttonSpacing = 21;
        int totalWidth = (buttonWidth * 2) + buttonSpacing;
        int startX = centerX - (totalWidth / 2);
        
        // Confirm button (left) - Uses trade_button_yes texture
        this.confirmButton = this.addRenderableWidget(new TexturedButton(
            startX, buttonY, buttonWidth, buttonHeight, true, button -> toggleConfirm()
        ));
        
        // Cancel button (right) - Uses trade_button_no texture
        this.cancelButton = this.addRenderableWidget(new TexturedButton(
            startX + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight, false, button -> cancelTrade()
        ));
        
        // Final confirm button (only visible when both players confirmed) - Uses trade_button_yes texture
        this.finalConfirmButton = this.addRenderableWidget(new TexturedButton(
            startX, buttonY, buttonWidth, buttonHeight, true, button -> toggleFinalConfirm()
        ));
        this.finalConfirmButton.visible = false;
        
        // Complete trade button (only visible when both players final confirmed) - Uses trade_button_yes texture
        this.completeTradeButton = this.addRenderableWidget(new TexturedButton(
            startX + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight, true, button -> completeTrade()
        ));
        this.completeTradeButton.visible = false;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Draw the custom background texture
        // Parameters: texture, x, y, u, v, width, height, textureWidth, textureHeight
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 230, 195);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Draw player name (left side) - smaller font using scaling
        Component playerLabel = Component.literal("§6La Vostra Offerta");
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.8f, 0.8f, 1.0f);
        guiGraphics.drawString(this.font, playerLabel, (int)(20 / 0.8f), (int)(8 / 0.8f), 0x404040, false);
        guiGraphics.pose().popPose();
        
        // Draw other player name (right side) - smaller font using scaling
        Component otherLabel = Component.literal("§6" + this.menu.getOtherPlayerName());
        int otherLabelWidth = this.font.width(otherLabel);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.8f, 0.8f, 1.0f);
        guiGraphics.drawString(this.font, otherLabel, (int)((this.imageWidth - otherLabelWidth - 15) / 0.8f), (int)(8 / 0.8f), 0x404040, false);
        guiGraphics.pose().popPose();
        
        // Draw tax information
        drawTaxInfo(guiGraphics);
        
        // Draw confirmation status (below buttons)
        int statusY = 92;
        if (localConfirmed) {
            guiGraphics.drawString(this.font, "§2✓ Confermato", 16, statusY, 0x404040, false);
        } else {
            guiGraphics.drawString(this.font, "§7In attesa...", 16, statusY, 0x404040, false);
        }
        
        if (this.menu.isOtherPlayerConfirmed()) {
            Component otherConfirmed = Component.literal("§2Confermato");
            int width = this.font.width(otherConfirmed);
            guiGraphics.drawString(this.font, otherConfirmed, this.imageWidth - width - 16, statusY, 0x404040, false);
        } else {
            Component otherWaiting = Component.literal("§7In attesa...");
            int width = this.font.width(otherWaiting);
            guiGraphics.drawString(this.font, otherWaiting, this.imageWidth - width - 16, statusY, 0x404040, false);
        }
        
        // Manage button visibility based on state
        boolean bothConfirmed = this.menu.isPlayerConfirmed() && this.menu.isOtherPlayerConfirmed();
        boolean bothFinalConfirmed = this.menu.isPlayerFinalConfirmed() && this.menu.isOtherPlayerFinalConfirmed();
        
        if (bothFinalConfirmed) {
            // Show only complete trade button
            confirmButton.visible = false;
            cancelButton.visible = false;
            finalConfirmButton.visible = false;
            completeTradeButton.visible = true;
            completeTradeButton.active = true;
            
            // Draw attention
            int centerX = this.imageWidth / 2;
            guiGraphics.drawString(this.font, "§a§lCOMPLETA", centerX - 25, 95, 0x00FF00, false);
        } else if (bothConfirmed) {
            // Show final confirm and cancel buttons (both visible)
            confirmButton.visible = false;
            cancelButton.visible = true; // Keep cancel button visible
            finalConfirmButton.visible = true;
            finalConfirmButton.active = true;
            completeTradeButton.visible = false;
            
            if (localFinalConfirmed) {
                ((TexturedButton)finalConfirmButton).setTexture(false); // Use no texture for "Annulla"
            } else {
                ((TexturedButton)finalConfirmButton).setTexture(true); // Use yes texture for "Conferma"
            }
        } else {
            // Show normal confirm and cancel buttons
            confirmButton.visible = true;
            cancelButton.visible = true;
            finalConfirmButton.visible = false;
            completeTradeButton.visible = false;
            
            if (localConfirmed) {
                ((TexturedButton)confirmButton).setTexture(false); // Use no texture for "Annulla"
            } else {
                ((TexturedButton)confirmButton).setTexture(true); // Use yes texture for "Conferma"
            }
        }
    }
    
    private void drawTaxInfo(GuiGraphics guiGraphics) {
        // Get tax info from menu (synced from server)
        double taxRate = this.menu.getTaxRate();
        int taxAmount = this.menu.getTaxAmount();
        
        if (taxAmount > 0) {
            int taxPercent = (int) (taxRate * 100);
            
            // Calculate total currency from other player's offer
            int totalCurrency = 0;
            for (int i = 0; i < 6; i++) {
                ItemStack stack = this.menu.getOtherPlayerOffer().getItem(i);
                if (!stack.isEmpty() && isCurrencyItem(stack)) {
                    totalCurrency += stack.getCount();
                }
            }
            
            int taxedCurrency = totalCurrency - taxAmount;
            
            // Draw tax info in the center
            int centerX = this.imageWidth / 2;
            String taxInfo = String.format("§cTassa: %d%% (-%d)", taxPercent, taxAmount);
            String receiveInfo = String.format("§aRiceverai: %d", taxedCurrency);
            
            int taxInfoWidth = this.font.width(taxInfo);
            int receiveInfoWidth = this.font.width(receiveInfo);
            
            guiGraphics.drawString(this.font, taxInfo, centerX - taxInfoWidth / 2, 54, 0xFF0000, false);
            guiGraphics.drawString(this.font, receiveInfo, centerX - receiveInfoWidth / 2, 62, 0x00FF00, false);
        }
    }
    
    private boolean isCurrencyItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        // This is a simplified check - in production, use Config.TRADE_CURRENCY_ITEMS
        return stack.getItem().toString().contains("potato") || 
               stack.getItem().toString().contains("gold_nugget");
    }
    
    private double getTaxRate() {
        // Default 10% - in production, use Config.TRADE_TAX_RATE.get()
        return 0.1;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        
        // Check if both players final confirmed - trade will complete server-side
        if (this.menu.isPlayerFinalConfirmed() && this.menu.isOtherPlayerFinalConfirmed()) {
            // Trade completed, menu will be closed by server
        }
    }

    private void toggleConfirm() {
        localConfirmed = !localConfirmed;
        
        // Collect items from player's offer slots
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ItemStack stack = this.menu.getPlayerOffer().getItem(i);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        
        // Send update to server
        PacketDistributor.sendToServer(new TradeUpdatePacket(
            Minecraft.getInstance().player.getUUID(),
            items,
            localConfirmed
        ));
        
        this.menu.setPlayerConfirmed(localConfirmed);
        
        // Log for debugging
        if (localConfirmed) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§aHai confermato! Aspetta che anche l'altro giocatore confermi."));
        } else {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§7Conferma annullata."));
        }
    }

    private void toggleFinalConfirm() {
        localFinalConfirmed = !localFinalConfirmed;
        
        // Send final confirmation to server
        PacketDistributor.sendToServer(new TradeFinalConfirmPacket(
            Minecraft.getInstance().player.getUUID(),
            localFinalConfirmed
        ));
        
        this.menu.setPlayerFinalConfirmed(localFinalConfirmed);
        
        // Log for debugging
        if (localFinalConfirmed) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§a§lHai confermato FINALMENTE! Aspetta che anche l'altro giocatore confermi."));
        } else {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§7Conferma finale annullata."));
        }
    }

    private void completeTrade() {
        // Reset camera
        com.tharidia.tharidia_things.client.TradeCameraHandler.resetCamera();
        
        // The trade is already completed server-side when both players final confirmed
        // This button just acknowledges and closes the GUI
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("§a§lSCAMBIO COMPLETATO!"));
        Minecraft.getInstance().player.closeContainer();
    }

    private void cancelTrade() {
        // Reset camera
        com.tharidia.tharidia_things.client.TradeCameraHandler.resetCamera();
        
        PacketDistributor.sendToServer(new TradeCancelPacket(
            Minecraft.getInstance().player.getUUID()
        ));
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Block inventory key (E) when in trade
        if (keyCode == 69) { // E key
            return true;
        }
        // Block ESC key to prevent closing
        if (keyCode == 256) { // ESC key
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void onClose() {
        // Prevent closing the screen - players must use Cancel button
        // This prevents accidental closure and item loss
    }

    /**
     * Custom textured button for trade GUI
     */
    private static class TexturedButton extends Button {
        private ResourceLocation texture;
        private final ResourceLocation yesTexture;
        private final ResourceLocation noTexture;
        private final boolean isYesButton;

        public TexturedButton(int x, int y, int width, int height, boolean isYesButton, OnPress onPress) {
            super(x, y, width, height, Component.literal(""), onPress, DEFAULT_NARRATION);
            this.isYesButton = isYesButton;
            this.yesTexture = YES_BUTTON_TEXTURE;
            this.noTexture = NO_BUTTON_TEXTURE;
            this.texture = isYesButton ? yesTexture : noTexture;
        }

        public void setTexture(boolean useYesTexture) {
            this.texture = useYesTexture ? yesTexture : noTexture;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // Draw the button texture at full size (64x32)
            guiGraphics.blit(texture, getX(), getY(), 0, 0, width, height, width, height);
            
            // Draw subtle hover effect
            if (isHovered()) {
                guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
            }
        }
    }
}
