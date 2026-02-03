package com.THproject.tharidia_things.client.screen;

import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import com.THproject.tharidia_things.network.revive.ReviveGiveUpPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ReviveScreen extends Screen {

    public ReviveScreen() {
        super(Component.translatable("gui.tharidia_things.revive.title"));
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 100;
        int buttonHeight = 20;
        int barY = this.height / 2 + 10;
        if (Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.getData(ReviveAttachments.REVIVE_DATA.get()).canRevive()) {
            this.addRenderableWidget(Button.builder(Component.literal("Arrenditi"), button -> {
                PacketDistributor.sendToServer(new ReviveGiveUpPacket());
                this.minecraft.setScreen(null);
            })
                    .pos(this.width / 2 - buttonWidth / 2, barY + 30)
                    .size(buttonWidth, buttonHeight)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Darken background
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (Minecraft.getInstance().player != null
                && !Minecraft.getInstance().player.getData(ReviveAttachments.REVIVE_DATA.get()).canRevive()) {
            return;
        }

        // Draw additional overlay
        guiGraphics.fill(0, 0, this.width, this.height, 0xCC000000);

        int width = this.width;
        int height = this.height;

        // Title text
        Component title = Component.literal("SEI A TERRA");
        float titleScale = 2.0f;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(titleScale, titleScale, 1.0f);
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (int) ((width / 2.0f / titleScale) - (titleWidth / 2.0f)),
                (int) ((height / 2.0f / titleScale) - 20), 0xFFFFFF);
        guiGraphics.pose().popPose();

        // Get player data for progress bar
        var player = Minecraft.getInstance().player;
        if (player != null) {
            var reviveData = player.getData(ReviveAttachments.REVIVE_DATA);
            if (reviveData != null) {
                // Progress Bar
                int barWidth = 182;
                int barHeight = 5;
                int barX = (width - barWidth) / 2;
                int barY = height / 2 + 10;

                int maxTime = ReviveAttachments.MAX_FALLEN_TICK;
                int currentTime = reviveData.getTimeFallen();

                float progress = Math.max(0.0f, Math.min(1.0f, (float) currentTime / maxTime));
                int filledWidth = (int) (barWidth * progress);

                // Draw background of bar (Dark Gray)
                guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);

                // Draw filled part (Red)
                guiGraphics.fill(barX, barY, barX + filledWidth, barY + barHeight, 0xFFCC0000);

                // Reviving status display
                if (reviveData.getRevivingPlayer() != null) {
                    Component revivingText = Component.literal("Qualcuno ti sta rianimando...");
                    int revTextWidth = this.font.width(revivingText);
                    guiGraphics.drawString(this.font, revivingText, (width - revTextWidth) / 2, barY + 15, 0x55FF55);
                }
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
