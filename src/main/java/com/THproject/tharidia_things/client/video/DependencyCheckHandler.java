package com.THproject.tharidia_things.client.video;

import com.THproject.tharidia_things.Config;
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

        // Check if auto-install is enabled in config (with safety check for config not loaded)
        boolean autoInstallEnabled;
        try {
            autoInstallEnabled = Config.VIDEO_TOOLS_AUTO_INSTALL.get();
        } catch (IllegalStateException e) {
            // Config not loaded yet, retry on next tick
            TharidiaThings.LOGGER.debug("Config not loaded yet, will retry dependency check later");
            hasChecked = false;
            isCheckScheduled = false;
            return;
        }

        // Use VideoToolsManager's detection logic
        VideoToolsManager toolsManager = VideoToolsManager.getInstance();

        if (autoInstallEnabled) {
            // Original behavior: check and show installer GUI if missing
            boolean allPresent = toolsManager.checkAndInstallTools();

            if (allPresent) {
                TharidiaThings.LOGGER.info("All video dependencies are installed");
                return;
            }

            // Create missing list based on VideoToolsManager's findings
            List<DependencyDownloader.Dependency> missing = new ArrayList<>();
            if (!toolsManager.isFfmpegFound() || !toolsManager.isFfplayFound()) {
                missing.add(DependencyDownloader.Dependency.FFMPEG);
            }
            if (!toolsManager.isYtDlpFound()) {
                missing.add(DependencyDownloader.Dependency.YTDLP);
            }
            if (!isExecutableInPath("streamlink")) {
                missing.add(DependencyDownloader.Dependency.STREAMLINK);
            }

            TharidiaThings.LOGGER.warn("Missing dependencies: {}", missing);

            // Open setup screen (with download capability)
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                mc.setScreen(new DependencySetupScreen(mc.screen, missing));
            });
        } else {
            // CurseForge-compliant mode: only check presence, show info screen if missing
            boolean allPresent = toolsManager.checkToolsPresenceOnly();

            if (allPresent) {
                TharidiaThings.LOGGER.info("All video dependencies are installed (manual install mode)");
                return;
            }

            TharidiaThings.LOGGER.info("Video tools missing - auto-install disabled (CurseForge mode)");
            TharidiaThings.LOGGER.info("Users must install FFmpeg, yt-dlp, and streamlink manually");
            // Don't show any GUI on startup - only inform when user tries to use video features
        }
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
     * DISABLED: ProcessBuilder execution not allowed for CurseForge compliance
     * Now delegates to VideoToolsManager which checks file existence only
     */
    private static boolean isExecutableInPath(String execName) {
        // ProcessBuilder disabled for CurseForge compliance
        // Check using VideoToolsManager's file-based detection
        VideoToolsManager manager = VideoToolsManager.getInstance();
        if (execName.equals("streamlink")) {
            return manager.isStreamlinkFound();
        }
        return false;
    }
    
    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new DependencyCheckHandler());
    }
}
