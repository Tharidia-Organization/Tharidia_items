package com.tharidia.tharidia_things.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.network.VideoScreenSyncPacket;
import com.tharidia.tharidia_things.video.VideoScreen;
import com.tharidia.tharidia_things.video.VideoScreenRegistry;
import com.tharidia.tharidia_things.video.YouTubeUrlExtractor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * Commands for managing video screens
 * /videoscreen pos1 - Set first corner
 * /videoscreen pos2 - Set second corner and create screen
 * /videoscreen seturl <url> - Set YouTube URL for nearest screen
 * /videoscreen play - Start playback
 * /videoscreen stop - Stop playback
 * /videoscreen restart - Restart playback
 * /videoscreen delete - Delete nearest screen
 * /videoscreen list - List all screens
 */
public class VideoScreenCommands {
    
    // Temporary storage for corner positions per player
    private static final Map<UUID, BlockPos> playerCorner1 = new HashMap<>();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("videoscreen")
            .requires(source -> source.hasPermission(2)) // Require OP level 2
            
            // Set first corner
            .then(Commands.literal("pos1")
                .executes(VideoScreenCommands::setPos1))
            
            // Set second corner and create screen
            .then(Commands.literal("pos2")
                .executes(VideoScreenCommands::setPos2))
            
            // Set YouTube URL
            .then(Commands.literal("seturl")
                .then(Commands.argument("url", StringArgumentType.greedyString())
                    .executes(VideoScreenCommands::setUrl)))
            
            // Play video
            .then(Commands.literal("play")
                .executes(VideoScreenCommands::playVideo))
            
            // Pause video (keeps frame visible, can resume)
            .then(Commands.literal("pause")
                .executes(VideoScreenCommands::pauseVideo))
            
            // Stop video (alias for pause for backward compatibility)
            .then(Commands.literal("stop")
                .executes(VideoScreenCommands::pauseVideo))
            
            // Restart video (reload from beginning)
            .then(Commands.literal("restart")
                .executes(VideoScreenCommands::restartVideo))
            
