package com.THproject.tharidia_things.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * A progress bar that uses a PNG texture image as the background.
 * The progress fill is rendered inside the bar area.
 */
public class ImageProgressBar {

    private final ResourceLocation texture;
    private int x, y;
    private int displayWidth;
    private int displayHeight;
    private float progress = 0f;
    private int currentValue = 0;
    private int maxValue = 0;
    private String suffix = "";

    // Bar fill area margins (inside the texture frame)
    private static final int FILL_MARGIN_LEFT = 8;
    private static final int FILL_MARGIN_RIGHT = 8;
    private static final int FILL_MARGIN_TOP = 6;
    private static final int FILL_MARGIN_BOTTOM = 6;

    // Fill colors
    private static final int FILL_COLOR = 0xFFDAA520;  // Gold
    private static final int FILL_GRADIENT_TOP = 0xFFFFD700;  // Bright gold
    private static final int FILL_GRADIENT_BOTTOM = 0xFFB8860B;  // Dark gold

    public ImageProgressBar(ResourceLocation texture, int x, int y, int displayWidth, int displayHeight) {
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
    }

    public ImageProgressBar position(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public ImageProgressBar setProgress(float progress) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        return this;
    }

    public ImageProgressBar showValueText(int current, int max, String suffix) {
        this.currentValue = current;
        this.maxValue = max;
        this.suffix = suffix;
        return this;
    }

    public void render(GuiGraphics gui) {
        // Render the background texture (bar frame)
        gui.blit(texture, x, y, 0, 0, displayWidth, displayHeight, displayWidth, displayHeight);

        // Calculate fill area
        int fillAreaWidth = displayWidth - FILL_MARGIN_LEFT - FILL_MARGIN_RIGHT;
        int fillAreaHeight = displayHeight - FILL_MARGIN_TOP - FILL_MARGIN_BOTTOM;
        int fillX = x + FILL_MARGIN_LEFT;
        int fillY = y + FILL_MARGIN_TOP;

        // Calculate actual fill width based on progress
        int fillWidth = (int) (fillAreaWidth * progress);

        if (fillWidth > 0) {
            // Render fill with gradient effect
            for (int j = 0; j < fillAreaHeight; j++) {
                float ratio = (float) j / fillAreaHeight;
                int r = (int) ((FILL_GRADIENT_TOP >> 16 & 0xFF) * (1 - ratio) + (FILL_GRADIENT_BOTTOM >> 16 & 0xFF) * ratio);
                int g = (int) ((FILL_GRADIENT_TOP >> 8 & 0xFF) * (1 - ratio) + (FILL_GRADIENT_BOTTOM >> 8 & 0xFF) * ratio);
                int b = (int) ((FILL_GRADIENT_TOP & 0xFF) * (1 - ratio) + (FILL_GRADIENT_BOTTOM & 0xFF) * ratio);
                int color = 0xFF000000 | (r << 16) | (g << 8) | b;
                gui.fill(fillX, fillY + j, fillX + fillWidth, fillY + j + 1, color);
            }
        }

        // Render value text centered on bar
        if (maxValue > 0) {
            String text = currentValue + "/" + maxValue + suffix;
            int textWidth = Minecraft.getInstance().font.width(text);
            int textX = x + (displayWidth - textWidth) / 2;
            int textY = y + (displayHeight - 9) / 2;
            gui.drawString(Minecraft.getInstance().font, text, textX, textY, 0xFFFFFFFF, true);
        }
    }
}
