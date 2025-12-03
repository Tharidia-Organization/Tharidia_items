package com.tharidia.tharidia_things.client.video;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tharidia.tharidia_things.TharidiaThings;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Event handler for rendering video screens in the world
 */
public class VideoScreenRenderHandler {
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Render during the AFTER_TRANSLUCENT_BLOCKS stage to ensure proper depth
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            
            PoseStack poseStack = event.getPoseStack();
            
            // Get camera position
            var camera = mc.gameRenderer.getMainCamera();
            var cameraPos = camera.getPosition();
            
            // Render all video screens
            VideoScreenRenderer.renderScreens(poseStack, cameraPos);
        }
    }
    
    /**
     * Called when a level is unloaded (world exit, dimension change, disconnect)
     * Ensures all video/audio processes are killed
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            TharidiaThings.LOGGER.info("[VIDEO] Level unloading - stopping all video players");
            ClientVideoScreenManager.getInstance().clear();
        }
    }
    
    /**
     * Called every client tick to update video players
     */
    public static void onClientTick() {
        ClientVideoScreenManager.getInstance().tick();
    }
}
