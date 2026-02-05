package com.THproject.tharidia_things.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MasterMenuScreen extends Screen {
    public MasterMenuScreen() {
        super(Component.literal("Master Menu"));
    }

    @Override
    protected void init() {
        super.init();
        // Add buttons or widgets here later
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw the title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
