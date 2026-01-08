package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.stamina.StaminaAttachments;
import com.THproject.tharidia_things.stamina.StaminaData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class StaminaHudOverlay implements LayeredDraw.Layer {
    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 10;
    private static final int TOP_MARGIN = 4;
    private static final int DOT_GAP = 6;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }

        StaminaData data = player.getData(StaminaAttachments.STAMINA_DATA);
        float current = data.getCurrentStamina();
        float max = data.getMaxStamina();
        boolean inCombat = data.isInCombat();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = TOP_MARGIN;

        int bgColor = 0xAA000000;
        int borderColor = 0xFFFFFFFF;

        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, bgColor);
        drawBorder(guiGraphics, x, y, BAR_WIDTH, BAR_HEIGHT, borderColor);

        float denom = max <= 0.0f ? 1.0f : max;
        float ratio = Mth.clamp(current / denom, 0.0f, 1.0f);
        int filled = Math.max(0, Math.min(BAR_WIDTH - 2, Math.round(ratio * (BAR_WIDTH - 2))));
        int fillColor = 0xFF3AA0FF;
        guiGraphics.fill(x + 1, y + 1, x + 1 + filled, y + BAR_HEIGHT - 1, fillColor);

        int curInt = Math.round(current);
        int maxInt = Math.round(max);
        String text = maxInt > 0 ? (curInt + "/" + maxInt) : String.valueOf(curInt);
        int textX = x + (BAR_WIDTH - mc.font.width(text)) / 2;
        int textY = y + (BAR_HEIGHT - mc.font.lineHeight) / 2;
        guiGraphics.drawString(mc.font, text, textX + 1, textY + 1, 0xFF000000, false);
        guiGraphics.drawString(mc.font, text, textX, textY, 0xFFFFFFFF, false);

        String dot = "●";
        int dotColor = inCombat ? 0xFFFF3333 : 0xFF33FF66;
        int dotX = x + BAR_WIDTH + DOT_GAP;
        int dotY = y + (BAR_HEIGHT - mc.font.lineHeight) / 2;
        guiGraphics.drawString(mc.font, dot, dotX, dotY, dotColor, false);
    }

    private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y, x + 1, y + height, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}

