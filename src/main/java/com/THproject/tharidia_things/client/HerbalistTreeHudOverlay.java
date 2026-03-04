package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.herbalist.herbalist_tree.HerbalistTreeBlock;
import com.THproject.tharidia_things.block.herbalist.herbalist_tree.HerbalistTreeBlockEntity;
import com.THproject.tharidia_things.block.herbalist.herbalist_tree.HerbalistTreeDummyBlock;
import com.THproject.tharidia_things.client.gui.medieval.MedievalGuiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class HerbalistTreeHudOverlay {

    private static final long UPDATE_INTERVAL_MS = 2000;
    private static final double RAYCAST_DISTANCE = 8.0;

    private static HerbalistTreeBlockEntity cachedTree = null;
    private static long lastUpdateTime = 0;
    private static List<LineData> cachedLines = new ArrayList<>();

    private enum LineType {
        TITLE, HEADER, NORMAL, GOOD, WARNING, DANGER, GRAY, EMPTY
    }

    private record LineData(LineType type, Component text) {}

    private static final int COLOR_TITLE = MedievalGuiRenderer.GOLD_LEAF;
    private static final int COLOR_HEADER = MedievalGuiRenderer.ROYAL_GOLD;
    private static final int COLOR_NORMAL = MedievalGuiRenderer.BROWN_INK;
    private static final int COLOR_GOOD = MedievalGuiRenderer.BRONZE;
    private static final int COLOR_WARNING = MedievalGuiRenderer.PURPLE_REGAL;
    private static final int COLOR_DANGER = MedievalGuiRenderer.DEEP_CRIMSON;
    private static final int COLOR_GRAY = MedievalGuiRenderer.BROWN_INK;

    private static final float BODY_SCALE = 0.75f;
    private static final int BODY_LINE_SPACING = 2;

    private static final int IND_GOOD = 0x55AA55;
    private static final int IND_OK = 0x8B6914;
    private static final int IND_WARNING = 0xCC8800;
    private static final int IND_DANGER = 0xCC2222;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.level == null || mc.options.hideGui) return;
        if (mc.screen != null) return;

        HerbalistTreeBlockEntity tree = getTargetedTree(mc, player);

        if (tree == null) {
            cachedTree = null;
            cachedLines.clear();
            return;
        }

        long now = System.currentTimeMillis();
        // Update every frame during step 3 for smooth timer, 500ms during other steps, 2s otherwise
        long interval = tree.isCrafting() ? (tree.getStep() == 3 ? 50 : 500) : UPDATE_INTERVAL_MS;
        if (tree != cachedTree || now - lastUpdateTime >= interval) {
            cachedTree = tree;
            lastUpdateTime = now;
            cachedLines = buildInfoLines(tree);
        }

        if (!cachedLines.isEmpty()) {
            renderOverlay(event.getGuiGraphics(), mc, cachedLines);
        }
    }

    private static HerbalistTreeBlockEntity getTargetedTree(Minecraft mc, LocalPlayer player) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(RAYCAST_DISTANCE));

        BlockHitResult hitResult = mc.level.clip(new ClipContext(
            eyePos, endPos,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player
        ));

        if (hitResult.getType() != HitResult.Type.BLOCK) return null;

        BlockPos hitPos = hitResult.getBlockPos();
        BlockState state = mc.level.getBlockState(hitPos);

        if (state.getBlock() instanceof HerbalistTreeBlock) {
            BlockEntity be = mc.level.getBlockEntity(hitPos);
            if (be instanceof HerbalistTreeBlockEntity tree) return tree;
        } else if (state.getBlock() instanceof HerbalistTreeDummyBlock) {
            int offsetY = state.getValue(HerbalistTreeDummyBlock.OFFSET_Y);
            BlockPos masterPos = hitPos.below(offsetY);
            BlockEntity be = mc.level.getBlockEntity(masterPos);
            if (be instanceof HerbalistTreeBlockEntity tree) return tree;
        }

        return null;
    }

    private static List<LineData> buildInfoLines(HerbalistTreeBlockEntity tree) {
        List<LineData> lines = new ArrayList<>();

        // Title
        lines.add(new LineData(LineType.TITLE,
                Component.translatable("gui.tharidiathings.herbalist_tree.title")));

        // HP section
        lines.add(new LineData(LineType.HEADER,
                Component.translatable("gui.tharidiathings.herbalist_tree.status")));

        int hp = tree.getTreeHp();
        int hpStatus = tree.getHpStatus();
        LineType hpType;
        String hpKey;
        if (hpStatus == 0) { hpType = LineType.GOOD; hpKey = "gui.tharidiathings.herbalist_tree.hp.healthy"; }
        else if (hpStatus == 1) { hpType = LineType.WARNING; hpKey = "gui.tharidiathings.herbalist_tree.hp.weak"; }
        else if (hpStatus == 2) { hpType = LineType.DANGER; hpKey = "gui.tharidiathings.herbalist_tree.hp.critical"; }
        else { hpType = LineType.DANGER; hpKey = "gui.tharidiathings.herbalist_tree.hp.dead"; }

        lines.add(new LineData(hpType, Component.translatable("gui.tharidiathings.herbalist_tree.hp", hp)
                .append(" - ").append(Component.translatable(hpKey))));

        // Fame (hunger)
        int fameStatus = tree.getFameStatus();
        LineType fameType;
        String fameKey;
        if (fameStatus == 0) { fameType = LineType.GOOD; fameKey = "gui.tharidiathings.herbalist_tree.fame.sated"; }
        else if (fameStatus == 1) { fameType = LineType.WARNING; fameKey = "gui.tharidiathings.herbalist_tree.fame.hungry"; }
        else if (fameStatus == 2) { fameType = LineType.WARNING; fameKey = "gui.tharidiathings.herbalist_tree.fame.very_hungry"; }
        else { fameType = LineType.DANGER; fameKey = "gui.tharidiathings.herbalist_tree.fame.starving"; }

        lines.add(new LineData(fameType, Component.translatable("gui.tharidiathings.herbalist_tree.fame")
                .append(": ").append(Component.translatable(fameKey))));

        // Sete (thirst)
        int seteStatus = tree.getSeteStatus();
        LineType seteType;
        String seteKey;
        if (seteStatus == 0) { seteType = LineType.GOOD; seteKey = "gui.tharidiathings.herbalist_tree.sete.hydrated"; }
        else if (seteStatus == 1) { seteType = LineType.WARNING; seteKey = "gui.tharidiathings.herbalist_tree.sete.thirsty"; }
        else { seteType = LineType.DANGER; seteKey = "gui.tharidiathings.herbalist_tree.sete.dehydrated"; }

        lines.add(new LineData(seteType, Component.translatable("gui.tharidiathings.herbalist_tree.sete")
                .append(": ").append(Component.translatable(seteKey))));

        // Minigame section (if active)
        if (tree.isCrafting()) {
            lines.add(new LineData(LineType.EMPTY, Component.empty()));
            lines.add(new LineData(LineType.HEADER,
                    Component.translatable("gui.tharidiathings.herbalist_tree.minigame")));

            int step = tree.getStep();
            String stepKey = switch (step) {
                case -1 -> "gui.tharidiathings.herbalist_tree.step.awakening";
                case 0 -> "gui.tharidiathings.herbalist_tree.step.tree_plays";
                case 1 -> "gui.tharidiathings.herbalist_tree.step.pots_correct";
                case 2 -> "gui.tharidiathings.herbalist_tree.step.pots_mix";
                case 3 -> "gui.tharidiathings.herbalist_tree.step.waiting";
                default -> "gui.tharidiathings.herbalist_tree.step.unknown";
            };
            lines.add(new LineData(LineType.NORMAL, Component.translatable(stepKey)));

            // Filled pots progress
            int filled = tree.getFilledPotsCount();
            lines.add(new LineData(LineType.NORMAL,
                    Component.translatable("gui.tharidiathings.herbalist_tree.pots_filled", filled, 8)));

            // Errors
            int errors = tree.getErrorCount();
            LineType errorType = errors >= 5 ? LineType.DANGER : errors >= 3 ? LineType.WARNING : LineType.NORMAL;
            lines.add(new LineData(errorType,
                    Component.translatable("gui.tharidiathings.herbalist_tree.errors", errors, 6)));

            // Timer
            if (step == 3) {
                int timerTicks = tree.getStepTimer();
                float seconds = Math.max(0, timerTicks) / 20.0f;
                String timeStr;
                if (seconds > 2.0f) {
                    timeStr = String.valueOf((int) seconds);
                } else {
                    timeStr = String.format("%.1f", seconds);
                }
                LineType timerType = seconds <= 3.0f ? LineType.DANGER : seconds <= 5.0f ? LineType.WARNING : LineType.NORMAL;
                lines.add(new LineData(timerType,
                        Component.translatable("gui.tharidiathings.herbalist_tree.timer", timeStr)));
            } else {
                lines.add(new LineData(LineType.GRAY,
                        Component.translatable("gui.tharidiathings.herbalist_tree.timer", "---")));
            }
        } else if (tree.isMinigameComplete()) {
            lines.add(new LineData(LineType.EMPTY, Component.empty()));
            lines.add(new LineData(LineType.GOOD,
                    Component.translatable("gui.tharidiathings.herbalist_tree.complete")));
        }

        // Hints
        if (tree.isTreeDead()) {
            lines.add(new LineData(LineType.EMPTY, Component.empty()));
            lines.add(new LineData(LineType.DANGER,
                    Component.translatable("gui.tharidiathings.herbalist_tree.hint.dead")));
        } else if (!tree.isCrafting() && !tree.isMinigameComplete()) {
            if (!tree.hasAllPots()) {
                lines.add(new LineData(LineType.EMPTY, Component.empty()));
                lines.add(new LineData(LineType.NORMAL, Component.literal("\u25B8 ")
                        .append(Component.translatable("gui.tharidiathings.herbalist_tree.hint.place_pots"))));
            }
        }

        return lines;
    }

    // ==================== RENDERING (same as StableHudOverlay) ====================

    private static void renderOverlay(GuiGraphics guiGraphics, Minecraft mc, List<LineData> lines) {
        int padding = 6;
        int lineHeight = 9;
        int maxWidth = 0;

        for (LineData line : lines) {
            if (line.type() == LineType.EMPTY) continue;
            Component styledLine = line.text().copy().withStyle(style -> style.withFont(MedievalGuiRenderer.MEDIEVAL_FONT));
            int width = mc.font.width(styledLine);
            if (line.type() != LineType.TITLE && line.type() != LineType.HEADER) {
                width = Math.round(width * BODY_SCALE);
            }
            if (width > maxWidth) maxWidth = width;
        }

        int boxWidth = maxWidth + padding * 2;
        int boxHeight = measureHeight(mc, lines, lineHeight) + padding * 2;

        int startX = 10;
        int startY = 10;

        guiGraphics.fill(startX + 2, startY + 2, startX + boxWidth + 2, startY + boxHeight + 2,
                MedievalGuiRenderer.SHADOW_DARK);
        MedievalGuiRenderer.renderParchmentBackground(guiGraphics, startX, startY, boxWidth, boxHeight);
        guiGraphics.renderOutline(startX, startY, boxWidth, boxHeight, MedievalGuiRenderer.BRONZE);
        guiGraphics.renderOutline(startX + 2, startY + 2, boxWidth - 4, boxHeight - 4,
            ((MedievalGuiRenderer.BRONZE & 0x00FFFFFF) | 0x40000000));

        int scaledLineHeight = Math.round(lineHeight * BODY_SCALE) + BODY_LINE_SPACING;
        int y = startY + padding;
        for (LineData line : lines) {
            if (line.type() == LineType.EMPTY) {
                y += 3;
                continue;
            }

            int color = getColorForType(line.type());
            int x = startX + padding;
            Component styledText = line.text().copy().withStyle(style -> style.withFont(MedievalGuiRenderer.MEDIEVAL_FONT));

            if (line.type() == LineType.TITLE) {
                int titleWidth = mc.font.width(styledText);
                int titleX = startX + (boxWidth - titleWidth) / 2;
                guiGraphics.drawString(mc.font, styledText, titleX, y, color, false);
                y += mc.font.lineHeight + 2;
                continue;
            }

            if (line.type() == LineType.HEADER) {
                guiGraphics.drawString(mc.font, styledText, x, y, color, false);
                y += lineHeight;
            } else {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(BODY_SCALE, BODY_SCALE, 1.0f);
                guiGraphics.drawString(mc.font, styledText,
                    Math.round(x / BODY_SCALE), Math.round(y / BODY_SCALE), color, false);
                guiGraphics.pose().popPose();
                y += scaledLineHeight;
            }
        }
    }

    private static int measureHeight(Minecraft mc, List<LineData> lines, int lineHeight) {
        int scaledLineHeight = Math.round(lineHeight * BODY_SCALE) + BODY_LINE_SPACING;
        int height = 0;
        for (LineData line : lines) {
            if (line.type() == LineType.EMPTY) {
                height += 3;
            } else if (line.type() == LineType.TITLE) {
                height += mc.font.lineHeight + 2;
            } else if (line.type() == LineType.HEADER) {
                height += lineHeight;
            } else {
                height += scaledLineHeight;
            }
        }
        return height;
    }

    private static int getColorForType(LineType type) {
        return switch (type) {
            case TITLE -> COLOR_TITLE;
            case HEADER -> COLOR_HEADER;
            case GOOD -> COLOR_GOOD;
            case WARNING -> COLOR_WARNING;
            case DANGER -> COLOR_DANGER;
            case GRAY -> COLOR_GRAY;
            default -> COLOR_NORMAL;
        };
    }
}
