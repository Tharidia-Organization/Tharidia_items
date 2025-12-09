package com.tharidia.tharidia_things.video;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Represents a video screen in the world
 * The screen is defined by two corner positions and must be flat (aligned to a single axis)
 */
public class VideoScreen {
    private final UUID id;
    private final BlockPos corner1;
    private final BlockPos corner2;
    private final Direction.Axis axis; // The axis the screen is perpendicular to
    private final Direction facing; // Which direction the screen faces
    private String videoUrl;
    private VideoPlaybackState playbackState;
    private float volume; // Volume level 0.0 to 1.0
    private final AABB bounds;
    
    // Calculated dimensions
    private final int width;
    private final int height;
    private final double aspectRatio;
    
    public VideoScreen(BlockPos corner1, BlockPos corner2) {
        this(null, corner1, corner2, null);
    }
    
    public VideoScreen(BlockPos corner1, BlockPos corner2, Direction playerFacing) {
        this(null, corner1, corner2, playerFacing);
    }

    public VideoScreen(UUID id, BlockPos corner1, BlockPos corner2) {
        this(id, corner1, corner2, null);
    }

    public VideoScreen(UUID id, BlockPos corner1, BlockPos corner2, Direction playerFacing) {
        this.id = id != null ? id : UUID.randomUUID();
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.videoUrl = "";
        this.playbackState = VideoPlaybackState.STOPPED;
        this.volume = 1.0f; // Default volume 100%
        
        // Calculate which axis the screen is on
        this.axis = calculateAxis(corner1, corner2);
        
        // Use player facing if provided and compatible with screen axis
        if (playerFacing != null && playerFacing.getAxis() == axis) {
            this.facing = playerFacing;
        } else {
            this.facing = calculateFacing(corner1, corner2, axis);
        }
        
        // Calculate dimensions
        int dx = Math.abs(corner2.getX() - corner1.getX());
        int dy = Math.abs(corner2.getY() - corner1.getY());
        int dz = Math.abs(corner2.getZ() - corner1.getZ());
        
        // Determine width and height based on axis
        switch (axis) {
            case X -> {
                this.width = dz;
                this.height = dy;
            }
            case Y -> {
                this.width = dx;
                this.height = dz;
            }
            case Z -> {
                this.width = dx;
                this.height = dy;
            }
            default -> {
                this.width = 1;
                this.height = 1;
            }
        }
        
        this.aspectRatio = (double) width / height;
        this.bounds = createBounds(corner1, corner2);
    }
    
    private static Direction.Axis calculateAxis(BlockPos pos1, BlockPos pos2) {
        int dx = Math.abs(pos2.getX() - pos1.getX());
        int dy = Math.abs(pos2.getY() - pos1.getY());
        int dz = Math.abs(pos2.getZ() - pos1.getZ());
        
        // The screen must be flat on one axis
        if (dx == 0) return Direction.Axis.X;
        if (dy == 0) return Direction.Axis.Y;
        if (dz == 0) return Direction.Axis.Z;
        
        throw new IllegalArgumentException("Screen must be flat (aligned to one axis)");
    }
    
    private static Direction calculateFacing(BlockPos pos1, BlockPos pos2, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos1.getX() < pos2.getX() ? Direction.EAST : Direction.WEST;
            case Y -> pos1.getY() < pos2.getY() ? Direction.UP : Direction.DOWN;
            case Z -> pos1.getZ() < pos2.getZ() ? Direction.SOUTH : Direction.NORTH;
        };
    }
    
    private static AABB createBounds(BlockPos pos1, BlockPos pos2) {
        return new AABB(
            Math.min(pos1.getX(), pos2.getX()),
            Math.min(pos1.getY(), pos2.getY()),
            Math.min(pos1.getZ(), pos2.getZ()),
            Math.max(pos1.getX(), pos2.getX()) + 1,
            Math.max(pos1.getY(), pos2.getY()) + 1,
            Math.max(pos1.getZ(), pos2.getZ()) + 1
        );
    }
    
    // Getters
    public UUID getId() { return id; }
    public BlockPos getCorner1() { return corner1; }
    public BlockPos getCorner2() { return corner2; }
    public Direction.Axis getAxis() { return axis; }
    public Direction getFacing() { return facing; }
    public String getVideoUrl() { return videoUrl; }
    public VideoPlaybackState getPlaybackState() { return playbackState; }
    public AABB getBounds() { return bounds; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public double getAspectRatio() { return aspectRatio; }
    public float getVolume() { return volume; }
    
    // Setters
    public void setVideoUrl(String url) { this.videoUrl = url; }
    public void setPlaybackState(VideoPlaybackState state) { this.playbackState = state; }
    public void setVolume(float volume) { this.volume = Math.max(0.0f, Math.min(1.0f, volume)); }
    
    // Get center position of the screen
    public Vec3 getCenter() {
        return new Vec3(
            (corner1.getX() + corner2.getX()) / 2.0,
            (corner1.getY() + corner2.getY()) / 2.0,
            (corner1.getZ() + corner2.getZ()) / 2.0
        );
    }
    
    // Check if a position is within the screen bounds
    public boolean contains(BlockPos pos) {
        return bounds.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
    
    // NBT serialization
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putInt("corner1X", corner1.getX());
        tag.putInt("corner1Y", corner1.getY());
        tag.putInt("corner1Z", corner1.getZ());
        tag.putInt("corner2X", corner2.getX());
        tag.putInt("corner2Y", corner2.getY());
        tag.putInt("corner2Z", corner2.getZ());
        tag.putString("videoUrl", videoUrl);
        tag.putString("playbackState", playbackState.name());
        tag.putFloat("volume", volume);
        return tag;
    }
    
    public static VideoScreen load(CompoundTag tag) {
        BlockPos corner1 = new BlockPos(
            tag.getInt("corner1X"),
            tag.getInt("corner1Y"),
            tag.getInt("corner1Z")
        );
        BlockPos corner2 = new BlockPos(
            tag.getInt("corner2X"),
            tag.getInt("corner2Y"),
            tag.getInt("corner2Z")
        );
        
        UUID id = tag.hasUUID("id") ? tag.getUUID("id") : null;
        VideoScreen screen = new VideoScreen(id, corner1, corner2);
        screen.setVideoUrl(tag.getString("videoUrl"));
        screen.setPlaybackState(VideoPlaybackState.valueOf(tag.getString("playbackState")));
        screen.setVolume(tag.contains("volume") ? tag.getFloat("volume") : 1.0f);
        
        return screen;
    }
    
    public enum VideoPlaybackState {
        STOPPED,
        PLAYING,
        PAUSED
    }
}
