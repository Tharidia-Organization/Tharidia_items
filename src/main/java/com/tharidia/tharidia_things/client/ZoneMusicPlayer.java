package com.tharidia.tharidia_things.client;

import com.mojang.logging.LogUtils;
import com.tharidia.tharidia_things.network.MusicFileDataPacket;
import com.tharidia.tharidia_things.network.RequestMusicFilePacket;
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
        
        // Stop current music if playing
        stopMusic();
        
        currentMusicFile = musicFile;
        shouldLoop = loop;
        shouldStop = false;
        
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
     * Stops current music playback
     */
    public static void stopMusic() {
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
                    float currentVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC);
                    setVolume(currentVolume);
                    
                    audioLine.start();
                    LOGGER.info("Playing: {} at {}% volume", musicFile.getFileName(), (int)(currentVolume * 100));
                    
                    // Decode and play
                    bitstream.closeFrame();
                    int frameCount = 0;
                    while (!shouldStop) {
                        header = bitstream.readFrame();
                        if (header == null) {
                            break; // End of file
                        }
                        
                        SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                        short[] samples = output.getBuffer();
                        
                        // Update volume every 10 frames (~0.25 seconds) to reflect Minecraft settings changes
                        if (frameCount % 10 == 0) {
                            float newVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC);
                            if (Math.abs(newVolume - currentVolume) > 0.01f) {
                                currentVolume = newVolume;
                                setVolume(currentVolume);
                                LOGGER.debug("Volume updated to {}%", (int)(currentVolume * 100));
                            }
                        }
                        frameCount++;
                        
                        // Apply volume by scaling samples
                        applyVolume(samples, currentVolume);
                        
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
