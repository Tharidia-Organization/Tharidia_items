package com.THproject.tharidia_things.client.video;

import com.THproject.tharidia_things.TharidiaThings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Downloads and extracts video tools (FFmpeg, yt-dlp) for Windows
 */
public class VideoToolsDownloader {
    
    public static class DownloadProgress {
        public final String toolName;
        public final long bytesDownloaded;
        public final long totalBytes;
        public final double percentage;
        
        public DownloadProgress(String toolName, long bytesDownloaded, long totalBytes) {
            this.toolName = toolName;
            this.bytesDownloaded = bytesDownloaded;
            this.totalBytes = totalBytes;
            this.percentage = totalBytes > 0 ? (bytesDownloaded * 100.0 / totalBytes) : 0.0;
        }
    }
    
    // Download URLs - using reliable sources
    private static final String FFMPEG_DOWNLOAD_URL = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip";
    private static final String YT_DLP_DOWNLOAD_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";
    
    // Tools directory
    private static final String TOOLS_DIR = System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming" + File.separator + ".minecraft" + File.separator + "tharidia" + File.separator + "bin";
    
    /**
     * Ensure the tools directory exists
     */
    public static void ensureToolsDirectory() {
        try {
            Path toolsPath = Paths.get(TOOLS_DIR);
            if (!Files.exists(toolsPath)) {
                Files.createDirectories(toolsPath);
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] Created tools directory: {}", TOOLS_DIR);
            }
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("[VIDEO TOOLS] Failed to create tools directory: {}", e.getMessage());
            throw new RuntimeException("Failed to create tools directory", e);
        }
    }
    
    /**
     * Download and extract FFmpeg (includes FFplay)
     */
    public static CompletableFuture<Boolean> downloadFFmpeg(Consumer<DownloadProgress> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] Downloading FFmpeg...");
                Path tempZip = Files.createTempFile("ffmpeg", ".zip");
                
                // Download the zip file
                downloadWithProgress(FFMPEG_DOWNLOAD_URL, tempZip, "FFmpeg", progressCallback);
                
                // Extract only ffmpeg.exe and ffplay.exe
                extractFromZip(tempZip, Paths.get(TOOLS_DIR), 
                    new String[]{"ffmpeg.exe", "ffplay.exe"});
                
                // Clean up
                Files.deleteIfExists(tempZip);
                
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] FFmpeg downloaded and extracted successfully");
                return true;
            } catch (Exception e) {
                TharidiaThings.LOGGER.error("[VIDEO TOOLS] Failed to download FFmpeg: {}", e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Download yt-dlp
     */
    public static CompletableFuture<Boolean> downloadYtDlp(Consumer<DownloadProgress> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] Downloading yt-dlp...");
                Path targetPath = Paths.get(TOOLS_DIR, "yt-dlp.exe");
                
                downloadWithProgress(YT_DLP_DOWNLOAD_URL, targetPath, "yt-dlp", progressCallback);
                
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] yt-dlp downloaded successfully");
                return true;
            } catch (Exception e) {
                TharidiaThings.LOGGER.error("[VIDEO TOOLS] Failed to download yt-dlp: {}", e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Download streamlink
     */
    public static CompletableFuture<Boolean> downloadStreamlink(Consumer<DownloadProgress> progressCallback) {
        return DependencyDownloader.downloadDependency(
            DependencyDownloader.Dependency.STREAMLINK,
            progress -> progressCallback.accept(new DownloadProgress("streamlink", (long)(progress * 100), 100))
        );
    }
    
    /**
     * Download a file with progress reporting
     */
    private static void downloadWithProgress(String urlString, Path targetFile, String toolName, 
                                           Consumer<DownloadProgress> progressCallback) throws IOException {
        URL url = new URL(urlString);
        
        // Configure connection for better performance
        java.net.URLConnection conn = url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "Tharidia-VideoTools/1.0");
        
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(targetFile.toFile())) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            
            // Get file size for progress
            long fileSize = conn.getContentLengthLong();
            if (fileSize <= 0) {
                TharidiaThings.LOGGER.debug("[VIDEO TOOLS] Could not determine file size for {}", toolName);
            }
            
            long lastProgressUpdate = 0;
            long progressUpdateInterval = fileSize > 0 ? fileSize / 100 : 100000; // Update every 1% or 100KB
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                
                // Update progress periodically
                if (progressCallback != null && 
                    (totalBytes - lastProgressUpdate > progressUpdateInterval || totalBytes == fileSize)) {
                    progressCallback.accept(new DownloadProgress(toolName, totalBytes, fileSize));
                    lastProgressUpdate = totalBytes;
                }
            }
        }
    }
    
    /**
     * Extract specific files from a zip archive
     */
    private static void extractFromZip(Path zipFile, Path extractDir, String[] filesToExtract) throws IOException {
        try (var fs = FileSystems.newFileSystem(zipFile)) {
            // Find the ffmpeg bin directory
            Path binDir = null;
            for (Path root : fs.getRootDirectories()) {
                Path potentialBin = root.resolve("ffmpeg-master-latest-win64-gpl").resolve("bin");
                if (Files.exists(potentialBin)) {
                    binDir = potentialBin;
                    break;
                }
            }
            
            if (binDir == null) {
                throw new IOException("Could not find ffmpeg bin directory in zip");
            }
            
            // Extract required files
            for (String file : filesToExtract) {
                Path source = binDir.resolve(file);
                if (Files.exists(source)) {
                    Path target = extractDir.resolve(file);
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    TharidiaThings.LOGGER.info("[VIDEO TOOLS] Extracted {} to {}", file, target);
                    
                    // Verify the extracted file
                    if (!Files.exists(target) || Files.size(target) == 0) {
                        throw new IOException("Extracted file is invalid: " + file);
                    }
                } else {
                    throw new IOException("Required file not found in archive: " + file);
                }
            }
        }
    }
    
    /**
     * Get the size of a download (for progress reporting)
     */
    public static long getFileSize(String urlString) {
        try {
            URL url = new URL(urlString);
            java.net.URLConnection conn = url.openConnection();
            return conn.getContentLengthLong();
        } catch (Exception e) {
            return -1;
        }
    }
}
