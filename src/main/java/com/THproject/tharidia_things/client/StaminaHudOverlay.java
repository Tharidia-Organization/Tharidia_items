package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.stamina.StaminaAttachments;
import com.THproject.tharidia_things.stamina.StaminaData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

public class StaminaHudOverlay implements LayeredDraw.Layer {
    private static final int XP_BAR_WIDTH = 182;
    private static final int XP_BAR_HEIGHT = 5;
    private static final int XP_BAR_Y_OFFSET = 32;
    private static final int XP_BAR_Y_INSET = 3;
    private static final int COMBAT_DOT_SIZE = 3;
    private static final int COMBAT_DOT_X_OFFSET = 5;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }

        StaminaData data = player.getData(StaminaAttachments.STAMINA_DATA);
        if (!data.isInCombat()) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int x = (screenWidth - XP_BAR_WIDTH) / 2;
        int y = screenHeight - XP_BAR_Y_OFFSET + XP_BAR_Y_INSET;
        
        int dotX = x + XP_BAR_WIDTH + COMBAT_DOT_X_OFFSET;
        int dotY = y + (XP_BAR_HEIGHT - COMBAT_DOT_SIZE) / 2;
        guiGraphics.fill(dotX, dotY, dotX + COMBAT_DOT_SIZE, dotY + COMBAT_DOT_SIZE, 0xFFFF3333);
    }
}

