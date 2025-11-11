package com.tharidia.tharidia_things.client.gui;

import com.tharidia.tharidia_things.gui.TradeMenu;
import com.tharidia.tharidia_things.network.TradeCancelPacket;
import com.tharidia.tharidia_things.network.TradeUpdatePacket;
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
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "textures/gui/trade_gui.png");
    
    private Button confirmButton;
    private boolean localConfirmed = false;

    public TradeScreen(TradeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 276;
        this.imageHeight = 240;
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.leftPos + this.imageWidth / 2;
        int buttonY = this.topPos + 130;
        
        // Confirm/Cancel button in the center
        this.confirmButton = this.addRenderableWidget(Button.builder(
            Component.literal("§6§l⚜ Conferma ⚜"),
            button -> toggleConfirm()
        ).bounds(centerX - 50, buttonY, 100, 20).build());
        
        // Cancel trade button
        this.addRenderableWidget(Button.builder(
            Component.literal("§c✗ Annulla"),
            button -> cancelTrade()
        ).bounds(centerX - 40, buttonY + 25, 80, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Draw background
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF8B4513); // Brown background
        guiGraphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, 0xFFC0C0C0); // Light gray inner
        
        // Draw divider in the middle
        int centerX = x + this.imageWidth / 2;
        guiGraphics.fill(centerX - 1, y + 10, centerX + 1, y + 130, 0xFF8B4513);
        
        // Draw slot backgrounds for player's side (left)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 4; col++) {
                int slotX = x + 8 + col * 18;
                int slotY = y + 18 + row * 18;
                guiGraphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
            }
        }
        
        // Draw slot backgrounds for other player's side (right)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 4; col++) {
                int slotX = x + 186 + col * 18;
                int slotY = y + 18 + row * 18;
                guiGraphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Draw player name (left side)
        Component playerLabel = Component.literal("§6La Vostra Offerta");
        guiGraphics.drawString(this.font, playerLabel, 8, 6, 0x404040, false);
        
        // Draw other player name (right side)
        Component otherLabel = Component.literal("§6" + this.menu.getOtherPlayerName());
        int otherLabelWidth = this.font.width(otherLabel);
        guiGraphics.drawString(this.font, otherLabel, this.imageWidth - otherLabelWidth - 8, 6, 0x404040, false);
        
        // Draw confirmation status
        int statusY = 135;
        if (localConfirmed) {
            guiGraphics.drawString(this.font, "§2✓ Confermato", 10, statusY, 0x404040, false);
        } else {
            guiGraphics.drawString(this.font, "§7In attesa...", 10, statusY, 0x404040, false);
        }
        
        if (this.menu.isOtherPlayerConfirmed()) {
            Component otherConfirmed = Component.literal("§2✓ Confermato");
            int width = this.font.width(otherConfirmed);
            guiGraphics.drawString(this.font, otherConfirmed, this.imageWidth - width - 10, statusY, 0x404040, false);
        } else {
            Component otherWaiting = Component.literal("§7In attesa...");
            int width = this.font.width(otherWaiting);
            guiGraphics.drawString(this.font, otherWaiting, this.imageWidth - width - 10, statusY, 0x404040, false);
        }
        
        // Update button text based on confirmation state
        if (localConfirmed) {
            confirmButton.setMessage(Component.literal("§e§l⚠ Annulla Conferma"));
        } else {
            confirmButton.setMessage(Component.literal("§6§l⚜ Conferma ⚜"));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        
        // Check if both players confirmed - trade will complete server-side
        if (this.menu.isPlayerConfirmed() && this.menu.isOtherPlayerConfirmed()) {
            // Trade completed, menu will be closed by server
        }
    }

    private void toggleConfirm() {
        localConfirmed = !localConfirmed;
        
        // Collect items from player's offer slots
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
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
    }

    private void cancelTrade() {
        PacketDistributor.sendToServer(new TradeCancelPacket(
            Minecraft.getInstance().player.getUUID()
        ));
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
