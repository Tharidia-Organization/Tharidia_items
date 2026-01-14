package com.THproject.tharidia_things.client.video;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks for missing video dependencies on client startup
 */
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
        
        // Check on all platforms
        TharidiaThings.LOGGER.info("Checking for missing video dependencies...");
        
        // Use VideoToolsManager's detection logic instead
        VideoToolsManager toolsManager = VideoToolsManager.getInstance();
        boolean allPresent = toolsManager.checkAndInstallTools();
        
        if (allPresent) {
            TharidiaThings.LOGGER.info("All video dependencies are installed");
            return;
        }
        
        // Create missing list based on VideoToolsManager's findings
        List<DependencyDownloader.Dependency> missing = new ArrayList<>();
        if (!toolsManager.isFfmpegFound() || !toolsManager.isFfplayFound()) {
            missing.add(DependencyDownloader.Dependency.FFMPEG); // FFmpeg and FFplay are bundled together
        }
        if (!toolsManager.isYtDlpFound()) {
            missing.add(DependencyDownloader.Dependency.YTDLP);
        }
        // Check if streamlink is available in PATH
        if (!isExecutableInPath("streamlink")) {
            missing.add(DependencyDownloader.Dependency.STREAMLINK);
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
    
    /**
     * Check if an executable is available in PATH
     */
    private static boolean isExecutableInPath(String execName) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            
            if (os.contains("win")) {
                pb = new ProcessBuilder("where", execName + ".exe");
            } else {
                pb = new ProcessBuilder("which", execName);
            }
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new DependencyCheckHandler());
    }
}
