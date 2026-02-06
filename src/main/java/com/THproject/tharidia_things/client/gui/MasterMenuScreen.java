package com.THproject.tharidia_things.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.THproject.tharidia_things.TharidiaThings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MasterMenuScreen extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "textures/gui/master_menu.png");
    private static final int IMAGE_WIDTH = 350;
    private static final int IMAGE_HEIGHT = 300;
    private static final File CONFIG_FILE = new File("th_masterMenu/commands.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Static storage to persist across screen open/close
    private static final Map<Page, List<CommandEntry>> SAVED_COMMANDS = new HashMap<>();

    static {
        // Initialize lists for all pages
        for (Page p : Page.values()) {
            SAVED_COMMANDS.put(p, new ArrayList<>());
        }
        loadCommands();
    }

    private enum Page {
        WALKERS,
        SKIN,
        EQUIP
    }

    private float scrollAmount = 0f;
    private final List<EntryRow> entryRows = new ArrayList<>();
    private static final int LIST_ITEM_HEIGHT = 22; // 20 button + 2 padding

    public static class CommandEntry {
        public String name;
        public String value;

        public CommandEntry(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    private static void loadCommands() {
        if (!CONFIG_FILE.exists())
            return;

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null || !json.has("values"))
                return;

            // Clear to avoid duplicates
            for (List<CommandEntry> list : SAVED_COMMANDS.values()) {
                list.clear();
            }

            JsonArray values = json.getAsJsonArray("values");
            for (JsonElement elem : values) {
                JsonObject obj = elem.getAsJsonObject();
                if (!obj.has("type") || !obj.has("name"))
                    continue;

                String typeStr = obj.get("type").getAsString();
                String name = obj.get("name").getAsString();
                String val = "";

                // Read 'value' field first (new format)
                if (obj.has("value")) {
                    val = obj.get("value").getAsString();
                } else if (obj.has("commands")) {
                    // Fallback or migration if needed, though we are changing logic completely
                    // For now, if no value, try to grab first command or empty
                    JsonArray cmds = obj.getAsJsonArray("commands");
                    if (cmds.size() > 0)
                        val = cmds.get(0).getAsString();
                }

                try {
                    Page p = Page.valueOf(typeStr.toUpperCase());
                    SAVED_COMMANDS.get(p).add(new CommandEntry(name, val));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid pages or format
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveCommands() {
        JsonObject root = new JsonObject();
        JsonArray values = new JsonArray();

        for (Map.Entry<Page, List<CommandEntry>> entry : SAVED_COMMANDS.entrySet()) {
            Page p = entry.getKey();
            for (CommandEntry cmdEntry : entry.getValue()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", p.name().toLowerCase());
                obj.addProperty("name", cmdEntry.name);
                obj.addProperty("value", cmdEntry.value);
                values.add(obj);
            }
        }
        root.add("values", values);

        File parent = CONFIG_FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Page currentPage = Page.WALKERS;

    public MasterMenuScreen() {
        super(Component.literal("Master Menu"));
    }

    @Override
    protected void init() {
        super.init();

        int leftPos = (this.width - IMAGE_WIDTH) / 2;
        int topPos = (this.height - IMAGE_HEIGHT) / 2;

        // 1. Navigation Buttons (Tabs) - Moved up to approx y=10 relative to bg
        int buttonWidth = 70;
        int buttonHeight = 20;
        int spacing = 10;
        int startX = leftPos + (IMAGE_WIDTH - (buttonWidth * 3 + spacing * 2)) / 2;
        int buttonsY = topPos + 20;

        this.addRenderableWidget(Button.builder(Component.literal("Walkers"), (button) -> {
            this.currentPage = Page.WALKERS;
            this.init(this.minecraft, this.width, this.height);
        }).bounds(startX, buttonsY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("Skin"), (button) -> {
            this.currentPage = Page.SKIN;
            this.init(this.minecraft, this.width, this.height);
        }).bounds(startX + buttonWidth + spacing, buttonsY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("Equip"), (button) -> {
            this.currentPage = Page.EQUIP;
            this.init(this.minecraft, this.width, this.height);
        }).bounds(startX + (buttonWidth + spacing) * 2, buttonsY, buttonWidth, buttonHeight).build());

        // 2. Control Buttons (Reset & Add) - Below Tabs
        int controlsY = buttonsY + buttonHeight + 5;

        // Add Button (Left)
        if (this.currentPage != Page.EQUIP) {
            this.addRenderableWidget(Button.builder(Component.literal("Add"), (btn) -> {
                this.minecraft.setScreen(new AddCommandScreen(this, this.currentPage));
            }).bounds(leftPos + 60, controlsY, 100, 20).build());
        }

        // Reset Button (Right)
        if (this.currentPage == Page.WALKERS) {
            this.addRenderableWidget(Button.builder(Component.literal("Reset Shape"), (btn) -> {
                this.runCommand("/walkers switchShape @p normal");
            }).bounds(leftPos + IMAGE_WIDTH - 160, controlsY, 100, 20).build());
        } else if (this.currentPage == Page.SKIN) {
            this.addRenderableWidget(Button.builder(Component.literal("Reset Skin"), (btn) -> {
                this.runCommand("/skinshifter reset @p");
            }).bounds(leftPos + IMAGE_WIDTH - 160, controlsY, 100, 20).build());
        }

        // 3. List Area
        this.entryRows.clear();
        if (this.currentPage != Page.EQUIP) {
            List<CommandEntry> commands = SAVED_COMMANDS.get(this.currentPage);
            for (int i = 0; i < commands.size(); i++) {
                this.entryRows.add(new EntryRow(commands.get(i), i));
            }
        }
    }

    private void runCommand(String command) {
        if (command == null || command.isEmpty())
            return;
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.sendCommand(command.startsWith("/") ? command.substring(1) : command);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int leftPos = (this.width - IMAGE_WIDTH) / 2;
        int topPos = (this.height - IMAGE_HEIGHT) / 2;

        guiGraphics.blit(BACKGROUND_TEXTURE, leftPos, topPos, 0.0F, 0.0F, IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH,
                IMAGE_HEIGHT);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw the title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, topPos + 6, 0x404040);

        // 4. Render Scrollable List
        if (this.currentPage != Page.EQUIP) {
            int listTop = topPos + 75; // Matches calculation from init (controlsY + 25 + 5 based on padding)
            int listBottom = topPos + IMAGE_HEIGHT - 10;
            int listHeight = listBottom - listTop;
            int fullContentHeight = this.entryRows.size() * LIST_ITEM_HEIGHT;

            // Adjust scroll
            this.scrollAmount = Mth.clamp(this.scrollAmount, 0.0f, Math.max(0, fullContentHeight - listHeight));

            // Scissor
            guiGraphics.enableScissor(leftPos, listTop, leftPos + IMAGE_WIDTH, listBottom);

            for (EntryRow row : this.entryRows) {
                int rowY = listTop + (row.index * LIST_ITEM_HEIGHT) - (int) this.scrollAmount;
                // Optimization: only render if visible
                if (rowY + LIST_ITEM_HEIGHT > listTop && rowY < listBottom) {
                    row.updateY(rowY, leftPos);
                    row.render(guiGraphics, mouseX, mouseY, partialTick);
                }
            }

            guiGraphics.disableScissor();

            // Render Scrollbar
            if (fullContentHeight > listHeight) {
                int barX = leftPos + IMAGE_WIDTH - 8;
                int barWidth = 6;
                int barHeight = (int) ((float) (listHeight * listHeight) / fullContentHeight);
                barHeight = Math.max(32, barHeight);
                int barY = listTop
                        + (int) ((listHeight - barHeight) * (this.scrollAmount / (fullContentHeight - listHeight)));

                guiGraphics.fill(barX, listTop, barX + barWidth, listBottom, 0xFF000000); // Track
                guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF808080); // Thumb
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.currentPage != Page.EQUIP && !this.entryRows.isEmpty()) {
            this.scrollAmount -= (float) (scrollY * LIST_ITEM_HEIGHT / 2);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.currentPage != Page.EQUIP) {
            int leftPos = (this.width - IMAGE_WIDTH) / 2;
            int topPos = (this.height - IMAGE_HEIGHT) / 2;
            int listTop = topPos + 75;
            int listBottom = topPos + IMAGE_HEIGHT - 10;

            if (mouseY >= listTop && mouseY <= listBottom && mouseX >= leftPos && mouseX <= leftPos + IMAGE_WIDTH) {
                for (EntryRow row : this.entryRows) {
                    if (row.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private class EntryRow {
        final int index;
        final Button deleteBtn;
        final Button editBtn;
        final Button actionBtn;

        EntryRow(CommandEntry entry, int index) {
            this.index = index;
            int cmdButtonHeight = 20;

            this.deleteBtn = Button.builder(Component.literal("X"), (btn) -> {
                SAVED_COMMANDS.get(currentPage).remove(index);
                saveCommands();
                init(minecraft, width, height);
            }).bounds(0, 0, 20, cmdButtonHeight)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Delete")))
                    .build();

            this.editBtn = Button.builder(Component.literal("E"), (btn) -> {
                minecraft.setScreen(new AddCommandScreen(MasterMenuScreen.this, currentPage, entry));
            }).bounds(0, 0, 20, cmdButtonHeight)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Edit")))
                    .build();

            int actionBtnWidth = IMAGE_WIDTH - 170;
            this.actionBtn = Button.builder(Component.literal(entry.name), (btn) -> {
                if (currentPage == Page.WALKERS) {
                    runCommand("/walkers switchShape @p " + entry.value);
                } else if (currentPage == Page.SKIN) {
                    runCommand("/skinshifter set @p " + entry.value);
                }
            }).bounds(0, 0, actionBtnWidth, cmdButtonHeight)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(entry.value)))
                    .build();
        }

        void updateY(int y, int leftPos) {
            this.deleteBtn.setY(y);
            this.deleteBtn.setX(leftPos + 60);

            this.editBtn.setY(y);
            this.editBtn.setX(leftPos + 85);

            this.actionBtn.setY(y);
            this.actionBtn.setX(leftPos + 110);
        }

        void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            this.deleteBtn.render(gui, mouseX, mouseY, partialTick);
            this.editBtn.render(gui, mouseX, mouseY, partialTick);
            this.actionBtn.render(gui, mouseX, mouseY, partialTick);
        }

        boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.deleteBtn.mouseClicked(mouseX, mouseY, button))
                return true;
            if (this.editBtn.mouseClicked(mouseX, mouseY, button))
                return true;
            if (this.actionBtn.mouseClicked(mouseX, mouseY, button))
                return true;
            return false;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    class AddCommandScreen extends Screen {
        private final Screen parent;
        private final Page targetPage;
        private final CommandEntry entryToEdit;
        private EditBox nameEdit;
        private EditBox valueEdit;

        public AddCommandScreen(Screen parent, Page page) {
            this(parent, page, null);
        }

        public AddCommandScreen(Screen parent, Page page, CommandEntry entryToEdit) {
            super(Component.literal(entryToEdit == null ? "Add Command" : "Edit Command"));
            this.parent = parent;
            this.targetPage = page;
            this.entryToEdit = entryToEdit;
        }

        @Override
        protected void init() {
            super.init();
            int leftPos = (this.width - 200) / 2;
            int topPos = (this.height - 150) / 2;

            this.nameEdit = new EditBox(this.font, leftPos, topPos + 20, 200, 20, Component.literal("Name"));
            this.nameEdit.setMaxLength(32);
            this.nameEdit.setHint(Component.literal("Enter name..."));
            if (this.entryToEdit != null)
                this.nameEdit.setValue(this.entryToEdit.name);
            this.addRenderableWidget(this.nameEdit);

            this.valueEdit = new EditBox(this.font, leftPos, topPos + 50, 200, 20, Component.literal("Value"));
            this.valueEdit.setMaxLength(256);
            if (this.targetPage == Page.WALKERS) {
                this.valueEdit.setHint(Component.literal("Entity ID (e.g. minecraft:zombie)"));
            } else if (this.targetPage == Page.SKIN) {
                this.valueEdit.setHint(Component.literal("Player Name"));
            }
            if (this.entryToEdit != null)
                this.valueEdit.setValue(this.entryToEdit.value);
            this.addRenderableWidget(this.valueEdit);

            this.addRenderableWidget(Button.builder(Component.literal("Save"), (btn) -> {
                String name = this.nameEdit.getValue();
                String val = this.valueEdit.getValue();
                if (!name.isEmpty() && !val.isEmpty()) {
                    List<CommandEntry> list = MasterMenuScreen.SAVED_COMMANDS.get(this.targetPage);
                    if (this.entryToEdit != null) {
                        // Update existing object
                        this.entryToEdit.name = name;
                        this.entryToEdit.value = val;
                    } else {
                        // Add new
                        list.add(new CommandEntry(name, val));
                    }
                    MasterMenuScreen.saveCommands();
                    this.minecraft.setScreen(this.parent);
                }
            }).bounds(leftPos, topPos + 80, 95, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Cancel"), (btn) -> {
                this.minecraft.setScreen(this.parent);
            }).bounds(leftPos + 105, topPos + 80, 95, 20).build());
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            String title = (this.entryToEdit == null ? "Add " : "Edit ") +
                    (this.targetPage == Page.WALKERS ? "Walker" : this.targetPage == Page.SKIN ? "Skin" : "Item");

            guiGraphics.drawCenteredString(this.font, title, this.width / 2, (this.height - 150) / 2, 0xFFFFFF);
            guiGraphics.drawString(this.font, "Name", (this.width - 200) / 2, (this.height - 150) / 2 + 10, 0xA0A0A0);

            String label = "Value";
            if (this.targetPage == Page.WALKERS)
                label = "Entity ID";
            else if (this.targetPage == Page.SKIN)
                label = "Player Name";
            guiGraphics.drawString(this.font, label, (this.width - 200) / 2, (this.height - 150) / 2 + 40, 0xA0A0A0);

            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
