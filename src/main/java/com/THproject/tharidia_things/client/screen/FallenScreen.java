package com.THproject.tharidia_things.client.screen;

import com.THproject.tharidia_things.client.ReviveProgressHudOverlay;
import com.THproject.tharidia_things.network.GiveUpPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class FallenScreen extends Screen {

    public FallenScreen() {
        super(Component.translatable("gui.tharidia_things.fallen_title"));
    }

    @Override
    protected void init() {
        super.init();

        // Add "Give Up" button
        this.addRenderableWidget(Button.builder(Component.literal("Give Up"), button -> {
            PacketDistributor.sendToServer(new GiveUpPacket());
            this.onClose();
        })
                .bounds(this.width / 2 - 50, this.height / 2 + 50, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw background
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render Progress Bar (using static fields from ReviveProgressHudOverlay)
        if (ReviveProgressHudOverlay.currentResTime >= 0 && ReviveProgressHudOverlay.maxResTime > 0) {
            int barWidth = 182;
            int barHeight = 5;
            int x = (this.width - barWidth) / 2;
            int y = (this.height / 2) + 20;

            // Background (Black)
            guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF000000);

            float progress = 1.0f
                    - ((float) ReviveProgressHudOverlay.currentResTime / (float) ReviveProgressHudOverlay.maxResTime);
            int filledWidth = (int) (barWidth * progress);

            if (filledWidth < 0)
                filledWidth = 0;
            if (filledWidth > barWidth)
                filledWidth = barWidth;

            // Foreground (Green)
            guiGraphics.fill(x, y, x + filledWidth, y + barHeight, 0xFF00FF00);

            // Text
            if (!ReviveProgressHudOverlay.text.isEmpty()) {
                int textWidth = this.font.width(ReviveProgressHudOverlay.text);
                guiGraphics.drawString(this.font, ReviveProgressHudOverlay.text, (this.width - textWidth) / 2,
                        y + barHeight + 2, 0xFFFFFFFF, true);
            }
        }
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
