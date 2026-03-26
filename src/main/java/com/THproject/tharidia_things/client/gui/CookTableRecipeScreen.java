package com.THproject.tharidia_things.client.gui;

import com.THproject.tharidia_things.client.CookTableHudOverlay;
import com.THproject.tharidia_things.client.gui.medieval.MedievalGuiRenderer;
import com.THproject.tharidia_things.network.OpenCookRecipePacket;
import com.THproject.tharidia_things.network.StartCookingPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Recipe book screen for the Cook Table.
 * Shows all food-producing recipes auto-discovered from the game,
 * with item icon, translated item name, and cook time.
 */
public class CookTableRecipeScreen extends Screen {

    private final BlockPos blockPos;
    private final List<OpenCookRecipePacket.RecipeData> recipes;
    private String activeRecipeId;
    private int timerTicks;
    private final int totalTimerTicks;

    // Layout constants
    private static final int BOX_WIDTH   = 220;
    private static final int BOX_HEIGHT  = 230;
    private static final int ENTRY_H     = 24;
    private static final int PADDING     = 10;
    private static final int ICON_SIZE   = 16;
    private static final int VISIBLE_ROWS = 7;

    private int selectedIndex = -1;
    private int scrollOffset  = 0;

    public CookTableRecipeScreen(BlockPos blockPos,
                                  List<OpenCookRecipePacket.RecipeData> recipes,
                                  String activeRecipeId, int timerTicks, int totalTimerTicks) {
        super(Component.literal("Ricettario del Cuoco"));
        this.blockPos        = blockPos;
        this.recipes         = recipes;
        this.activeRecipeId  = activeRecipeId;
        this.timerTicks      = timerTicks;
        this.totalTimerTicks = totalTimerTicks;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        int startX = (width  - BOX_WIDTH)  / 2;
        int startY = (height - BOX_HEIGHT) / 2;

        // Shadow + parchment background
        g.fill(startX + 2, startY + 2, startX + BOX_WIDTH + 2, startY + BOX_HEIGHT + 2,
                MedievalGuiRenderer.SHADOW_DARK);
        MedievalGuiRenderer.renderParchmentBackground(g, startX, startY, BOX_WIDTH, BOX_HEIGHT);
        g.renderOutline(startX, startY, BOX_WIDTH, BOX_HEIGHT, MedievalGuiRenderer.BRONZE);

        // Title
        Component title = Component.literal("Ricettario del Cuoco")
                .withStyle(s -> s.withFont(MedievalGuiRenderer.MEDIEVAL_FONT));
        int titleX = startX + (BOX_WIDTH - font.width(title)) / 2;
        g.drawString(font, title, titleX, startY + PADDING, MedievalGuiRenderer.GOLD_LEAF, false);

        // Separator
        int sepY = startY + PADDING + font.lineHeight + 4;
        g.fill(startX + PADDING, sepY, startX + BOX_WIDTH - PADDING, sepY + 1, MedievalGuiRenderer.BRONZE);

        int contentY = sepY + 4;

        // Timer bar (if session active)
        if (!activeRecipeId.isEmpty() && timerTicks > 0) {
            float secs = timerTicks / 20.0f;
            int timerColor = secs <= 5  ? MedievalGuiRenderer.DEEP_CRIMSON
                           : secs <= 15 ? MedievalGuiRenderer.PURPLE_REGAL
                           : MedievalGuiRenderer.BRONZE;
            String timerStr = String.format("⏱ %.1fs", secs);
            Component timerComp = Component.literal(timerStr)
                    .withStyle(s -> s.withFont(MedievalGuiRenderer.MEDIEVAL_FONT));
            g.drawString(font, timerComp, startX + PADDING, contentY, timerColor, false);
            contentY += font.lineHeight + 4;

            int barW = BOX_WIDTH - PADDING * 2;
            float progress = totalTimerTicks > 0 ? (float) timerTicks / totalTimerTicks : 0f;
            g.fill(startX + PADDING, contentY, startX + PADDING + barW, contentY + 4, 0xFF333333);
            g.fill(startX + PADDING, contentY, startX + PADDING + (int)(barW * progress), contentY + 4, timerColor);
            contentY += 8;
        }

        // Recipe list
        int listStartY = contentY + 2;
        int visibleEnd = Math.min(scrollOffset + VISIBLE_ROWS, recipes.size());

        for (int i = scrollOffset; i < visibleEnd; i++) {
            OpenCookRecipePacket.RecipeData recipe = recipes.get(i);
            int entryY = listStartY + (i - scrollOffset) * ENTRY_H;

            boolean isSelected = (i == selectedIndex);
            boolean isActive   = recipe.recipeId().equals(activeRecipeId);

            // Entry background
            int bgColor = isActive   ? 0x6000AA44
                        : isSelected ? 0x60AA8800
                        : (i % 2 == 0 ? 0x20000000 : 0x10000000);
            g.fill(startX + PADDING, entryY,
                   startX + BOX_WIDTH - PADDING, entryY + ENTRY_H - 2, bgColor);

            // Item icon (16×16)
            ItemStack stack = recipe.result();
            int iconX = startX + PADDING + 2;
            int iconY = entryY + (ENTRY_H - 2 - ICON_SIZE) / 2;
            g.renderItem(stack, iconX, iconY);

            // Item name (translated) + time
            String displayName = stack.getHoverName().getString();
            String timeLabel   = " (" + (recipe.timeTicks() / 20) + "s)";
            Component entry = Component.literal(displayName + timeLabel)
                    .withStyle(s -> s.withFont(MedievalGuiRenderer.MEDIEVAL_FONT));

            int textColor = isActive ? MedievalGuiRenderer.BRONZE : MedievalGuiRenderer.BROWN_INK;
            int textX = iconX + ICON_SIZE + 3;
            int textY = entryY + (ENTRY_H - 2 - font.lineHeight) / 2;

            g.pose().pushPose();
            g.pose().scale(0.85f, 0.85f, 1f);
            g.drawString(font, entry,
                    (int)(textX / 0.85f),
                    (int)(textY / 0.85f),
                    textColor, false);
            g.pose().popPose();
        }

        // Scroll indicators
        if (scrollOffset > 0) {
            g.drawString(font, "▲", startX + BOX_WIDTH - PADDING - 6,
                    listStartY - 10, MedievalGuiRenderer.BRONZE, false);
        }
        if (scrollOffset + VISIBLE_ROWS < recipes.size()) {
            g.drawString(font, "▼", startX + BOX_WIDTH - PADDING - 6,
                    listStartY + VISIBLE_ROWS * ENTRY_H, MedievalGuiRenderer.BRONZE, false);
        }

        // "Inizia cottura" button
        if (selectedIndex >= 0 && activeRecipeId.isEmpty()) {
            int btnY = startY + BOX_HEIGHT - PADDING - 16;
            int btnX = startX + (BOX_WIDTH - 88) / 2;
            boolean hovered = mouseX >= btnX && mouseX <= btnX + 88
                           && mouseY >= btnY && mouseY <= btnY + 14;
            g.fill(btnX, btnY, btnX + 88, btnY + 14, hovered ? 0xFFAA8800 : 0xFF886600);
            g.renderOutline(btnX, btnY, 88, 14, MedievalGuiRenderer.GOLD_LEAF);
            Component btnText = Component.literal("Inizia cottura")
                    .withStyle(s -> s.withFont(MedievalGuiRenderer.MEDIEVAL_FONT));
            g.pose().pushPose();
            g.pose().scale(0.8f, 0.8f, 1f);
            g.drawString(font, btnText,
                    (int)((btnX + 6) / 0.8f), (int)((btnY + 3) / 0.8f),
                    MedievalGuiRenderer.GOLD_LEAF, false);
            g.pose().popPose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xA0000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startX = (width  - BOX_WIDTH)  / 2;
        int startY = (height - BOX_HEIGHT) / 2;

        int contentY = startY + PADDING + font.lineHeight + 4 + 4;
        if (!activeRecipeId.isEmpty() && timerTicks > 0) {
            contentY += font.lineHeight + 4 + 8;
        }
        int listStartY = contentY + 2;

        // Recipe entry click
        for (int i = scrollOffset; i < Math.min(scrollOffset + VISIBLE_ROWS, recipes.size()); i++) {
            int entryY = listStartY + (i - scrollOffset) * ENTRY_H;
            if (mouseX >= startX + PADDING && mouseX <= startX + BOX_WIDTH - PADDING
                    && mouseY >= entryY && mouseY <= entryY + ENTRY_H - 2) {
                selectedIndex = i;
                net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
        }

        // Confirm button
        if (selectedIndex >= 0 && activeRecipeId.isEmpty()) {
            int btnY = startY + BOX_HEIGHT - PADDING - 16;
            int btnX = startX + (BOX_WIDTH - 88) / 2;
            if (mouseX >= btnX && mouseX <= btnX + 88 && mouseY >= btnY && mouseY <= btnY + 14) {
                String recipeId = recipes.get(selectedIndex).recipeId();
                PacketDistributor.sendToServer(new StartCookingPacket(blockPos, recipeId));
                // Track the active cook table so the HUD stays visible during the session
                CookTableHudOverlay.activeCookTablePos        = blockPos;
                CookTableHudOverlay.cookingRequestTime        = System.currentTimeMillis();
                CookTableHudOverlay.cookingSessionConfirmed   = false;
                onClose();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0) {
            scrollOffset = Math.min(scrollOffset + 1, Math.max(0, recipes.size() - VISIBLE_ROWS));
        } else {
            scrollOffset = Math.max(0, scrollOffset - 1);
        }
        return true;
    }
}
