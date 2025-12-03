package com.tharidia.tharidia_things.video;

import com.tharidia.tharidia_things.TharidiaThings;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing all video screens in the world
 * Stores screens per dimension and persists them to disk
 */
public class VideoScreenRegistry extends SavedData {
    private static final String DATA_NAME = "tharidia_video_screens";
    
    // Map of dimension -> (screen UUID -> VideoScreen)
    private final Map<String, Map<UUID, VideoScreen>> screensByDimension = new ConcurrentHashMap<>();
    
    public VideoScreenRegistry() {
        super();
    }
    
    // Get or create registry for a server level
    public static VideoScreenRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<VideoScreenRegistry>(
                VideoScreenRegistry::new,
                VideoScreenRegistry::load,
                null
            ),
            DATA_NAME
        );
    }
    
    // Add a new screen
    public void addScreen(String dimension, VideoScreen screen) {
        screensByDimension.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>())
            .put(screen.getId(), screen);
        setDirty();
        TharidiaThings.LOGGER.info("Added video screen {} in dimension {}", screen.getId(), dimension);
    }
    
    // Remove a screen by UUID
    public boolean removeScreen(String dimension, UUID screenId) {
        Map<UUID, VideoScreen> screens = screensByDimension.get(dimension);
        if (screens != null) {
            VideoScreen removed = screens.remove(screenId);
            if (removed != null) {
                setDirty();
                TharidiaThings.LOGGER.info("Removed video screen {} from dimension {}", screenId, dimension);
                return true;
            }
        }
        return false;
    }
    
    // Get a screen by UUID
    public VideoScreen getScreen(String dimension, UUID screenId) {
        Map<UUID, VideoScreen> screens = screensByDimension.get(dimension);
        return screens != null ? screens.get(screenId) : null;
    }
    
    // Get screen at a specific position
    public VideoScreen getScreenAt(String dimension, BlockPos pos) {
        Map<UUID, VideoScreen> screens = screensByDimension.get(dimension);
        if (screens != null) {
            for (VideoScreen screen : screens.values()) {
                if (screen.contains(pos)) {
                    return screen;
                }
            }
        }
        return null;
    }
    
    // Get all screens in a dimension
    public Collection<VideoScreen> getScreensInDimension(String dimension) {
        Map<UUID, VideoScreen> screens = screensByDimension.get(dimension);
        return screens != null ? screens.values() : Collections.emptyList();
    }
    
    // Get all screens
    public Map<String, Map<UUID, VideoScreen>> getAllScreens() {
        return screensByDimension;
    }
    
    // Check if a position overlaps with any existing screen
    public boolean overlapsExistingScreen(String dimension, BlockPos corner1, BlockPos corner2) {
        Map<UUID, VideoScreen> screens = screensByDimension.get(dimension);
        if (screens == null) return false;
        
        for (VideoScreen screen : screens.values()) {
            if (screen.contains(corner1) || screen.contains(corner2)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        ListTag dimensionsList = new ListTag();
        
        for (Map.Entry<String, Map<UUID, VideoScreen>> dimensionEntry : screensByDimension.entrySet()) {
            CompoundTag dimensionTag = new CompoundTag();
            dimensionTag.putString("dimension", dimensionEntry.getKey());
            
            ListTag screensList = new ListTag();
            for (VideoScreen screen : dimensionEntry.getValue().values()) {
                screensList.add(screen.save());
            }
            
            dimensionTag.put("screens", screensList);
            dimensionsList.add(dimensionTag);
        }
        
        tag.put("dimensions", dimensionsList);
        return tag;
    }
    
    public static VideoScreenRegistry load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        VideoScreenRegistry registry = new VideoScreenRegistry();
        
        ListTag dimensionsList = tag.getList("dimensions", Tag.TAG_COMPOUND);
        for (int i = 0; i < dimensionsList.size(); i++) {
            CompoundTag dimensionTag = dimensionsList.getCompound(i);
            String dimension = dimensionTag.getString("dimension");
            
            ListTag screensList = dimensionTag.getList("screens", Tag.TAG_COMPOUND);
            Map<UUID, VideoScreen> screens = new ConcurrentHashMap<>();
            
            for (int j = 0; j < screensList.size(); j++) {
                CompoundTag screenTag = screensList.getCompound(j);
                VideoScreen screen = VideoScreen.load(screenTag);
                screens.put(screen.getId(), screen);
            }
            
            registry.screensByDimension.put(dimension, screens);
        }
        
        return registry;
    }
}
