package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.gui.TradeMenu;
import com.THproject.tharidia_things.trade.TradeManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Handles camera manipulation during trades
 * Switches to third person and applies shoulder view offset
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class TradeCameraHandler {
    
    private static CameraType previousCameraType = null;
    private static boolean wasInTrade = false;
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != player) {
            return;
        }
        
        // Check if player is in a trade
        boolean inTrade = TradeManager.isPlayerInTrade(player.getUUID()) && 
                         player.containerMenu instanceof TradeMenu;
        
        // Handle camera type switching
        if (inTrade && !wasInTrade) {
            // Entering trade - switch to third person
            previousCameraType = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            wasInTrade = true;
        } else if (!inTrade && wasInTrade) {
            // Exiting trade - restore previous camera
            if (previousCameraType != null) {
                minecraft.options.setCameraType(previousCameraType);
                previousCameraType = null;
            }
            wasInTrade = false;
        }
    }
    
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player == null) {
            return;
        }
        
        // Check if player is in a trade
        boolean inTrade = TradeManager.isPlayerInTrade(player.getUUID()) && 
                         player.containerMenu instanceof TradeMenu;
        
        if (inTrade && minecraft.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
            // Adjust camera angles for better shoulder view
            // Rotate camera slightly to the right and up
            double yaw = event.getYaw();
            double pitch = event.getPitch();
            
            // Add offset to yaw (rotate right) and pitch (look down slightly)
            event.setYaw((float) (yaw + 15)); // 15 degrees to the right
            event.setPitch((float) (pitch - 10)); // 10 degrees up
        }
    }
    
    /**
     * Reset camera (called when trade ends)
     */
    public static void resetCamera() {
        Minecraft minecraft = Minecraft.getInstance();
        if (previousCameraType != null) {
            minecraft.options.setCameraType(previousCameraType);
            previousCameraType = null;
        }
        wasInTrade = false;
    }
}
