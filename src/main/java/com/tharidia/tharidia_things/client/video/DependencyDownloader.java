package com.tharidia.tharidia_things.client.video;

import com.tharidia.tharidia_things.TharidiaThings;

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
    
    private static final String YTDLP_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";
    private static final String STREAMLINK_URL = "https://github.com/streamlink/windows-builds/releases/latest/download/streamlink-portable.zip";
    private static final String FFMPEG_URL = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";
    
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
        
        if (!os.contains("win")) {
            // Only auto-download on Windows
            return missing;
        }
        
        Path binDir = getBinDirectory();
        
        for (Dependency dep : Dependency.values()) {
            Path execPath = binDir.resolve(dep.executableName);
            if (!Files.exists(execPath) || !Files.isExecutable(execPath)) {
                missing.add(dep);
            }
        }
        
        return missing;
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
                        return downloadAndExtractFFmpeg(progressCallback);
                    case YTDLP:
                        return downloadYtDlp(progressCallback);
                    case STREAMLINK:
                        return downloadAndExtractStreamlink(progressCallback);
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
     * Download yt-dlp.exe (single file)
     */
    private static boolean downloadYtDlp(Consumer<Double> progressCallback) throws IOException {
        Path binDir = getBinDirectory();
        Path targetPath = binDir.resolve("yt-dlp.exe");
        
        return downloadFile(YTDLP_URL, targetPath, progressCallback);
    }
    
    /**
     * Download and extract streamlink portable
     */
    private static boolean downloadAndExtractStreamlink(Consumer<Double> progressCallback) throws IOException {
        Path binDir = getBinDirectory();
        Path tempZip = Files.createTempFile("streamlink", ".zip");
        
        try {
            // Download zip
            if (!downloadFile(STREAMLINK_URL, tempZip, progress -> progressCallback.accept(progress * 0.5))) {
                return false;
            }
            
            // Extract streamlink.exe from zip
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip.toFile()))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().endsWith("streamlink.exe")) {
                        Path targetPath = binDir.resolve("streamlink.exe");
                        Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        progressCallback.accept(1.0);
                        TharidiaThings.LOGGER.info("Extracted streamlink.exe to {}", targetPath);
                        return true;
                    }
                }
            }
            
            TharidiaThings.LOGGER.error("streamlink.exe not found in zip");
            return false;
            
        } finally {
            Files.deleteIfExists(tempZip);
        }
    }
    
    /**
     * Download and extract FFmpeg essentials
     */
    private static boolean downloadAndExtractFFmpeg(Consumer<Double> progressCallback) throws IOException {
        Path binDir = getBinDirectory();
        Path tempZip = Files.createTempFile("ffmpeg", ".zip");
        
        try {
            // Download zip (large file ~100MB)
            if (!downloadFile(FFMPEG_URL, tempZip, progress -> progressCallback.accept(progress * 0.7))) {
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
     */
    public static CompletableFuture<Boolean> installFFmpegWithWinget() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TharidiaThings.LOGGER.info("Installing FFmpeg via winget...");
                
                // Open CMD with winget command
                ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c", "start", "cmd.exe", "/k",
                    "echo Installing FFmpeg... && winget install ffmpeg && echo. && echo Installation complete! You can close this window. && pause"
                );
                pb.start();
                
                TharidiaThings.LOGGER.info("Winget command launched. User must accept in CMD window.");
                return true;
                
            } catch (Exception e) {
                TharidiaThings.LOGGER.error("Failed to launch winget", e);
                return false;
            }
        });
    }
}
