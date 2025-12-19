package com.THproject.tharidia_things.client.video;

import com.THproject.tharidia_things.TharidiaThings;

import javax.swing.SwingUtilities;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private boolean streamlinkFound = false;
    
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
        return checkAndInstallTools("");
    }
    
    /**
     * Check if required video tools are available for a specific URL.
     * Shows installation GUI on Windows if tools are missing.
     * @param videoUrl The video URL to check tools for
     * @return true if required tools are available, false otherwise
     */
    public boolean checkAndInstallTools(String videoUrl) {
        if (hasChecked.get()) {
            return allToolsPresent.get();
        }
        
        // Detect OS
        String os = System.getProperty("os.name").toLowerCase();
        isWindows = os.contains("win");
        
        // Check for tools on all systems
        checkWindowsTools(); // This method name is misleading - it checks tools regardless of OS
        
        // Determine if streamlink is needed (only for Twitch URLs)
        boolean needsStreamlink = videoUrl != null && videoUrl.contains("twitch.tv");
        
        // Set tool status based on what we found
        if (!ffmpegFound || !ffplayFound || !ytDlpFound || (needsStreamlink && !streamlinkFound)) {
            TharidiaThings.LOGGER.warn("[VIDEO TOOLS] Missing tools detected - FFmpeg: {}, FFplay: {}, yt-dlp: {}, streamlink: {} (needed: {})", 
                ffmpegFound, ffplayFound, ytDlpFound, streamlinkFound, needsStreamlink);
            allToolsPresent.set(false);
            
            // Only show installation GUI if tools are missing
            TharidiaThings.LOGGER.info("[VIDEO TOOLS] Missing tools detected - showing installation GUI");
            showInstallationGUI();
        } else {
            TharidiaThings.LOGGER.info("[VIDEO TOOLS] All required tools found");
            allToolsPresent.set(true);
            // Don't show GUI when all tools are present
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
        
        // Check streamlink
        streamlinkFound = isExecutableAvailable("streamlink");
    }
    
    private boolean isExecutableAvailable(String execName) {
        // On Linux, try 'which' command FIRST - this is the most reliable way to find tools
        if (!isWindows) {
            String pathFromWhich = findExecutableWithWhich(execName);
            if (pathFromWhich != null) {
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] Found {} via 'which' at: {}", execName, pathFromWhich);
                // Verify it works
                if (verifyExecutableWorks(pathFromWhich, execName)) {
                    return true;
                }
            }
        }
        
        // Check hardcoded paths
        List<String> searchPaths = getSearchPaths(execName);
        String foundPath = null;
        
        TharidiaThings.LOGGER.info("[VIDEO TOOLS] Checking for {} in {} locations", execName, searchPaths.size());
        
        for (String path : searchPaths) {
            File file = new File(path);
            TharidiaThings.LOGGER.info("[VIDEO TOOLS] Checking path: {}", path);
            if (file.exists()) {
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] File exists at: {}", path);
                if (file.canExecute()) {
                    TharidiaThings.LOGGER.info("[VIDEO TOOLS] Found {} at: {}", execName, path);
                    foundPath = path;
                    break;
                } else {
                    TharidiaThings.LOGGER.warn("[VIDEO TOOLS] File exists but not executable: {}", path);
                }
            } else {
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] File does not exist: {}", path);
            }
        }
        
        // Try PATH as last resort (Windows uses 'where', Linux uses 'which')
        if (foundPath == null) {
            TharidiaThings.LOGGER.info("[VIDEO TOOLS] Checking PATH for {}", execName);
            try {
                ProcessBuilder pb;
                if (isWindows) {
                    pb = new ProcessBuilder("where", execName + ".exe");
                } else {
                    pb = new ProcessBuilder("which", execName);
                }
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    // Read the path from output
                    String output = new String(process.getInputStream().readAllBytes()).trim();
                    if (!output.isEmpty()) {
                        String firstPath = output.split("\n")[0].trim();
                        TharidiaThings.LOGGER.info("[VIDEO TOOLS] Found {} in PATH at: {}", execName, firstPath);
                        foundPath = firstPath;
                    } else {
                        TharidiaThings.LOGGER.info("[VIDEO TOOLS] Found {} in PATH", execName);
                        foundPath = isWindows ? execName + ".exe" : execName;
                    }
                } else {
                    TharidiaThings.LOGGER.warn("[VIDEO TOOLS] {} not found in PATH (exit code: {})", execName, exitCode);
                }
            } catch (Exception e) {
                TharidiaThings.LOGGER.warn("[VIDEO TOOLS] Error checking PATH for {}: {}", execName, e.getMessage());
            }
        }
        
        if (foundPath == null) {
            TharidiaThings.LOGGER.warn("[VIDEO TOOLS] {} not found anywhere", execName);
            return false;
        }
        
        // Verify the tool actually works
        TharidiaThings.LOGGER.info("[VIDEO TOOLS] Verifying {} works at: {}", execName, foundPath);
        if (verifyExecutableWorks(foundPath, execName)) {
            TharidiaThings.LOGGER.info("[VIDEO TOOLS] {} verified and working!", execName);
            return true;
        } else {
            TharidiaThings.LOGGER.warn("[VIDEO TOOLS] {} found but failed verification test", execName);
            return false;
        }
    }
    
    /**
     * Use 'which' command to find executable in PATH (Linux/Mac only)
     */
    private String findExecutableWithWhich(String execName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", execName);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return null;
            }
            
            if (process.exitValue() == 0) {
                String output = new String(process.getInputStream().readAllBytes()).trim();
                if (!output.isEmpty()) {
                    return output.split("\n")[0].trim();
                }
            }
        } catch (Exception e) {
            TharidiaThings.LOGGER.debug("[VIDEO TOOLS] 'which' command failed for {}: {}", execName, e.getMessage());
        }
        return null;
    }
    
    private boolean verifyExecutableWorks(String path, String execName) {
        try {
            ProcessBuilder pb;
            if (execName.equals("ffmpeg") || execName.equals("ffplay")) {
                // Test with -version flag
                pb = new ProcessBuilder(path, "-version");
            } else if (execName.equals("yt-dlp") || execName.equals("streamlink")) {
                // Test with --version flag
                pb = new ProcessBuilder(path, "--version");
            } else {
                // Generic test
                pb = new ProcessBuilder(path, "--help");
            }
            
            // Redirect error stream to output stream
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!finished) {
                TharidiaThings.LOGGER.warn("[VIDEO TOOLS] {} verification timed out", execName);
                process.destroyForcibly();
                return false;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                // Success - read first line of output to confirm it's the right tool
                String output = new String(process.getInputStream().readAllBytes());
                String firstLine = output.split("\n")[0];
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] {} verification output: {}", execName, firstLine);
                return true;
            } else {
                TharidiaThings.LOGGER.warn("[VIDEO TOOLS] {} verification failed with exit code: {}", execName, exitCode);
                return false;
            }
        } catch (Exception e) {
            TharidiaThings.LOGGER.warn("[VIDEO TOOLS] Error verifying {}: {}", execName, e.getMessage());
            return false;
        }
    }
    
    private List<String> getSearchPaths(String execName) {
        List<String> paths = new ArrayList<>();
        String execFile = withExecutableExtension(execName);
        Path workingDir = Paths.get(System.getProperty("user.dir"));
        Path binDir = DependencyDownloader.getBinDirectory();
        
        // Current working directory
        paths.add(workingDir.resolve(execFile).toString());
        
        // bin directory inside working dir (which is also DependencyDownloader target)
        paths.add(workingDir.resolve("bin").resolve(execFile).toString());
        if (execName.equals("streamlink")) {
            paths.add(workingDir.resolve("bin").resolve("streamlink").resolve("bin").resolve(execFile).toString());
        }
        
        // Explicit DependencyDownloader bin dir in case launcher sets different user.dir
        if (!binDir.equals(workingDir.resolve("bin"))) {
            paths.add(binDir.resolve(execFile).toString());
            if (execName.equals("streamlink")) {
                paths.add(binDir.resolve("streamlink").resolve("bin").resolve(execFile).toString());
            }
        }
        
        if (isWindows) {
            String minecraftDir = System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming" + File.separator + ".minecraft";
            paths.add(minecraftDir + File.separator + execFile);
            paths.add(minecraftDir + File.separator + "bin" + File.separator + execFile);
            String tharidiaDir = minecraftDir + File.separator + "tharidia" + File.separator + "bin" + File.separator + execFile;
            paths.add(tharidiaDir);
            paths.add("C:\\ffmpeg\\bin\\" + execFile);
            paths.add("C:\\Program Files\\ffmpeg\\bin\\" + execFile);
            paths.add("C:\\Program Files (x86)\\ffmpeg\\bin\\" + execFile);
        } else {
            Path home = Paths.get(System.getProperty("user.home"));
            paths.add(home.resolve(".local").resolve("bin").resolve(execFile).toString());
            paths.add(home.resolve("bin").resolve(execFile).toString());
            paths.add("/usr/local/bin/" + execFile);
            paths.add("/usr/bin/" + execFile);
        }
        
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
        
        // Return just the name (respecting platform extension) and hope it's in PATH
        return withExecutableExtension(execName);
    }

    private String withExecutableExtension(String execName) {
        if (isWindows) {
            return execName.endsWith(".exe") ? execName : execName + ".exe";
        }
        return execName;
    }
    
    private void showInstallationGUI() {
        // CRITICAL: Disable headless mode BEFORE SwingUtilities is invoked
        System.setProperty("java.awt.headless", "false");
        
        SwingUtilities.invokeLater(() -> {
            // Set it again inside the EDT to be absolutely sure
            System.setProperty("java.awt.headless", "false");
            
            List<ToolStatus> toolStatuses = new ArrayList<>();
            toolStatuses.add(new ToolStatus("FFmpeg", "ffmpeg.exe", ffmpegFound, null));
            toolStatuses.add(new ToolStatus("FFplay", "ffplay.exe", ffplayFound, null));
            toolStatuses.add(new ToolStatus("yt-dlp", "yt-dlp.exe", ytDlpFound, null));
            toolStatuses.add(new ToolStatus("streamlink", "streamlink.exe", streamlinkFound, null));
            
            VideoToolsInstallerGUI gui = new VideoToolsInstallerGUI(toolStatuses, this);
            gui.show();
        });
    }
    
    /**
     * Force show the installation GUI (for testing purposes)
     */
    public void forceShowInstallationGUI() {
        TharidiaThings.LOGGER.info("[VIDEO TOOLS] Force-showing installation GUI for testing");
        SwingUtilities.invokeLater(() -> {
            try {
                List<ToolStatus> toolStatuses = new ArrayList<>();
                toolStatuses.add(new ToolStatus("FFmpeg", "ffmpeg.exe", ffmpegFound, findExecutable("ffmpeg")));
                toolStatuses.add(new ToolStatus("FFplay", "ffplay.exe", ffplayFound, findExecutable("ffplay")));
                toolStatuses.add(new ToolStatus("yt-dlp", "yt-dlp.exe", ytDlpFound, findExecutable("yt-dlp")));
                toolStatuses.add(new ToolStatus("streamlink", "streamlink.exe", streamlinkFound, findExecutable("streamlink")));
                
                VideoToolsInstallerGUI gui = new VideoToolsInstallerGUI(toolStatuses, this);
                gui.show();
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] GUI show() called successfully");
            } catch (Exception e) {
                TharidiaThings.LOGGER.error("[VIDEO TOOLS] Failed to show installation GUI: {}", e.getMessage(), e);
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Called by the installer GUI after successful installation
     */
    public void onInstallationComplete() {
        TharidiaThings.LOGGER.info("[VIDEO TOOLS] Installation completed, rechecking tools");
        recheckTools();
    }
    
    // Getter methods for tool status
    public boolean isFfmpegFound() {
        return ffmpegFound;
    }
    
    public boolean isFfplayFound() {
        return ffplayFound;
    }
    
    public boolean isYtDlpFound() {
        return ytDlpFound;
    }
    
    public boolean isStreamlinkFound() {
        return streamlinkFound;
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
