package com.tharidia.tharidia_things.client.video;

import com.mojang.blaze3d.platform.NativeImage;
import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.video.VideoScreen;
import com.tharidia.tharidia_things.video.YouTubeUrlExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unified Video Player using FFmpeg for ALL video sources
 * 
 * Strategy: Use FFmpeg for everything (YouTube, Twitch, direct URLs)
 * - FFmpeg handles video decoding → raw RGB frames
 * - FFplay handles audio playback (separate process, synced via timestamp)
 * - Single consistent approach = fewer bugs
 * 
 * Why FFmpeg over VLC:
 * 1. VLC callback rendering has known issues with certain codecs
 * 2. FFmpeg gives us raw pixel data we can control
 * 3. yt-dlp + FFmpeg is the most reliable combo for YouTube/Twitch
 * 4. Simpler architecture = easier debugging
 */
public class VLCVideoPlayer {
    private final VideoScreen screen;
    
    // Video processing
    private Process videoProcess;
    private Process audioProcess;
    private Thread readerThread;
    private DynamicTexture texture;
    private NativeImage image;
    
    // State
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean isReleased = new AtomicBoolean(false);
    private String videoUrl = "";
    private volatile float volume = 1.0f;
    
    // Frame buffer with atomic swap
    private final Object frameLock = new Object();
    private byte[] frameBuffer1;
    private byte[] frameBuffer2;
    private final AtomicReference<byte[]> readyFrame = new AtomicReference<>(null);
    private volatile boolean hasNewFrame = false;
    
    // Resolution - 16:9, balanced quality/performance
    private static final int VIDEO_WIDTH = 854;
    private static final int VIDEO_HEIGHT = 480;
    private static final int FRAME_SIZE = VIDEO_WIDTH * VIDEO_HEIGHT * 3; // RGB24
    
    // Frame timing
    private long lastFrameTime = 0;
    private static final long FRAME_DELAY_MS = 33; // ~30 FPS
    private int frameCount = 0;
    
    // Initialization flag
    private boolean isInitialized = false;
    
    // Get executable paths from VideoToolsManager
    private String getFfmpegPath() {
        return VideoToolsManager.getInstance().getFfmpegPath();
    }
    
    private String getFfplayPath() {
        return VideoToolsManager.getInstance().getFfplayPath();
    }
    
    private String getYtDlpPath() {
        return VideoToolsManager.getInstance().getYtDlpPath();
    }
    
    public VLCVideoPlayer(VideoScreen screen) {
        this.screen = screen;
        initialize();
    }
    
