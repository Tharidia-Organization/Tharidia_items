package com.THproject.tharidia_things.client.gui;

import com.THproject.tharidia_things.network.SubmitNamePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Pre-login screen for name selection
 * This screen appears BEFORE the player enters the world
 */
public class PreLoginNameScreen extends Screen {
    private EditBox nameField;
    private Button confirmButton;
    private String errorMessage = "";
    private int errorTimer = 0;
    private boolean nameSubmitted = false;

    public PreLoginNameScreen() {
        super(Component.literal("Choose Your Name"));
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Create text field for name input
        this.nameField = new EditBox(this.font, centerX - 80, centerY - 10, 160, 20, Component.literal("Name"));
        this.nameField.setMaxLength(16);
        this.nameField.setHint(Component.literal("Enter your name..."));
        this.nameField.setResponder(text -> {
            // Enable confirm button only if text is not empty
            if (this.confirmButton != null) {
                this.confirmButton.active = !text.trim().isEmpty() && !nameSubmitted;
            }
        });
        this.addRenderableWidget(this.nameField);
        
        // Create confirm button
        this.confirmButton = Button.builder(
            Component.literal("Confirm"),
            button -> this.confirmName())
            .bounds(centerX - 50, centerY + 25, 100, 20)
            .build();
        this.confirmButton.active = false; // Disabled by default
        this.addRenderableWidget(this.confirmButton);
        
        // Set focus on the text field
        this.setInitialFocus(this.nameField);
    }

    private void confirmName() {
        if (nameSubmitted) {
            return; // Prevent double submission
        }
        
        String chosenName = this.nameField.getValue().trim();
        
        if (chosenName.isEmpty()) {
            this.errorMessage = "Name cannot be empty!";
            this.errorTimer = 60; // Show error for 3 seconds (60 ticks)
            return;
        }
        
        // Send packet to server
        PacketDistributor.sendToServer(new SubmitNamePacket(chosenName));
        
        // Mark as submitted
        nameSubmitted = true;
        this.confirmButton.active = false;
        this.nameField.setEditable(false);
        
        // Show waiting message
        this.errorMessage = "§aSubmitting name...";
        this.errorTimer = 100;
        
        // Close screen after a short delay
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                if (this.minecraft != null) {
                    this.minecraft.execute(this::onClose);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render dark background
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render a panel background
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelWidth = 220;
        int panelHeight = 120;
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;
        
        // Draw panel background
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xC0101010);
        guiGraphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xFF8B8B8B);
        guiGraphics.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + panelHeight - 2, 0xFF3C3C3C);
        
        // Render title centered at the top
        Component titleText = Component.literal("Inserisci il tuo Nome");
        int titleX = centerX - this.font.width(titleText) / 2;
        guiGraphics.drawString(this.font, titleText, titleX, panelY + 15, 0xFFFFFF, true);
        
        // Render instructions
        Component instructions = Component.literal("Questo è il nome del tuo personaggio");
        int instructionsX = centerX - this.font.width(instructions) / 2;
        guiGraphics.drawString(this.font, instructions, instructionsX, panelY + 35, 0xAAAAAA, false);
        
        // Render widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render error/status message if present
        if (!this.errorMessage.isEmpty() && this.errorTimer > 0) {
            Component message = Component.literal(this.errorMessage);
            int messageX = centerX - this.font.width(message) / 2;
            int color = this.errorMessage.startsWith("§a") ? 0x00FF00 : 0xFF0000;
            guiGraphics.drawString(this.font, message, messageX, panelY + 95, color, true);
        }
        
        // Update error timer
        if (this.errorTimer > 0) {
            this.errorTimer--;
            if (this.errorTimer == 0 && !nameSubmitted) {
                this.errorMessage = "";
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle Enter key to confirm
        if (keyCode == 257 && this.confirmButton.active && !nameSubmitted) { // 257 = ENTER
            this.confirmName();
            return true;
        }
        // Prevent ESC from closing the screen
        if (keyCode == 256) { // 256 = ESC
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Prevent closing with ESC
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        // Don't pause the game
        return false;
    }
}
