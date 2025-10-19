package com.tharidia.tharidia_things.client.gui;

import com.tharidia.tharidia_things.gui.NameSelectionMenu;
import com.tharidia.tharidia_things.network.SubmitNamePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for name selection on first join
 */
public class NameSelectionScreen extends AbstractContainerScreen<NameSelectionMenu> {
    private static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "textures/gui/name_selection.png");
    
    private EditBox nameField;
    private Button confirmButton;
    private String errorMessage = "";
    private int errorTimer = 0;

    public NameSelectionScreen(NameSelectionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 200;
        this.imageHeight = 150;
    }

    @Override
    protected void init() {
        super.init();
        
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Create text field for name input
        this.nameField = new EditBox(this.font, x + 20, y + 60, 160, 20, Component.literal("Name"));
        this.nameField.setMaxLength(16);
        this.nameField.setHint(Component.literal("Enter your name..."));
        this.nameField.setResponder(text -> {
            // Enable confirm button only if text is not empty
            if (this.confirmButton != null) {
                this.confirmButton.active = !text.trim().isEmpty();
            }
        });
        this.addRenderableWidget(this.nameField);
        
        // Create confirm button
        this.confirmButton = Button.builder(
            Component.literal("Confirm"),
            button -> this.confirmName())
            .bounds(x + 50, y + 95, 100, 20)
            .build();
        this.confirmButton.active = false; // Disabled by default
        this.addRenderableWidget(this.confirmButton);
        
        // Set focus on the text field
        this.setInitialFocus(this.nameField);
    }

    private void confirmName() {
        String chosenName = this.nameField.getValue().trim();
        
        if (chosenName.isEmpty()) {
            this.errorMessage = "Name cannot be empty!";
            this.errorTimer = 60; // Show error for 3 seconds (60 ticks)
            return;
        }
        
        // Send packet to server
        PacketDistributor.sendToServer(new SubmitNamePacket(chosenName));
        
        // Clear error message
        this.errorMessage = "";
        this.errorTimer = 0;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Draw background - try to load custom texture, fallback to solid color
        try {
            guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 200, 150);
        } catch (Exception e) {
            // Fallback: draw a simple background
            guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xC0101010);
            guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF8B8B8B);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Don't render background (no world behind)
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // Update error timer
        if (this.errorTimer > 0) {
            this.errorTimer--;
            if (this.errorTimer == 0) {
                this.errorMessage = "";
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render title centered at the top
        Component titleText = Component.literal("Choose Your Name");
        int titleX = (this.imageWidth - this.font.width(titleText)) / 2;
        guiGraphics.drawString(this.font, titleText, titleX, 8, 0x404040, false);
        
        // Render instructions
        Component instructions = Component.literal("This name will be visible to other players");
        int instructionsX = (this.imageWidth - this.font.width(instructions)) / 2;
        guiGraphics.drawString(this.font, instructions, instructionsX, 30, 0x606060, false);
        
        // Render error message if present
        if (!this.errorMessage.isEmpty() && this.errorTimer > 0) {
            Component error = Component.literal("§c" + this.errorMessage);
            int errorX = (this.imageWidth - this.font.width(error)) / 2;
            guiGraphics.drawString(this.font, error, errorX, 120, 0xFF0000, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle Enter key to confirm
        if (keyCode == 257 && this.confirmButton.active) { // 257 = ENTER
            this.confirmName();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        // Don't pause the game
        return false;
    }
    
    @Override
    public void onClose() {
        // Prevent closing without choosing a name by not calling super.onClose()
        // The menu will handle reopening if needed
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
    }
}