    /**
     * Find FFmpeg/FFplay executable in multiple locations (Windows-friendly)
     */
    private static String findFfmpegExecutable(String execName, boolean isWindows) {
        List<String> searchPaths = new ArrayList<>();
        
        if (isWindows) {
            String exeName = execName + ".exe";
            
            // 1. Current working directory
            searchPaths.add(System.getProperty("user.dir") + File.separator + exeName);
            
            // 2. .minecraft folder
            String minecraftDir = System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming" + File.separator + ".minecraft";
            searchPaths.add(minecraftDir + File.separator + exeName);
            searchPaths.add(minecraftDir + File.separator + "bin" + File.separator + exeName);
            
            // 3. Common FFmpeg installation paths
            searchPaths.add("C:\\ffmpeg\\bin\\" + exeName);
            searchPaths.add("C:\\Program Files\\ffmpeg\\bin\\" + exeName);
            searchPaths.add("C:\\Program Files (x86)\\ffmpeg\\bin\\" + exeName);
            
            // 4. Try PATH
            searchPaths.add(exeName);
        } else {
            searchPaths.add(execName);
        }
        
        // Check each path
        for (String path : searchPaths) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                TharidiaThings.LOGGER.info("[VIDEO] Found {} at: {}", execName, path);
                return path;
            }
        }
        
        // If not found, return command name and hope it's in PATH
        String fallback = isWindows ? execName + ".exe" : execName;
        TharidiaThings.LOGGER.warn("[VIDEO] {} not found in common locations, trying PATH: {}", execName, fallback);
        return fallback;
    }
    
    private void initialize() {
        try {
            TharidiaThings.LOGGER.info("[VIDEO] Initializing player for screen {}", screen.getId());
            
            // Pre-allocate frame buffers
            frameBuffer1 = new byte[FRAME_SIZE];
            frameBuffer2 = new byte[FRAME_SIZE];
            
            // Create texture
            texture = new DynamicTexture(VIDEO_WIDTH, VIDEO_HEIGHT, false);
            image = texture.getPixels();
            
            if (image == null) {
                TharidiaThings.LOGGER.error("[VIDEO] Failed to get NativeImage from texture");
                return;
            }
            
            // Fill with black initially
            for (int y = 0; y < VIDEO_HEIGHT; y++) {
                for (int x = 0; x < VIDEO_WIDTH; x++) {
                    image.setPixelRGBA(x, y, 0xFF000000); // Black, full alpha
                }
            }
            texture.upload();
            
            isInitialized = true;
            TharidiaThings.LOGGER.info("[VIDEO] Player initialized successfully ({}x{})", VIDEO_WIDTH, VIDEO_HEIGHT);
            
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("[VIDEO] Failed to initialize", e);
            isInitialized = false;
        }
    }
    
    public void loadVideo(String url) {
        if (!isInitialized) {
            TharidiaThings.LOGGER.error("[VIDEO] Cannot load - not initialized");
            return;
        }
        
        // Stop any existing playback
        stopInternal();
        
        this.videoUrl = url;
        TharidiaThings.LOGGER.info("[VIDEO] Loading: {}", url);
        
        // Run loading in background to not block render thread
        new Thread(() -> {
            try {
                boolean isYouTubeOrTwitch = YouTubeUrlExtractor.isValidYouTubeUrl(url) || url.contains("twitch.tv");
                
                if (isYouTubeOrTwitch) {
                    String platform = url.contains("twitch.tv") ? "Twitch" : "YouTube";
                    TharidiaThings.LOGGER.info("[VIDEO] Extracting {} stream URL...", platform);
                    
                    // Use YouTubeUrlExtractor which has proper executable finding logic
                    String streamUrl = YouTubeUrlExtractor.getBestStreamUrl(url);
                    if (streamUrl == null) {
                        TharidiaThings.LOGGER.error("[VIDEO] Failed to extract stream URL");
                        return;
                    }
                    
                    TharidiaThings.LOGGER.info("[VIDEO] Stream URL ready, starting synchronized playback");
                    
                    // Start both processes at the same time with the same URL
                    startVideoProcess(streamUrl);
                    startAudioProcess(streamUrl);
                } else {
                    // Direct URL - use FFmpeg directly
                    TharidiaThings.LOGGER.info("[VIDEO] Using direct URL");
                    startVideoProcess(url);
                    startAudioProcess(url);
                }
                
                // Start frame reader thread
                running.set(true);
                readerThread = new Thread(this::readFrames, "VideoReader-" + screen.getId());
                readerThread.setDaemon(true);
                readerThread.start();
                
                TharidiaThings.LOGGER.info("[VIDEO] Playback started");
                
            } catch (Exception e) {
                TharidiaThings.LOGGER.error("[VIDEO] Failed to load video", e);
            }
        }, "VideoLoader").start();
    }
    
    private void startVideoProcess(String url) throws Exception {
        // Get FFmpeg path from VideoToolsManager
        String ffmpeg = getFfmpegPath();
        
        // Check if URL is HLS (Twitch streams)
        boolean isHls = url.contains(".m3u8");
        
        // Build FFmpeg command with HLS support for Twitch
        List<String> command = new ArrayList<>();
        command.add(ffmpeg);
        
        // Input options - realtime reading
        command.add("-re");
        command.add("-reconnect");
        command.add("1");
        command.add("-reconnect_streamed");
        command.add("1");
        command.add("-reconnect_delay_max");
        command.add("5");
        
        // HLS-specific options for Twitch
        if (isHls) {
            command.add("-fflags");
            command.add("nobuffer");
            command.add("-flags");
            command.add("low_delay");
            command.add("-rw_timeout");
            command.add("15000000"); // 15s read timeout
        }
        
        command.add("-i");
        command.add(url);
        
        // Video processing
        command.add("-vf");
        command.add("scale=" + VIDEO_WIDTH + ":" + VIDEO_HEIGHT + ":force_original_aspect_ratio=decrease," +
                   "pad=" + VIDEO_WIDTH + ":" + VIDEO_HEIGHT + ":(ow-iw)/2:(oh-ih)/2:black," +
                   "fps=30");
        
        // Output framerate
        command.add("-r");
        command.add("30");
        command.add("-vsync");
        command.add("cfr");
        
        // Output format: raw RGB24 pixels
        command.add("-f");
        command.add("rawvideo");
        command.add("-pix_fmt");
        command.add("rgb24");
        
        // No audio in video stream
        command.add("-an");
        
        // Output to stdout
        command.add("-");
        
        ProcessBuilder pb = new ProcessBuilder(command);
        
        pb.redirectErrorStream(false);
        try {
            videoProcess = pb.start();
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("[VIDEO] Failed to start FFmpeg process: {}", e.getMessage());
            String os = System.getProperty("os.name").toLowerCase();
            boolean isWindows = os.contains("win");
            if (isWindows) {
                TharidiaThings.LOGGER.error("=== WINDOWS FFMPEG INSTALLATION ===");
                TharidiaThings.LOGGER.error("1. Download FFmpeg from: https://www.gyan.dev/ffmpeg/builds/");
                TharidiaThings.LOGGER.error("2. Extract to C:\\ffmpeg");
                TharidiaThings.LOGGER.error("3. Add C:\\ffmpeg\\bin to Windows PATH");
                TharidiaThings.LOGGER.error("4. Restart Minecraft");
                TharidiaThings.LOGGER.error("See WINDOWS_SETUP.md for detailed instructions");
            }
            throw e;
        }
        
        // Log FFmpeg errors in background
        Thread errorLogger = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(videoProcess.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Error") || line.contains("error")) {
                        TharidiaThings.LOGGER.warn("[FFMPEG] {}", line);
                    } else {
                        TharidiaThings.LOGGER.debug("[FFMPEG] {}", line);
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }, "FFmpeg-ErrorLog");
        errorLogger.setDaemon(true);
        errorLogger.start();
        
        TharidiaThings.LOGGER.info("[VIDEO] FFmpeg video process started");
    }
    
    private void startAudioProcess(String url) {
        // Log stack trace to identify duplicate calls
        TharidiaThings.LOGGER.info("[VIDEO] startAudioProcess called for screen {} - URL hash: {}", 
            screen.getId(), url.hashCode());
        Thread.dumpStack();
        
        // Kill any existing audio process first
        if (audioProcess != null && audioProcess.isAlive()) {
            TharidiaThings.LOGGER.warn("[VIDEO] Audio process already running! Killing it first.");
            audioProcess.descendants().forEach(ph -> ph.destroyForcibly());
            audioProcess.destroyForcibly();
            try {
                audioProcess.waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                TharidiaThings.LOGGER.warn("[VIDEO] Error waiting for old audio to die: {}", e.getMessage());
            }
            audioProcess = null;
        }
        
        try {
            String ffplay = getFfplayPath();
            
            // Check if URL is HLS (Twitch streams)
            boolean isHls = url.contains(".m3u8");
            
            // Build FFplay command with HLS support for Twitch
            List<String> command = new ArrayList<>();
            command.add(ffplay);
            command.add("-nodisp");           // No video display
            command.add("-autoexit");         // Exit when done
            command.add("-vn");               // No video
            command.add("-loglevel");
            command.add("error");
            
            // HLS-specific options for Twitch
            if (isHls) {
                command.add("-fflags");
                command.add("nobuffer");
                command.add("-flags");
                command.add("low_delay");
            }
            
            command.add("-af");
            command.add("volume=" + volume);
            command.add("-i");
            command.add(url);
            
            ProcessBuilder pb = new ProcessBuilder(command);
            
            pb.redirectErrorStream(true);
            audioProcess = pb.start();
            
            TharidiaThings.LOGGER.info("[VIDEO] FFplay audio process started (volume: {}, PID: {})", 
                volume, audioProcess.pid());
            
        } catch (Exception e) {
            TharidiaThings.LOGGER.warn("[VIDEO] Failed to start audio: {}", e.getMessage());
        }
    }
    
    private void readFrames() {
        TharidiaThings.LOGGER.info("[VIDEO] Frame reader thread started");
        
        try (InputStream is = new BufferedInputStream(videoProcess.getInputStream(), FRAME_SIZE * 4)) {
            byte[] readBuffer = frameBuffer1;
            int localFrameCount = 0;
            
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                // Read exactly one frame
                int bytesRead = 0;
                while (bytesRead < FRAME_SIZE && running.get()) {
                    int available = is.available();
                    if (available == 0) {
                        Thread.sleep(5);
                        continue;
                    }
                    
                    int toRead = Math.min(available, FRAME_SIZE - bytesRead);
                    int read = is.read(readBuffer, bytesRead, toRead);
                    
                    if (read == -1) {
                        TharidiaThings.LOGGER.info("[VIDEO] Stream ended");
                        running.set(false);
                        return;
                    }
                    
                    bytesRead += read;
                }
                
                if (bytesRead == FRAME_SIZE) {
                    // Frame complete - make it available
                    synchronized (frameLock) {
                        // Swap buffers
                        readyFrame.set(readBuffer);
                        readBuffer = (readBuffer == frameBuffer1) ? frameBuffer2 : frameBuffer1;
                        hasNewFrame = true;
                    }
                    
                    localFrameCount++;
                    if (localFrameCount == 1 || localFrameCount % 100 == 0) {
                        TharidiaThings.LOGGER.info("[VIDEO] Read frame {}", localFrameCount);
                    }
                }
            }
        } catch (InterruptedException e) {
            TharidiaThings.LOGGER.info("[VIDEO] Reader thread interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (running.get()) {
                TharidiaThings.LOGGER.error("[VIDEO] Frame reading error: {}", e.getMessage());
            }
        }
        
        TharidiaThings.LOGGER.info("[VIDEO] Frame reader thread ended");
    }
    
    public void update() {
        if (isReleased.get() || !isInitialized || image == null || texture == null) {
            return;
        }
        
        // Frame rate limiting
        long now = System.currentTimeMillis();
        if (now - lastFrameTime < FRAME_DELAY_MS) {
            return;
        }
        
        // Check for new frame
        if (!hasNewFrame) {
            return;
        }
        
        byte[] frame;
        synchronized (frameLock) {
            frame = readyFrame.get();
            if (frame == null) {
                return;
            }
            hasNewFrame = false;
        }
        
        lastFrameTime = now;
        frameCount++;
        
        try {
            // Copy RGB24 frame to texture (RGB → ABGR for NativeImage)
            int pixelIndex = 0;
            for (int y = 0; y < VIDEO_HEIGHT; y++) {
                for (int x = 0; x < VIDEO_WIDTH; x++) {
                    int r = frame[pixelIndex++] & 0xFF;
                    int g = frame[pixelIndex++] & 0xFF;
                    int b = frame[pixelIndex++] & 0xFF;
                    
                    // NativeImage uses ABGR format
                    int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
                    image.setPixelRGBA(x, y, abgr);
                }
            }
            
            // Upload to GPU
            texture.upload();
            
            if (frameCount == 1 || frameCount % 100 == 0) {
                TharidiaThings.LOGGER.info("[VIDEO] Rendered frame {}", frameCount);
            }
            
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("[VIDEO] Texture update error: {}", e.getMessage());
        }
    }
    
    public void play() {
        // For live streams, just ensure processes are running
        // Don't reload if already playing
        if (!running.get() && !videoUrl.isEmpty()) {
            TharidiaThings.LOGGER.info("[VIDEO] Play called - loading video");
            loadVideo(videoUrl);
        } else if (running.get()) {
            TharidiaThings.LOGGER.debug("[VIDEO] Play called but already playing - ignoring");
        }
    }
    
    public void pause() {
        // Can't really pause a live stream, just stop
        stopInternal();
    }
    
    public void stop() {
        stopInternal();
    }
    
    private void stopInternal() {
        running.set(false);
        
        TharidiaThings.LOGGER.info("[VIDEO] Stopping playback for screen {}", screen.getId());
        
        // Stop reader thread
        if (readerThread != null) {
            readerThread.interrupt();
            try {
                readerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            readerThread = null;
        }
        
        // Stop video process forcefully
        if (videoProcess != null) {
            try {
                videoProcess.destroyForcibly();
                videoProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                TharidiaThings.LOGGER.info("[VIDEO] FFmpeg video process terminated");
            } catch (Exception e) {
                TharidiaThings.LOGGER.warn("[VIDEO] Error stopping video process: {}", e.getMessage());
            }
            videoProcess = null;
        }
        
        // Stop audio process forcefully and ensure it's killed
        if (audioProcess != null) {
            try {
                // Kill all descendants (important for shell-spawned processes)
                if (audioProcess.isAlive()) {
                    audioProcess.descendants().forEach(ph -> {
                        ph.destroyForcibly();
                        TharidiaThings.LOGGER.debug("[VIDEO] Killed audio subprocess");
                    });
                    audioProcess.destroyForcibly();
                    audioProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                    TharidiaThings.LOGGER.info("[VIDEO] FFplay audio process terminated");
                }
            } catch (Exception e) {
                TharidiaThings.LOGGER.warn("[VIDEO] Error stopping audio process: {}", e.getMessage());
            }
            audioProcess = null;
        }
        
        hasNewFrame = false;
        readyFrame.set(null);
    }
    
    public void setVolume(float vol) {
        this.volume = Math.max(0.0f, Math.min(1.0f, vol));
        
        // Restart audio with new volume if playing
        if (running.get() && audioProcess != null && !videoUrl.isEmpty()) {
            // Ensure only one volume change thread runs at a time
            synchronized (this) {
                if (!running.get()) return; // Check again in synchronized block
                
                // Kill existing audio process completely
                try {
                    if (audioProcess.isAlive()) {
                        audioProcess.descendants().forEach(ph -> ph.destroyForcibly());
                        audioProcess.destroyForcibly();
                        audioProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                    }
                } catch (Exception e) {
                    TharidiaThings.LOGGER.warn("[VIDEO] Error stopping audio for volume change: {}", e.getMessage());
                }
                audioProcess = null;
                
                // Restart audio with new volume
                new Thread(() -> {
                    try {
                        // Small delay to ensure process cleanup
                        Thread.sleep(100);
                        if (!running.get()) return; // Don't start if stopped
                        
                        boolean isYouTubeOrTwitch = YouTubeUrlExtractor.isValidYouTubeUrl(videoUrl) || videoUrl.contains("twitch.tv");
                        String streamUrl = isYouTubeOrTwitch ? YouTubeUrlExtractor.getBestStreamUrl(videoUrl) : videoUrl;
                        if (streamUrl != null && running.get()) {
                            startAudioProcess(streamUrl);
                        }
                    } catch (Exception e) {
                        TharidiaThings.LOGGER.warn("[VIDEO] Failed to restart audio with new volume: {}", e.getMessage());
                    }
                }, "VolumeChange-" + System.currentTimeMillis()).start();
            }
        }
    }
    
    public float getVolume() {
        return volume;
    }
    
    public void seekForward(int seconds) {
        TharidiaThings.LOGGER.info("[VIDEO] Seek not supported for streams");
    }
    
    public void seekBackward(int seconds) {
        TharidiaThings.LOGGER.info("[VIDEO] Seek not supported for streams");
    }
    
    public void release() {
        if (isReleased.getAndSet(true)) {
            return; // Already released
        }
        
        TharidiaThings.LOGGER.info("[VIDEO] Releasing player for screen {}", screen.getId());
        
        stopInternal();
        
        if (texture != null) {
            texture.close();
            texture = null;
        }
        
        if (image != null) {
            image.close();
            image = null;
        }
        
        frameBuffer1 = null;
        frameBuffer2 = null;
        
        TharidiaThings.LOGGER.info("[VIDEO] Player released");
    }
    
    // Getters
    public DynamicTexture getTexture() {
        return texture;
    }
    
    public VideoScreen getScreen() {
        return screen;
    }
    
    public String getVideoUrl() {
        return videoUrl;
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
}
