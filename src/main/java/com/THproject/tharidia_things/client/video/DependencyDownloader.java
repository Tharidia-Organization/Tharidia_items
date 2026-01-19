package com.THproject.tharidia_things.client.video;

import com.THproject.tharidia_things.TharidiaThings;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads and installs video dependencies (FFmpeg, yt-dlp, streamlink)
 */
public class DependencyDownloader {
    
    private static final String YTDLP_URL_WIN = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";
    private static final String YTDLP_URL_LINUX = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux";
    private static final String STREAMLINK_API_URL = "https://api.github.com/repos/streamlink/windows-builds/releases/latest";
    private static final String FFMPEG_URL_WIN = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip";
    private static final String FFMPEG_URL_LINUX = "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz";
    
    public enum Dependency {
        FFMPEG("FFmpeg", "ffmpeg.exe"),
        YTDLP("yt-dlp", "yt-dlp.exe"),
        STREAMLINK("Streamlink", "streamlink.exe");
        
        public final String displayName;
        public final String executableName;
        
        Dependency(String displayName, String executableName) {
            this.displayName = displayName;
            this.executableName = executableName;
        }
    }
    
    /**
     * Check which dependencies are missing
     */
    public static List<Dependency> checkMissingDependencies() {
        List<Dependency> missing = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        
        // Check on all platforms
        Path binDir = getBinDirectory();
        
        for (Dependency dep : Dependency.values()) {
            String execName = getExecutableNameForOS(dep, os);
            Path execPath = binDir.resolve(execName);
            if (!Files.exists(execPath)) {
                missing.add(dep);
            }
        }
        
        return missing;
    }
    
    /**
     * Get the correct executable name for the current OS
     */
    private static String getExecutableNameForOS(Dependency dep, String os) {
        if (os.contains("win")) {
            return dep.executableName;
        } else {
            // Linux/macOS executables don't have .exe extension
            return dep.executableName.replace(".exe", "");
        }
    }
    
    /**
     * Get the bin directory where executables will be installed
     */
    public static Path getBinDirectory() {
        // Use .minecraft/bin directory
        Path minecraftDir = Paths.get(System.getProperty("user.dir"));
        Path binDir = minecraftDir.resolve("bin");
        
        try {
            Files.createDirectories(binDir);
        } catch (IOException e) {
            TharidiaThings.LOGGER.error("Failed to create bin directory", e);
        }
        
        return binDir;
    }
    
