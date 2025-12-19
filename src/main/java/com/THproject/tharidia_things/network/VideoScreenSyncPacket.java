package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.video.VideoScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Packet to sync video screen data from server to client
 */
public record VideoScreenSyncPacket(
    UUID screenId,
    String dimension,
    BlockPos corner1,
    BlockPos corner2,
    Direction facing,
    String videoUrl,
    VideoScreen.VideoPlaybackState playbackState,
    float volume
) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<VideoScreenSyncPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "video_screen_sync"));
    
    public static final StreamCodec<ByteBuf, VideoScreenSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ByteBuf buf, VideoScreenSyncPacket packet) {
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.screenId.toString());
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.dimension);
            BlockPos.STREAM_CODEC.encode(buf, packet.corner1);
            BlockPos.STREAM_CODEC.encode(buf, packet.corner2);
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.videoUrl);
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.playbackState.name());
            ByteBufCodecs.FLOAT.encode(buf, packet.volume);
            if (packet.facing != null) {
                buf.writeBoolean(true);
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.facing.getName());
            } else {
                buf.writeBoolean(false);
            }
        }
        
        @Override
        public VideoScreenSyncPacket decode(ByteBuf buf) {
            String screenIdStr = ByteBufCodecs.STRING_UTF8.decode(buf);
            String dimension = ByteBufCodecs.STRING_UTF8.decode(buf);
            BlockPos corner1 = BlockPos.STREAM_CODEC.decode(buf);
            BlockPos corner2 = BlockPos.STREAM_CODEC.decode(buf);
            String videoUrl = ByteBufCodecs.STRING_UTF8.decode(buf);
            String playbackStateStr = ByteBufCodecs.STRING_UTF8.decode(buf);
            float volume = ByteBufCodecs.FLOAT.decode(buf);
            Direction facing = Direction.NORTH;
            if (buf.isReadable()) {
                boolean hasFacing = buf.readBoolean();
                if (hasFacing && buf.isReadable()) {
                    Direction decodedFacing = Direction.byName(ByteBufCodecs.STRING_UTF8.decode(buf));
                    if (decodedFacing != null) {
                        facing = decodedFacing;
                    }
                }
            }
            
            return new VideoScreenSyncPacket(
                UUID.fromString(screenIdStr),
                dimension,
                corner1,
                corner2,
                facing,
                videoUrl,
                VideoScreen.VideoPlaybackState.valueOf(playbackStateStr),
                volume
            );
        }
    };
    
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
