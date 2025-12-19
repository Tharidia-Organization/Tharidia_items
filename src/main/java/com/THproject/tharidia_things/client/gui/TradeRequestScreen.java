package com.THproject.tharidia_things.client.gui;

import com.THproject.tharidia_things.network.TradeResponsePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Medieval-themed screen shown when receiving a trade request
 */
public class TradeRequestScreen extends Screen {
    private final UUID requesterId;
    private final String requesterName;
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 120;

    public TradeRequestScreen(UUID requesterId, String requesterName) {
        super(Component.literal("Richiesta di Scambio"));
        this.requesterId = requesterId;
        this.requesterName = requesterName;
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Accept button (green-ish)
        this.addRenderableWidget(Button.builder(
            Component.literal("§2§l✓ Accetta"),
            button -> acceptTrade()
        ).bounds(centerX - 80, centerY + 20, 70, 20).build());
        
        // Decline button (red-ish)
        this.addRenderableWidget(Button.builder(
            Component.literal("§4§l✗ Rifiuta"),
            button -> declineTrade()
        ).bounds(centerX + 10, centerY + 20, 70, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;
        
        // Draw panel background
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC8B4513); // Dark brown
        guiGraphics.fill(panelX + 2, panelY + 2, panelX + PANEL_WIDTH - 2, panelY + PANEL_HEIGHT - 2, 0xCCD2691E); // Lighter brown
        
        // Draw border
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 2, 0xFF654321); // Top
        guiGraphics.fill(panelX, panelY + PANEL_HEIGHT - 2, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF654321); // Bottom
        guiGraphics.fill(panelX, panelY, panelX + 2, panelY + PANEL_HEIGHT, 0xFF654321); // Left
        guiGraphics.fill(panelX + PANEL_WIDTH - 2, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF654321); // Right
        
        // Draw title
        Component title = Component.literal("§6§l⚜ Richiesta di Scambio ⚜");
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, centerX - titleWidth / 2, panelY + 15, 0xFFFFFF, true);
        
        // Draw message
        Component message1 = Component.literal("§7Il mercante §f" + requesterName);
        Component message2 = Component.literal("§7desidera commerciare con voi.");
        Component message3 = Component.literal("§7Accettate la proposta?");
        
        int msg1Width = this.font.width(message1);
        int msg2Width = this.font.width(message2);
        int msg3Width = this.font.width(message3);
        
        guiGraphics.drawString(this.font, message1, centerX - msg1Width / 2, panelY + 40, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, message2, centerX - msg2Width / 2, panelY + 55, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, message3, centerX - msg3Width / 2, panelY + 70, 0xFFFFFF, false);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void acceptTrade() {
        PacketDistributor.sendToServer(new TradeResponsePacket(requesterId, true));
        this.onClose();
    }

    private void declineTrade() {
        PacketDistributor.sendToServer(new TradeResponsePacket(requesterId, false));
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