            // Seek forward
            .then(Commands.literal("forward")
                .then(Commands.argument("seconds", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 300))
                    .executes(VideoScreenCommands::seekForward)))
            
            // Seek backward
            .then(Commands.literal("backward")
                .then(Commands.argument("seconds", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 300))
                    .executes(VideoScreenCommands::seekBackward)))
            
            // Set volume (0-100)
            .then(Commands.literal("volume")
                .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 100))
                    .executes(VideoScreenCommands::setVolume)))
            
            // Delete screen
            .then(Commands.literal("delete")
                .executes(VideoScreenCommands::deleteScreen))
            
            // List all screens
            .then(Commands.literal("list")
                .executes(VideoScreenCommands::listScreens))
        );
    }
    
    private static int setPos1(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        BlockPos targetPos = getTargetBlock(player);
        if (targetPos == null) {
            source.sendFailure(Component.literal("You must be looking at a block"));
            return 0;
        }
        
        playerCorner1.put(player.getUUID(), targetPos);
        source.sendSuccess(() -> Component.literal("First corner set at " + 
            targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()), false);
        
        return 1;
    }
    
    private static int setPos2(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        BlockPos corner1 = playerCorner1.get(player.getUUID());
        if (corner1 == null) {
            source.sendFailure(Component.literal("You must set the first corner with /videoscreen pos1 first"));
            return 0;
        }
        
        BlockPos corner2 = getTargetBlock(player);
        if (corner2 == null) {
            source.sendFailure(Component.literal("You must be looking at a block"));
            return 0;
        }
        
        // Validate that the screen is flat
        try {
            ServerLevel level = player.serverLevel();
            String dimension = level.dimension().location().toString();
            
            VideoScreenRegistry registry = VideoScreenRegistry.get(level);
            
            // Check for overlaps
            if (registry.overlapsExistingScreen(dimension, corner1, corner2)) {
                source.sendFailure(Component.literal("This area overlaps with an existing screen"));
                return 0;
            }
            
            // Create the screen with player's facing direction
            Direction playerFacing = player.getDirection();
            VideoScreen screen = new VideoScreen(corner1, corner2, playerFacing);
            registry.addScreen(dimension, screen);
            
            // Sync to all players
            try {
                PacketDistributor.sendToAllPlayers(new VideoScreenSyncPacket(
                    screen.getId(),
                    dimension,
                    corner1,
                    corner2,
                    "",
                    VideoScreen.VideoPlaybackState.STOPPED,
                    1.0f  // Default volume 100%
                ));
                source.sendSuccess(() -> Component.literal("Video screen created! " +
                    "Size: " + screen.getWidth() + "x" + screen.getHeight() + " blocks " +
                    "(Aspect ratio: " + String.format("%.2f", screen.getAspectRatio()) + ") " +
                    "Use /videoscreen seturl <youtube_url> to set a video"), false);
            } catch (Exception e) {
                source.sendFailure(Component.literal("Failed to sync video screen to clients: " + e.getMessage()));
                TharidiaThings.LOGGER.error("Failed to send video screen sync packet", e);
                // Remove the screen since sync failed
                registry.removeScreen(dimension, screen.getId());
                return 0;
            }
            
            playerCorner1.remove(player.getUUID());
            return 1;
            
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int setUrl(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String url = StringArgumentType.getString(context, "url");
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        // Validate URL - accept YouTube URLs, Twitch URLs, or direct video URLs (http/https)
        boolean isYouTube = YouTubeUrlExtractor.isValidYouTubeUrl(url);
        boolean isTwitch = url.contains("twitch.tv");
        boolean isDirectUrl = url.startsWith("http://") || url.startsWith("https://");
        
        if (!isYouTube && !isTwitch && !isDirectUrl) {
            source.sendFailure(Component.literal("Invalid URL. Please provide a YouTube link, Twitch link, or direct video URL (http/https)"));
            return 0;
        }
        
        ServerLevel level = player.serverLevel();
        String dimension = level.dimension().location().toString();
        VideoScreenRegistry registry = VideoScreenRegistry.get(level);
        
        VideoScreen screen = findNearestScreen(player, registry, dimension);
        if (screen == null) {
            source.sendFailure(Component.literal("No video screen found nearby. Stand closer to a screen"));
            return 0;
        }
        
        screen.setVideoUrl(url);
        screen.setPlaybackState(VideoScreen.VideoPlaybackState.PLAYING);
        registry.setDirty();
        
        // Sync to all players
        try {
            PacketDistributor.sendToAllPlayers(new VideoScreenSyncPacket(
                screen.getId(),
                dimension,
                screen.getCorner1(),
                screen.getCorner2(),
                url,
                VideoScreen.VideoPlaybackState.PLAYING,
                screen.getVolume()
            ));
            source.sendSuccess(() -> Component.literal("Video URL set and playback started!"), false);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to sync video to clients: " + e.getMessage()));
            TharidiaThings.LOGGER.error("Failed to send video sync packet", e);
            // Reset the screen state since sync failed
            screen.setVideoUrl("");
            screen.setPlaybackState(VideoScreen.VideoPlaybackState.STOPPED);
            registry.setDirty();
            return 0;
        }
        return 1;
    }
    
    private static int playVideo(CommandContext<CommandSourceStack> context) {
        return changePlaybackState(context, VideoScreen.VideoPlaybackState.PLAYING, "Video playback started");
    }
    
    private static int pauseVideo(CommandContext<CommandSourceStack> context) {
        return changePlaybackState(context, VideoScreen.VideoPlaybackState.PAUSED, "Video playback paused");
    }
    
    private static int restartVideo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ServerLevel level = player.serverLevel();
        String dimension = level.dimension().location().toString();
        VideoScreenRegistry registry = VideoScreenRegistry.get(level);
        
        VideoScreen screen = findNearestScreen(player, registry, dimension);
        if (screen == null) {
            source.sendFailure(Component.literal("No video screen found nearby"));
            return 0;
        }
        
        if (screen.getVideoUrl().isEmpty()) {
            source.sendFailure(Component.literal("No video URL set for this screen"));
            return 0;
        }
        
        // Stop then play to restart
        screen.setPlaybackState(VideoScreen.VideoPlaybackState.STOPPED);
        registry.setDirty();
        
        // Sync stop
        PacketDistributor.sendToAllPlayers(new VideoScreenSyncPacket(
            screen.getId(),
            dimension,
            screen.getCorner1(),
            screen.getCorner2(),
            screen.getVideoUrl(),
            VideoScreen.VideoPlaybackState.STOPPED,
            screen.getVolume()
        ));
        
        // Then immediately play
        screen.setPlaybackState(VideoScreen.VideoPlaybackState.PLAYING);
        PacketDistributor.sendToAllPlayers(new VideoScreenSyncPacket(
            screen.getId(),
            dimension,
            screen.getCorner1(),
            screen.getCorner2(),
            screen.getVideoUrl(),
            VideoScreen.VideoPlaybackState.PLAYING,
            screen.getVolume()
        ));
        
        source.sendSuccess(() -> Component.literal("Video restarted"), false);
        return 1;
    }
    
    private static int deleteScreen(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ServerLevel level = player.serverLevel();
        String dimension = level.dimension().location().toString();
        VideoScreenRegistry registry = VideoScreenRegistry.get(level);
        
        VideoScreen screen = findNearestScreen(player, registry, dimension);
        if (screen == null) {
            source.sendFailure(Component.literal("No video screen found nearby"));
            return 0;
        }
        
        UUID screenId = screen.getId();
        registry.removeScreen(dimension, screenId);
        
        // Sync deletion to all players
        PacketDistributor.sendToAllPlayers(new com.tharidia.tharidia_things.network.VideoScreenDeletePacket(
            screenId,
            dimension
        ));
        
        source.sendSuccess(() -> Component.literal("Video screen deleted"), false);
        return 1;
    }
    
    private static int listScreens(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ServerLevel level = player.serverLevel();
        String dimension = level.dimension().location().toString();
        VideoScreenRegistry registry = VideoScreenRegistry.get(level);
        
        Collection<VideoScreen> screens = registry.getScreensInDimension(dimension);
        
        if (screens.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No video screens in this dimension"), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("Video screens in this dimension:"), false);
        for (VideoScreen screen : screens) {
            Vec3 center = screen.getCenter();
            source.sendSuccess(() -> Component.literal(String.format(
                "- Screen at (%.1f, %.1f, %.1f) - Size: %dx%d - State: %s - URL: %s",
                center.x, center.y, center.z,
                screen.getWidth(), screen.getHeight(),
                screen.getPlaybackState(),
                screen.getVideoUrl().isEmpty() ? "None" : screen.getVideoUrl()
            )), false);
        }
        
        return 1;
    }
    
    private static int changePlaybackState(CommandContext<CommandSourceStack> context, 
                                          VideoScreen.VideoPlaybackState state, String message) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ServerLevel level = player.serverLevel();
        String dimension = level.dimension().location().toString();
        VideoScreenRegistry registry = VideoScreenRegistry.get(level);
        
        VideoScreen screen = findNearestScreen(player, registry, dimension);
        if (screen == null) {
            source.sendFailure(Component.literal("No video screen found nearby"));
            return 0;
        }
        
        if (screen.getVideoUrl().isEmpty()) {
            source.sendFailure(Component.literal("No video URL set for this screen"));
            return 0;
        }
        
        screen.setPlaybackState(state);
        registry.setDirty();
        
        // Sync to all players
        PacketDistributor.sendToAllPlayers(new VideoScreenSyncPacket(
            screen.getId(),
            dimension,
            screen.getCorner1(),
            screen.getCorner2(),
            screen.getVideoUrl(),
            state,
            screen.getVolume()
        ));
        
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
    
    private static int seekForward(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        int seconds = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "seconds");
        
        // Send packet to client to perform seek
        PacketDistributor.sendToPlayer(player, new com.tharidia.tharidia_things.network.VideoScreenSeekPacket(
            findNearestScreenId(player),
            seconds,
            true // forward
        ));
        
        source.sendSuccess(() -> Component.literal("Seeking forward " + seconds + " seconds"), false);
        return 1;
    }
    
    private static int seekBackward(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        int seconds = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "seconds");
        
        // Send packet to client to perform seek
        PacketDistributor.sendToPlayer(player, new com.tharidia.tharidia_things.network.VideoScreenSeekPacket(
            findNearestScreenId(player),
            seconds,
            false // backward
        ));
        
        source.sendSuccess(() -> Component.literal("Seeking backward " + seconds + " seconds"), false);
        return 1;
    }
    
    private static int setVolume(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        int volumeLevel = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "level");
        float volume = volumeLevel / 100.0f;
        
        ServerLevel level = player.serverLevel();
        String dimension = level.dimension().location().toString();
        VideoScreenRegistry registry = VideoScreenRegistry.get(level);
        VideoScreen screen = findNearestScreen(player, registry, dimension);
        
        if (screen == null) {
            source.sendFailure(Component.literal("No video screen found nearby"));
            return 0;
        }
        
        // Save volume in the screen
        screen.setVolume(volume);
        registry.setDirty();
        
        // Send packet to client to set volume
        PacketDistributor.sendToPlayer(player, new com.tharidia.tharidia_things.network.VideoScreenVolumePacket(
            screen.getId(),
            volume
        ));
        
        source.sendSuccess(() -> Component.literal("Volume set to " + volumeLevel + "%"), false);
        return 1;
    }
    
    private static UUID findNearestScreenId(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        String dimension = level.dimension().location().toString();
        VideoScreenRegistry registry = VideoScreenRegistry.get(level);
        VideoScreen screen = findNearestScreen(player, registry, dimension);
        return screen != null ? screen.getId() : null;
    }
    
    // Helper method to get the block the player is looking at
    private static BlockPos getTargetBlock(ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(10)); // 10 blocks reach
        
        var hitResult = player.level().clip(new net.minecraft.world.level.ClipContext(
            eyePos,
            endPos,
            net.minecraft.world.level.ClipContext.Block.OUTLINE,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            player
        ));
        
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return hitResult.getBlockPos();
        }
        
        return null;
    }
    
    // Helper method to find the nearest screen to a player
    private static VideoScreen findNearestScreen(ServerPlayer player, VideoScreenRegistry registry, String dimension) {
        Collection<VideoScreen> screens = registry.getScreensInDimension(dimension);
        if (screens.isEmpty()) {
            return null;
        }
        
        Vec3 playerPos = player.position();
        VideoScreen nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (VideoScreen screen : screens) {
            Vec3 center = screen.getCenter();
            double dist = playerPos.distanceToSqr(center);
            if (dist < nearestDist && dist < 100) { // Within 10 blocks
                nearestDist = dist;
                nearest = screen;
            }
        }
        
        return nearest;
    }
}
