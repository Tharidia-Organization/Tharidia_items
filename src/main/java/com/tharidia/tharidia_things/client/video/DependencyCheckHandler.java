package com.tharidia.tharidia_things.client.video;

import com.tharidia.tharidia_things.TharidiaThings;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

/**
 * Checks for missing video dependencies on client startup
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class DependencyCheckHandler {
    
    private static boolean hasChecked = false;
    private static boolean isCheckScheduled = false;
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Check once after the client has fully loaded
        if (!hasChecked && !isCheckScheduled) {
            Minecraft mc = Minecraft.getInstance();
            
            // Wait until we're in the main menu or in-game
            if (mc.screen != null || mc.level != null) {
                isCheckScheduled = true;
                
                // Schedule check for next tick to avoid concurrent modification
                mc.execute(() -> {
                    checkDependencies();
                    hasChecked = true;
                });
            }
        }
    }
    
    private static void checkDependencies() {
        String os = System.getProperty("os.name").toLowerCase();
        
        // Only check on Windows
        if (!os.contains("win")) {
            TharidiaThings.LOGGER.info("Not on Windows, skipping dependency check");
            return;
        }
        
        TharidiaThings.LOGGER.info("Checking for missing video dependencies...");
        
        List<DependencyDownloader.Dependency> missing = DependencyDownloader.checkMissingDependencies();
        
        if (missing.isEmpty()) {
            TharidiaThings.LOGGER.info("All video dependencies are installed");
            return;
        }
        
        TharidiaThings.LOGGER.warn("Missing dependencies: {}", missing);
        
        // Open setup screen
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.setScreen(new DependencySetupScreen(mc.screen, missing));
        });
    }
    
    /**
     * Force a re-check (useful for testing or after manual installation)
     */
    public static void forceRecheck() {
        hasChecked = false;
        isCheckScheduled = false;
    }
}