    /**
     * Download a dependency asynchronously
     * @param dep The dependency to download
     * @param progressCallback Called with progress updates (0.0 to 1.0)
     * @return CompletableFuture that completes when download is done
     */
    public static CompletableFuture<Boolean> downloadDependency(Dependency dep, Consumer<Double> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TharidiaThings.LOGGER.info("Starting download of {}", dep.displayName);
                
                switch (dep) {
                    case FFMPEG:
                        String os = System.getProperty("os.name").toLowerCase();
                        if (os.contains("win")) {
                            return installFFmpegWithWinget().join();
                        } else {
                            return downloadAndExtractFFmpeg(progressCallback);
                        }
                    case YTDLP:
                        return downloadYtDlp(progressCallback);
                    case STREAMLINK:
                        String osStream = System.getProperty("os.name").toLowerCase();
                        if (osStream.contains("win")) {
                            return downloadAndExtractStreamlink(progressCallback);
                        } else {
                            // Skip streamlink on Linux for now - it's optional
                            TharidiaThings.LOGGER.info("Skipping streamlink download on Linux (optional)");
                            return true;
                        }
                    default:
                        return false;
                }
                
            } catch (Exception e) {
                TharidiaThings.LOGGER.error("Failed to download {}", dep.displayName, e);
                return false;
            }
        });
    }
    
    /**
     * Download yt-dlp (single file)
     */
    private static boolean downloadYtDlp(Consumer<Double> progressCallback) throws IOException {
        Path binDir = getBinDirectory();
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            Path targetPath = binDir.resolve("yt-dlp.exe");
            return downloadFile(YTDLP_URL_WIN, targetPath, progressCallback);
        } else {
            Path targetPath = binDir.resolve("yt-dlp");
            boolean success = downloadFile(YTDLP_URL_LINUX, targetPath, progressCallback);
            if (success) {
                // Make executable on Linux/macOS
                targetPath.toFile().setExecutable(true);
            }
            return success;
        }
    }
    
    /**
     * Download and extract streamlink portable
     */
    private static boolean downloadAndExtractStreamlink(Consumer<Double> progressCallback) throws IOException {
        Path binDir = getBinDirectory();
        Path tempZip = Files.createTempFile("streamlink", ".zip");
        
        try {
            // Get the latest release download URL from GitHub API
            TharidiaThings.LOGGER.info("Fetching latest streamlink release info...");
            String downloadUrl = getStreamlinkDownloadUrl();
            if (downloadUrl == null) {
                TharidiaThings.LOGGER.error("Failed to get streamlink download URL");
                return false;
            }
            TharidiaThings.LOGGER.info("Streamlink download URL: {}", downloadUrl);
            
            // Download zip
            if (!downloadFile(downloadUrl, tempZip, progress -> progressCallback.accept(progress * 0.5))) {
                return false;
            }
            
            // Extract entire streamlink directory structure
            TharidiaThings.LOGGER.info("Extracting streamlink from zip...");
            
            // Delete old streamlink.exe if it exists (from previous installation)
            Path oldStreamlinkExe = binDir.resolve("streamlink.exe");
            if (Files.exists(oldStreamlinkExe)) {
                TharidiaThings.LOGGER.info("Removing old streamlink.exe at {}", oldStreamlinkExe);
                Files.delete(oldStreamlinkExe);
            }
            
            Path streamlinkDir = binDir.resolve("streamlink");
            Files.createDirectories(streamlinkDir);
            
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip.toFile()))) {
                ZipEntry entry;
                String rootFolder = null;
                int extractedFiles = 0;
                
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    
                    // Find root folder (e.g., "streamlink-8.0.0-1-py313-x86_64/")
                    if (rootFolder == null && entryName.contains("/")) {
                        rootFolder = entryName.substring(0, entryName.indexOf("/") + 1);
                        TharidiaThings.LOGGER.info("Detected root folder: {}", rootFolder);
                    }
                    
                    // Skip if not in root folder
                    if (rootFolder == null || !entryName.startsWith(rootFolder)) {
                        continue;
                    }
                    
                    // Remove root folder prefix
                    String relativePath = entryName.substring(rootFolder.length());
                    if (relativePath.isEmpty()) {
                        continue;
                    }
                    
                    Path targetPath = streamlinkDir.resolve(relativePath);
                    
                    if (entry.isDirectory()) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        extractedFiles++;
                    }
                }
                
                TharidiaThings.LOGGER.info("Extracted {} files to {}", extractedFiles, streamlinkDir);
                
                // Verify streamlink.exe exists
                Path streamlinkExe = streamlinkDir.resolve("bin").resolve("streamlink.exe");
                if (!Files.exists(streamlinkExe)) {
                    TharidiaThings.LOGGER.error("streamlink.exe not found at {}", streamlinkExe);
                    return false;
                }
                
                progressCallback.accept(1.0);
                TharidiaThings.LOGGER.info("Streamlink installed successfully at {}", streamlinkExe);
                return true;
            }
            
        } finally {
            Files.deleteIfExists(tempZip);
        }
    }
    
    /**
     * Get the download URL for the latest streamlink Windows portable build
     */
    private static String getStreamlinkDownloadUrl() {
        try {
            URL url = new URL(STREAMLINK_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            if (conn.getResponseCode() != 200) {
                TharidiaThings.LOGGER.error("GitHub API returned status: {}", conn.getResponseCode());
                return null;
            }
            
            // Read JSON response
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            // Parse JSON to find the .zip download URL (simple string search)
            String json = response.toString();
            String searchPattern = "\"browser_download_url\":\"";
            int startIndex = json.indexOf(searchPattern);
            while (startIndex != -1) {
                startIndex += searchPattern.length();
                int endIndex = json.indexOf("\"", startIndex);
                if (endIndex != -1) {
                    String downloadUrl = json.substring(startIndex, endIndex);
                    // Look for the x86_64.zip file (not the .exe installer)
                    if (downloadUrl.endsWith("x86_64.zip")) {
                        return downloadUrl;
                    }
                }
                startIndex = json.indexOf(searchPattern, endIndex);
            }
            
            TharidiaThings.LOGGER.error("Could not find streamlink .zip download URL in GitHub API response");
            return null;
            
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed to fetch streamlink download URL: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Download and extract FFmpeg
     */
    private static boolean downloadAndExtractFFmpeg(Consumer<Double> progressCallback) throws IOException {
        Path binDir = getBinDirectory();
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            return downloadAndExtractFFmpegWindows(binDir, progressCallback);
        } else {
            return downloadAndExtractFFmpegLinux(binDir, progressCallback);
        }
    }
    
    private static boolean downloadAndExtractFFmpegWindows(Path binDir, Consumer<Double> progressCallback) throws IOException {
        Path tempZip = Files.createTempFile("ffmpeg", ".zip");
        
        try {
            // Download zip (large file ~100MB)
            if (!downloadFile(FFMPEG_URL_WIN, tempZip, progress -> progressCallback.accept(progress * 0.7))) {
                return false;
            }
            
            // Extract ffmpeg.exe and ffplay.exe from zip
            boolean foundFfmpeg = false;
            boolean foundFfplay = false;
            
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip.toFile()))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    
                    if (name.endsWith("bin/ffmpeg.exe")) {
                        Path targetPath = binDir.resolve("ffmpeg.exe");
                        Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        foundFfmpeg = true;
                        progressCallback.accept(0.85);
                        TharidiaThings.LOGGER.info("Extracted ffmpeg.exe to {}", targetPath);
                    } else if (name.endsWith("bin/ffplay.exe")) {
                        Path targetPath = binDir.resolve("ffplay.exe");
                        Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        foundFfplay = true;
                        progressCallback.accept(1.0);
                        TharidiaThings.LOGGER.info("Extracted ffplay.exe to {}", targetPath);
                    }
                    
                    if (foundFfmpeg && foundFfplay) {
                        break;
                    }
                }
            }
            
            return foundFfmpeg && foundFfplay;
            
        } finally {
            Files.deleteIfExists(tempZip);
        }
    }
    
    /**
     * Download and extract FFmpeg on Linux
     * DISABLED: ProcessBuilder execution not allowed for CurseForge compliance
     * Users must install FFmpeg manually on Linux
     */
    private static boolean downloadAndExtractFFmpegLinux(Path binDir, Consumer<Double> progressCallback) throws IOException {
        // ProcessBuilder (tar command) disabled for CurseForge compliance
        TharidiaThings.LOGGER.info("[CURSEFORGE MODE] Automatic FFmpeg installation on Linux is disabled.");
        TharidiaThings.LOGGER.info("[CURSEFORGE MODE] Please install FFmpeg manually using your package manager:");
        TharidiaThings.LOGGER.info("[CURSEFORGE MODE]   Ubuntu/Debian: sudo apt install ffmpeg");
        TharidiaThings.LOGGER.info("[CURSEFORGE MODE]   Fedora: sudo dnf install ffmpeg");
        TharidiaThings.LOGGER.info("[CURSEFORGE MODE]   Arch: sudo pacman -S ffmpeg");
        progressCallback.accept(1.0);
        return false;
    }
    
    /**
     * Download a file from URL with progress tracking
     */
    private static boolean downloadFile(String urlString, Path targetPath, Consumer<Double> progressCallback) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            TharidiaThings.LOGGER.error("Failed to download from {}: HTTP {}", urlString, responseCode);
            return false;
        }
        
        long fileSize = conn.getContentLengthLong();
        
        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(targetPath.toFile())) {
            
            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int bytesRead;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                
                if (fileSize > 0) {
                    double progress = (double) totalRead / fileSize;
                    progressCallback.accept(progress);
                }
            }
        }
        
        TharidiaThings.LOGGER.info("Downloaded {} to {}", urlString, targetPath);
        return true;
    }
    
    /**
     * Install FFmpeg using winget (requires user confirmation in CMD)
     * DISABLED: ProcessBuilder execution not allowed for CurseForge compliance
     * Users must install FFmpeg manually
     */
    public static CompletableFuture<Boolean> installFFmpegWithWinget() {
        return CompletableFuture.supplyAsync(() -> {
            // ProcessBuilder (cmd.exe/winget) disabled for CurseForge compliance
            TharidiaThings.LOGGER.info("[CURSEFORGE MODE] Automatic FFmpeg installation via winget is disabled.");
            TharidiaThings.LOGGER.info("[CURSEFORGE MODE] Please install FFmpeg manually:");
            TharidiaThings.LOGGER.info("[CURSEFORGE MODE]   Option 1: Run 'winget install ffmpeg' in Command Prompt");
            TharidiaThings.LOGGER.info("[CURSEFORGE MODE]   Option 2: Download from https://www.gyan.dev/ffmpeg/builds/");
            return false;
        });
    }
}
