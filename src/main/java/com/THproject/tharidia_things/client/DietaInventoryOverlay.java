package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.client.gui.DietaScreen;
import com.THproject.tharidia_things.network.OpenArmorMenuPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Renders feature buttons tied to the Dieta screen on the player inventory.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class DietaInventoryOverlay {

    // Feature button layout
    private static final int FEATURE_SLOT_SIZE = 18;
    private static final int FEATURE_SLOT_SPACING = 4;
    private static final int HEAD_SLOT_X_OFFSET = 7;
    private static final int HEAD_SLOT_Y_OFFSET = 8;
    private static final int FEATURE_VERTICAL_GAP = 4;
    private static final int FEATURE_VERTICAL_OFFSET = 5;

    /**
     * Renders feature buttons on inventory screens
     */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        // Only render on player inventory screen
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        InventoryScreen screen = (InventoryScreen) event.getScreen();

        // Calculate position based on inventory screen position
        int guiLeft = (screen.width - 176) / 2; // Standard inventory width is 176
        int guiTop = (screen.height - 166) / 2; // Standard inventory height is 166

        // Mouse info for shading/press state
        Minecraft mcInstance = Minecraft.getInstance();
        double rawMouseX = mcInstance.mouseHandler.xpos() * screen.width / mcInstance.getWindow().getScreenWidth();
        double rawMouseY = mcInstance.mouseHandler.ypos() * screen.height / mcInstance.getWindow().getScreenHeight();
        boolean mouseDown = mcInstance.mouseHandler.isLeftPressed();

        // Draw feature buttons aligned with the player inventory
        renderFeatureButtons(guiGraphics, guiLeft, guiTop, rawMouseX, rawMouseY, mouseDown);
    }

    /**
     * Renders the three feature buttons above the armor/head slot column.
     */
    public static void renderFeatureButtons(GuiGraphics gui, int guiLeft, int guiTop, double mouseX, double mouseY,
            boolean mouseDown) {
        ItemStack[] icons = new ItemStack[] {
                new ItemStack(Items.BOOK),
                new ItemStack(Items.IRON_CHESTPLATE),
                new ItemStack(Items.COOKED_BEEF),
                new ItemStack(Items.IRON_SWORD)
        };

        for (int i = 0; i < icons.length; i++) {
            SlotBounds bounds = getFeatureButtonBounds(guiLeft, guiTop, i);
            boolean hovered = bounds.contains((int) mouseX, (int) mouseY);
            boolean pressed = mouseDown && hovered;
            renderSlotButton(gui, bounds.x(), bounds.y(), bounds.size(), icons[i], hovered, pressed);
        }
    }

    /**
     * Draws a vanilla-styled inventory slot button with the given icon.
     */
    private static void renderSlotButton(GuiGraphics gui, int x, int y, int size, ItemStack icon, boolean hovered,
            boolean pressed) {
        int backgroundColor = 0xE0C6C6C6;
        int topHighlight = 0xE0FFFFFF;
        int sideShadow = 0xE0555555;
        int innerColor = pressed ? 0xCC6E6E6E : hovered ? 0xCC9D9D9D : 0xCC8B8B8B;

        gui.fill(x, y, x + size, y + size, backgroundColor);
        gui.fill(x, y, x + size, y + 1, topHighlight);
        gui.fill(x, y, x + 1, y + size, topHighlight);
        gui.fill(x, y + size - 1, x + size, y + size, sideShadow);
        gui.fill(x + size - 1, y, x + size, y + size, sideShadow);
        gui.fill(x + 1, y + 1, x + size - 1, y + size - 1, innerColor);

        if (hovered && !pressed) {
            gui.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x30FFFFFF);
        } else if (pressed) {
            gui.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x40000000);
        }

        int iconOffset = pressed ? 2 : 1;
        gui.renderItem(icon, x + iconOffset, y + iconOffset);
    }

    /**
     * Handles mouse clicks to detect dieta button clicks
     */
    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        // Only handle on player inventory screen
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }

        // Check if it's a left click
        if (event.getButton() != 0) {
            return;
        }

        InventoryScreen screen = (InventoryScreen) event.getScreen();

        // Calculate position based on inventory screen position
        int guiLeft = (screen.width - 176) / 2;
        int guiTop = (screen.height - 166) / 2;

        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();

        SlotBounds dietaButton = getFeatureButtonBounds(guiLeft, guiTop, 2);

        if (dietaButton.contains(mouseX, mouseY)) {

            // Open dieta screen
            Minecraft.getInstance().setScreen(new DietaScreen(screen));
            event.setCanceled(true); // Prevent the click from propagating
        }

        // Calculate position based on inventory screen position
        int ArmorGuiLeft = (screen.width - 220) / 2;
        int ArmorGuiTop = (screen.height - 166) / 2;

        SlotBounds armorButton = getFeatureButtonBounds(ArmorGuiLeft, ArmorGuiTop, 2);

        if (armorButton.contains(mouseX, mouseY)) {

            // Open armor screen via server packet to ensure container is valid
            PacketDistributor.sendToServer(new OpenArmorMenuPacket());
            event.setCanceled(true); // Prevent the click from propagating
        }
    }

    private static SlotBounds getFeatureButtonBounds(int guiLeft, int guiTop, int index) {
        int startX = guiLeft + HEAD_SLOT_X_OFFSET;
        int y = guiTop + HEAD_SLOT_Y_OFFSET - FEATURE_SLOT_SIZE - FEATURE_VERTICAL_GAP - FEATURE_VERTICAL_OFFSET;
        int x = startX + index * (FEATURE_SLOT_SIZE + FEATURE_SLOT_SPACING);
        return new SlotBounds(x, y, FEATURE_SLOT_SIZE);
    }

    private record SlotBounds(int x, int y, int size) {
        boolean contains(int px, int py) {
            return px >= x && px <= x + size && py >= y && py <= y + size;
        }
    }
}
