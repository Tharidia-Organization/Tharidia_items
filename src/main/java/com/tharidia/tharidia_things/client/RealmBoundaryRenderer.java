package com.tharidia.tharidia_things.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.block.entity.PietroBlockEntity;
import com.tharidia.tharidia_things.client.ClientPacketHandler;
import com.tharidia.tharidia_things.realm.RealmManager;
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
    private static boolean wasHoldingMonocolo = false;
    private static float colorSeedR = 0.9f;
    private static float colorSeedG = 0.5f;
    private static float colorSeedB = 0.8f;
    private static java.util.Random colorRandom = new java.util.Random();

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
        if (mc.player == null) {
            return;
        }

        // Check if player is holding monocolo in either hand
        boolean holdingMonocolo = mc.player.getMainHandItem().is(TharidiaThings.MONOCOLO.get()) ||
                                   mc.player.getOffhandItem().is(TharidiaThings.MONOCOLO.get());
        
        if (!holdingMonocolo) {
            wasHoldingMonocolo = false;
            return;
        }
        
        // Randomize colors when first picking up monocolo
        if (!wasHoldingMonocolo) {
            wasHoldingMonocolo = true;
            randomizeColors();
        }

        updateNearbyRealms();

        if (nearbyRealms.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        // Setup rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        poseStack.pushPose();

        for (PietroBlockEntity realm : nearbyRealms) {
            renderRealmBoundary(realm, poseStack, bufferBuilder, cameraPos);
        }

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        poseStack.popPose();

        // Restore render state
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void renderRealmBoundary(PietroBlockEntity realm, PoseStack poseStack, BufferBuilder bufferBuilder, Vec3 cameraPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        ChunkPos minChunk = realm.getMinChunk();
        ChunkPos maxChunk = realm.getMaxChunk();

        // Convert chunk coordinates to block coordinates
        int minX = minChunk.getMinBlockX();
        int minZ = minChunk.getMinBlockZ();
        int maxX = maxChunk.getMaxBlockX() + 1;
        int maxZ = maxChunk.getMaxBlockZ() + 1;

        // Get Y coordinates (from bedrock to build limit)
        int minY = mc.level.getMinBuildHeight();
        int maxY = mc.level.getMaxBuildHeight();

        // Offset by camera position for world rendering
        double offsetX = -cameraPos.x;
        double offsetY = -cameraPos.y;
        double offsetZ = -cameraPos.z;

        // Magical animated color with shimmer effect
        float time = (System.currentTimeMillis() % 10000) / 10000.0f;
        float shimmer = (float) Math.sin(time * Math.PI * 2.0) * 0.15f + 0.85f;
        
        // Use randomized colors with shimmer
        float baseR = colorSeedR * shimmer;
        float baseG = colorSeedG * shimmer;
        float baseB = colorSeedB * shimmer;

        Matrix4f matrix = poseStack.last().pose();

        // Render the four vertical walls with enhanced effects
        renderEnhancedWall(bufferBuilder, matrix, mc.level,
                minX, minZ, maxX, minZ, minY, maxY, // North wall
                offsetX, offsetY, offsetZ, baseR, baseG, baseB, time);

        renderEnhancedWall(bufferBuilder, matrix, mc.level,
                minX, maxZ, maxX, maxZ, minY, maxY, // South wall
                offsetX, offsetY, offsetZ, baseR, baseG, baseB, time);

        renderEnhancedWall(bufferBuilder, matrix, mc.level,
                minX, minZ, minX, maxZ, minY, maxY, // West wall
                offsetX, offsetY, offsetZ, baseR, baseG, baseB, time);

        renderEnhancedWall(bufferBuilder, matrix, mc.level,
                maxX, minZ, maxX, maxZ, minY, maxY, // East wall
                offsetX, offsetY, offsetZ, baseR, baseG, baseB, time);
        
        // Add bubble-like particles rising from wall surfaces
        renderBubbleParticles(bufferBuilder, matrix, minX, minZ, maxX, maxZ, minY, maxY,
                offsetX, offsetY, offsetZ, time, baseR, baseG, baseB);
        
        // Render clear ground contact lines
        renderGroundLines(bufferBuilder, matrix, minX, minZ, maxX, maxZ, minY,
                offsetX, offsetY, offsetZ, baseR, baseG, baseB);
        
        // Render bright vertical edges at corners
        renderVerticalEdges(bufferBuilder, matrix, minX, minZ, maxX, maxZ, minY, maxY,
                offsetX, offsetY, offsetZ, baseR, baseG, baseB);
    }
    
    private static void renderEnhancedWall(BufferBuilder bufferBuilder, Matrix4f matrix, net.minecraft.world.level.Level level,
                                           double x1, double z1, double x2, double z2,
                                           int minY, int maxY,
                                           double offsetX, double offsetY, double offsetZ,
                                           float baseR, float baseG, float baseB, float time) {
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
            
            // Add the quad segment
            bufferBuilder.addVertex(matrix, (float) (x1 + offsetX), (float) (y1 + offsetY), (float) (z1 + offsetZ))
                    .setColor(baseR, baseG, baseB, alpha1);
            bufferBuilder.addVertex(matrix, (float) (x2 + offsetX), (float) (y1 + offsetY), (float) (z2 + offsetZ))
                    .setColor(baseR, baseG, baseB, alpha1);
            bufferBuilder.addVertex(matrix, (float) (x2 + offsetX), (float) (y2 + offsetY), (float) (z2 + offsetZ))
                    .setColor(baseR, baseG, baseB, alpha2);
            bufferBuilder.addVertex(matrix, (float) (x1 + offsetX), (float) (y2 + offsetY), (float) (z1 + offsetZ))
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
    
    private static void renderBubbleParticles(BufferBuilder bufferBuilder, Matrix4f matrix,
                                              double minX, double minZ, double maxX, double maxZ,
                                              int minY, int maxY,
                                              double offsetX, double offsetY, double offsetZ,
                                              float time, float baseR, float baseG, float baseB) {
        // Bubble-like particles rising from each wall
        int particlesPerWall = 80;
        
        // North wall (minZ)
        renderWallBubbles(bufferBuilder, matrix, minX, minZ, maxX, minZ, minY, maxY,
                offsetX, offsetY, offsetZ, time, baseR, baseG, baseB, particlesPerWall, 0);
        
        // South wall (maxZ)
        renderWallBubbles(bufferBuilder, matrix, minX, maxZ, maxX, maxZ, minY, maxY,
                offsetX, offsetY, offsetZ, time, baseR, baseG, baseB, particlesPerWall, 1000);
        
        // West wall (minX)
        renderWallBubbles(bufferBuilder, matrix, minX, minZ, minX, maxZ, minY, maxY,
                offsetX, offsetY, offsetZ, time, baseR, baseG, baseB, particlesPerWall, 2000);
        
        // East wall (maxX)
        renderWallBubbles(bufferBuilder, matrix, maxX, minZ, maxX, maxZ, minY, maxY,
                offsetX, offsetY, offsetZ, time, baseR, baseG, baseB, particlesPerWall, 3000);
    }
    
    private static void renderWallBubbles(BufferBuilder bufferBuilder, Matrix4f matrix,
                                          double x1, double z1, double x2, double z2,
                                          int minY, int maxY,
                                          double offsetX, double offsetY, double offsetZ,
                                          float time, float baseR, float baseG, float baseB,
                                          int particleCount, int seedOffset) {
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
            
            addSparkle(bufferBuilder, matrix, x + offsetX, y + offsetY, z + offsetZ,
                    particleSize, r, g, b, alpha);
        }
    }
    
    private static void renderGroundLines(BufferBuilder bufferBuilder, Matrix4f matrix,
                                          double minX, double minZ, double maxX, double maxZ,
                                          int minY,
                                          double offsetX, double offsetY, double offsetZ,
                                          float baseR, float baseG, float baseB) {
        // Bright ground contact lines
        float lineHeight = 0.1f;
        float lineAlpha = 1.0f; // Fully opaque for clear visibility
        
        // Brighter version of base color
        float r = Math.min(1.0f, baseR * 1.5f);
        float g = Math.min(1.0f, baseG * 1.5f);
        float b = Math.min(1.0f, baseB * 1.5f);
        
        // North line
        addGroundLine(bufferBuilder, matrix, minX, minZ, maxX, minZ, minY, lineHeight,
                offsetX, offsetY, offsetZ, r, g, b, lineAlpha);
        
        // South line
        addGroundLine(bufferBuilder, matrix, minX, maxZ, maxX, maxZ, minY, lineHeight,
                offsetX, offsetY, offsetZ, r, g, b, lineAlpha);
        
        // West line
        addGroundLine(bufferBuilder, matrix, minX, minZ, minX, maxZ, minY, lineHeight,
                offsetX, offsetY, offsetZ, r, g, b, lineAlpha);
        
        // East line
        addGroundLine(bufferBuilder, matrix, maxX, minZ, maxX, maxZ, minY, lineHeight,
                offsetX, offsetY, offsetZ, r, g, b, lineAlpha);
    }
    
    private static void addGroundLine(BufferBuilder bufferBuilder, Matrix4f matrix,
                                      double x1, double z1, double x2, double z2,
                                      int y, float height,
                                      double offsetX, double offsetY, double offsetZ,
                                      float r, float g, float b, float a) {
        // Create a bright line at ground level
        bufferBuilder.addVertex(matrix, (float) (x1 + offsetX), (float) (y + offsetY), (float) (z1 + offsetZ))
                .setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, (float) (x2 + offsetX), (float) (y + offsetY), (float) (z2 + offsetZ))
                .setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, (float) (x2 + offsetX), (float) (y + height + offsetY), (float) (z2 + offsetZ))
                .setColor(r, g, b, a * 0.3f);
        bufferBuilder.addVertex(matrix, (float) (x1 + offsetX), (float) (y + height + offsetY), (float) (z1 + offsetZ))
                .setColor(r, g, b, a * 0.3f);
    }
    
    private static void renderVerticalEdges(BufferBuilder bufferBuilder, Matrix4f matrix,
                                            double minX, double minZ, double maxX, double maxZ,
                                            int minY, int maxY,
                                            double offsetX, double offsetY, double offsetZ,
                                            float baseR, float baseG, float baseB) {
        // Brighter color for edges
        float r = Math.min(1.0f, baseR * 1.8f);
        float g = Math.min(1.0f, baseG * 1.5f);
        float b = Math.min(1.0f, baseB * 1.4f);
        float edgeWidth = 0.08f;
        float alpha = 1.0f; // Fully opaque
        
        // Render 4 vertical edge lines at corners
        // NW corner
        renderVerticalEdge(bufferBuilder, matrix, minX, minZ, minY, maxY, edgeWidth,
                offsetX, offsetY, offsetZ, r, g, b, alpha);
        
        // NE corner
        renderVerticalEdge(bufferBuilder, matrix, maxX, minZ, minY, maxY, edgeWidth,
                offsetX, offsetY, offsetZ, r, g, b, alpha);
        
        // SW corner
        renderVerticalEdge(bufferBuilder, matrix, minX, maxZ, minY, maxY, edgeWidth,
                offsetX, offsetY, offsetZ, r, g, b, alpha);
        
        // SE corner
        renderVerticalEdge(bufferBuilder, matrix, maxX, maxZ, minY, maxY, edgeWidth,
                offsetX, offsetY, offsetZ, r, g, b, alpha);
    }
    
    private static void renderVerticalEdge(BufferBuilder bufferBuilder, Matrix4f matrix,
                                           double x, double z, int minY, int maxY, float width,
                                           double offsetX, double offsetY, double offsetZ,
                                           float r, float g, float b, float a) {
        // Vertical edge line as a thin quad
        bufferBuilder.addVertex(matrix, (float) (x - width + offsetX), (float) (minY + offsetY), (float) (z + offsetZ))
                .setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, (float) (x + width + offsetX), (float) (minY + offsetY), (float) (z + offsetZ))
                .setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, (float) (x + width + offsetX), (float) (maxY + offsetY), (float) (z + offsetZ))
                .setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, (float) (x - width + offsetX), (float) (maxY + offsetY), (float) (z + offsetZ))
                .setColor(r, g, b, a);
    }
    
    private static void addSparkle(BufferBuilder bufferBuilder, Matrix4f matrix,
                                   double x, double y, double z, float size,
                                   float r, float g, float b, float a) {
        // Create a small quad for sparkle (billboard-like effect)
        bufferBuilder.addVertex(matrix, (float) (x - size), (float) (y - size), (float) z).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, (float) (x + size), (float) (y - size), (float) z).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, (float) (x + size), (float) (y + size), (float) z).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, (float) (x - size), (float) (y + size), (float) z).setColor(r, g, b, a);
    }

    private static void addQuad(BufferBuilder bufferBuilder, Matrix4f matrix,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 double x3, double y3, double z3,
                                 double x4, double y4, double z4,
                                 float r, float g, float b, float a) {
        bufferBuilder.addVertex(matrix, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, (float) x3, (float) y3, (float) z3).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, (float) x4, (float) y4, (float) z4).setColor(r, g, b, a);
    }

    /**
     * Clears the cached realm data
     */
    public static void clear() {
        nearbyRealms.clear();
    }
}
