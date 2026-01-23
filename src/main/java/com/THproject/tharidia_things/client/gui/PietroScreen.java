package com.THproject.tharidia_things.client.gui;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import com.THproject.tharidia_things.client.ClientPacketHandler;
import com.THproject.tharidia_things.client.gui.medieval.MedievalGuiRenderer;
import com.THproject.tharidia_things.client.gui.medieval.MedievalButton;
import com.THproject.tharidia_things.client.gui.components.ImageTabButton;
import com.THproject.tharidia_things.client.gui.components.ImageProgressBar;
import com.THproject.tharidia_things.dungeon_query.DungeonQueryInstance;
import com.THproject.tharidia_things.gui.PietroMenu;
import com.THproject.tharidia_things.gui.inventory.PlayerInventoryPanelLayout;
import com.THproject.tharidia_things.network.JoinGroupQueuePacket;
import com.THproject.tharidia_things.network.LeaveGroupQueuePacket;
import com.THproject.tharidia_things.network.StartGroupDungeonPacket;
import com.THproject.tharidia_things.client.ClientGroupQueueHandler;
import com.THproject.tharidia_things.realm.HierarchyRank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.*;

/**
 * Medieval-styled realm management screen with PNG-based textures
 */
public class PietroScreen extends AbstractContainerScreen<PietroMenu> {
    // PNG Texture Resources
    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/gui/realm_background.png");
    private static final ResourceLocation INVENTORY_PANEL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/gui/inventory_panel.png");
    private static final ResourceLocation BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/gui/bar.png");
    private static final ResourceLocation EXPANSION_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/gui/expansion_button.png");
    private static final ResourceLocation EXPANSION_BUTTON_PRESSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/gui/expansion_button_pressed.png");
    private static final ResourceLocation CLAIMS_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/gui/claims_button.png");
    private static final ResourceLocation CLAIMS_BUTTON_PRESSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/gui/claims_button_pressed.png");
    private static final ResourceLocation DUNGEON_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/gui/dungeon_button.png");
    private static final ResourceLocation DUNGEON_BUTTON_PRESSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/gui/dungeon_button_pressed.png");
    private static final ResourceLocation ENTER_LARGE_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/gui/enter_large_button.png");

    // Enter large button texture dimensions
    private static final int ENTER_BTN_TEX_WIDTH = 650;
    private static final int ENTER_BTN_TEX_HEIGHT = 110;

