package com.THproject.tharidia_things.client;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.diet.DietAttachments;
import com.THproject.tharidia_things.diet.DietCategory;
import com.THproject.tharidia_things.diet.DietData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class CookHudOverlay {
    private static final float PANEL_WIDTH = 1.2f; // Increased width
    private static final float PANEL_HEIGHT = 1.0f; // Increased height
    private static final float OFFSET_Y = 1.0f; // Height above mob
    private static final float MAX_DISTANCE = 15.0f; // Max render distance

    @SubscribeEvent
    public static void onRenderGui(RenderLevelStageEvent event) {
        // Only render in the AFTER_TRANSLUCENT_BLOCKS stage to avoid double rendering
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        if (player == null || mc.level == null || mc.options.hideGui)
            return;

        if (mc.screen != null)
            return;

        // Find player being looked at within 3 blocks and within 5 degrees of center
        Player targetedPlayer = getTargetedPlayer(mc, player);

        if (targetedPlayer == null)
            return;
        if (targetedPlayer == player)
            return;

        // Get mob position
        Vec3 playerPos = targetedPlayer.getEyePosition(partialTick).add(0, OFFSET_Y, 0);

        double distance = mc.player.distanceToSqr(targetedPlayer);
        // Don't render if too far away
        if (distance > MAX_DISTANCE * MAX_DISTANCE)
            return;

        // Check if player has line of sight to entity
        if (!mc.player.hasLineOfSight(targetedPlayer))
            return;

        float alpha = 1.0f - (float) (Math.sqrt(distance) / MAX_DISTANCE);
        alpha = Mth.clamp(alpha, 0.3f, 1.0f);

        poseStack.pushPose();

        // Translate to player position
        poseStack.translate(playerPos.x - cameraPos.x,
                playerPos.y - cameraPos.y,
                playerPos.z - cameraPos.z);

        // Make panel always face the camera (billboard effect)
        Quaternionf rotation = mc.gameRenderer.getMainCamera().rotation();
        poseStack.mulPose(rotation);

        // Scale based on distance - make it bigger to ensure visibility
        float scale = 1.0f; // Increased from 0.5f
        poseStack.scale(scale, scale, scale);

        // Calculate animations
        float time = (System.currentTimeMillis() % 2000) / 1000f;
        float pulse = (float) Math.sin(time * Math.PI) * 0.1f + 0.9f;

        renderPanel(poseStack, targetedPlayer, alpha, pulse);

        poseStack.popPose();
    }

    private static Player getTargetedPlayer(Minecraft mc, LocalPlayer player) {
        // Ray trace to find the entity the player is looking at (any part of bounding
        // box)
        double reach = 3.0D;
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Player closest = null;
        double closestDist = reach;
        for (Player other : mc.level.players()) {
            if (other == player)
                continue;
            // Check intersection with bounding box along look vector
            var box = other.getBoundingBox();
            double minX = box.minX, minY = box.minY, minZ = box.minZ;
            double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;
            // Step along the look vector in small increments up to reach distance
            double step = 0.1;
            for (double d = 0; d <= reach; d += step) {
                Vec3 point = eyePos.add(lookVec.scale(d));
                if (point.x >= minX && point.x <= maxX &&
                        point.y >= minY && point.y <= maxY &&
                        point.z >= minZ && point.z <= maxZ) {
                    if (d < closestDist) {
                        closest = other;
                        closestDist = d;
                    }
                    break;
                }
            }
        }
        return closest;
    }

    private static void renderPanel(PoseStack poseStack, Player player, float alpha, float scale) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = poseStack.last().pose();

        float halfWidth = PANEL_WIDTH / 2f;
        float halfHeight = PANEL_HEIGHT / 2f;

        // Panel colors
        float bgAlpha = alpha * 0.9f;
        float borderAlpha = alpha;
        float glowAlpha = alpha * 0.3f;

        // Render background panel
        renderPanelBackground(bufferBuilder, matrix, -halfWidth, -halfHeight, halfWidth, halfHeight, bgAlpha);

        // Render animated borders
        renderAnimatedBorders(bufferBuilder, matrix, -halfWidth, -halfHeight, halfWidth, halfHeight,
                borderAlpha, glowAlpha, scale);

        // Draw the panel geometry
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        // Reset render state for text
        RenderSystem.depthMask(true);

        // Render player text info
        renderPlayerInfo(poseStack, player, alpha);
    }

    private static void renderPanelBackground(BufferBuilder builder, Matrix4f matrix,
            float x1, float y1, float x2, float y2, float alpha) {
        // Main panel with dark blue color
        builder.addVertex(matrix, x1, y1, 0).setColor(0.1f, 0.1f, 0.18f, alpha);
        builder.addVertex(matrix, x1, y2, 0).setColor(0.08f, 0.08f, 0.14f, alpha);
        builder.addVertex(matrix, x2, y2, 0).setColor(0.08f, 0.08f, 0.14f, alpha);
        builder.addVertex(matrix, x2, y1, 0).setColor(0.1f, 0.1f, 0.18f, alpha);

        // Inner frame with subtle blue
        float inset = 0.02f;
        builder.addVertex(matrix, x1 + inset, y1 + inset, 0.01f).setColor(0, 0.5f, 0.8f, alpha * 0.5f);
        builder.addVertex(matrix, x1 + inset, y2 - inset, 0.01f).setColor(0, 0.5f, 0.8f, alpha * 0.5f);
        builder.addVertex(matrix, x2 - inset, y2 - inset, 0.01f).setColor(0, 0.5f, 0.8f, alpha * 0.5f);
        builder.addVertex(matrix, x2 - inset, y1 + inset, 0.01f).setColor(0, 0.5f, 0.8f, alpha * 0.5f);
    }

    private static void renderAnimatedBorders(BufferBuilder builder, Matrix4f matrix,
            float x1, float y1, float x2, float y2,
            float borderAlpha, float glowAlpha, float pulse) {

        float borderThickness = 0.03f * pulse;

        // Cyan border color
        float r = 0, g = 0.667f, b = 1.0f;

        // Top border
        renderBorderSegment(builder, matrix, x1, y1, x2, y1 + borderThickness, r, g, b, borderAlpha);

        // Bottom border
        renderBorderSegment(builder, matrix, x1, y2 - borderThickness, x2, y2, r, g, b, borderAlpha);

        // Left border
        renderBorderSegment(builder, matrix, x1, y1, x1 + borderThickness, y2, r, g, b, borderAlpha);

        // Right border
        renderBorderSegment(builder, matrix, x2 - borderThickness, y1, x2, y2, r, g, b, borderAlpha);

        // Corner accents with glow
        float accentSize = 0.05f;
        float gr = 0, gg = 1.0f, gb = 1.0f; // Cyan glow

        // Top-left corner
        renderCornerAccent(builder, matrix, x1, y1, accentSize, gr, gg, gb, glowAlpha * pulse);
        // Top-right corner
        renderCornerAccent(builder, matrix, x2, y1, accentSize, gr, gg, gb, glowAlpha * pulse);
        // Bottom-left corner
        renderCornerAccent(builder, matrix, x1, y2, accentSize, gr, gg, gb, glowAlpha * pulse);
        // Bottom-right corner
        renderCornerAccent(builder, matrix, x2, y2, accentSize, gr, gg, gb, glowAlpha * pulse);
    }

    private static void renderBorderSegment(BufferBuilder builder, Matrix4f matrix,
            float x1, float y1, float x2, float y2,
            float r, float g, float b, float a) {
        builder.addVertex(matrix, x1, y1, 0.02f).setColor(r, g, b, a);
        builder.addVertex(matrix, x1, y2, 0.02f).setColor(r, g, b, a);
        builder.addVertex(matrix, x2, y2, 0.02f).setColor(r, g, b, a);
        builder.addVertex(matrix, x2, y1, 0.02f).setColor(r, g, b, a);
    }

    private static void renderCornerAccent(BufferBuilder builder, Matrix4f matrix,
            float x, float y, float size,
            float r, float g, float b, float a) {
        // Render a small glowing diamond at corners (4 vertices for QUADS)
        builder.addVertex(matrix, x - size, y, 0.03f).setColor(r, g, b, a);
        builder.addVertex(matrix, x, y - size, 0.03f).setColor(r, g, b, a);
        builder.addVertex(matrix, x + size, y, 0.03f).setColor(r, g, b, a);
        builder.addVertex(matrix, x, y + size, 0.03f).setColor(r, g, b, a);
    }

    private static void renderPlayerInfo(PoseStack poseStack, Player player, float alpha) {
        Minecraft mc = Minecraft.getInstance();

        // Calculate text position - centered in panel with proper margins
        float textStartX = -PANEL_WIDTH / 2f + 0.15f; // More margin from left
        float textStartY = -PANEL_HEIGHT / 2f + 0.12f; // More margin from top
        float lineHeight = 0.12f; // Reduced line height

        // Enable proper text rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Setup text rendering pose
        poseStack.pushPose();
        poseStack.translate(0, 0, 0.05f); // Slight Z offset for text
        poseStack.scale(0.01f, -0.01f, 0.01f); // Reduced scale back to 0.01f

        int currentLine = 0;

        // Render diet values
        DietData player_diet = player.getData(DietAttachments.DIET_DATA.get());
        for (DietCategory diet_type : DietCategory.VALUES) {
            int color;
            float diet_value = player_diet.get(diet_type);
            if ((diet_value / 100) < 0.25f) {
                color = ((int) (alpha * 255) << 24) | 0xFF5555; // Red for low values
            } else if ((diet_value / 100) < 0.5f) {
                color = ((int) (alpha * 255) << 24) | 0xFFAA00; // Orange for medium values
            }else{
                color = ((int) (alpha * 255) << 24) | 0x55FF55; // Green for good values
            }
            mc.font.drawInBatch(String.format("- %s: %.2f", diet_type, diet_value),
                    (int) (textStartX * 100), (int) ((textStartY + lineHeight * currentLine) * 100),
                    color, false, poseStack.last().pose(),
                    mc.renderBuffers().bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
            currentLine++;
        }

        poseStack.popPose();

        // Flush text buffer
        mc.renderBuffers().bufferSource().endBatch();
    }
}
