package com.THproject.tharidia_things.client.video;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.video.VideoScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side manager for video screens
 * Maintains a registry of screens and their associated video players
 */
public class ClientVideoScreenManager {
    private static final ClientVideoScreenManager INSTANCE = new ClientVideoScreenManager();
    
    // Map of screen UUID -> VideoScreen
    private final Map<UUID, VideoScreen> screens = new ConcurrentHashMap<>();
    private final Map<UUID, Direction> screenFacing = new ConcurrentHashMap<>();
    
    // Map of screen UUID -> VLCVideoPlayer (handles both VLC and FFmpeg internally)
    private final Map<UUID, VLCVideoPlayer> players = new ConcurrentHashMap<>();
    
    private ClientVideoScreenManager() {}
    
    public static ClientVideoScreenManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Add or update a video screen
     */
    public void addOrUpdateScreen(UUID screenId, String dimension, BlockPos corner1, BlockPos corner2,
                                 Direction facing, String videoUrl,
                                 VideoScreen.VideoPlaybackState playbackState, float volume) {
        
        TharidiaThings.LOGGER.info("[VIDEO MANAGER] addOrUpdateScreen called - ScreenId: {}, URL: '{}', State: {}, Volume: {}", 
            screenId, videoUrl, playbackState, (int)(volume * 100));
        
        // Check if we're in the correct dimension
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        String currentDimension = mc.level.dimension().location().toString();
        if (!currentDimension.equals(dimension)) {
            // Screen is in a different dimension, ignore
            TharidiaThings.LOGGER.info("[VIDEO MANAGER] Ignoring screen in different dimension: {} vs {}", dimension, currentDimension);
            return;
        }
        
        // Create or update screen
        VideoScreen screen = screens.get(screenId);
        Direction effectiveFacing = facing != null ? facing : screenFacing.getOrDefault(screenId, Direction.NORTH);
        if (screen == null) {
            screen = new VideoScreen(screenId, corner1, corner2, effectiveFacing);
            screens.put(screenId, screen);
            TharidiaThings.LOGGER.info("Added video screen {} to client", screenId);
        } else if (!screen.getCorner1().equals(corner1) || !screen.getCorner2().equals(corner2) || !screen.getFacing().equals(effectiveFacing)) {
            screen = new VideoScreen(screenId, corner1, corner2, effectiveFacing);
            screens.put(screenId, screen);
            TharidiaThings.LOGGER.info("Updated video screen {} geometry/facing", screenId);
        }
        screenFacing.put(screenId, screen.getFacing());

        screen.setVideoUrl(videoUrl);
        screen.setPlaybackState(playbackState);
        screen.setVolume(volume);
        
        // Handle video player
        VLCVideoPlayer player = players.get(screenId);
        
        if (playbackState == VideoScreen.VideoPlaybackState.STOPPED) {
            // Stop and release player completely (used for restart)
            if (player != null) {
                player.stop();
                player.release();
                players.remove(screenId);
                TharidiaThings.LOGGER.info("Stopped and released video player for screen {}", screenId);
            }
        } else if (playbackState == VideoScreen.VideoPlaybackState.PLAYING) {
            if (player == null || !player.getVideoUrl().equals(videoUrl)) {
                // Check for required video tools before creating player
                boolean toolsOk = VideoToolsManager.getInstance().checkAndInstallTools(videoUrl);
                TharidiaThings.LOGGER.info("[VIDEO MANAGER] Video tools check result: {} for URL: {}", toolsOk, videoUrl);
                
                if (!toolsOk) {
                    TharidiaThings.LOGGER.warn("[VIDEO] Cannot start playback - video tools not installed");
                    return;
                }
                
                // Create new player or URL changed
                if (player != null) {
                    player.stop();
                    player.release();
                }
                
                TharidiaThings.LOGGER.info("[VIDEO MANAGER] Creating new VLCVideoPlayer for screen: {}", screenId);
                player = new VLCVideoPlayer(screen);
                players.put(screenId, player);
                
                if (!videoUrl.isEmpty()) {
                    TharidiaThings.LOGGER.info("[VIDEO MANAGER] Loading video URL: {} for screen: {}", videoUrl, screenId);
                    player.setVolume(volume);
                    player.loadVideo(videoUrl);  // loadVideo already starts playback, no need to call play()
                    TharidiaThings.LOGGER.info("Started video player for screen {} with URL: {} at volume {}", screenId, videoUrl, (int)(volume * 100));
                }
            } else {
                // Resume existing player
                player.play();
                TharidiaThings.LOGGER.info("Resumed video player for screen {}", screenId);
            }
        } else if (playbackState == VideoScreen.VideoPlaybackState.PAUSED) {
            if (player != null) {
                player.pause();
                TharidiaThings.LOGGER.info("Paused video player for screen {}", screenId);
            }
        }
    }
    
    /**
     * Remove a video screen
     */
    public void removeScreen(UUID screenId) {
        VideoScreen screen = screens.remove(screenId);
        if (screen != null) {
            VLCVideoPlayer player = players.remove(screenId);
            if (player != null) {
                player.stop();
                player.release();
            }
            TharidiaThings.LOGGER.info("Removed video screen {} from client", screenId);
        }
    }
    
    /**
     * Get a video screen by ID
     */
    public VideoScreen getScreen(UUID screenId) {
        return screens.get(screenId);
    }
    
    /**
     * Get a video player by screen ID
     */
    public VLCVideoPlayer getPlayer(UUID screenId) {
        return players.get(screenId);
    }
    
    /**
     * Get all screens
     */
    public Map<UUID, VideoScreen> getAllScreens() {
        return screens;
    }
    
    /**
     * Clear all screens (called on disconnect)
     */
    public void clear() {
        for (VLCVideoPlayer player : players.values()) {
            player.stop();
            player.release();
        }
        players.clear();
        screens.clear();
        TharidiaThings.LOGGER.info("Cleared all video screens from client");
    }
    
    /**
     * Update all video players (called every frame)
     */
    public void tick() {
        for (Map.Entry<UUID, VLCVideoPlayer> entry : players.entrySet()) {
            VLCVideoPlayer player = entry.getValue();
            if (player != null) {
                player.update();
            }
        }
    }
}
