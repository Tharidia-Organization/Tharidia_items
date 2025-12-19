package com.THproject.tharidia_things.client.gui.components;

import com.THproject.tharidia_things.client.gui.medieval.MedievalGuiRenderer;
import com.THproject.tharidia_things.gui.inventory.PlayerInventoryPanelLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Renders the shared medieval-styled player inventory panel used by multiple GUIs.
 */
public final class PlayerInventoryPanelRenderer {
    private PlayerInventoryPanelRenderer() {
    }

    public static void renderPanel(GuiGraphics gui, int panelX, int panelY, int slotStartX, int slotStartY) {
        // Panel background
        gui.fill(panelX, panelY, panelX + PlayerInventoryPanelLayout.PANEL_WIDTH,
            panelY + PlayerInventoryPanelLayout.PANEL_HEIGHT, MedievalGuiRenderer.WOOD_DARK);
        gui.renderOutline(panelX, panelY, PlayerInventoryPanelLayout.PANEL_WIDTH,
            PlayerInventoryPanelLayout.PANEL_HEIGHT, MedievalGuiRenderer.BRONZE);

        // Title
        gui.drawString(Minecraft.getInstance().font, "§6Inventario", panelX + 50, panelY + 5,
            MedievalGuiRenderer.ROYAL_GOLD);

        // Slot borders
        renderSlotBorders(gui, slotStartX, slotStartY, 3);
        renderSlotBorders(gui, slotStartX, slotStartY + 78, 1);
    }

    private static void renderSlotBorders(GuiGraphics gui, int startX, int startY, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = startX + col * 18;
                int slotY = startY + row * 18;

                gui.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, MedievalGuiRenderer.BRONZE);
                gui.fill(slotX, slotY, slotX + 16, slotY + 16, MedievalGuiRenderer.WOOD_DARK);
                gui.fill(slotX + 1, slotY + 1, slotX + 15, slotY + 15, MedievalGuiRenderer.BLACK_INK);
            }
        }
    }
}
