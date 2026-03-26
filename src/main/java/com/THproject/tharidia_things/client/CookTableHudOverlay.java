package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.CookTableBlock;
import com.THproject.tharidia_things.block.CookTableDummyBlock;
import com.THproject.tharidia_things.block.entity.CookTableBlockEntity;
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
public class CookTableHudOverlay {

    private static final double RAYCAST_DISTANCE = 8.0;
    private static final float BODY_SCALE = 0.75f;
    private static final int BODY_LINE_SPACING = 2;

    private enum LineType { TITLE, HEADER, NORMAL, GOOD, WARNING, DANGER, EMPTY }
    private record LineData(LineType type, Component text) {}

    private static CookTableBlockEntity cachedBe = null;
    private static long lastUpdate = 0;
    private static List<LineData> cachedLines = new ArrayList<>();

    /** Set by CookTableRecipeScreen when the player confirms a recipe. Cleared when session ends. */
    public static BlockPos activeCookTablePos = null;
    /** True once the first sync confirming isCooking() arrives. Prevents premature clear. */
    public static boolean cookingSessionConfirmed = false;
    /** Timestamp of when the player clicked "Inizia cottura", used for server-confirm timeout. */
    public static long cookingRequestTime = 0L;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui) return;
        if (mc.screen != null) return;

        // During an active session: always show the overlay for the player's active cook table.
        CookTableBlockEntity be = null;
        if (activeCookTablePos != null) {
            net.minecraft.world.level.block.entity.BlockEntity raw =
                    mc.level.getBlockEntity(activeCookTablePos);
            if (raw instanceof CookTableBlockEntity cbe) {
                if (cbe.isCooking()) {
                    be = cbe;
                    cookingSessionConfirmed = true;
                } else if (cookingSessionConfirmed) {
                    // Was cooking, now stopped → clear
                    activeCookTablePos = null;
                    cookingSessionConfirmed = false;
                } else if (System.currentTimeMillis() - cookingRequestTime > 3000L) {
                    // Server never confirmed the session within 3 s → clear
                    activeCookTablePos = null;
                }
            } else {
                activeCookTablePos = null;
                cookingSessionConfirmed = false;
            }
        }

        // Fallback: raycast (shows info when looking at block even outside a session)
        if (be == null) {
            be = getTargetedBlockEntity(mc, player);
        }

        if (be == null) { cachedBe = null; cachedLines.clear(); return; }

        long now = System.currentTimeMillis();
        long interval = be.isCooking() ? 50L : 2000L;
        if (be != cachedBe || now - lastUpdate >= interval) {
            cachedBe = be;
            lastUpdate = now;
            cachedLines = buildLines(be);
        }

        if (!cachedLines.isEmpty()) {
            renderOverlay(event.getGuiGraphics(), mc, cachedLines);
        }
    }

    private static CookTableBlockEntity getTargetedBlockEntity(Minecraft mc, LocalPlayer player) {
        Vec3 eye    = player.getEyePosition(1.0f);
        Vec3 look   = player.getLookAngle();
        Vec3 end    = eye.add(look.scale(RAYCAST_DISTANCE));

        BlockHitResult hit = mc.level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        if (hit.getType() != HitResult.Type.BLOCK) return null;

        BlockPos hitPos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(hitPos);

        BlockPos masterPos = null;
        if (state.getBlock() instanceof CookTableBlock) {
            masterPos = hitPos;
        } else if (state.getBlock() instanceof CookTableDummyBlock) {
            masterPos = CookTableDummyBlock.getMasterPos(mc.level, hitPos);
        }

        if (masterPos == null) return null;
        BlockEntity blockEntity = mc.level.getBlockEntity(masterPos);
        return blockEntity instanceof CookTableBlockEntity cbe ? cbe : null;
    }

    private static List<LineData> buildLines(CookTableBlockEntity be) {
        List<LineData> lines = new ArrayList<>();

        lines.add(new LineData(LineType.TITLE,
                Component.literal("Tavolo del Cuoco")));

        if (be.isCooking()) {
            lines.add(new LineData(LineType.HEADER,
                    Component.literal("Sessione in corso")));

            // Active recipe name (synced from server as translated item name)
            String recipeName = be.getActiveResultName();
            if (recipeName.isEmpty()) recipeName = be.getActiveRecipeId();
            lines.add(new LineData(LineType.NORMAL,
                    Component.literal("Ricetta: " + recipeName)));

            // Timer
            float secs = be.getTimerTicks() / 20.0f;
            String timeStr = secs > 2f ? String.valueOf((int) secs) : String.format("%.1f", secs);
            LineType timerType = secs <= 5  ? LineType.DANGER
                               : secs <= 15 ? LineType.WARNING
                               : LineType.GOOD;
            lines.add(new LineData(timerType,
                    Component.literal("Tempo rimanente: " + timeStr + "s")));

            // Progress bar via text
            int total = be.getTotalTimerTicks();
            int current = be.getTimerTicks();
            if (total > 0) {
                int filled = (int)(10.0 * current / total);
                String bar = "█".repeat(filled) + "░".repeat(10 - filled);
                lines.add(new LineData(timerType, Component.literal("[" + bar + "]")));
            }
        } else {
            lines.add(new LineData(LineType.NORMAL,
                    Component.literal("Apri il ricettario")));
        }

        return lines;
    }

    private static void renderOverlay(GuiGraphics g, Minecraft mc, List<LineData> lines) {
        int padding     = 6;
        int lineHeight  = 9;
        int maxWidth    = 0;

        for (LineData line : lines) {
            if (line.type() == LineType.EMPTY) continue;
            Component styled = line.text().copy().withStyle(s -> s.withFont(MedievalGuiRenderer.MEDIEVAL_FONT));
            int w = mc.font.width(styled);
            if (line.type() != LineType.TITLE && line.type() != LineType.HEADER)
                w = Math.round(w * BODY_SCALE);
            if (w > maxWidth) maxWidth = w;
        }

        int boxWidth  = maxWidth + padding * 2;
        int boxHeight = measureHeight(mc, lines, lineHeight) + padding * 2;
        int startX = 10, startY = 10;

        g.fill(startX + 2, startY + 2, startX + boxWidth + 2, startY + boxHeight + 2,
                MedievalGuiRenderer.SHADOW_DARK);
        MedievalGuiRenderer.renderParchmentBackground(g, startX, startY, boxWidth, boxHeight);
        g.renderOutline(startX, startY, boxWidth, boxHeight, MedievalGuiRenderer.BRONZE);

        int scaledLH = Math.round(lineHeight * BODY_SCALE) + BODY_LINE_SPACING;
        int y = startY + padding;

        for (LineData line : lines) {
            if (line.type() == LineType.EMPTY) { y += 3; continue; }
            int color = switch (line.type()) {
                case TITLE   -> MedievalGuiRenderer.GOLD_LEAF;
                case HEADER  -> MedievalGuiRenderer.ROYAL_GOLD;
                case GOOD    -> MedievalGuiRenderer.BRONZE;
                case WARNING -> MedievalGuiRenderer.PURPLE_REGAL;
                case DANGER  -> MedievalGuiRenderer.DEEP_CRIMSON;
                default      -> MedievalGuiRenderer.BROWN_INK;
            };
            Component styled = line.text().copy().withStyle(s -> s.withFont(MedievalGuiRenderer.MEDIEVAL_FONT));

            if (line.type() == LineType.TITLE) {
                int tw = mc.font.width(styled);
                g.drawString(mc.font, styled, startX + (boxWidth - tw) / 2, y, color, false);
                y += mc.font.lineHeight + 2;
            } else if (line.type() == LineType.HEADER) {
                g.drawString(mc.font, styled, startX + padding, y, color, false);
                y += lineHeight;
            } else {
                g.pose().pushPose();
                g.pose().scale(BODY_SCALE, BODY_SCALE, 1f);
                g.drawString(mc.font, styled,
                        Math.round((startX + padding) / BODY_SCALE),
                        Math.round(y / BODY_SCALE), color, false);
                g.pose().popPose();
                y += scaledLH;
            }
        }
    }

    private static int measureHeight(Minecraft mc, List<LineData> lines, int lineHeight) {
        int scaledLH = Math.round(lineHeight * BODY_SCALE) + BODY_LINE_SPACING;
        int h = 0;
        for (LineData line : lines) {
            if (line.type() == LineType.EMPTY)       h += 3;
            else if (line.type() == LineType.TITLE)  h += mc.font.lineHeight + 2;
            else if (line.type() == LineType.HEADER) h += lineHeight;
            else                                      h += scaledLH;
        }
        return h;
    }
}
