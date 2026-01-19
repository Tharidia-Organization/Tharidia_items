package com.THproject.tharidia_things.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import com.THproject.tharidia_things.realm.RealmManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RealmBoundaryRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static List<PietroBlockEntity> nearbyRealms = new ArrayList<>();
    private static long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 1000; // Update every second
    private static long lastLogTime = 0;
    private static final long LOG_INTERVAL = 5000; // Log every 5 seconds
    
    // Color randomization
    private static float colorSeedR = 0.9f;
    private static float colorSeedG = 0.5f;
    private static float colorSeedB = 0.8f;
    private static java.util.Random colorRandom = new java.util.Random();
    
    private static boolean boundariesVisible = false;

    public static void toggleBoundaries() {
        boolean wasVisible = boundariesVisible;
        boundariesVisible = !boundariesVisible;
        // Randomize colors when turning visibility on
        if (boundariesVisible && !wasVisible) {
            randomizeColors();
        }
    }
    
    /**
     * Gets the current boundary visibility state
     */
    public static boolean areBoundariesVisible() {
        return boundariesVisible;
    }
    
    /**
     * Sets the boundary visibility state (used when syncing data)
     */
    public static void setBoundariesVisible(boolean visible) {
        if (visible && !boundariesVisible) {
            randomizeColors();
        }
        boundariesVisible = visible;
    }

    /**
     * Randomizes the color palette for the realm boundaries
     */
    private static void randomizeColors() {
        // Generate random magical colors
        colorSeedR = 0.6f + colorRandom.nextFloat() * 0.4f; // 0.6-1.0
        colorSeedG = 0.3f + colorRandom.nextFloat() * 0.5f; // 0.3-0.8
        colorSeedB = 0.5f + colorRandom.nextFloat() * 0.5f; // 0.5-1.0
        
        LOGGER.info("Randomized realm colors: R={}, G={}, B={}", colorSeedR, colorSeedG, colorSeedB);
    }
    
    /**
     * Updates the list of nearby realms
     */
    public static void updateNearbyRealms() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate < UPDATE_INTERVAL) {
            return;
        }
        lastUpdate = currentTime;

        nearbyRealms.clear();
        Minecraft mc = Minecraft.getInstance();
        
        if (mc.player == null || mc.level == null) {
            return;
        }

        // In single player, we can access server data
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            ServerLevel serverLevel = mc.getSingleplayerServer().getLevel(mc.level.dimension());
            if (serverLevel != null) {
                List<PietroBlockEntity> allRealms = RealmManager.getRealms(serverLevel);
                
                // Filter to nearby realms (within render distance)
                Vec3 playerPos = mc.player.position();
                for (PietroBlockEntity realm : allRealms) {
                    double distance = Math.sqrt(realm.getBlockPos().distToCenterSqr(playerPos));
                    if (distance < 256) { // 16 chunks * 16 blocks
                        nearbyRealms.add(realm);
                    }
                }
            }
        } else {
            // For multiplayer, use synced data
            if (currentTime - lastLogTime > LOG_INTERVAL) {
                LOGGER.info("Multiplayer mode: {} synced realms available", ClientPacketHandler.syncedRealms.size());
                lastLogTime = currentTime;
            }
            Vec3 playerPos = mc.player.position();
            for (PietroBlockEntity realm : ClientPacketHandler.syncedRealms) {
                double distance = Math.sqrt(realm.getBlockPos().distToCenterSqr(playerPos));
                if (distance < 256) { // 16 chunks * 16 blocks
                    nearbyRealms.add(realm);
                }
            }
            if (currentTime - lastLogTime > LOG_INTERVAL) {
                LOGGER.info("Found {} nearby realms in multiplayer", nearbyRealms.size());
            }
        }
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

        if (!boundariesVisible) {
            return;
        }

        updateNearbyRealms();

        if (nearbyRealms.isEmpty()) {
            return;
        }
        
        // Debug logging to confirm rendering loop is active
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > LOG_INTERVAL) {
             LOGGER.info("Rendering {} realm boundaries", nearbyRealms.size());
             lastLogTime = currentTime;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        
        poseStack.pushPose();
        
        // --- Setup Render System State ---
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        // --- Pass 1: Translucent Walls & Particles (Depth Mask OFF) ---
        RenderSystem.depthMask(false);
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        for (PietroBlockEntity realm : nearbyRealms) {
            renderRealmWallsAndParticles(realm, poseStack, bufferBuilder, cameraPos);
        }
        
        try {
            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        } catch (Exception e) {
            LOGGER.error("Failed to render realm walls", e);
        }
        
        // --- Pass 2: Lines & Edges (Depth Mask OFF to avoid z-fighting with walls, or ON if preferred) ---
        // Using false to keep them consistent with the ghostly look, but ensuring they are drawn after walls
        RenderSystem.depthMask(false); 
        bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        for (PietroBlockEntity realm : nearbyRealms) {
            renderRealmLines(realm, poseStack, bufferBuilder, cameraPos);
        }
        
        try {
            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        } catch (Exception e) {
            LOGGER.error("Failed to render realm lines", e);
        }

        // Restore state
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        
        poseStack.popPose();
    }

    private static void renderRealmWallsAndParticles(PietroBlockEntity realm, PoseStack poseStack, VertexConsumer buffer, Vec3 cameraPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        int minY = mc.level.getMinBuildHeight();
        int maxY = mc.level.getMaxBuildHeight();

        double offsetX = -cameraPos.x;
        double offsetY = -cameraPos.y;
        double offsetZ = -cameraPos.z;

        float time = (System.currentTimeMillis() % 10000) / 10000.0f;
        float shimmer = (float) Math.sin(time * Math.PI * 2.0) * 0.15f + 0.85f;
        
        float baseR = colorSeedR * shimmer;
        float baseG = colorSeedG * shimmer;
        float baseB = colorSeedB * shimmer;

        Matrix4f matrix = poseStack.last().pose();

        ChunkPos minChunk = realm.getMinChunk();
        ChunkPos maxChunk = realm.getMaxChunk();

        int minX = minChunk.getMinBlockX();
        int minZ = minChunk.getMinBlockZ();
        int maxX = maxChunk.getMaxBlockX() + 1;
        int maxZ = maxChunk.getMaxBlockZ() + 1;

        // Render the four vertical walls with enhanced effects
        renderEnhancedWall(buffer, matrix, mc.level,
                minX, minZ, maxX, minZ, minY, maxY, // North wall
                offsetX, offsetY, offsetZ, baseR, baseG, baseB, time);

        renderEnhancedWall(buffer, matrix, mc.level,
                minX, maxZ, maxX, maxZ, minY, maxY, // South wall
                offsetX, offsetY, offsetZ, baseR, baseG, baseB, time);

        renderEnhancedWall(buffer, matrix, mc.level,
                minX, minZ, minX, maxZ, minY, maxY, // West wall
                offsetX, offsetY, offsetZ, baseR, baseG, baseB, time);

        renderEnhancedWall(buffer, matrix, mc.level,
                maxX, minZ, maxX, maxZ, minY, maxY, // East wall
                offsetX, offsetY, offsetZ, baseR, baseG, baseB, time);
        
        // Add bubble-like particles
        renderBubbleParticles(buffer, matrix, minX, minZ, maxX, maxZ, minY, maxY,
                offsetX, offsetY, offsetZ, time, baseR, baseG, baseB);
        
        // Render outer layer walls
        renderOuterLayerWalls(realm, buffer, matrix, mc.level, minY, maxY,
                offsetX, offsetY, offsetZ, baseR, baseG, baseB, time);
    }
    
    private static void renderRealmLines(PietroBlockEntity realm, PoseStack poseStack, VertexConsumer buffer, Vec3 cameraPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        int minY = mc.level.getMinBuildHeight();
        int maxY = mc.level.getMaxBuildHeight();

        double offsetX = -cameraPos.x;
        double offsetY = -cameraPos.y;
        double offsetZ = -cameraPos.z;

        float time = (System.currentTimeMillis() % 10000) / 10000.0f;
        float shimmer = (float) Math.sin(time * Math.PI * 2.0) * 0.15f + 0.85f;
        
        float baseR = colorSeedR * shimmer;
        float baseG = colorSeedG * shimmer;
        float baseB = colorSeedB * shimmer;

        Matrix4f matrix = poseStack.last().pose();

        ChunkPos minChunk = realm.getMinChunk();
        ChunkPos maxChunk = realm.getMaxChunk();

        int minX = minChunk.getMinBlockX();
        int minZ = minChunk.getMinBlockZ();
        int maxX = maxChunk.getMaxBlockX() + 1;
        int maxZ = maxChunk.getMaxBlockZ() + 1;
        
        // Render clear ground contact lines
        renderGroundLines(buffer, matrix, minX, minZ, maxX, maxZ, minY,
                offsetX, offsetY, offsetZ, baseR, baseG, baseB);
        
        // Render bright vertical edges at corners
        renderVerticalEdges(buffer, matrix, minX, minZ, maxX, maxZ, minY, maxY,
                offsetX, offsetY, offsetZ, baseR, baseG, baseB);
        
        // Render outer layer lines
        renderOuterLayerLines(realm, buffer, matrix, minY,
                offsetX, offsetY, offsetZ, baseR, baseG, baseB);
    }
    
    private static void renderOuterLayerWalls(PietroBlockEntity realm, VertexConsumer buffer,
            Matrix4f matrix, net.minecraft.world.level.Level level, int minY, int maxY,
            double offsetX, double offsetY, double offsetZ, 
            float baseR, float baseG, float baseB, float time) {
        
        ChunkPos outerMinChunk = realm.getOuterLayerMinChunk();
        ChunkPos outerMaxChunk = realm.getOuterLayerMaxChunk();

        int outerMinX = outerMinChunk.getMinBlockX();
        int outerMinZ = outerMinChunk.getMinBlockZ();
        int outerMaxX = outerMaxChunk.getMaxBlockX() + 1;
        int outerMaxZ = outerMaxChunk.getMaxBlockZ() + 1;
        
        float outerR = baseR * 0.5f;
        float outerG = baseG * 0.5f;
        float outerB = baseB * 0.5f;
        float outerAlpha = 0.35f;
        
        renderSimpleWall(buffer, matrix, level,
                outerMinX, outerMinZ, outerMaxX, outerMinZ, minY, maxY,
                offsetX, offsetY, offsetZ, outerR, outerG, outerB, outerAlpha);

        renderSimpleWall(buffer, matrix, level,
                outerMinX, outerMaxZ, outerMaxX, outerMaxZ, minY, maxY,
                offsetX, offsetY, offsetZ, outerR, outerG, outerB, outerAlpha);

        renderSimpleWall(buffer, matrix, level,
                outerMinX, outerMinZ, outerMinX, outerMaxZ, minY, maxY,
                offsetX, offsetY, offsetZ, outerR, outerG, outerB, outerAlpha);

        renderSimpleWall(buffer, matrix, level,
                outerMaxX, outerMinZ, outerMaxX, outerMaxZ, minY, maxY,
                offsetX, offsetY, offsetZ, outerR, outerG, outerB, outerAlpha);
    }

    private static void renderOuterLayerLines(PietroBlockEntity realm, VertexConsumer buffer,
            Matrix4f matrix, int minY,
            double offsetX, double offsetY, double offsetZ, 
            float baseR, float baseG, float baseB) {
        
        ChunkPos outerMinChunk = realm.getOuterLayerMinChunk();
        ChunkPos outerMaxChunk = realm.getOuterLayerMaxChunk();

        int outerMinX = outerMinChunk.getMinBlockX();
        int outerMinZ = outerMinChunk.getMinBlockZ();
        int outerMaxX = outerMaxChunk.getMaxBlockX() + 1;
        int outerMaxZ = outerMaxChunk.getMaxBlockZ() + 1;
        
        float outerR = baseR * 0.5f;
        float outerG = baseG * 0.5f;
        float outerB = baseB * 0.5f;

        renderSimpleGroundLines(buffer, matrix, outerMinX, outerMinZ, outerMaxX, outerMaxZ, minY,
                offsetX, offsetY, offsetZ, outerR, outerG, outerB);
    }
    
    /**
     * Renders a simple wall for the outer layer
     */
    private static void renderSimpleWall(VertexConsumer buffer, Matrix4f matrix, net.minecraft.world.level.Level level,
            int x1, int z1, int x2, int z2, int minY, int maxY,
            double offsetX, double offsetY, double offsetZ,
            float r, float g, float b, float alpha) {
        // Safety check
        if (buffer == null) return;
        
        int segments = 8;
        float heightPerSegment = (maxY - minY) / (float) segments;
        
        for (int i = 0; i < segments; i++) {
            float y1 = minY + i * heightPerSegment;
            float y2 = minY + (i + 1) * heightPerSegment;
            
            buffer.addVertex(matrix, (float) (x1 + offsetX), (float) (y1 + offsetY), (float) (z1 + offsetZ))
                    .setColor(r, g, b, alpha);
            buffer.addVertex(matrix, (float) (x2 + offsetX), (float) (y1 + offsetY), (float) (z2 + offsetZ))
                    .setColor(r, g, b, alpha);
            buffer.addVertex(matrix, (float) (x2 + offsetX), (float) (y2 + offsetY), (float) (z2 + offsetZ))
                    .setColor(r, g, b, alpha * 0.5f);
            buffer.addVertex(matrix, (float) (x1 + offsetX), (float) (y2 + offsetY), (float) (z1 + offsetZ))
                    .setColor(r, g, b, alpha * 0.5f);
        }
    }
    
    /**
     * Renders simple ground lines for the outer layer
     */
    private static void renderSimpleGroundLines(VertexConsumer buffer, Matrix4f matrix,
            int minX, int minZ, int maxX, int maxZ, int groundY,
            double offsetX, double offsetY, double offsetZ,
            float r, float g, float b) {
        // Safety check
        if (buffer == null) return;
        
        float lineThickness = 0.1f;
        float alpha = 0.6f;
        
        // North line
        buffer.addVertex(matrix, (float) (minX + offsetX), (float) (groundY + offsetY), (float) (minZ + offsetZ))
                .setColor(r, g, b, alpha);
        buffer.addVertex(matrix, (float) (maxX + offsetX), (float) (groundY + offsetY), (float) (minZ + offsetZ))
                .setColor(r, g, b, alpha);
        buffer.addVertex(matrix, (float) (maxX + offsetX), (float) (groundY + lineThickness + offsetY), (float) (minZ + offsetZ))
                .setColor(r, g, b, alpha);
        buffer.addVertex(matrix, (float) (minX + offsetX), (float) (groundY + lineThickness + offsetY), (float) (minZ + offsetZ))
                .setColor(r, g, b, alpha);
        
        // South line
        buffer.addVertex(matrix, (float) (minX + offsetX), (float) (groundY + offsetY), (float) (maxZ + offsetZ))
                .setColor(r, g, b, alpha);
        buffer.addVertex(matrix, (float) (maxX + offsetX), (float) (groundY + offsetY), (float) (maxZ + offsetZ))
                .setColor(r, g, b, alpha);
        buffer.addVertex(matrix, (float) (maxX + offsetX), (float) (groundY + lineThickness + offsetY), (float) (maxZ + offsetZ))
                .setColor(r, g, b, alpha);
        buffer.addVertex(matrix, (float) (minX + offsetX), (float) (groundY + lineThickness + offsetY), (float) (maxZ + offsetZ))
                .setColor(r, g, b, alpha);
        
        // West line
        buffer.addVertex(matrix, (float) (minX + offsetX), (float) (groundY + offsetY), (float) (minZ + offsetZ))
                .setColor(r, g, b, alpha);
        buffer.addVertex(matrix, (float) (minX + offsetX), (float) (groundY + offsetY), (float) (maxZ + offsetZ))
                .setColor(r, g, b, alpha);
        buffer.addVertex(matrix, (float) (minX + offsetX), (float) (groundY + lineThickness + offsetY), (float) (maxZ + offsetZ))
                .setColor(r, g, b, alpha);
        buffer.addVertex(matrix, (float) (minX + offsetX), (float) (groundY + lineThickness + offsetY), (float) (minZ + offsetZ))
                .setColor(r, g, b, alpha);
        
        // East line
        buffer.addVertex(matrix, (float) (maxX + offsetX), (float) (groundY + offsetY), (float) (minZ + offsetZ))
                .setColor(r, g, b, alpha);
        buffer.addVertex(matrix, (float) (maxX + offsetX), (float) (groundY + offsetY), (float) (maxZ + offsetZ))
                .setColor(r, g, b, alpha);
        buffer.addVertex(matrix, (float) (maxX + offsetX), (float) (groundY + lineThickness + offsetY), (float) (minZ + offsetZ))
                .setColor(r, g, b, alpha);
        buffer.addVertex(matrix, (float) (maxX + offsetX), (float) (groundY + lineThickness + offsetY), (float) (minZ + offsetZ))
                .setColor(r, g, b, alpha);
    }
    
    private static void renderEnhancedWall(VertexConsumer buffer, Matrix4f matrix, net.minecraft.world.level.Level level,
                                           double x1, double z1, double x2, double z2,
                                           int minY, int maxY,
                                           double offsetX, double offsetY, double offsetZ,
                                           float baseR, float baseG, float baseB, float time) {
        // Safety check
        if (buffer == null) return;
        
        int segments = 16; // Number of vertical segments for smooth fade
        double yStep = (maxY - minY) / (double) segments;
        
        for (int i = 0; i < segments; i++) {
            double y1 = minY + i * yStep;
            double y2 = minY + (i + 1) * yStep;
            
            // Calculate alpha based on height (fade to sky)
            float heightRatio1 = (float) ((y1 - minY) / (maxY - minY));
            float heightRatio2 = (float) ((y2 - minY) / (maxY - minY));
            
            // Fade effect: solid at bottom, transparent at top
            float alpha1 = calculateAlpha(level, x1, y1, z1, x2, y2, z2, heightRatio1, time);
            float alpha2 = calculateAlpha(level, x1, y2, z1, x2, y2, z2, heightRatio2, time);
            
            // Add pulsing wave effect
            float wave = (float) Math.sin((heightRatio1 * 4 + time * 2) * Math.PI) * 0.1f;
            alpha1 = Math.max(0.0f, Math.min(1.0f, alpha1 + wave));
            alpha2 = Math.max(0.0f, Math.min(1.0f, alpha2 + wave));
            
            buffer.addVertex(matrix, (float) (x1 + offsetX), (float) (y1 + offsetY), (float) (z1 + offsetZ))
                    .setColor(baseR, baseG, baseB, alpha1);
            buffer.addVertex(matrix, (float) (x2 + offsetX), (float) (y1 + offsetY), (float) (z2 + offsetZ))
                    .setColor(baseR, baseG, baseB, alpha1);
            buffer.addVertex(matrix, (float) (x2 + offsetX), (float) (y2 + offsetY), (float) (z2 + offsetZ))
                    .setColor(baseR, baseG, baseB, alpha2);
            buffer.addVertex(matrix, (float) (x1 + offsetX), (float) (y2 + offsetY), (float) (z1 + offsetZ))
                    .setColor(baseR, baseG, baseB, alpha2);
        }
    }
    
    private static float calculateAlpha(net.minecraft.world.level.Level level, double x1, double y, double z1,
                                        double x2, double y2, double z2, float heightRatio, float time) {
        // More transparent base alpha with fade to sky
        float baseAlpha = 0.25f * (1.0f - heightRatio * 0.9f);
        
        // Check for nearby blocks to increase visibility slightly
        net.minecraft.core.BlockPos checkPos1 = new net.minecraft.core.BlockPos((int) x1, (int) y, (int) z1);
        net.minecraft.core.BlockPos checkPos2 = new net.minecraft.core.BlockPos((int) x2, (int) y, (int) z2);
        
        boolean hasBlockNearby = false;
        
        // Check positions near the wall
        for (int offset = -1; offset <= 1; offset++) {
            net.minecraft.core.BlockPos pos1 = checkPos1.offset(offset, 0, offset);
            net.minecraft.core.BlockPos pos2 = checkPos2.offset(offset, 0, offset);
            
            if (!level.getBlockState(pos1).isAir() || !level.getBlockState(pos2).isAir()) {
                hasBlockNearby = true;
                break;
            }
        }
        
        // Slight boost when near blocks
        if (hasBlockNearby) {
            baseAlpha = Math.min(0.5f, baseAlpha + 0.2f);
        }
        
        return baseAlpha;
    }
    
    private static void renderBubbleParticles(VertexConsumer buffer, Matrix4f matrix,
                                              double minX, double minZ, double maxX, double maxZ,
                                              int minY, int maxY,
                                              double offsetX, double offsetY, double offsetZ,
                                              float time, float baseR, float baseG, float baseB) {
        // Safety check
        if (buffer == null) return;
        
        // Bubble-like particles rising from each wall
        int particlesPerWall = 80;
        
        // North wall (minZ)
        renderWallBubbles(buffer, matrix, minX, minZ, maxX, minZ, minY, maxY,
                offsetX, offsetY, offsetZ, time, baseR, baseG, baseB, particlesPerWall, 0);
        
        // South wall (maxZ)
        renderWallBubbles(buffer, matrix, minX, maxZ, maxX, maxZ, minY, maxY,
                offsetX, offsetY, offsetZ, time, baseR, baseG, baseB, particlesPerWall, 1000);
        
        // West wall (minX)
        renderWallBubbles(buffer, matrix, minX, minZ, minX, maxZ, minY, maxY,
                offsetX, offsetY, offsetZ, time, baseR, baseG, baseB, particlesPerWall, 2000);
        
        // East wall (maxX)
        renderWallBubbles(buffer, matrix, maxX, minZ, maxX, maxZ, minY, maxY,
                offsetX, offsetY, offsetZ, time, baseR, baseG, baseB, particlesPerWall, 3000);
    }
    
    private static void renderWallBubbles(VertexConsumer buffer, Matrix4f matrix,
                                          double x1, double z1, double x2, double z2,
                                          int minY, int maxY,
                                          double offsetX, double offsetY, double offsetZ,
                                          float time, float baseR, float baseG, float baseB,
                                          int particleCount, int seedOffset) {
        // Safety check
        if (buffer == null) return;
        
        float heightRange = maxY - minY;
        float particleSize = 0.25f; // Larger, more visible particles
        
        for (int i = 0; i < particleCount; i++) {
            // Use seed for consistent random behavior per particle
            float seed = (i + seedOffset) * 0.1234f;
            
            // Random position along wall
            float positionRatio = (float) (Math.sin(seed * 12.9898 + 78.233) * 0.5 + 0.5);
            double baseX = x1 + (x2 - x1) * positionRatio;
            double baseZ = z1 + (z2 - z1) * positionRatio;
            
            // Bubble rises with random speed
            float riseSpeed = 0.15f + (float) Math.sin(seed * 7.123) * 0.05f;
            float cycleProgress = ((time * riseSpeed + seed) % 1.0f);
            
            // Height with wobble effect
            double y = minY + cycleProgress * heightRange;
            
            // Random wobble side-to-side (bubble-like motion)
            float wobbleFreq = 3.0f + (float) Math.sin(seed * 5.456) * 2.0f;
            float wobbleAmount = 0.3f;
            float wobbleX = (float) Math.sin((time + seed) * wobbleFreq) * wobbleAmount;
            float wobbleZ = (float) Math.cos((time + seed * 1.5f) * wobbleFreq) * wobbleAmount;
            
            // Apply wobble perpendicular to wall direction
            double dx = x2 - x1;
            double dz = z2 - z1;
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 0) {
                dx /= length;
                dz /= length;
            }
            
            // Perpendicular direction for wobble along wall
            double perpX = -dz;
            double perpZ = dx;
            
            double x = baseX + perpX * wobbleX;
            double z = baseZ + perpZ * wobbleX;
            
            // Fade in at bottom, fade out at top
            float fadeIn = Math.min(1.0f, cycleProgress * 8.0f);
            float fadeOut = Math.max(0.0f, 1.0f - (cycleProgress - 0.6f) * 2.5f);
            float alpha = fadeIn * fadeOut * 0.9f; // Higher alpha for visibility
            
            // Pulsing glow effect
            float pulse = (float) Math.sin((time * 2 + seed * 3) * Math.PI) * 0.3f + 0.7f;
            
            // Brighter colors
            float r = baseR * pulse * 1.2f;
            float g = baseG * pulse * 1.2f;
            float b = baseB * pulse * 1.2f;
            
            addSparkle(buffer, matrix, x + offsetX, y + offsetY, z + offsetZ,
                    particleSize, r, g, b, alpha);
        }
    }
    
    private static void renderGroundLines(VertexConsumer buffer, Matrix4f matrix,
                                          double minX, double minZ, double maxX, double maxZ,
                                          int minY,
                                          double offsetX, double offsetY, double offsetZ,
                                          float baseR, float baseG, float baseB) {
        // Safety check
        if (buffer == null) return;
        
        // Bright ground contact lines
        float lineHeight = 0.1f;
        float lineAlpha = 1.0f; // Fully opaque for clear visibility
        
        // Brighter version of base color
        float r = Math.min(1.0f, baseR * 1.5f);
        float g = Math.min(1.0f, baseG * 1.5f);
        float b = Math.min(1.0f, baseB * 1.5f);
        
        // North line
        addGroundLine(buffer, matrix, minX, minZ, maxX, minZ, minY, lineHeight,
                offsetX, offsetY, offsetZ, r, g, b, lineAlpha);
        
        // South line
        addGroundLine(buffer, matrix, minX, maxZ, maxX, maxZ, minY, lineHeight,
                offsetX, offsetY, offsetZ, r, g, b, lineAlpha);
        
        // West line
        addGroundLine(buffer, matrix, minX, minZ, minX, maxZ, minY, lineHeight,
                offsetX, offsetY, offsetZ, r, g, b, lineAlpha);
        
        // East line
        addGroundLine(buffer, matrix, maxX, minZ, maxX, maxZ, minY, lineHeight,
                offsetX, offsetY, offsetZ, r, g, b, lineAlpha);
    }
    
    private static void addGroundLine(VertexConsumer buffer, Matrix4f matrix,
                                      double x1, double z1, double x2, double z2,
                                      int y, float height,
                                      double offsetX, double offsetY, double offsetZ,
                                      float r, float g, float b, float a) {
        // Safety check
        if (buffer == null) return;
        
        // Create a bright line at ground level
        buffer.addVertex(matrix, (float) (x1 + offsetX), (float) (y + offsetY), (float) (z1 + offsetZ))
                .setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) (x2 + offsetX), (float) (y + offsetY), (float) (z2 + offsetZ))
                .setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) (x2 + offsetX), (float) (y + height + offsetY), (float) (z2 + offsetZ))
                .setColor(r, g, b, a * 0.3f);
        buffer.addVertex(matrix, (float) (x1 + offsetX), (float) (y + height + offsetY), (float) (z1 + offsetZ))
                .setColor(r, g, b, a * 0.3f);
    }
    
    private static void renderVerticalEdges(VertexConsumer buffer, Matrix4f matrix,
                                            double minX, double minZ, double maxX, double maxZ,
                                            int minY, int maxY,
                                            double offsetX, double offsetY, double offsetZ,
                                            float baseR, float baseG, float baseB) {
        // Safety check
        if (buffer == null) return;
        
        // Brighter color for edges
        float r = Math.min(1.0f, baseR * 1.8f);
        float g = Math.min(1.0f, baseG * 1.5f);
        float b = Math.min(1.0f, baseB * 1.4f);
        float edgeWidth = 0.08f;
        float alpha = 1.0f; // Fully opaque
        
        // Render 4 vertical edge lines at corners
        // NW corner
        renderVerticalEdge(buffer, matrix, minX, minZ, minY, maxY, edgeWidth,
                offsetX, offsetY, offsetZ, r, g, b, alpha);
        
        // NE corner
        renderVerticalEdge(buffer, matrix, maxX, minZ, minY, maxY, edgeWidth,
                offsetX, offsetY, offsetZ, r, g, b, alpha);
        
        // SW corner
        renderVerticalEdge(buffer, matrix, minX, maxZ, minY, maxY, edgeWidth,
                offsetX, offsetY, offsetZ, r, g, b, alpha);
        
        // SE corner
        renderVerticalEdge(buffer, matrix, maxX, maxZ, minY, maxY, edgeWidth,
                offsetX, offsetY, offsetZ, r, g, b, alpha);
    }
    
    private static void renderVerticalEdge(VertexConsumer buffer, Matrix4f matrix,
                                           double x, double z, int minY, int maxY, float width,
                                           double offsetX, double offsetY, double offsetZ,
                                           float r, float g, float b, float a) {
        // Safety check
        if (buffer == null) return;
        
        // Vertical edge line as a thin quad
        buffer.addVertex(matrix, (float) (x - width + offsetX), (float) (minY + offsetY), (float) (z + offsetZ))
                .setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) (x + width + offsetX), (float) (minY + offsetY), (float) (z + offsetZ))
                .setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) (x + width + offsetX), (float) (maxY + offsetY), (float) (z + offsetZ))
                .setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) (x - width + offsetX), (float) (maxY + offsetY), (float) (z + offsetZ))
                .setColor(r, g, b, a);
    }
    
    private static void addSparkle(VertexConsumer buffer, Matrix4f matrix,
                                   double x, double y, double z, float size,
                                   float r, float g, float b, float a) {
        // Safety check
        if (buffer == null) return;
        
        // Create a small quad for sparkle (billboard-like effect)
        buffer.addVertex(matrix, (float) (x - size), (float) (y - size), (float) z).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) (x + size), (float) (y - size), (float) z).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) (x + size), (float) (y + size), (float) z).setColor(r, g, b, a);
        buffer.addVertex(matrix, (float) (x - size), (float) (y + size), (float) z).setColor(r, g, b, a);
    }

    /**
     * Clears the cached realm data
     */
    public static void clear() {
        nearbyRealms.clear();
    }
}
