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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class RealmBoundaryRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static List<PietroBlockEntity> nearbyRealms = new ArrayList<>();
    private static long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 1000; // Update every second
    private static long lastLogTime = 0;
    private static final long LOG_INTERVAL = 5000; // Log every 5 seconds

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
        if (!mc.player.getMainHandItem().is(TharidiaThings.MONOCOLO.get()) &&
            !mc.player.getOffhandItem().is(TharidiaThings.MONOCOLO.get())) {
            return;
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
        ChunkPos minChunk = realm.getMinChunk();
        ChunkPos maxChunk = realm.getMaxChunk();

        // Convert chunk coordinates to block coordinates
        int minX = minChunk.getMinBlockX();
        int minZ = minChunk.getMinBlockZ();
        int maxX = maxChunk.getMaxBlockX() + 1;
        int maxZ = maxChunk.getMaxBlockZ() + 1;

        // Get Y coordinates (from bedrock to build limit)
        int minY = -64;
        int maxY = 320;

        // Offset by camera position for world rendering
        double offsetX = -cameraPos.x;
        double offsetY = -cameraPos.y;
        double offsetZ = -cameraPos.z;

        // Color: Golden with transparency
        float r = 1.0f;
        float g = 0.67f;
        float b = 0.0f;
        float a = 0.3f;

        Matrix4f matrix = poseStack.last().pose();

        // Render the four vertical walls
        // North wall (minZ)
        addQuad(bufferBuilder, matrix,
                minX + offsetX, minY + offsetY, minZ + offsetZ,
                maxX + offsetX, minY + offsetY, minZ + offsetZ,
                maxX + offsetX, maxY + offsetY, minZ + offsetZ,
                minX + offsetX, maxY + offsetY, minZ + offsetZ,
                r, g, b, a);

        // South wall (maxZ)
        addQuad(bufferBuilder, matrix,
                minX + offsetX, minY + offsetY, maxZ + offsetZ,
                maxX + offsetX, minY + offsetY, maxZ + offsetZ,
                maxX + offsetX, maxY + offsetY, maxZ + offsetZ,
                minX + offsetX, maxY + offsetY, maxZ + offsetZ,
                r, g, b, a);

        // West wall (minX)
        addQuad(bufferBuilder, matrix,
                minX + offsetX, minY + offsetY, minZ + offsetZ,
                minX + offsetX, minY + offsetY, maxZ + offsetZ,
                minX + offsetX, maxY + offsetY, maxZ + offsetZ,
                minX + offsetX, maxY + offsetY, minZ + offsetZ,
                r, g, b, a);

        // East wall (maxX)
        addQuad(bufferBuilder, matrix,
                maxX + offsetX, minY + offsetY, minZ + offsetZ,
                maxX + offsetX, minY + offsetY, maxZ + offsetZ,
                maxX + offsetX, maxY + offsetY, maxZ + offsetZ,
                maxX + offsetX, maxY + offsetY, minZ + offsetZ,
                r, g, b, a);
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
