package com.THproject.tharidia_things.client;

import com.mojang.logging.LogUtils;
import com.THproject.tharidia_things.network.MusicFileDataPacket;
import com.THproject.tharidia_things.network.RequestMusicFilePacket;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles client-side music playback for zone music system
 * Plays MP3 files from local cache using JLayer
 * Files must be manually copied to the zone_music_cache directory
 */
public class ZoneMusicPlayer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ZoneMusicPlayer");
        t.setDaemon(true);
        return t;
    });
    
    private static SourceDataLine audioLine = null;
    private static Thread playbackThread = null;
    private static String currentMusicFile = "";
    private static boolean shouldLoop = false;
    private static volatile boolean shouldStop = false;
    
    // Volume transition settings
    private static final long FADE_DURATION_MS = 2000; // 2 seconds for fade in/out
    private static volatile float targetVolume = 1.0f;
    private static volatile float currentVolume = 0.0f;
    private static volatile boolean isFadingIn = false;
    private static volatile boolean isFadingOut = false;
    
    // Minecraft music suppression
    private static Thread musicSuppressionThread = null;
    private static volatile boolean suppressMinecraftMusic = false;
    
    // Cache directory for music files
    private static Path musicCacheDir = null;
    
    // Download management
    private static final Map<String, DownloadState> activeDownloads = new HashMap<>();
    private static String pendingMusicFile = null;
    private static boolean pendingLoop = false;
    
    /**
     * State for tracking multi-chunk downloads
     */
    private static class DownloadState {
        final String musicFile;
        final int totalChunks;
        final Map<Integer, byte[]> chunks;
        
        DownloadState(String musicFile, int totalChunks) {
            this.musicFile = musicFile;
            this.totalChunks = totalChunks;
            this.chunks = new HashMap<>();
        }
        
        void addChunk(int index, byte[] data) {
            chunks.put(index, data);
        }
        
        boolean isComplete() {
            return chunks.size() == totalChunks;
        }
        
        byte[] assembleFile() {
            int totalSize = 0;
            for (byte[] chunk : chunks.values()) {
                totalSize += chunk.length;
            }
            
            byte[] result = new byte[totalSize];
            int offset = 0;
            
            for (int i = 0; i < totalChunks; i++) {
                byte[] chunk = chunks.get(i);
                if (chunk != null) {
                    System.arraycopy(chunk, 0, result, offset, chunk.length);
                    offset += chunk.length;
                }
            }
            
            return result;
        }
    }
    
    /**
     * Initializes the music cache directory
     */
    public static void initialize() {
        try {
            musicCacheDir = Paths.get(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "zone_music_cache");
            if (!Files.exists(musicCacheDir)) {
                Files.createDirectories(musicCacheDir);
                LOGGER.info("Created music cache directory at: {}", musicCacheDir);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create music cache directory", e);
        }
    }
    
    /**
     * Plays a music file (must exist in cache directory)
     */
    public static void playMusic(String musicFile, boolean loop) {
        if (musicFile == null || musicFile.isEmpty()) {
            LOGGER.warn("Attempted to play empty music file");
            return;
        }
        
        // Stop current music if playing (non-blocking)
        if (isPlaying()) {
            isFadingOut = true;
            isFadingIn = false;
            // Don't wait - let it fade out asynchronously
            try {
                Thread.sleep(100); // Brief pause to start fade-out
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        stopMusicImmediate();
        
        // Immediately stop any playing Minecraft music
        stopMinecraftMusicImmediate();
        
        // Start suppressing Minecraft's background music
        startMinecraftMusicSuppression();
        
        currentMusicFile = musicFile;
        shouldLoop = loop;
        shouldStop = false;
        currentVolume = 0.0f; // Start from silence
        targetVolume = 1.0f; // Will be adjusted to actual volume in playback
        isFadingIn = true;
        isFadingOut = false;
        
        LOGGER.info("Starting music playback: {} (loop: {})", musicFile, loop);
        
        // Start playback in background thread
        EXECUTOR.submit(() -> {
            try {
                Path cachedFile = getCachedMusicFile(musicFile);
                
                if (cachedFile != null && Files.exists(cachedFile)) {
                    playMusicFile(cachedFile);
                } else {
                    LOGGER.info("Music file not found in cache: {}, requesting from server...", musicFile);
                    requestMusicFileFromServer(musicFile, loop);
                }
            } catch (Exception e) {
                LOGGER.error("Error playing music: {}", e.getMessage(), e);
            }
        });
    }
    
    /**
     * Stops current music playback with fade out (non-blocking)
     */
    public static void stopMusic() {
        if (isPlaying()) {
            LOGGER.info("Stopping music with fade-out");
            // Trigger fade out asynchronously
            isFadingOut = true;
            isFadingIn = false;
            targetVolume = 0.0f;
        } else {
            stopMusicImmediate();
        }
    }
    
    /**
     * Stops music immediately without fade (internal use)
     */
    private static void stopMusicImmediate() {
        shouldStop = true;
        
        if (audioLine != null) {
            try {
                audioLine.stop();
                audioLine.close();
            } catch (Exception e) {
                LOGGER.debug("Error closing audio line: {}", e.getMessage());
            }
            audioLine = null;
        }
        
        if (playbackThread != null && playbackThread.isAlive()) {
            playbackThread.interrupt();
            playbackThread = null;
        }
        
        currentMusicFile = "";
        isFadingIn = false;
        isFadingOut = false;
        currentVolume = 0.0f;
        
        // Stop suppressing Minecraft's background music
        stopMinecraftMusicSuppression();
        
        LOGGER.debug("Music playback stopped");
    }
    
    /**
     * Gets the cached music file path
     */
    private static Path getCachedMusicFile(String musicFile) {
        if (musicCacheDir == null) {
            initialize();
        }
        return musicCacheDir.resolve(musicFile);
    }
    
    /**
     * Plays a music file from disk with volume control
     */
    private static void playMusicFile(Path musicFile) {
        playbackThread = new Thread(() -> {
            do {
                if (shouldStop) {
                    break;
                }
                
                try (FileInputStream fis = new FileInputStream(musicFile.toFile());
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                    
                    Bitstream bitstream = new Bitstream(bis);
                    Decoder decoder = new Decoder();
                    
                    // Read first frame to get audio format
                    Header header = bitstream.readFrame();
                    if (header == null) {
                        LOGGER.error("Could not read MP3 header");
                        break;
                    }
                    
                    // Setup audio format
                    int sampleRate = header.frequency();
                    int channels = (header.mode() == Header.SINGLE_CHANNEL) ? 1 : 2;
                    AudioFormat format = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        sampleRate,
                        16,
                        channels,
                        channels * 2,
                        sampleRate,
                        false
                    );
                    
                    // Open audio line
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    audioLine = (SourceDataLine) AudioSystem.getLine(info);
                    audioLine.open(format);
                    
                    // Set initial volume based on Minecraft settings
                    float baseVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC);
                    targetVolume = baseVolume;
                    
                    // Ensure fade-in is active
                    if (!isFadingIn && !isFadingOut) {
                        currentVolume = 0.0f;
                        isFadingIn = true;
                    }
                    
                    audioLine.start();
                    LOGGER.info("Playing: {}", musicFile.getFileName());
                    
                    // Decode and play
                    bitstream.closeFrame();
                    int frameCount = 0;
                    long lastVolumeCheck = System.currentTimeMillis();
                    
                    while (!shouldStop) {
                        header = bitstream.readFrame();
                        if (header == null) {
                            break; // End of file
                        }
                        
                        SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                        short[] samples = output.getBuffer();
                        
                        // Update base volume periodically (every ~500ms)
                        long now = System.currentTimeMillis();
                        if (now - lastVolumeCheck > 500) {
                            float newBaseVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC);
                            if (Math.abs(newBaseVolume - baseVolume) > 0.01f) {
                                baseVolume = newBaseVolume;
                                targetVolume = baseVolume;
                            }
                            lastVolumeCheck = now;
                        }
                        frameCount++;
                        
                        // Handle volume fading - simplified calculation
                        float effectiveVolume;
                        if (isFadingIn) {
                            // Smooth fade in
                            currentVolume = Math.min(currentVolume + (targetVolume / (FADE_DURATION_MS / 26f)), targetVolume);
                            effectiveVolume = currentVolume;
                            if (currentVolume >= targetVolume) {
                                isFadingIn = false;
                                LOGGER.info("Fade-in complete");
                            }
                        } else if (isFadingOut) {
                            // Smooth fade out
                            currentVolume = Math.max(currentVolume - (baseVolume / (FADE_DURATION_MS / 26f)), 0.0f);
                            effectiveVolume = currentVolume;
                            if (currentVolume <= 0.0f) {
                                isFadingOut = false;
                                shouldStop = true;
                                LOGGER.info("Fade-out complete");
                            }
                        } else {
                            // No fading, use base volume
                            currentVolume = baseVolume;
                            effectiveVolume = baseVolume;
                        }
                        
                        // Apply volume by scaling samples
                        applyVolume(samples, effectiveVolume);
                        
                        // Convert to bytes and write to audio line
                        byte[] bytes = samplesToBytes(samples, output.getBufferLength());
                        audioLine.write(bytes, 0, bytes.length);
                        
                        bitstream.closeFrame();
                    }
                    
                    // Drain and close
                    audioLine.drain();
                    audioLine.stop();
                    audioLine.close();
                    audioLine = null;
                    
                    bitstream.close();
                    
                    // If we reach here, playback finished naturally
                    if (!shouldLoop || shouldStop) {
                        break;
                    }
                    
                    LOGGER.debug("Looping music: {}", musicFile.getFileName());
                    
                } catch (Exception e) {
                    if (!shouldStop) {
                        LOGGER.error("Error during playback: {}", e.getMessage(), e);
                    }
                    break;
                } finally {
                    if (audioLine != null) {
                        audioLine.close();
                        audioLine = null;
                    }
                }
                
                // Small delay before looping
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
                
            } while (shouldLoop && !shouldStop);
            
            LOGGER.debug("Playback thread finished");
        }, "MusicPlayback");
        
        playbackThread.setDaemon(true);
        playbackThread.start();
    }
    
    /**
     * Applies volume to audio samples
     */
    private static void applyVolume(short[] samples, float volume) {
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) (samples[i] * volume);
        }
    }
    
    /**
     * Converts short samples to byte array
     */
    private static byte[] samplesToBytes(short[] samples, int length) {
        byte[] bytes = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            bytes[i * 2] = (byte) (samples[i] & 0xff);
            bytes[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xff);
        }
        return bytes;
    }
    
    /**
     * Sets volume using hardware control if available
     */
    private static void setVolume(float volume) {
        if (audioLine != null && audioLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            try {
                FloatControl volumeControl = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
                float min = volumeControl.getMinimum();
                float max = volumeControl.getMaximum();
                
                // Convert 0.0-1.0 to decibel range
                float db;
                if (volume <= 0.0f) {
                    db = min;
                } else {
                    db = (float) (Math.log10(volume) * 20.0);
                    db = Math.max(min, Math.min(max, db));
                }
                
                volumeControl.setValue(db);
                LOGGER.debug("Hardware volume set to {} dB", db);
            } catch (Exception e) {
                LOGGER.debug("Could not set hardware volume: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Immediately stops Minecraft music (called once at start)
     */
    private static void stopMinecraftMusicImmediate() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getMusicManager() != null) {
                mc.getMusicManager().stopPlaying();
            }
            // Also stop any music playing through the sound manager
            if (mc.getSoundManager() != null) {
                mc.getSoundManager().stop(null, SoundSource.MUSIC);
            }
            LOGGER.debug("Stopped Minecraft music immediately");
        } catch (Exception e) {
            LOGGER.debug("Error stopping Minecraft music: {}", e.getMessage());
        }
    }
    
    /**
     * Starts a background thread to continuously suppress Minecraft's music
     */
    private static void startMinecraftMusicSuppression() {
        stopMinecraftMusicSuppression(); // Stop any existing suppression
        
        suppressMinecraftMusic = true;
        musicSuppressionThread = new Thread(() -> {
            LOGGER.debug("Started Minecraft music suppression");
            while (suppressMinecraftMusic && !Thread.currentThread().isInterrupted()) {
                try {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.getMusicManager() != null) {
                        // Continuously stop any music that tries to play
                        mc.getMusicManager().stopPlaying();
                    }
                    // Also suppress through sound manager
                    if (mc.getSoundManager() != null) {
                        mc.getSoundManager().stop(null, SoundSource.MUSIC);
                    }
                    Thread.sleep(250); // Check every 250ms for responsive suppression
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    // Silently ignore errors
                }
            }
            LOGGER.debug("Stopped Minecraft music suppression");
        }, "MinecraftMusicSuppression");
        musicSuppressionThread.setDaemon(true);
        musicSuppressionThread.start();
    }
    
    /**
     * Stops the Minecraft music suppression thread
     */
    private static void stopMinecraftMusicSuppression() {
        suppressMinecraftMusic = false;
        if (musicSuppressionThread != null && musicSuppressionThread.isAlive()) {
            musicSuppressionThread.interrupt();
            try {
                musicSuppressionThread.join(1000); // Wait up to 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            musicSuppressionThread = null;
        }
        LOGGER.debug("Minecraft background music will resume");
    }
    
    /**
     * Cleans up resources
     */
    public static void shutdown() {
        stopMusic();
        EXECUTOR.shutdown();
    }
    
    /**
     * Gets current playing music file name
     */
    public static String getCurrentMusicFile() {
        return currentMusicFile;
    }
    
    /**
     * Checks if music is currently playing
     */
    public static boolean isPlaying() {
        return audioLine != null && playbackThread != null && playbackThread.isAlive();
    }
    
    /**
     * Requests a music file from the server
     */
    private static void requestMusicFileFromServer(String musicFile, boolean loop) {
        LOGGER.info("[MUSIC DOWNLOAD] Requesting music file from server: {}", musicFile);
        pendingMusicFile = musicFile;
        pendingLoop = loop;
        
        // Send request to server
        RequestMusicFilePacket packet = new RequestMusicFilePacket(musicFile);
        PacketDistributor.sendToServer(packet);
    }
    
    /**
     * Receives a chunk of music file data from the server
     */
    public static void receiveMusicChunk(MusicFileDataPacket packet) {
        String musicFile = packet.musicFile();
        
        // Get or create download state
        DownloadState state = activeDownloads.computeIfAbsent(
            musicFile, 
            f -> new DownloadState(musicFile, packet.totalChunks())
        );
        
        // Add this chunk
        state.addChunk(packet.chunkIndex(), packet.data());
        
        LOGGER.info("[MUSIC DOWNLOAD] Received chunk {}/{} for {}", 
            packet.chunkIndex() + 1, packet.totalChunks(), musicFile);
        
        // Check if download is complete
        if (state.isComplete()) {
            LOGGER.info("[MUSIC DOWNLOAD] Download complete for {}, saving to cache...", musicFile);
            
            try {
                // Assemble the file
                byte[] fileData = state.assembleFile();
                
                // Save to cache
                Path cachedFile = getCachedMusicFile(musicFile);
                if (cachedFile != null) {
                    Files.write(cachedFile, fileData);
                    LOGGER.info("[MUSIC DOWNLOAD] Saved {} ({} bytes) to cache", musicFile, fileData.length);
                    
                    // Clean up download state
                    activeDownloads.remove(musicFile);
                    
                    // If this was the pending music file, play it now
                    if (musicFile.equals(pendingMusicFile)) {
                        LOGGER.info("[MUSIC DOWNLOAD] Starting playback of downloaded file: {}", musicFile);
                        playMusicFile(cachedFile);
                        pendingMusicFile = null;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("[MUSIC DOWNLOAD] Failed to save music file to cache: {}", e.getMessage(), e);
                activeDownloads.remove(musicFile);
            }
        }
    }
}
