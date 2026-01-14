package com.THproject.tharidia_things.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.THproject.tharidia_things.block.entity.ClaimBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ClaimBoundaryRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static List<ClaimData> nearbyClaims = new ArrayList<>();
    private static long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 1000; // Update every second
    
    // Toggle state for claim boundaries visibility
    private static boolean boundariesVisible = false;

    private record ClaimData(BlockPos claimPos, int minX, int minZ, int maxX, int maxZ, int minY, int maxY) {}
    
    /**
     * Toggles the visibility of claim boundaries
     */
    public static void toggleBoundaries() {
        boundariesVisible = !boundariesVisible;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String status = boundariesVisible ? "§aON" : "§cOFF";
            mc.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§6Boundaries: " + status),
                true // Show in action bar
            );
        }
        LOGGER.info("Boundaries visibility toggled: {}", boundariesVisible);
    }
    
    /**
     * Returns the current visibility state
     */
    public static boolean areBoundariesVisible() {
        return boundariesVisible;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Check if boundaries are toggled on
        if (!boundariesVisible) {
            return;
        }

        updateNearbyClaims(mc);

        if (nearbyClaims.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        // Setup rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        poseStack.pushPose();

        float time = (System.currentTimeMillis() % 10000) / 10000.0f;

        for (ClaimData claim : nearbyClaims) {
            renderClaimBoundary(claim, poseStack, bufferBuilder, cameraPos, time);
        }

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        poseStack.popPose();

        // Restore render state
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void updateNearbyClaims(Minecraft mc) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate < UPDATE_INTERVAL) {
            return;
        }
        lastUpdate = currentTime;

        nearbyClaims.clear();

        if (mc.level == null || mc.player == null) {
            return;
        }

        Vec3 playerPos = mc.player.position();
        int playerChunkX = ((int) playerPos.x) >> 4;
        int playerChunkZ = ((int) playerPos.z) >> 4;

        // Search for claim blocks in nearby chunks (5 chunk radius) - OPTIMIZED
        for (int chunkX = playerChunkX - 5; chunkX <= playerChunkX + 5; chunkX++) {
            for (int chunkZ = playerChunkZ - 5; chunkZ <= playerChunkZ + 5; chunkZ++) {
                // Check if chunk is loaded
                if (!mc.level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                // Get chunk and check block entities directly (much faster)
                net.minecraft.world.level.chunk.LevelChunk chunk = mc.level.getChunk(chunkX, chunkZ);
                
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof ClaimBlockEntity) {
                        BlockPos pos = blockEntity.getBlockPos();
                        
                        // Calculate claim protection area
                        int claimMinY = pos.getY() - 20;
                        int claimMaxY = pos.getY() + 40;
                        
                        // Calculate chunk bounds
                        int minX = chunkX << 4;
                        int minZ = chunkZ << 4;
                        
                        // Chunk boundaries for rendering
                        nearbyClaims.add(new ClaimData(pos, minX, minZ, minX + 16, minZ + 16, claimMinY, claimMaxY));
                        
                        // Only one claim per chunk
                        break;
                    }
                }
            }
        }
    }

    private static void renderClaimBoundary(ClaimData claim, PoseStack poseStack, BufferBuilder bufferBuilder, Vec3 cameraPos, float time) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        double offsetX = -cameraPos.x;
        double offsetY = -cameraPos.y;
        double offsetZ = -cameraPos.z;

        // Different color for claims - cyan/blue tint
        float shimmer = (float) Math.sin(time * Math.PI * 2.0) * 0.15f + 0.85f;
        float baseR = 0.3f * shimmer;
        float baseG = 0.8f * shimmer;
        float baseB = 0.9f * shimmer;

        Matrix4f matrix = poseStack.last().pose();

        // Render the four vertical walls (solid, no segments)
        renderSolidWall(bufferBuilder, matrix, claim.minX, claim.minZ, claim.maxX, claim.minZ,
                claim.minY, claim.maxY, offsetX, offsetY, offsetZ, baseR, baseG, baseB);

        renderSolidWall(bufferBuilder, matrix, claim.minX, claim.maxZ, claim.maxX, claim.maxZ,
                claim.minY, claim.maxY, offsetX, offsetY, offsetZ, baseR, baseG, baseB);

        renderSolidWall(bufferBuilder, matrix, claim.minX, claim.minZ, claim.minX, claim.maxZ,
                claim.minY, claim.maxY, offsetX, offsetY, offsetZ, baseR, baseG, baseB);

        renderSolidWall(bufferBuilder, matrix, claim.maxX, claim.minZ, claim.maxX, claim.maxZ,
                claim.minY, claim.maxY, offsetX, offsetY, offsetZ, baseR, baseG, baseB);
        
        // Render horizontal surfaces (top and bottom)
        renderHorizontalSurface(bufferBuilder, matrix, claim.minX, claim.minZ, claim.maxX, claim.maxZ,
                claim.minY, offsetX, offsetY, offsetZ, baseR, baseG, baseB, time);
        
        renderHorizontalSurface(bufferBuilder, matrix, claim.minX, claim.minZ, claim.maxX, claim.maxZ,
                claim.maxY, offsetX, offsetY, offsetZ, baseR, baseG, baseB, time);
        
        // Render all 12 edges of the box for clear definition
        renderBoxEdges(bufferBuilder, matrix, claim.minX, claim.minZ, claim.maxX, claim.maxZ,
                claim.minY, claim.maxY, offsetX, offsetY, offsetZ, baseR, baseG, baseB);
    }

    private static void renderSolidWall(BufferBuilder bufferBuilder, Matrix4f matrix,
                                        double x1, double z1, double x2, double z2,
                                        int minY, int maxY,
                                        double offsetX, double offsetY, double offsetZ,
                                        float baseR, float baseG, float baseB) {
        // Solid wall with no segments - plain uniform color
        float alpha = 0.6f;
        
        // Render as single quad
        bufferBuilder.addVertex(matrix, (float) (x1 + offsetX), (float) (minY + offsetY), (float) (z1 + offsetZ))
                .setColor(baseR, baseG, baseB, alpha);
        bufferBuilder.addVertex(matrix, (float) (x2 + offsetX), (float) (minY + offsetY), (float) (z2 + offsetZ))
                .setColor(baseR, baseG, baseB, alpha);
        bufferBuilder.addVertex(matrix, (float) (x2 + offsetX), (float) (maxY + offsetY), (float) (z2 + offsetZ))
                .setColor(baseR, baseG, baseB, alpha);
        bufferBuilder.addVertex(matrix, (float) (x1 + offsetX), (float) (maxY + offsetY), (float) (z1 + offsetZ))
                .setColor(baseR, baseG, baseB, alpha);
    }
    
    private static void renderBoxEdges(BufferBuilder bufferBuilder, Matrix4f matrix,
                                       double minX, double minZ, double maxX, double maxZ,
                                       int minY, int maxY,
                                       double offsetX, double offsetY, double offsetZ,
                                       float baseR, float baseG, float baseB) {
        // Brighter color for edges
        float r = Math.min(1.0f, baseR * 1.8f);
        float g = Math.min(1.0f, baseG * 1.5f);
        float b = Math.min(1.0f, baseB * 1.4f);
        float edgeWidth = 0.05f;
        
        // 4 vertical edges
        renderEdge(bufferBuilder, matrix, minX, minZ, minY, maxY, edgeWidth, offsetX, offsetY, offsetZ, r, g, b);
        renderEdge(bufferBuilder, matrix, maxX, minZ, minY, maxY, edgeWidth, offsetX, offsetY, offsetZ, r, g, b);
        renderEdge(bufferBuilder, matrix, minX, maxZ, minY, maxY, edgeWidth, offsetX, offsetY, offsetZ, r, g, b);
        renderEdge(bufferBuilder, matrix, maxX, maxZ, minY, maxY, edgeWidth, offsetX, offsetY, offsetZ, r, g, b);
        
        // 4 bottom horizontal edges
        renderHorizontalEdge(bufferBuilder, matrix, minX, minZ, maxX, minZ, minY, edgeWidth, offsetX, offsetY, offsetZ, r, g, b);
        renderHorizontalEdge(bufferBuilder, matrix, minX, maxZ, maxX, maxZ, minY, edgeWidth, offsetX, offsetY, offsetZ, r, g, b);
        renderHorizontalEdge(bufferBuilder, matrix, minX, minZ, minX, maxZ, minY, edgeWidth, offsetX, offsetY, offsetZ, r, g, b);
        renderHorizontalEdge(bufferBuilder, matrix, maxX, minZ, maxX, maxZ, minY, edgeWidth, offsetX, offsetY, offsetZ, r, g, b);
        
        // 4 top horizontal edges
        renderHorizontalEdge(bufferBuilder, matrix, minX, minZ, maxX, minZ, maxY, edgeWidth, offsetX, offsetY, offsetZ, r, g, b);
        renderHorizontalEdge(bufferBuilder, matrix, minX, maxZ, maxX, maxZ, maxY, edgeWidth, offsetX, offsetY, offsetZ, r, g, b);
        renderHorizontalEdge(bufferBuilder, matrix, minX, minZ, minX, maxZ, maxY, edgeWidth, offsetX, offsetY, offsetZ, r, g, b);
        renderHorizontalEdge(bufferBuilder, matrix, maxX, minZ, maxX, maxZ, maxY, edgeWidth, offsetX, offsetY, offsetZ, r, g, b);
    }
    
    private static void renderEdge(BufferBuilder bufferBuilder, Matrix4f matrix,
                                   double x, double z, int minY, int maxY, float width,
                                   double offsetX, double offsetY, double offsetZ,
                                   float r, float g, float b) {
        // Vertical edge line
        float alpha = 1.0f; // Fully opaque
        
        bufferBuilder.addVertex(matrix, (float) (x - width + offsetX), (float) (minY + offsetY), (float) (z + offsetZ))
                .setColor(r, g, b, alpha);
        bufferBuilder.addVertex(matrix, (float) (x + width + offsetX), (float) (minY + offsetY), (float) (z + offsetZ))
                .setColor(r, g, b, alpha);
        bufferBuilder.addVertex(matrix, (float) (x + width + offsetX), (float) (maxY + offsetY), (float) (z + offsetZ))
                .setColor(r, g, b, alpha);
        bufferBuilder.addVertex(matrix, (float) (x - width + offsetX), (float) (maxY + offsetY), (float) (z + offsetZ))
                .setColor(r, g, b, alpha);
    }
    
    private static void renderHorizontalEdge(BufferBuilder bufferBuilder, Matrix4f matrix,
                                             double x1, double z1, double x2, double z2,
                                             int y, float width,
                                             double offsetX, double offsetY, double offsetZ,
                                             float r, float g, float b) {
        // Horizontal edge line
        float alpha = 1.0f; // Fully opaque
        
        bufferBuilder.addVertex(matrix, (float) (x1 + offsetX), (float) (y - width + offsetY), (float) (z1 + offsetZ))
                .setColor(r, g, b, alpha);
        bufferBuilder.addVertex(matrix, (float) (x2 + offsetX), (float) (y - width + offsetY), (float) (z2 + offsetZ))
                .setColor(r, g, b, alpha);
        bufferBuilder.addVertex(matrix, (float) (x2 + offsetX), (float) (y + width + offsetY), (float) (z2 + offsetZ))
                .setColor(r, g, b, alpha);
        bufferBuilder.addVertex(matrix, (float) (x1 + offsetX), (float) (y + width + offsetY), (float) (z1 + offsetZ))
                .setColor(r, g, b, alpha);
    }
    
    private static void renderHorizontalSurface(BufferBuilder bufferBuilder, Matrix4f matrix,
                                                double minX, double minZ, double maxX, double maxZ,
                                                int y,
                                                double offsetX, double offsetY, double offsetZ,
                                                float baseR, float baseG, float baseB, float time) {
        // Plain opacity for horizontal surfaces - slightly less than walls
        float alpha = 0.5f;
        
        // Render the horizontal surface
        bufferBuilder.addVertex(matrix, (float) (minX + offsetX), (float) (y + offsetY), (float) (minZ + offsetZ))
                .setColor(baseR, baseG, baseB, alpha);
        bufferBuilder.addVertex(matrix, (float) (maxX + offsetX), (float) (y + offsetY), (float) (minZ + offsetZ))
                .setColor(baseR, baseG, baseB, alpha);
        bufferBuilder.addVertex(matrix, (float) (maxX + offsetX), (float) (y + offsetY), (float) (maxZ + offsetZ))
                .setColor(baseR, baseG, baseB, alpha);
        bufferBuilder.addVertex(matrix, (float) (minX + offsetX), (float) (y + offsetY), (float) (maxZ + offsetZ))
                .setColor(baseR, baseG, baseB, alpha);
    }

    public static void clear() {
        nearbyClaims.clear();
    }
}
