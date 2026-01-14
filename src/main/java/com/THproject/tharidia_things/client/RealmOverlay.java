package com.THproject.tharidia_things.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;


        // Only render if player is in a realm
        if (!RealmClientHandler.isPlayerInRealm()) {
            return;
        }

        Font font = mc.font;

        // Get screen dimensions
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        // Get owner name and create text
        String ownerName = RealmClientHandler.getCurrentRealmOwner();
        String text;
        if (!ownerName.isEmpty()) {
            text = "Regno di " + ownerName;
        } else {
            text = "Sei a Casa";
        }

        // Position at bottom right corner with some padding
        int textWidth = font.width(text);
        int flagWidth = textWidth + 20; // Add padding for the dot and spacing
        int flagHeight = 20;
        int padding = 5;
        int x = screenWidth - flagWidth - padding;
        int y = screenHeight - flagHeight - padding;

        // Draw background box with semi-transparent dark background
        guiGraphics.fill(x, y, x + flagWidth, y + flagHeight, 0x88000000);

        // Draw border
        // Top border
        guiGraphics.fill(x, y, x + flagWidth, y + 1, 0xFFFFAA00);
        // Bottom border
        guiGraphics.fill(x, y + flagHeight - 1, x + flagWidth, y + flagHeight, 0xFFFFAA00);
        // Left border
        guiGraphics.fill(x, y, x + 1, y + flagHeight, 0xFFFFAA00);
        // Right border
        guiGraphics.fill(x + flagWidth - 1, y, x + flagWidth, y + flagHeight, 0xFFFFAA00);

        // Optional: Add a small animated element (pulsing effect)
        float time = (System.currentTimeMillis() % 2000) / 2000.0f;
        float pulse = (float) (Math.sin(time * Math.PI * 2) * 0.3 + 0.7);
        int pulseColor = (int) (pulse * 255) << 24 | 0xFFAA00;

        // Draw a small indicator dot
        int dotSize = 4;
        int dotX = x + 5;
        int dotY = y + flagHeight / 2 - dotSize / 2;
        guiGraphics.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, pulseColor);

        // Draw text after the dot
        int textX = x + 12; // Position after the dot
        int textY = y + (flagHeight / 2) - (font.lineHeight / 2); // Vertically center

        // Enable blending for nice text rendering
        RenderSystem.enableBlend();
        guiGraphics.drawString(font, text, textX, textY, 0xFFAA00, false);
        RenderSystem.disableBlend();
    }
}
