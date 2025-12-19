package com.THproject.tharidia_things.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;

import java.util.*;

/**
 * Configuration for crop protection in realm outer layers.
 * Loaded from datapacks at data/tharidiathings/crop_protection/config.json
 * Supports both specific block IDs and block tags for automatic modded crop detection
 */
public class CropProtectionConfig extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static Set<Block> protectedCrops = new HashSet<>();
    private static Set<Block> protectedFarmland = new HashSet<>();
    private static Set<TagKey<Block>> protectedCropTags = new HashSet<>();
    private static Set<TagKey<Block>> protectedFarmlandTags = new HashSet<>();
    
    public CropProtectionConfig() {
        super(GSON, "crop_protection");
        initializeDefaults();
    }
    
    /**
     * Initialize default protected crops
     */
    private void initializeDefaults() {
        protectedCrops.clear();
        protectedFarmland.clear();
        protectedCropTags.clear();
        protectedFarmlandTags.clear();
        
        // Default crops
        protectedCrops.add(Blocks.WHEAT);
        protectedCrops.add(Blocks.CARROTS);
        protectedCrops.add(Blocks.POTATOES);
        protectedCrops.add(Blocks.BEETROOTS);
        protectedCrops.add(Blocks.MELON_STEM);
        protectedCrops.add(Blocks.PUMPKIN_STEM);
        protectedCrops.add(Blocks.ATTACHED_MELON_STEM);
        protectedCrops.add(Blocks.ATTACHED_PUMPKIN_STEM);
        protectedCrops.add(Blocks.SWEET_BERRY_BUSH);
        protectedCrops.add(Blocks.NETHER_WART);
        protectedCrops.add(Blocks.COCOA);
        
        // Default farmland
        protectedFarmland.add(Blocks.FARMLAND);
        
        // Default tags - automatically includes all blocks with these tags
        protectedCropTags.add(BlockTags.CROPS);
        protectedCropTags.add(BlockTags.SAPLINGS); // Optional: protect saplings too
        
        LOGGER.info("Initialized default crop protection with {} crops, {} farmland types, {} crop tags, {} farmland tags", 
            protectedCrops.size(), protectedFarmland.size(), protectedCropTags.size(), protectedFarmlandTags.size());
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> configs, ResourceManager resourceManager, ProfilerFiller profiler) {
        // Reset to defaults before loading
        initializeDefaults();
        
        int loaded = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : configs.entrySet()) {
            try {
                ResourceLocation location = entry.getKey();
                JsonObject json = entry.getValue().getAsJsonObject();
                
                LOGGER.info("Loading crop protection config from: {}", location);
                
                // Load protected crops
                if (json.has("protected_crops")) {
                    Set<Block> crops = new HashSet<>();
                    json.getAsJsonArray("protected_crops").forEach(element -> {
                        String blockId = element.getAsString();
                        ResourceLocation blockLocation = ResourceLocation.tryParse(blockId);
                        if (blockLocation != null) {
                            Block block = BuiltInRegistries.BLOCK.get(blockLocation);
                            if (block != null && block != Blocks.AIR) {
                                crops.add(block);
                            } else {
                                LOGGER.warn("Unknown crop block: {}", blockId);
                            }
                        } else {
                            LOGGER.warn("Invalid block ID format: {}", blockId);
                        }
                    });
                    
                    if (!crops.isEmpty()) {
                        protectedCrops = crops;
                        LOGGER.info("Loaded {} protected crops", crops.size());
                    }
                }
                
                // Load protected farmland
                if (json.has("protected_farmland")) {
                    Set<Block> farmland = new HashSet<>();
                    json.getAsJsonArray("protected_farmland").forEach(element -> {
                        String blockId = element.getAsString();
                        ResourceLocation blockLocation = ResourceLocation.tryParse(blockId);
                        if (blockLocation != null) {
                            Block block = BuiltInRegistries.BLOCK.get(blockLocation);
                            if (block != null && block != Blocks.AIR) {
                                farmland.add(block);
                            } else {
                                LOGGER.warn("Unknown farmland block: {}", blockId);
                            }
                        } else {
                            LOGGER.warn("Invalid block ID format: {}", blockId);
                        }
                    });
                    
                    if (!farmland.isEmpty()) {
                        protectedFarmland = farmland;
                        LOGGER.info("Loaded {} protected farmland types", farmland.size());
                    }
                }
                
                // Load protected crop tags
                if (json.has("protected_crop_tags")) {
                    Set<TagKey<Block>> cropTags = new HashSet<>();
                    json.getAsJsonArray("protected_crop_tags").forEach(element -> {
                        String tagId = element.getAsString();
                        ResourceLocation tagLocation = ResourceLocation.tryParse(tagId);
                        if (tagLocation != null) {
                            TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), tagLocation);
                            cropTags.add(tag);
                            LOGGER.info("Added crop tag: {}", tagId);
                        } else {
                            LOGGER.warn("Invalid tag ID format: {}", tagId);
                        }
                    });
                    
                    if (!cropTags.isEmpty()) {
                        protectedCropTags = cropTags;
                        LOGGER.info("Loaded {} protected crop tags", cropTags.size());
                    }
                }
                
                // Load protected farmland tags
                if (json.has("protected_farmland_tags")) {
                    Set<TagKey<Block>> farmlandTags = new HashSet<>();
                    json.getAsJsonArray("protected_farmland_tags").forEach(element -> {
                        String tagId = element.getAsString();
                        ResourceLocation tagLocation = ResourceLocation.tryParse(tagId);
                        if (tagLocation != null) {
                            TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), tagLocation);
                            farmlandTags.add(tag);
                            LOGGER.info("Added farmland tag: {}", tagId);
                        } else {
                            LOGGER.warn("Invalid tag ID format: {}", tagId);
                        }
                    });
                    
                    if (!farmlandTags.isEmpty()) {
                        protectedFarmlandTags = farmlandTags;
                        LOGGER.info("Loaded {} protected farmland tags", farmlandTags.size());
                    }
                }
                
                loaded++;
            } catch (Exception e) {
                LOGGER.error("Error loading crop protection config from {}", entry.getKey(), e);
            }
        }
        
        if (loaded == 0) {
            LOGGER.info("No custom crop protection configs found, using defaults");
        }
        
        LOGGER.info("Crop protection configuration loaded: {} crops, {} farmland types", 
            protectedCrops.size(), protectedFarmland.size());
    }
    
    /**
     * Check if a block is a protected crop
     * Checks both specific blocks and block tags
     */
    public static boolean isProtectedCrop(Block block) {
        // Check specific blocks first
        if (protectedCrops.contains(block)) {
            return true;
        }
        
        // Check if block matches any protected crop tags
        for (TagKey<Block> tag : protectedCropTags) {
            if (block.builtInRegistryHolder().is(tag)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a block is protected farmland
     * Checks both specific blocks and block tags
     */
    public static boolean isProtectedFarmland(Block block) {
        // Check specific blocks first
        if (protectedFarmland.contains(block)) {
            return true;
        }
        
        // Check if block matches any protected farmland tags
        for (TagKey<Block> tag : protectedFarmlandTags) {
            if (block.builtInRegistryHolder().is(tag)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get all protected crops
     */
    public static Set<Block> getProtectedCrops() {
        return Collections.unmodifiableSet(protectedCrops);
    }
    
    /**
     * Get all protected farmland types
     */
    public static Set<Block> getProtectedFarmland() {
        return Collections.unmodifiableSet(protectedFarmland);
    }
}