    // Medieval font for GUI text
    private static final ResourceLocation MEDIEVAL_FONT =
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "medieval");

    // PNG Texture dimensions (original sizes)
    private static final int BG_TEX_WIDTH = 1024;
    private static final int BG_TEX_HEIGHT = 1536;
    private static final int INV_TEX_WIDTH = 340;
    private static final int INV_TEX_HEIGHT = 280;
    private static final int BAR_TEX_WIDTH = 754;
    private static final int BAR_TEX_HEIGHT = 70;

    // Tab identifiers
    private static final int TAB_EXPANSION = 0;
    private static final int TAB_CLAIMS = 1;
    private static final int TAB_DUNGEON = 2;

    // GUI display dimensions (scaled from texture)
    private static final int PARCHMENT_WIDTH = 256;  // Display width
    private static final int PARCHMENT_HEIGHT = 384; // Display height (maintains aspect ratio)
    private static final int BORDER_WIDTH = 20;

    private int currentTab = TAB_EXPANSION;
    private ImageTabButton expansionTabButton;
    private ImageTabButton claimsTabButton;
    private ImageTabButton dungeonTabButton;
    private List<MedievalButton> hierarchyButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_PLAYERS = 6;
    private UUID selectedPlayerForRankChange = null;
    private boolean showRankSelectionMenu = false;
    private int rankMenuX = 0;
    private int rankMenuY = 0;
    private ImageTabButton enterDungeonButtonGroup;
    private ImageProgressBar expansionProgressBar;

    // Group queue page state
    private boolean showGroupQueuePage = false;
    private MedievalButton exitGroupButton;
    private ImageTabButton startGroupButton;

    /**
     * Gets the Pietro block position for the particle system.
     * Uses the synced client data from ClientGroupQueueHandler.
     */
    public static BlockPos getActiveGroupQueueBlockPos() {
        // Check all positions with active queues
        Set<BlockPos> activePositions = ClientGroupQueueHandler.getActiveQueuePositions();
        return activePositions.isEmpty() ? null : activePositions.iterator().next();
    }

    public static boolean hasPlayersInGroupQueue() {
        return !ClientGroupQueueHandler.getActiveQueuePositions().isEmpty();
    }

    public PietroScreen(PietroMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PARCHMENT_WIDTH;
        this.imageHeight = PARCHMENT_HEIGHT;

        // Set the current Pietro position in ClientGroupQueueHandler
        if (menu.getBlockEntity() != null) {
            ClientGroupQueueHandler.setCurrentPietroPos(menu.getBlockEntity().getBlockPos());
        }
    }

    @Override
    public void onClose() {
        // Clear the current Pietro position reference
        ClientGroupQueueHandler.clearCurrentPietroPos();
        super.onClose();
    }

    @Override
    protected void init() {
        super.init();

        // Tab button positions matching the white paper areas in realm_background.png
        // The white torn paper areas are below the coins icon, around y=85-110 in display
        int tabY = this.topPos + 102;  // Y position matching the white paper areas
        int tabHeight = 26;           // Display height for tab buttons
        int tabSpacing = 8;

        // Tab button widths (matching the white paper areas proportionally)
        int expWidth = 62;
        int claimsWidth = 70;
        int dungeonWidth = 62;
        int totalTabWidth = expWidth + claimsWidth + dungeonWidth + (tabSpacing * 2);

        // Center the tabs horizontally in the GUI
        int tabStartX = this.leftPos + (PARCHMENT_WIDTH - totalTabWidth) / 2;

        // Create PNG-based tab buttons with pressed textures
        // Expansion button (left tab)
        int expX = tabStartX;
        expansionTabButton = ImageTabButton.builder(EXPANSION_BUTTON_TEXTURE, EXPANSION_BUTTON_PRESSED_TEXTURE, 254, 99,
                button -> switchTab(TAB_EXPANSION))
                .bounds(expX, tabY, expWidth, tabHeight)
                .setActive(true)
                .build();

        // Claims button (middle tab)
        int claimsX = expX + expWidth + tabSpacing;
        claimsTabButton = ImageTabButton.builder(CLAIMS_BUTTON_TEXTURE, CLAIMS_BUTTON_PRESSED_TEXTURE, 268, 104,
                button -> switchTab(TAB_CLAIMS))
                .bounds(claimsX, tabY, claimsWidth, tabHeight)
                .build();

        // Dungeon button (right tab)
        int dungeonX = claimsX + claimsWidth + tabSpacing;
        dungeonTabButton = ImageTabButton.builder(DUNGEON_BUTTON_TEXTURE, DUNGEON_BUTTON_PRESSED_TEXTURE, 256, 102,
                button -> switchTab(TAB_DUNGEON))
                .bounds(dungeonX, tabY, dungeonWidth, tabHeight)
                .build();

        this.addRenderableWidget(expansionTabButton);
        this.addRenderableWidget(claimsTabButton);
        this.addRenderableWidget(dungeonTabButton);

        // Initialize expansion progress bar with PNG texture
        int barDisplayWidth = 190;  // Scaled display width for the bar
        int barDisplayHeight = 18;  // Scaled display height
        expansionProgressBar = new ImageProgressBar(
                BAR_TEXTURE,
                BORDER_WIDTH + 20,
                180,
                barDisplayWidth,
                barDisplayHeight);

        // Create Enter button using PNG texture (centered)
        int enterBtnWidth = 140;
        int enterBtnHeight = 25;
        enterDungeonButtonGroup = ImageTabButton.builder(ENTER_LARGE_BUTTON_TEXTURE, ENTER_BTN_TEX_WIDTH, ENTER_BTN_TEX_HEIGHT,
                button -> {
                    // Send packet to join/create group queue
                    if (this.menu.getBlockEntity() != null) {
                        BlockPos pos = this.menu.getBlockEntity().getBlockPos();
                        PacketDistributor.sendToServer(new JoinGroupQueuePacket(pos));
                        showGroupQueuePage = true;
                        updateButtonVisibility();
                    }
                })
                .bounds(this.leftPos + (PARCHMENT_WIDTH - enterBtnWidth) / 2, this.topPos + PARCHMENT_HEIGHT - 120, enterBtnWidth, enterBtnHeight)
                .build();
        enterDungeonButtonGroup.visible = false;
        this.addRenderableWidget(enterDungeonButtonGroup);

        // Create Exit button for group queue page - DANGER style for exit action
        exitGroupButton = MedievalButton.builder(
                Component.translatable("gui.tharidiathings.realm.dungeon.exit_button"),
                button -> {
                    // Send packet to leave group queue
                    if (this.menu.getBlockEntity() != null) {
                        BlockPos pos = this.menu.getBlockEntity().getBlockPos();
                        PacketDistributor.sendToServer(new LeaveGroupQueuePacket(pos));
                        showGroupQueuePage = false;
                        updateButtonVisibility();
                    }
                },
                MedievalButton.ButtonStyle.DANGER)
                .bounds(this.leftPos + BORDER_WIDTH + 20, this.topPos + PARCHMENT_HEIGHT - 40, 80, 25).build();
        exitGroupButton.visible = false;
        this.addRenderableWidget(exitGroupButton);

        // Create Start button for group queue page using PNG texture (only visible for leader)
        int startBtnWidth = 70;
        int startBtnHeight = 20;
        startGroupButton = ImageTabButton.builder(ENTER_LARGE_BUTTON_TEXTURE, ENTER_BTN_TEX_WIDTH, ENTER_BTN_TEX_HEIGHT,
                button -> {
                    // Send packet to start the dungeon for all players in queue
                    if (this.menu.getBlockEntity() != null) {
                        BlockPos pos = this.menu.getBlockEntity().getBlockPos();
                        PacketDistributor.sendToServer(new StartGroupDungeonPacket(pos));
                    }
                })
                .bounds(this.leftPos + (PARCHMENT_WIDTH - startBtnWidth) / 2, this.topPos + PARCHMENT_HEIGHT - 50, startBtnWidth, startBtnHeight)
                .build();
        startGroupButton.visible = false;
        this.addRenderableWidget(startGroupButton);

        updateTabButtons();
    }

    private void updateTabButtons() {
        expansionTabButton.setActive(currentTab == TAB_EXPANSION);
        claimsTabButton.setActive(currentTab == TAB_CLAIMS);
        dungeonTabButton.setActive(currentTab == TAB_DUNGEON);

        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        boolean onDungeonTab = (currentTab == TAB_DUNGEON);
        boolean onGroupPage = showGroupQueuePage && onDungeonTab;

        // Dungeon group button - show only on dungeon tab when NOT on group page
        if (enterDungeonButtonGroup != null) {
            enterDungeonButtonGroup.visible = onDungeonTab && !showGroupQueuePage;
        }

        // Group queue page buttons - show only when on group page
        if (exitGroupButton != null) {
            exitGroupButton.visible = onGroupPage;
        }

        // Start button - only visible for the leader
        if (startGroupButton != null) {
            boolean isLeader = false;
            if (this.menu.getBlockEntity() != null && Minecraft.getInstance().player != null) {
                BlockPos pos = this.menu.getBlockEntity().getBlockPos();
                UUID localPlayerUUID = Minecraft.getInstance().player.getUUID();
                isLeader = ClientGroupQueueHandler.isLocalPlayerLeader(pos, localPlayerUUID);
            }
            startGroupButton.visible = onGroupPage && isLeader;
        }
    }

    /**
     * Gets the current queue data from the server-synced client handler.
     */
    private ClientGroupQueueHandler.QueueData getCurrentQueueData() {
        if (this.menu.getBlockEntity() != null) {
            return ClientGroupQueueHandler.getQueueData(this.menu.getBlockEntity().getBlockPos());
        }
        return null;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Render PNG background texture (scaled to fit display dimensions)
        guiGraphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

        // Render player inventory panel using PNG texture (left side outside main GUI)
        int inventoryBgX = x + PlayerInventoryPanelLayout.PANEL_OFFSET_X;
        int inventoryBgY = y + PlayerInventoryPanelLayout.PANEL_OFFSET_Y;
        int panelWidth = PlayerInventoryPanelLayout.PANEL_WIDTH;
        int panelHeight = PlayerInventoryPanelLayout.PANEL_HEIGHT;

        // Render PNG inventory panel texture with scaling
        // blit(texture, x, y, destWidth, destHeight, srcX, srcY, srcWidth, srcHeight, texWidth, texHeight)
        guiGraphics.blit(INVENTORY_PANEL_TEXTURE, inventoryBgX, inventoryBgY, panelWidth, panelHeight,
                0, 0, INV_TEX_WIDTH, INV_TEX_HEIGHT, INV_TEX_WIDTH, INV_TEX_HEIGHT);
    }

    // Gap between main inventory and hotbar (in pixels)
    private static final int HOTBAR_GAP = 58;  // 54 (3 rows) + 4 pixel gap

    /**
     * Renders slot borders for the inventory panel
     */
    private void renderInventorySlotBorders(GuiGraphics gui, int slotStartX, int slotStartY) {
        // Main inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = slotStartX + col * 18;
                int slotY = slotStartY + row * 18;
                gui.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, MedievalGuiRenderer.BRONZE);
                gui.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF1A1A1A);
            }
        }
        // Hotbar (1 row, with small gap below main inventory)
        for (int col = 0; col < 9; col++) {
            int slotX = slotStartX + col * 18;
            int slotY = slotStartY + HOTBAR_GAP;
            gui.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, MedievalGuiRenderer.BRONZE);
            gui.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF1A1A1A);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // renderLabels uses relative coordinates (0,0 is top-left of GUI container)
        // Content area starts below the tab buttons (which are part of the PNG at y ~60)
        int contentStartY = 140; // Below the decorative divider in the PNG

        // Render content based on current tab
        if (currentTab == TAB_EXPANSION) {
            renderExpansionTab(guiGraphics);
        } else if (currentTab == TAB_CLAIMS) {
            renderClaimsTab(guiGraphics);
        } else if (currentTab == TAB_DUNGEON) {
            if (showGroupQueuePage) {
                renderGroupQueuePage(guiGraphics);
            } else {
                renderDungeonTab(guiGraphics);
            }
        }
    }

    // Dark text color for contrast on parchment - near black brown
    private static final int TEXT_DARK = 0xFF1A1208;  // Very dark brown, almost black

    private void renderExpansionTab(GuiGraphics guiGraphics) {
        PietroBlockEntity pietroEntity = this.menu.getBlockEntity();
        if (pietroEntity != null) {
            // Use relative coordinates (0,0 = top-left of GUI)
            // Content starts below the first PNG horizontal divider line (around y=140)
            int yPos = 160;
            int textX = BORDER_WIDTH + 15;

            // Lord information
            renderMedievalTextLine(guiGraphics,
                    Component.translatable("gui.tharidiathings.realm.lord_label").getString(), textX, yPos,
                    TEXT_DARK);
            yPos += 18;

            String owner = pietroEntity.getOwnerName();
            if (owner == null || owner.isEmpty()) {
                owner = Component.translatable("gui.tharidiathings.common.unknown").getString();
            }
            renderMedievalTextLine(guiGraphics, owner, textX + 15, yPos, TEXT_DARK);
            yPos += 40;

            // Realm size information (below second PNG divider line)
            int size = this.menu.getRealmSize();
            renderMedievalTextLine(guiGraphics,
                    Component.translatable("gui.tharidiathings.realm.size_label").getString(), textX, yPos,
                    TEXT_DARK);
            yPos += 18;
            renderMedievalTextLine(guiGraphics,
                    Component.translatable("gui.tharidiathings.realm.size_value", size).getString(), textX + 15, yPos,
                    TEXT_DARK);
            yPos += 35;

            // Expansion progress with progress bar
            if (size >= 15) {
                renderMedievalTextLine(guiGraphics,
                        Component.translatable("gui.tharidiathings.realm.max_level").getString(), textX, yPos,
                        TEXT_DARK);
            } else {
                renderMedievalTextLine(guiGraphics,
                        Component.translatable("gui.tharidiathings.realm.expansion_cost_label").getString(), textX,
                        yPos, TEXT_DARK);
                yPos += 22;

                int stored = this.menu.getStoredPotatoes();
                int required = pietroEntity.getPotatoCostForNextLevel();
                float progress = required > 0 ? (float) stored / required : 0f;

                // Update and render progress bar
                expansionProgressBar.position(BORDER_WIDTH + 15, yPos).setProgress(progress).showValueText(stored,
                        required, "");
                expansionProgressBar.render(guiGraphics);

                yPos += 22;
                int remaining = required - stored;
                renderMedievalTextLine(guiGraphics,
                        Component.translatable("gui.tharidiathings.realm.coins_needed", remaining).getString(), textX,
                        yPos, TEXT_DARK);
            }
        }
    }

    /**
     * Renders text with medieval font and dark color for authentic parchment appearance
     */
    private void renderMedievalTextLine(GuiGraphics gui, String text, int x, int y, int color) {
        // Use medieval font with specified color - no shadow for clean parchment look
        Component styledText = Component.literal(text).withStyle(style -> style.withFont(MEDIEVAL_FONT));
        gui.drawString(Minecraft.getInstance().font, styledText, x, y, color, false);
    }

    private void renderClaimsTab(GuiGraphics guiGraphics) {
        // Use relative coordinates (0,0 = top-left of GUI)
        // Content starts below the first PNG horizontal divider line
        int yPos = 160;
        int textX = BORDER_WIDTH + 15;

        // Title
        renderMedievalTextLine(guiGraphics, Component.translatable("gui.tharidiathings.realm.claims_title").getString(),
                textX, yPos, TEXT_DARK);
        yPos += 18;

        // Total coins
        int totalPotatoes = this.menu.getTotalClaimPotatoes();
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.treasury", totalPotatoes).getString(), textX, yPos,
                TEXT_DARK);
        yPos += 40;

        // Hierarchy section (below second PNG divider line)
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.hierarchy_title").getString(), textX, yPos,
                TEXT_DARK);
        yPos += 22;

        // Get hierarchy data from client cache
        Map<UUID, Integer> hierarchyData = ClientPacketHandler.getCachedHierarchyData();
        UUID ownerUUID = ClientPacketHandler.getCachedOwnerUUID();
        String ownerName = ClientPacketHandler.getCachedOwnerName();

        // Check if current player is the lord
        UUID currentPlayerUUID = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID()
                : null;
        boolean isLord = currentPlayerUUID != null && currentPlayerUUID.equals(ownerUUID);

        // Add lord to the list if available
        List<PlayerHierarchyEntry> entries = new ArrayList<>();
        if (ownerUUID != null) {
            entries.add(new PlayerHierarchyEntry(ownerUUID, ownerName, HierarchyRank.LORD.getLevel()));
        }

        // Add other vassals from hierarchy data
        for (Map.Entry<UUID, Integer> entry : hierarchyData.entrySet()) {
            String playerName = getPlayerNameFromUUID(entry.getKey());
            entries.add(new PlayerHierarchyEntry(entry.getKey(), playerName, entry.getValue()));
        }

        // Sort by rank level (descending)
        entries.sort((a, b) -> Integer.compare(b.rankLevel, a.rankLevel));

        // Display vassal list with medieval styling
        int displayStartIndex = scrollOffset;
        int displayEndIndex = Math.min(scrollOffset + MAX_VISIBLE_PLAYERS, entries.size());

        for (int i = displayStartIndex; i < displayEndIndex; i++) {
            PlayerHierarchyEntry entry = entries.get(i);
            HierarchyRank rank = HierarchyRank.fromLevel(entry.rankLevel);

            // Alternate background for medieval list effect
            boolean even = (i % 2 == 0);
            MedievalGuiRenderer.renderListItem(
                    guiGraphics,
                    textX - 5,
                    yPos - 2,
                    this.imageWidth - BORDER_WIDTH * 2 - 30,
                    18,
                    "",
                    even,
                    false);

            // Player name with dark styling
            String displayName = entry.playerName.length() > 15 ? entry.playerName.substring(0, 15) : entry.playerName;
            renderMedievalTextLine(guiGraphics, displayName, textX + 5, yPos + 4, TEXT_DARK);

            // Rank title
            renderMedievalTextLine(guiGraphics,
                    Component.translatable(getRankTranslationKey(rank)).getString(), textX + 120, yPos + 4,
                    TEXT_DARK);

            // Change rank button (only for lord and not for themselves)
            if (isLord && !entry.playerUUID.equals(ownerUUID)) {
                renderMedievalRankButton(guiGraphics, textX + 200, yPos, entry.playerUUID);
            }

            yPos += 20;
        }

        // Medieval scroll indicators
        if (scrollOffset > 0) {
            MedievalGuiRenderer.renderScrollIndicator(guiGraphics, textX + this.imageWidth - BORDER_WIDTH * 2 - 50, 160,
                    true);
        }
        if (displayEndIndex < entries.size()) {
            MedievalGuiRenderer.renderScrollIndicator(guiGraphics, textX + this.imageWidth - BORDER_WIDTH * 2 - 50, 250,
                    false);
        }

        // Render rank selection menu if open
        if (showRankSelectionMenu && selectedPlayerForRankChange != null) {
            renderRankSelectionMenu(guiGraphics);
        }
    }

    /**
     * Renders a medieval-styled rank change button
     */
    private void renderMedievalRankButton(GuiGraphics gui, int x, int y, UUID playerUUID) {
        MedievalGuiRenderer.renderMedievalButton(gui, x, y, 45, 16,
                Component.translatable("gui.tharidiathings.realm.rank_change").getString(), false, true);
    }

    private void renderDungeonTab(GuiGraphics guiGraphics) {
        // Use relative coordinates (0,0 = top-left of GUI)
        // Content starts below the first PNG horizontal divider line
        int yPos = 160;
        int textX = BORDER_WIDTH + 15;

        // Dungeon information
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.dungeon_enter_label").getString(), textX, yPos,
                TEXT_DARK);
        yPos += 20;

        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.dungeon_desc_1").getString(), textX, yPos,
                TEXT_DARK);
        yPos += 14;
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.dungeon_desc_2").getString(), textX, yPos,
                TEXT_DARK);
        yPos += 35;

        // Status information (below second PNG divider line)
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.queue_status_label").getString(), textX, yPos,
                TEXT_DARK);
        yPos += 18;
        renderMedievalTextLine(guiGraphics, Component.translatable("gui.tharidiathings.realm.queue_ready").getString(),
                textX + 15, yPos, TEXT_DARK);
    }

    private void renderGroupQueuePage(GuiGraphics guiGraphics) {
        // Content starts below the first PNG horizontal divider line
        int yPos = 145;
        int textX = BORDER_WIDTH + 15;

        // Title
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.group_queue_title").getString(), textX, yPos,
                TEXT_DARK);
        yPos += 35;

        // Circle parameters
        int circleRadius = 15;
        int circleSpacing = 45;
        int startX = (this.imageWidth - (5 * circleSpacing - (circleSpacing - circleRadius * 2))) / 2;

        // Medieval colors for queue circles
        int emptyCircleColor = MedievalGuiRenderer.LEATHER_DARK;
        int filledCircleColor = MedievalGuiRenderer.BRONZE;
        int emptyBorderColor = MedievalGuiRenderer.WOOD_DARK;
        int filledBorderColor = MedievalGuiRenderer.GOLD_MAIN;
        int highlightColor = MedievalGuiRenderer.GOLD_BRIGHT;

        // Get synced queue data from server
        ClientGroupQueueHandler.QueueData queueData = getCurrentQueueData();
        List<UUID> queuePlayers = queueData != null ? queueData.playerUUIDs : Collections.emptyList();
        int playerCount = queuePlayers.size();

        // Render 2 rows of 5 circles
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int index = row * 5 + col;
                int centerX = startX + col * circleSpacing + circleRadius;
                int centerY = yPos + row * (circleRadius * 2 + 20) + circleRadius;

                // Determine if this slot is filled (using synced data)
                boolean isFilled = (index < playerCount);

                // Render decorated circle
                renderQueueCircle(guiGraphics, centerX, centerY, circleRadius,
                        isFilled ? filledBorderColor : emptyBorderColor,
                        isFilled ? filledCircleColor : emptyCircleColor,
                        isFilled ? highlightColor : emptyBorderColor,
                        isFilled);
            }
        }

        // Player count indicator
        yPos += circleRadius * 4 + 50;
        String countText = Component.translatable("gui.tharidiathings.realm.group_count", playerCount, 10).getString();
        int countTextWidth = Minecraft.getInstance().font.width(countText);
        renderMedievalTextLine(guiGraphics, countText, (this.imageWidth - countTextWidth) / 2, yPos, TEXT_DARK);
    }

    /**
     * Renders a decorated medieval-style queue circle using efficient horizontal line fills
     */
    private void renderQueueCircle(GuiGraphics gui, int centerX, int centerY, int radius,
                                   int borderColor, int fillColor, int highlightColor, boolean isFilled) {
        int radiusSq = radius * radius;
        int outerRadiusSq = (radius + 2) * (radius + 2);

        // Draw circles using horizontal line spans (much more efficient than pixel-by-pixel)
        for (int dy = -radius - 2; dy <= radius + 2; dy++) {
            int dySq = dy * dy;

            // Calculate horizontal span for outer border
            int outerSpan = (int) Math.sqrt(outerRadiusSq - dySq);
            int innerSpan = (dySq <= radiusSq) ? (int) Math.sqrt(radiusSq - dySq) : -1;

            // Shadow (only for main circle area, offset by 2)
            if (innerSpan >= 0) {
                gui.fill(centerX - innerSpan + 2, centerY + dy + 2,
                        centerX + innerSpan + 3, centerY + dy + 3,
                        MedievalGuiRenderer.SHADOW_LIGHT);
            }

            // Border ring (left and right segments)
            if (innerSpan >= 0 && outerSpan > innerSpan) {
                // Left border segment
                gui.fill(centerX - outerSpan, centerY + dy,
                        centerX - innerSpan, centerY + dy + 1, borderColor);
                // Right border segment
                gui.fill(centerX + innerSpan + 1, centerY + dy,
                        centerX + outerSpan + 1, centerY + dy + 1, borderColor);
            } else if (innerSpan < 0 && dySq <= outerRadiusSq) {
                // Full border line (top/bottom of ring where no inner circle)
                gui.fill(centerX - outerSpan, centerY + dy,
                        centerX + outerSpan + 1, centerY + dy + 1, borderColor);
            }

            // Main circle fill
            if (innerSpan >= 0) {
                gui.fill(centerX - innerSpan, centerY + dy,
                        centerX + innerSpan + 1, centerY + dy + 1, fillColor);
            }
        }

        // Simple highlight for filled circles (just a few rectangles instead of pixel loops)
        if (isFilled) {
            // Top-left highlight area (simplified)
            int highlightAlpha = 50;
            int highlightWithAlpha = (highlightAlpha << 24) | (highlightColor & 0x00FFFFFF);
            gui.fill(centerX - radius + 4, centerY - radius + 4,
                    centerX - 2, centerY - 2, highlightWithAlpha);

            // Center gold dot
            gui.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, highlightColor);
        }
    }

    @Override
    protected void rebuildWidgets() {
        super.rebuildWidgets();

        // Clear existing buttons
        if (enterDungeonButtonGroup != null) {
            this.removeWidget(enterDungeonButtonGroup);
            enterDungeonButtonGroup = null;
        }
        if (exitGroupButton != null) {
            this.removeWidget(exitGroupButton);
            exitGroupButton = null;
        }
        if (startGroupButton != null) {
            this.removeWidget(startGroupButton);
            startGroupButton = null;
        }

        // Add PNG-based dungeon button only on dungeon tab (centered)
        if (currentTab == TAB_DUNGEON) {
            int enterBtnWidth = 140;
            int enterBtnHeight = 25;
            enterDungeonButtonGroup = ImageTabButton.builder(ENTER_LARGE_BUTTON_TEXTURE, ENTER_BTN_TEX_WIDTH, ENTER_BTN_TEX_HEIGHT,
                    button -> {
                        // Send packet to join/create group queue
                        if (this.menu.getBlockEntity() != null) {
                            BlockPos pos = this.menu.getBlockEntity().getBlockPos();
                            PacketDistributor.sendToServer(new JoinGroupQueuePacket(pos));
                            showGroupQueuePage = true;
                            updateButtonVisibility();
                        }
                    })
                    .bounds(this.leftPos + (PARCHMENT_WIDTH - enterBtnWidth) / 2, this.topPos + PARCHMENT_HEIGHT - 120, enterBtnWidth, enterBtnHeight)
                    .build();
            this.addRenderableWidget(enterDungeonButtonGroup);

            exitGroupButton = MedievalButton.builder(
                    Component.translatable("gui.tharidiathings.realm.dungeon.exit_button"),
                    button -> {
                        // Send packet to leave group queue
                        if (this.menu.getBlockEntity() != null) {
                            BlockPos pos = this.menu.getBlockEntity().getBlockPos();
                            PacketDistributor.sendToServer(new LeaveGroupQueuePacket(pos));
                            showGroupQueuePage = false;
                            updateButtonVisibility();
                        }
                    },
                    MedievalButton.ButtonStyle.DANGER)
                    .bounds(this.leftPos + BORDER_WIDTH + 20, this.topPos + PARCHMENT_HEIGHT - 40, 80, 25).build();
            this.addRenderableWidget(exitGroupButton);

            int startBtnWidth = 70;
            int startBtnHeight = 20;
            startGroupButton = ImageTabButton.builder(ENTER_LARGE_BUTTON_TEXTURE, ENTER_BTN_TEX_WIDTH, ENTER_BTN_TEX_HEIGHT,
                    button -> {
                        // Send packet to start the dungeon for all players in queue
                        if (this.menu.getBlockEntity() != null) {
                            BlockPos pos = this.menu.getBlockEntity().getBlockPos();
                            PacketDistributor.sendToServer(new StartGroupDungeonPacket(pos));
                        }
                    })
                    .bounds(this.leftPos + (PARCHMENT_WIDTH - startBtnWidth) / 2, this.topPos + PARCHMENT_HEIGHT - 50, startBtnWidth, startBtnHeight)
                    .build();
            this.addRenderableWidget(startGroupButton);

            updateButtonVisibility();
        }
    }

    private void renderRankSelectionMenu(GuiGraphics guiGraphics) {
        int menuWidth = 120;
        int menuHeight = 110;

        // Use stored menu position (set when button is clicked)
        int menuX = rankMenuX;
        int menuY = rankMenuY;

        // Medieval-styled popup menu
        MedievalGuiRenderer.renderParchmentBackground(guiGraphics, menuX, menuY, menuWidth, menuHeight);
        MedievalGuiRenderer.renderOrnateBorder(guiGraphics, menuX, menuY, menuWidth, menuHeight,
                MedievalGuiRenderer.BRONZE);

        // Title
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.rank_select_title").getString(), menuX + 10,
                menuY + 10, TEXT_DARK);

        // Rank options (except LORD)
        HierarchyRank[] selectableRanks = { HierarchyRank.CONSIGLIERE, HierarchyRank.GUARDIA, HierarchyRank.MILIZIANO,
                HierarchyRank.COLONO };
        int optionY = menuY + 30;

        for (HierarchyRank rank : selectableRanks) {
            // Highlight on hover
            int relMouseX = (int) (Minecraft.getInstance().mouseHandler.xpos()
                    * Minecraft.getInstance().getWindow().getGuiScaledWidth()
                    / Minecraft.getInstance().getWindow().getScreenWidth());
            int relMouseY = (int) (Minecraft.getInstance().mouseHandler.ypos()
                    * Minecraft.getInstance().getWindow().getGuiScaledHeight()
                    / Minecraft.getInstance().getWindow().getScreenHeight());

            if (relMouseX >= menuX && relMouseX <= menuX + menuWidth && relMouseY >= optionY
                    && relMouseY <= optionY + 18) {
                MedievalGuiRenderer.renderListItem(guiGraphics, menuX + 5, optionY - 2, menuWidth - 10, 18, "", false,
                        true);
            }

            renderMedievalTextLine(guiGraphics,
                    Component.translatable(getRankTranslationKey(rank)).getString(), menuX + 15,
                    optionY + 4, TEXT_DARK);
            optionY += 20;
        }
    }

    private void switchTab(int tab) {
        currentTab = tab;
        scrollOffset = 0;
        updateTabButtons();
    }

    private String getPlayerNameFromUUID(UUID uuid) {
        // Try to get player name from various sources
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(uuid)) {
            return Minecraft.getInstance().player.getName().getString();
        }

        // Check hierarchy cache
        Map<UUID, Integer> hierarchyData = ClientPacketHandler.getCachedHierarchyData();
        if (hierarchyData.containsKey(uuid)) {
            // This is a simplified approach - in a real implementation you'd want a proper
            // UUID->name mapping
            return Component.translatable("gui.tharidiathings.common.player_unknown", uuid.toString().substring(0, 8))
                    .getString();
        }

        return Component.translatable("gui.tharidiathings.common.unknown").getString();
    }

    private static String getRankTranslationKey(HierarchyRank rank) {
        return "gui.tharidiathings.realm.rank." + rank.name().toLowerCase(Locale.ROOT);
    }

    private static class PlayerHierarchyEntry {
        final UUID playerUUID;
        final String playerName;
        final int rankLevel;

        PlayerHierarchyEntry(UUID uuid, String name, int level) {
            this.playerUUID = uuid;
            this.playerName = name != null ? name
                    : Component.translatable("gui.tharidiathings.common.unknown").getString();
            this.rankLevel = level;
        }
    }
}
