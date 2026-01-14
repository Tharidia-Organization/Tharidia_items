package com.THproject.tharidia_things.network;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Server-side handler for music file download requests
 */
public class ServerMusicFileHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Handles a request from a client to download a music file
     */
    public static void handleMusicFileRequest(RequestMusicFilePacket packet, ServerPlayer player) {
        String musicFile = packet.musicFile();
        LOGGER.info("Player {} requested music file: {}", player.getName().getString(), musicFile);
        
        // Get the server's music directory (in server root/zone_music)
        Path serverMusicDir = Paths.get("zone_music");
        
        // Create directory if it doesn't exist
        if (!Files.exists(serverMusicDir)) {
            try {
                Files.createDirectories(serverMusicDir);
                LOGGER.info("Created music directory at: {}", serverMusicDir);
            } catch (IOException e) {
                LOGGER.error("Failed to create music directory: {}", e.getMessage());
                return;
            }
        }
        
        Path musicFilePath = serverMusicDir.resolve(musicFile);
        
        // Security check: ensure the file is within the music directory
        if (!musicFilePath.normalize().startsWith(serverMusicDir.normalize())) {
            LOGGER.warn("Player {} attempted to access file outside music directory: {}", 
                player.getName().getString(), musicFile);
            return;
        }
        
        // Check if file exists
        if (!Files.exists(musicFilePath)) {
            LOGGER.warn("Requested music file does not exist: {}", musicFilePath.toAbsolutePath());
            LOGGER.warn("Please place MP3 files in: {}", serverMusicDir);
            return;
        }
        
        // Check if it's an MP3 file
        if (!musicFile.toLowerCase().endsWith(".mp3")) {
            LOGGER.warn("Player {} requested non-MP3 file: {}", player.getName().getString(), musicFile);
            return;
        }
        
        try {
            // Read the entire file
            byte[] fileData = Files.readAllBytes(musicFilePath);
            LOGGER.info("Sending music file {} ({} bytes) to player {}", 
                musicFile, fileData.length, player.getName().getString());
            
            // Calculate number of chunks needed
            int totalChunks = (int) Math.ceil((double) fileData.length / MusicFileDataPacket.MAX_CHUNK_SIZE);
            
            // Send file in chunks
            for (int i = 0; i < totalChunks; i++) {
                int offset = i * MusicFileDataPacket.MAX_CHUNK_SIZE;
                int chunkSize = Math.min(MusicFileDataPacket.MAX_CHUNK_SIZE, fileData.length - offset);
                
                byte[] chunkData = new byte[chunkSize];
                System.arraycopy(fileData, offset, chunkData, 0, chunkSize);
                
                boolean isLastChunk = (i == totalChunks - 1);
                
                MusicFileDataPacket dataPacket = new MusicFileDataPacket(
                    musicFile,
                    chunkData,
                    i,
                    totalChunks,
                    isLastChunk
                );
                
                PacketDistributor.sendToPlayer(player, dataPacket);
                
                LOGGER.debug("Sent chunk {}/{} of {} to player {}", 
                    i + 1, totalChunks, musicFile, player.getName().getString());
            }
            
            LOGGER.info("Successfully sent music file {} to player {}", 
                musicFile, player.getName().getString());
            
        } catch (IOException e) {
            LOGGER.error("Failed to read music file {}: {}", musicFilePath, e.getMessage(), e);
        }
    }
}
