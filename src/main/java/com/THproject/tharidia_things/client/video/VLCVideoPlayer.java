package com.THproject.tharidia_things.client.video;

import com.mojang.blaze3d.platform.NativeImage;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.video.VideoScreen;
import com.THproject.tharidia_things.video.YouTubeUrlExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private Process windowsAudioProcess;  // Separate audio process for Windows
    private Thread readerThread;
    private Thread audioThread;
    private DynamicTexture texture;
    private NativeImage image;
    
    // Audio playback
    private SourceDataLine audioLine;
    private volatile boolean audioRunning = false;
    private Path videoPipe;
    private Path audioPipe;
    
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
    
    // Audio format - 48kHz 16-bit stereo
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
        48000, 16, 2, true, false
    );
    private static final int AUDIO_BUFFER_SIZE = 4800; // 100ms at 48kHz
    
    // Performance monitoring
    private long lastFpsTime = 0;
    private int frameCounter = 0;
    private double currentFps = 0;
    private long lastCpuCheck = 0;
    private double cpuUsage = 0;
    
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

            // Register shutdown hook to ensure process cleanup on JVM exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (!isReleased.get()) {
                    forceCleanupProcesses();
                }
            }, "VLCVideoPlayer-Shutdown-" + screen.getId()));

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
    
    /**
     * Load and play a video
     * DISABLED: ProcessBuilder execution not allowed for CurseForge compliance
     * Video playback requires external tools (ffmpeg/ffplay) which cannot be executed
     */
    public void loadVideo(String url) {
        if (!isInitialized) {
            TharidiaThings.LOGGER.error("[VIDEO] Cannot load - not initialized");
            return;
        }

        this.videoUrl = url;

        // ProcessBuilder (ffmpeg/ffplay) disabled for CurseForge compliance
        TharidiaThings.LOGGER.warn("[VIDEO] Video playback is disabled in CurseForge mode.");
        TharidiaThings.LOGGER.warn("[VIDEO] This feature requires executing external tools (ffmpeg/ffplay)");
        TharidiaThings.LOGGER.warn("[VIDEO] which is not allowed by CurseForge guidelines.");
        TharidiaThings.LOGGER.info("[VIDEO] Requested URL (not playing): {}", url);
    }
    
    /**
     * Start unified FFmpeg process
     * DISABLED: ProcessBuilder execution not allowed for CurseForge compliance
     */
    private void startUnifiedProcess(String url) throws Exception {
        // ProcessBuilder (ffmpeg) disabled for CurseForge compliance
        TharidiaThings.LOGGER.warn("[VIDEO] startUnifiedProcess disabled - CurseForge compliance mode");
        throw new UnsupportedOperationException("Video playback disabled for CurseForge compliance");
    }
    
    private void initializeAudioLine() throws Exception {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                TharidiaThings.LOGGER.warn("[VIDEO] Audio line not supported, audio will be disabled");
                return;
            }
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(AUDIO_FORMAT, AUDIO_BUFFER_SIZE * 2);
            audioLine.start();
            TharidiaThings.LOGGER.info("[VIDEO] Audio line initialized: {}", AUDIO_FORMAT);
        } catch (Exception e) {
            TharidiaThings.LOGGER.warn("[VIDEO] Failed to initialize audio line: {}", e.getMessage());
            audioLine = null;
        }
    }
    
    /**
     * Create named pipes for video/audio streaming
     * DISABLED: ProcessBuilder execution not allowed for CurseForge compliance
     */
    private void createNamedPipes() throws Exception {
        // ProcessBuilder (mkfifo) disabled for CurseForge compliance
        TharidiaThings.LOGGER.warn("[VIDEO] createNamedPipes disabled - CurseForge compliance mode");
        throw new UnsupportedOperationException("Named pipes creation disabled for CurseForge compliance");
    }
    
    private void cleanupPipes() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");
        
        if (!isWindows) {
            // Only Unix pipes need cleanup
            try {
                if (videoPipe != null) {
                    Files.deleteIfExists(videoPipe);
                }
                if (audioPipe != null) {
                    Files.deleteIfExists(audioPipe);
                }
            } catch (Exception e) {
                TharidiaThings.LOGGER.warn("[VIDEO] Error cleaning up pipes: {}", e.getMessage());
            }
        }
        // Windows named pipes are automatically cleaned up when all handles are closed
    }
    
    private void startPipeReaders() {
        // Set running state
        running.set(true);
        
        // Start video reader thread
        readerThread = new Thread(this::readFramesFromPipe, "VideoReader-" + screen.getId());
        readerThread.setDaemon(true);
        readerThread.start();
        
        // Start audio reader thread
        audioThread = new Thread(this::readAudioFromPipe, "AudioReader-" + screen.getId());
        audioThread.setDaemon(true);
        audioThread.start();
        audioRunning = true;
    }
    
    private void readFramesFromPipe() {
        TharidiaThings.LOGGER.info("[VIDEO] Frame reader thread started from pipe");
        
        try {
            InputStream is;
            String os = System.getProperty("os.name").toLowerCase();
            boolean isWindows = os.contains("win");
            
            if (isWindows) {
                // Windows: Read raw video from stdout
                is = new BufferedInputStream(videoProcess.getInputStream(), FRAME_SIZE * 4);
                TharidiaThings.LOGGER.info("[VIDEO] Reading raw video from stdout (Windows sync limited)");
            } else {
                is = new BufferedInputStream(new FileInputStream(videoPipe.toFile()), FRAME_SIZE * 4);
            }
            
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
                }
            }
            
            is.close();
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
    
    /**
     * Read audio from pipe
     * DISABLED: ProcessBuilder execution not allowed for CurseForge compliance
     */
    private void readAudioFromPipe() {
        // ProcessBuilder (ffmpeg for audio) disabled for CurseForge compliance
        TharidiaThings.LOGGER.warn("[VIDEO] Audio playback disabled - CurseForge compliance mode");
    }
    
    public void update() {
        if (isReleased.get() || !isInitialized || image == null || texture == null) {
            return;
        }
        
        // Performance monitoring
        frameCounter++;
        long now = System.currentTimeMillis();
        
        if (now - lastFpsTime > 5000) {  // Update every 5 seconds
            currentFps = frameCounter * 1000.0 / (now - lastFpsTime);
            TharidiaThings.LOGGER.info("[VIDEO] Performance: {} FPS", 
                String.format("%.1f", currentFps));
            frameCounter = 0;
            lastFpsTime = now;
            
            // Adaptive quality check
            if (currentFps < 25 && VIDEO_WIDTH > 640) {
                TharidiaThings.LOGGER.warn("[VIDEO] Low FPS detected, consider lowering resolution");
            }
        }
        
        // Frame rate limiting
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
        audioRunning = false;
        
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
        
        // Stop audio thread
        if (audioThread != null) {
            audioThread.interrupt();
            try {
                audioThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            audioThread = null;
        }
        
        // Close audio line
        if (audioLine != null) {
            try {
                audioLine.drain();
                audioLine.close();
            } catch (Exception e) {
                TharidiaThings.LOGGER.warn("[VIDEO] Error closing audio line: {}", e.getMessage());
            }
            audioLine = null;
        }
        
        // Stop video process forcefully
        if (videoProcess != null) {
            try {
                videoProcess.destroyForcibly();
                videoProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                TharidiaThings.LOGGER.info("[VIDEO] FFmpeg process terminated");
            } catch (Exception e) {
                TharidiaThings.LOGGER.warn("[VIDEO] Error stopping video process: {}", e.getMessage());
            }
            videoProcess = null;
        }
        
        // Stop Windows audio process if exists
        if (windowsAudioProcess != null) {
            try {
                windowsAudioProcess.destroyForcibly();
                windowsAudioProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                TharidiaThings.LOGGER.info("[VIDEO] Windows audio process terminated");
            } catch (Exception e) {
                TharidiaThings.LOGGER.warn("[VIDEO] Error stopping Windows audio process: {}", e.getMessage());
            }
            windowsAudioProcess = null;
        }
        
        // Clean up pipes
        cleanupPipes();
        
        hasNewFrame = false;
        readyFrame.set(null);
    }
    
    public void setVolume(float vol) {
        this.volume = Math.max(0.0f, Math.min(1.0f, vol));
        
        // Update volume in real-time if playing
        if (audioLine != null && audioRunning) {
            // Java audio line volume control
            if (audioLine.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
                javax.sound.sampled.FloatControl gain = 
                    (javax.sound.sampled.FloatControl) audioLine.getControl(
                        javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                float min = gain.getMinimum();
                float max = gain.getMaximum();
                // Prevent log(0) = -Infinity
                float safeVolume = Math.max(0.0001f, this.volume);
                float dB = (float) (20 * Math.log10(safeVolume));
                gain.setValue(Math.max(min, Math.min(max, dB)));
            }
        }
        
        TharidiaThings.LOGGER.info("[VIDEO] Volume set to {}%", (int)(this.volume * 100));
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
    
    /**
     * Force cleanup of FFmpeg processes during JVM shutdown.
     * Called from shutdown hook - must be fast and non-blocking.
     */
    private void forceCleanupProcesses() {
        running.set(false);
        audioRunning = false;

        if (videoProcess != null) {
            videoProcess.destroyForcibly();
            videoProcess = null;
        }

        if (windowsAudioProcess != null) {
            windowsAudioProcess.destroyForcibly();
            windowsAudioProcess = null;
        }

        cleanupPipes();
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
