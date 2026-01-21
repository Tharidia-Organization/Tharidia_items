package com.THproject.tharidia_things.client.gui;

import com.THproject.tharidia_things.network.SubmitNamePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.regex.Pattern;

/**
 * Pre-login screen for name selection.
 * This screen appears BEFORE the player enters the world.
 */
public class PreLoginNameScreen extends Screen {
    private EditBox nameField;
    private Button confirmButton;
    private String errorMessage = "";
    private int errorTicksRemaining = 0;
    private long lastTickTime = 0;
    private boolean nameSubmitted = false;
    private int closeDelayTicks = 0;

    // Name validation
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 16;
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    // Message types for color coding
    private enum MessageType {
        ERROR(0xFF5555),
        SUCCESS(0x55FF55),
        INFO(0x5555FF);

        final int color;
        MessageType(int color) {
            this.color = color;
        }
    }

    private MessageType currentMessageType = MessageType.ERROR;

    public PreLoginNameScreen() {
        super(Component.translatable("gui.tharidiathings.name_selection"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Create text field for name input
        this.nameField = new EditBox(this.font, centerX - 80, centerY - 10, 160, 20,
                Component.translatable("gui.tharidiathings.name_entry.field_label"));
        this.nameField.setMaxLength(MAX_NAME_LENGTH);
        this.nameField.setHint(Component.translatable("gui.tharidiathings.name_entry.hint"));
        this.nameField.setResponder(text -> {
            if (this.confirmButton != null) {
                this.confirmButton.active = isValidName(text) && !nameSubmitted;
            }
            // Clear error message when user starts typing
            if (!text.isEmpty() && errorTicksRemaining > 0 && currentMessageType == MessageType.ERROR) {
                errorTicksRemaining = 0;
                errorMessage = "";
            }
        });
        this.addRenderableWidget(this.nameField);

        // Create confirm button
        this.confirmButton = Button.builder(
                        Component.translatable("gui.tharidiathings.name_entry.confirm_button"),
                        button -> this.confirmName())
                .bounds(centerX - 50, centerY + 25, 100, 20)
                .build();
        this.confirmButton.active = false;
        this.addRenderableWidget(this.confirmButton);

        // Set focus on the text field
        this.setInitialFocus(this.nameField);

        // Initialize tick time
        this.lastTickTime = System.currentTimeMillis();
    }

    /**
     * Validate name format
     */
    private boolean isValidName(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            return false;
        }
        return VALID_NAME_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Get validation error message for the name
     */
    private String getValidationError(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Component.translatable("gui.tharidiathings.name_entry.error.empty").getString();
        }
        String trimmed = name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH) {
            return "Il nome deve essere lungo almeno " + MIN_NAME_LENGTH + " caratteri";
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            return "Il nome non può superare " + MAX_NAME_LENGTH + " caratteri";
        }
        if (!VALID_NAME_PATTERN.matcher(trimmed).matches()) {
            return "Il nome può contenere solo lettere, numeri e underscore";
        }
        return null;
    }

    private void confirmName() {
        if (nameSubmitted) {
            return;
        }

        String chosenName = this.nameField.getValue().trim();

        // Validate name
        String validationError = getValidationError(chosenName);
        if (validationError != null) {
            showMessage(validationError, MessageType.ERROR, 60);
            return;
        }

        // Send packet to server
        PacketDistributor.sendToServer(new SubmitNamePacket(chosenName));

        // Mark as submitted
        nameSubmitted = true;
        this.confirmButton.active = false;
        this.nameField.setEditable(false);

        // Show waiting message and schedule close
        showMessage(Component.translatable("gui.tharidiathings.name_entry.status.submitting").getString(),
                MessageType.SUCCESS, 100);

        // Schedule close after ~1 second (20 ticks)
        this.closeDelayTicks = 20;
    }

    private void showMessage(String message, MessageType type, int durationTicks) {
        this.errorMessage = message;
        this.currentMessageType = type;
        this.errorTicksRemaining = durationTicks;
    }

    @Override
    public void tick() {
        super.tick();

        // Handle message timer (tick-based, not frame-based)
        if (this.errorTicksRemaining > 0) {
            this.errorTicksRemaining--;
            if (this.errorTicksRemaining == 0 && !nameSubmitted) {
                this.errorMessage = "";
            }
        }

        // Handle close delay (tick-based scheduling instead of Thread.sleep)
        if (this.closeDelayTicks > 0) {
            this.closeDelayTicks--;
            if (this.closeDelayTicks == 0) {
                this.onClose();
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render dark background
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Render panel
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

        // Render title
        Component titleText = Component.translatable("gui.tharidiathings.name_entry.title");
        int titleX = centerX - this.font.width(titleText) / 2;
        guiGraphics.drawString(this.font, titleText, titleX, panelY + 15, 0xFFFFFF, true);

        // Render instructions
        Component instructions = Component.translatable("gui.tharidiathings.name_entry.instructions");
        int instructionsX = centerX - this.font.width(instructions) / 2;
        guiGraphics.drawString(this.font, instructions, instructionsX, panelY + 35, 0xAAAAAA, false);

        // Render widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render message if present
        if (!this.errorMessage.isEmpty() && this.errorTicksRemaining > 0) {
            // Strip color codes for display (use currentMessageType for color)
            String displayMessage = this.errorMessage.replaceAll("§[0-9a-fk-or]", "");
            Component message = Component.literal(displayMessage);
            int messageX = centerX - this.font.width(message) / 2;
            guiGraphics.drawString(this.font, message, messageX, panelY + 95, currentMessageType.color, true);
        }

        // Show validation hint
        String currentText = this.nameField.getValue();
        if (!currentText.isEmpty() && !isValidName(currentText)) {
            String hint = getValidationError(currentText);
            if (hint != null) {
                Component hintComponent = Component.literal(hint);
                int hintX = centerX - this.font.width(hintComponent) / 2;
                guiGraphics.drawString(this.font, hintComponent, hintX, panelY + 105, 0xFFAA00, false);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle Enter key to confirm
        if (keyCode == 257 && this.confirmButton.active && !nameSubmitted) {
            this.confirmName();
            return true;
        }
        // Prevent ESC from closing the screen
        if (keyCode == 256) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
