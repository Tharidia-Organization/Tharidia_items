package com.tharidia.tharidia_things.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.gui.ComponentSelectionMenu;
import com.tharidia.tharidia_things.network.SelectComponentPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for selecting smithing components
 */
public class ComponentSelectionScreen extends AbstractContainerScreen<ComponentSelectionMenu> {
    
    private static final int INTERACTION_DELAY_TICKS = 20; // 1 second delay
    private int ticksOpen = 0;
    private Button lamaLungaButton;
    private Button lamaCortaButton;
    private Button elsaButton;
    
    public ComponentSelectionScreen(ComponentSelectionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170; // Increased height for 3 buttons
    }
    
    @Override
    protected void init() {
        super.init();
        this.ticksOpen = 0;
        
        int buttonX = leftPos + 20;
        int buttonY = topPos + 40;
        int buttonWidth = 136;
        int buttonHeight = 24;
        int spacing = 30;
        
        boolean isGoldAnvil = menu.isGoldAnvil();
        
        // Button for Lama Lunga
        lamaLungaButton = Button.builder(
            Component.translatable("gui.tharidiathings.component.lama_lunga"),
            button -> selectComponent("lama_lunga")
        ).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build();
        lamaLungaButton.active = false; // Disabled initially
        this.addRenderableWidget(lamaLungaButton);
        
        // Button for Lama Corta
        lamaCortaButton = Button.builder(
            Component.translatable("gui.tharidiathings.component.lama_corta"),
            button -> selectComponent("lama_corta")
        ).bounds(buttonX, buttonY + spacing, buttonWidth, buttonHeight).build();
        lamaCortaButton.active = false; // Disabled initially
        this.addRenderableWidget(lamaCortaButton);
        
        // Button for Elsa (not available for gold)
        if (!isGoldAnvil) {
            elsaButton = Button.builder(
                Component.translatable("gui.tharidiathings.component.elsa"),
                button -> selectComponent("elsa")
            ).bounds(buttonX, buttonY + spacing * 2, buttonWidth, buttonHeight).build();
            elsaButton.active = false; // Disabled initially
            this.addRenderableWidget(elsaButton);
        }
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        ticksOpen++;
        
        // Enable buttons after delay
        if (ticksOpen >= INTERACTION_DELAY_TICKS) {
            if (lamaLungaButton != null) lamaLungaButton.active = true;
            if (lamaCortaButton != null) lamaCortaButton.active = true;
            // elsaButton can be null if this is a gold anvil
            if (elsaButton != null) elsaButton.active = true;
        }
    }
    
    private void selectComponent(String componentId) {
        // Only allow selection after delay
        if (ticksOpen < INTERACTION_DELAY_TICKS) {
            return;
        }
        
        // Send packet to server
        PacketDistributor.sendToServer(new SelectComponentPacket(menu.getPos(), componentId));
        
        // Close the screen
        this.onClose();
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Render semi-transparent background
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xC0101010);
        
        // Render border
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, 0xFF8B8B8B);
        guiGraphics.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, 0xFF373737);
        guiGraphics.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF373737);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render title centered
        Component title = Component.translatable("gui.tharidiathings.component_selection");
        int titleX = (imageWidth - font.width(title)) / 2;
        guiGraphics.drawString(font, title, titleX, 10, 0xFFFFFF, false);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Block ALL mouse clicks during delay period
        if (ticksOpen < INTERACTION_DELAY_TICKS) {
            return true; // Consume the click without processing
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Block key presses during delay (like ESC to close)
        if (ticksOpen < INTERACTION_DELAY_TICKS) {
            return true; // Consume the key press
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void removed() {
        super.removed();
    }
}
