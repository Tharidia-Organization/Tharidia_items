package com.tharidia.tharidia_things.client.video;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.video.YouTubeUrlExtractor;

import javax.swing.SwingUtilities;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages detection and installation of required video tools (FFmpeg, FFplay, yt-dlp)
 * on Windows systems. Shows installation GUI when tools are missing.
 */
public class VideoToolsManager {
    private static final VideoToolsManager INSTANCE = new VideoToolsManager();
    
    // Detection state
    private final AtomicBoolean hasChecked = new AtomicBoolean(false);
    private final AtomicBoolean allToolsPresent = new AtomicBoolean(false);
    private boolean isWindows = false;
    
    // Tool status
    private boolean ffmpegFound = false;
    private boolean ffplayFound = false;
    private boolean ytDlpFound = false;
    
    private VideoToolsManager() {}
    
    public static VideoToolsManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Check if all required video tools are available.
     * Shows installation GUI on Windows if tools are missing.
     * @return true if all tools are available, false otherwise
     */
    public boolean checkAndInstallTools() {
        if (hasChecked.get()) {
            return allToolsPresent.get();
        }
        
        // Detect OS
        String os = System.getProperty("os.name").toLowerCase();
        isWindows = os.contains("win");
        
        if (!isWindows) {
            // On non-Windows systems, assume tools are in PATH
            allToolsPresent.set(true);
            hasChecked.set(true);
            return true;
        }
        
        // Check for tools on Windows
        checkWindowsTools();
        
        if (!ffmpegFound || !ffplayFound || !ytDlpFound) {
            TharidiaThings.LOGGER.warn("[VIDEO TOOLS] Missing tools detected - FFmpeg: {}, FFplay: {}, yt-dlp: {}", 
                ffmpegFound, ffplayFound, ytDlpFound);
            
            // Show installation GUI
            showInstallationGUI();
            
            // Return false for now - GUI will set this to true after successful installation
            allToolsPresent.set(false);
        } else {
            TharidiaThings.LOGGER.info("[VIDEO TOOLS] All required tools found");
            allToolsPresent.set(true);
        }
        
        hasChecked.set(true);
        return allToolsPresent.get();
    }
    
    /**
     * Get the path to FFmpeg executable
     */
    public String getFfmpegPath() {
        checkAndInstallTools();
        return findExecutable("ffmpeg");
    }
    
    /**
     * Get the path to FFplay executable
     */
    public String getFfplayPath() {
        checkAndInstallTools();
        return findExecutable("ffplay");
    }
    
    /**
     * Get the path to yt-dlp executable
     */
    public String getYtDlpPath() {
        checkAndInstallTools();
        return findExecutable("yt-dlp");
    }
    
    /**
     * Force re-check tools (useful after installation)
     */
    public void recheckTools() {
        hasChecked.set(false);
        allToolsPresent.set(false);
        checkAndInstallTools();
    }
    
    private void checkWindowsTools() {
        // Check FFmpeg
        ffmpegFound = isExecutableAvailable("ffmpeg");
        
        // Check FFplay
        ffplayFound = isExecutableAvailable("ffplay");
        
        // Check yt-dlp
        ytDlpFound = isExecutableAvailable("yt-dlp");
    }
    
    private boolean isExecutableAvailable(String execName) {
        List<String> searchPaths = getSearchPaths(execName);
        
        for (String path : searchPaths) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] Found {} at: {}", execName, path);
                return true;
            }
        }
        
        // Try PATH as last resort
        try {
            ProcessBuilder pb = new ProcessBuilder("where", execName + ".exe");
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] Found {} in PATH", execName);
                return true;
            }
        } catch (Exception e) {
            // Not found in PATH
        }
        
        TharidiaThings.LOGGER.warn("[VIDEO TOOLS] {} not found", execName);
        return false;
    }
    
    private List<String> getSearchPaths(String execName) {
        List<String> paths = new ArrayList<>();
        String exeName = execName + ".exe";
        
        // Current working directory
        paths.add(System.getProperty("user.dir") + File.separator + exeName);
        
        // .minecraft folder
        String minecraftDir = System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming" + File.separator + ".minecraft";
        paths.add(minecraftDir + File.separator + exeName);
        paths.add(minecraftDir + File.separator + "bin" + File.separator + exeName);
        
        // Tharidia tools directory
        String tharidiaDir = System.getProperty("user.home") + File.separator + ".tharidia" + File.separator + "bin";
        paths.add(tharidiaDir + File.separator + exeName);
        
        // Common installation paths
        paths.add("C:\\ffmpeg\\bin\\" + exeName);
        paths.add("C:\\Program Files\\ffmpeg\\bin\\" + exeName);
        paths.add("C:\\Program Files (x86)\\ffmpeg\\bin\\" + exeName);
        
        return paths;
    }
    
    private String findExecutable(String execName) {
        List<String> searchPaths = getSearchPaths(execName);
        
        for (String path : searchPaths) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                return path;
            }
        }
        
        // Return just the name and hope it's in PATH
        return execName + ".exe";
    }
    
    private void showInstallationGUI() {
        SwingUtilities.invokeLater(() -> {
            List<ToolStatus> toolStatuses = new ArrayList<>();
            toolStatuses.add(new ToolStatus("FFmpeg", "ffmpeg.exe", ffmpegFound, null));
            toolStatuses.add(new ToolStatus("FFplay", "ffplay.exe", ffplayFound, null));
            toolStatuses.add(new ToolStatus("yt-dlp", "yt-dlp.exe", ytDlpFound, null));
            
            VideoToolsInstallerGUI gui = new VideoToolsInstallerGUI(toolStatuses, this);
            gui.show();
        });
    }
    
    /**
     * Called by the installer GUI after successful installation
     */
    public void onInstallationComplete() {
        TharidiaThings.LOGGER.info("[VIDEO TOOLS] Installation completed, rechecking tools");
        recheckTools();
    }
    
    public static class ToolStatus {
        public final String name;
        public final String exeName;
        public final boolean isInstalled;
        public final String path;
        
        public ToolStatus(String name, String exeName, boolean isInstalled, String path) {
            this.name = name;
            this.exeName = exeName;
            this.isInstalled = isInstalled;
            this.path = path;
        }
    }
}
