package com.THproject.tharidia_things.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

public class ReviveProgressHudOverlay implements LayeredDraw.Layer {

    public static int currentResTime = -1;
    public static int maxResTime = -1;
    public static String text = "";
    public static long lastUpdateTime = 0;

    public static void reset() {
        currentResTime = -1;
        maxResTime = -1;
        text = "";
    }

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        // Hide if data is stale (over 1s old)
        if (System.currentTimeMillis() - lastUpdateTime > 1000) {
            reset();
        }

        if (currentResTime < 0 || maxResTime <= 0) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Position: Just below crosshair
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = (screenHeight / 2) + 20;

        // Background (Black)
        guiGraphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xFF000000);

        // Progress Calculation
        float progress = 1.0f - ((float) currentResTime / (float) maxResTime);
        int filledWidth = (int) (BAR_WIDTH * progress);

        if (filledWidth < 0)
            filledWidth = 0;
        if (filledWidth > BAR_WIDTH)
            filledWidth = BAR_WIDTH;

        // Foreground (Green)
        guiGraphics.fill(x, y, x + filledWidth, y + BAR_HEIGHT, 0xFF00FF00);

        // Text
        int textWidth = mc.font.width(text);
        guiGraphics.drawString(mc.font, text, (screenWidth - textWidth) / 2, y + BAR_HEIGHT + 2, 0xFFFFFFFF, true);
    }
}
