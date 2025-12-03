package com.tharidia.tharidia_things.client.video;

import com.mojang.blaze3d.platform.NativeImage;
import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.video.VideoScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FFmpegStreamPlayer {
    private final VideoScreen screen;
    private Process ffmpegProcess;
    private Process audioProcess;
    private Thread readerThread;
    private DynamicTexture texture;
    private NativeImage image;
    private volatile boolean running = false;
    private volatile boolean isReleased = false;
    private String videoUrl;
    private volatile float volume = 1.0f;
    
    // Thread synchronization for frame data
    private final Object frameLock = new Object();
    private byte[] frontFrameData = null;
    private byte[] backFrameData = null;
    private volatile boolean hasNewFrame = false;
    
    private int videoWidth = 1280;
    private int videoHeight = 720;
    
    // Error tracking
    private long lastFrameTime = 0;
    private static final long FRAME_TIMEOUT = 10000;
    
    public FFmpegStreamPlayer(VideoScreen screen) {
        this.screen = screen;
    }
    
    public void loadVideo(String url) {
        this.videoUrl = url;
        
        try {
            TharidiaThings.LOGGER.info("[FFmpeg] Starting stream capture: {}", url);
            
            // Detect OS
            String os = System.getProperty("os.name").toLowerCase();
            String ffmpegCmd = os.contains("win") ? "ffmpeg.exe" : "ffmpeg";
            String ffplayCmd = os.contains("win") ? "ffplay.exe" : "ffplay";
            
            // Use lower resolution for testing performance
            // TODO: Increase back to 1280x720 after optimization
            videoWidth = 640;
            videoHeight = 360;
            
            TharidiaThings.LOGGER.info("[FFmpeg] Using native 16:9 resolution: {}x{}", videoWidth, videoHeight);
            
            // Start FFmpeg with optimized parameters for streaming
            ProcessBuilder pb = new ProcessBuilder(
                ffmpegCmd,
                // Network options
                "-reconnect", "1",
                "-reconnect_streamed", "1",
                "-reconnect_delay_max", "5",
                "-timeout", "10000000",
                "-rw_timeout", "10000000",
                // Input
                "-i", url,
                // Video processing - scale to exact 640x360 with proper aspect ratio
                "-vf", "scale=640:360:force_original_aspect_ratio=decrease,pad=640:360:(ow-iw)/2:(oh-ih)/2:black",
                "-sws_flags", "fast_bilinear",
                // Output format
                "-f", "rawvideo",
                "-pix_fmt", "rgb24",
                "-r", "25",
                "-vsync", "1",
                // Buffer settings - optimized for live streaming
                "-fflags", "nobuffer",
                "-flags", "low_delay",
                "-probesize", "32",
                "-analyzeduration", "0",
                // No audio in video stream (separate process for sync)
                "-an",
                "-"
            );
            pb.redirectErrorStream(true);
            
            ffmpegProcess = pb.start();
            
            // Log FFmpeg errors in separate thread
            new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(ffmpegProcess.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        TharidiaThings.LOGGER.debug("[FFmpeg] {}", line);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }, "FFmpeg-Error-Logger").start();
            
            // Start separate FFplay process for audio - delayed start for sync
            try {
                // Small delay to ensure video process is ready
                Thread.sleep(100);
                
                ProcessBuilder audioPb = new ProcessBuilder(
                    ffplayCmd,
                    "-nodisp",
                    "-autoexit",
                    "-sync", "ext",
                    "-af", "volume=" + volume,
                    "-vn",
                    "-framedrop",
                    "-infbuf",
                    "-i", url
                );
                audioPb.redirectErrorStream(false);
                audioProcess = audioPb.start();
                TharidiaThings.LOGGER.info("[FFmpeg] Audio playback started at volume {}", volume);
            } catch (Exception e) {
                TharidiaThings.LOGGER.warn("[FFmpeg] Could not start audio playback: {}", e.getMessage());
            }
            
            texture = new DynamicTexture(videoWidth, videoHeight, false);
            image = texture.getPixels(); // Get the NativeImage from DynamicTexture
            
            running = true;
            
            readerThread = new Thread(this::readFrames, "FFmpeg-Reader-" + screen.getId());
            readerThread.setDaemon(true);
            readerThread.start();
            
            TharidiaThings.LOGGER.info("[FFmpeg] Stream capture started");
            
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("[FFmpeg] Failed to start stream", e);
        }
    }
    
    private void readFrames() {
        Thread currentThread = Thread.currentThread();
        try {
            InputStream inputStream = new BufferedInputStream(
                ffmpegProcess.getInputStream(), 
                2 * 1024 * 1024
            );
            byte[] frameBuffer = new byte[videoWidth * videoHeight * 3];
            int frameCount = 0;
            lastFrameTime = System.currentTimeMillis();
            
            while (running && ffmpegProcess.isAlive() && !currentThread.isInterrupted()) {
                int bytesRead = 0;
                
                while (bytesRead < frameBuffer.length && running && !currentThread.isInterrupted()) {
                    try {
                        // Check if data available
                        if (inputStream.available() == 0) {
                            // Check timeout
                            if (System.currentTimeMillis() - lastFrameTime > FRAME_TIMEOUT) {
                                TharidiaThings.LOGGER.warn("[FFmpeg] Frame timeout, stream may be frozen");
                                running = false;
                                return;
                            }
                            Thread.sleep(10);
                            continue;
                        }
                        
                        int read = inputStream.read(frameBuffer, bytesRead, frameBuffer.length - bytesRead);
                        if (read == -1) {
                            TharidiaThings.LOGGER.info("[FFmpeg] Stream ended");
                            running = false;
                            return;
                        }
                        bytesRead += read;
                        
                    } catch (InterruptedException e) {
                        TharidiaThings.LOGGER.info("[FFmpeg] Reader thread interrupted");
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                
                if (running && !isReleased && !currentThread.isInterrupted()) {
                    // Use double buffering
                    synchronized (frameLock) {
                        if (backFrameData == null) {
                            backFrameData = new byte[frameBuffer.length];
                        }
                        if (frontFrameData == null) {
                            frontFrameData = new byte[frameBuffer.length];
                        }
                        System.arraycopy(frameBuffer, 0, backFrameData, 0, frameBuffer.length);
                        
                        // Swap buffers
                        byte[] temp = frontFrameData;
                        frontFrameData = backFrameData;
                        backFrameData = temp;
                        hasNewFrame = true;
                    }
                    
                    lastFrameTime = System.currentTimeMillis();
                    frameCount++;
                    
                    if (frameCount % 100 == 0) {
                        TharidiaThings.LOGGER.debug("[FFmpeg] Processed {} frames", frameCount);
                    }
                }
            }
        } catch (Exception e) {
            if (running && !currentThread.isInterrupted()) {
                TharidiaThings.LOGGER.error("[FFmpeg] Frame reading error", e);
            }
        } finally {
            TharidiaThings.LOGGER.info("[FFmpeg] Reader thread exiting");
        }
    }
    
    private void updateTextureOnMainThread() {
        byte[] frameData;
        synchronized (frameLock) {
            if (!hasNewFrame || frontFrameData == null) {
                return;
            }
            frameData = frontFrameData;
            hasNewFrame = false;
        }
        
        try {
            if (isReleased || image == null || texture == null) {
                return;
            }
            
            // Verify data size
            int expectedBytes = videoWidth * videoHeight * 3;
            if (frameData.length < expectedBytes) {
                TharidiaThings.LOGGER.warn("[FFmpeg] Frame data size mismatch: expected {}, got {}", expectedBytes, frameData.length);
                return;
            }
            
            // Fast pixel writing - single pass
            int totalPixels = videoWidth * videoHeight;
            int index = 0;
            for (int i = 0; i < totalPixels; i++) {
                int x = i % videoWidth;
                int y = i / videoWidth;
                
                int r = frameData[index++] & 0xFF;
                int g = frameData[index++] & 0xFF;
                int b = frameData[index++] & 0xFF;
                
                // ABGR format for NativeImage
                int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
                image.setPixelRGBA(x, y, abgr);
            }
            
            // Upload texture to GPU (must be on main thread)
            texture.upload();
            
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("[FFmpeg] Texture update error: {}", e.getMessage());
        }
    }
    
    public void play() {
        // Stream is always playing
    }
    
    public void pause() {
        // Cannot pause live stream
    }
    
    public void stop() {
        running = false;
        
        if (readerThread != null) {
            readerThread.interrupt();
        }
        
        if (audioProcess != null && audioProcess.isAlive()) {
            audioProcess.destroy();
            try {
                audioProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            ffmpegProcess.destroy();
            try {
                ffmpegProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void seekForward(int seconds) {
        // Cannot seek live stream
    }
    
    public void seekBackward(int seconds) {
        // Cannot seek live stream
    }
    
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        TharidiaThings.LOGGER.info("[FFmpeg] Volume set to {}", volume);
        
        // Restart audio process with new volume
        if (audioProcess != null && videoUrl != null) {
            try {
                audioProcess.destroy();
                audioProcess.waitFor();
                
                String os = System.getProperty("os.name").toLowerCase();
                String ffplayCmd = os.contains("win") ? "ffplay.exe" : "ffplay";
                
                ProcessBuilder audioPb = new ProcessBuilder(
                    ffplayCmd,
                    "-nodisp",
                    "-autoexit",
                    "-af", "volume=" + volume,
                    "-i", videoUrl
                );
                audioPb.redirectErrorStream(false);
                audioProcess = audioPb.start();
                TharidiaThings.LOGGER.info("[FFmpeg] Audio restarted with volume {}", volume);
            } catch (Exception e) {
                TharidiaThings.LOGGER.warn("[FFmpeg] Failed to restart audio with new volume: {}", e.getMessage());
            }
        }
    }
    
    public float getVolume() {
        return volume;
    }
    
    public void release() {
        isReleased = true;
        stop();
        
        if (texture != null) {
            texture.close();
            texture = null;
        }
        
        if (image != null) {
            image.close();
            image = null;
        }
    }
    
    public void update() {
        // Called every frame on main render thread
        if (hasNewFrame && !isReleased) {
            updateTextureOnMainThread();
        }
    }
    
    public DynamicTexture getTexture() {
        return texture;
    }
    
    public VideoScreen getScreen() {
        return screen;
    }
}
