package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.UUID;

@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class ReviveOverlay {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null)
            return;

        ReviveAttachments playerAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
        UUID revivingUUID = playerAttachments.getRevivingPlayer();

        if (revivingUUID != null) {
            Player fallen = player.level().getPlayerByUUID(revivingUUID);

            if (fallen != null) {
                ReviveAttachments fallenAttachments = fallen.getData(ReviveAttachments.REVIVE_DATA.get());

                if (fallenAttachments.isFallen()) {
                    int currentTick = fallenAttachments.getResTick();
                    int maxTick = ReviveAttachments.MAX_RES_TICK;

                    // Calculate progress (0.0 to 1.0)
                    // currentTick goes from 50 down to 0
                    // So progress = (50 - current) / 50
                    float progress = Math.max(0.0f, Math.min(1.0f, (float) (maxTick - currentTick) / maxTick));

                    // Render Progress Bar
                    renderReviveProgressBar(event.getGuiGraphics(), mc, progress);
                }
            }
        }
    }

    private static void renderReviveProgressBar(GuiGraphics guiGraphics, Minecraft mc, float progress) {
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        int barWidth = 100;
        int barHeight = 5;
        int x = (width - barWidth) / 2;
        int y = (height / 2) + 20; // Below crosshair

        // Background (Black with transparency)
        guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0x80000000);

        // Progress (Green)
        int progressWidth = (int) (barWidth * progress);
        guiGraphics.fill(x, y, x + progressWidth, y + barHeight, 0xFF00FF00); // Green

        // Text
        String text = "Reviving...";
        int textWidth = mc.font.width(text);
        guiGraphics.drawString(mc.font, text, (width - textWidth) / 2, y + barHeight + 4, 0xFFFFFF);
    }
}
