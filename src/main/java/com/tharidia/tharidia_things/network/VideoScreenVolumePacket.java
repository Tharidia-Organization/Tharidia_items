package com.tharidia.tharidia_things.network;

import com.tharidia.tharidia_things.client.video.ClientVideoScreenManager;
import com.tharidia.tharidia_things.client.video.VLCVideoPlayer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record VideoScreenVolumePacket(UUID screenId, float volume) implements CustomPacketPayload {
    
    public static final Type<VideoScreenVolumePacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("tharidiathings", "video_screen_volume"));
    
    public static final StreamCodec<ByteBuf, VideoScreenVolumePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.fromCodec(net.minecraft.core.UUIDUtil.CODEC),
        VideoScreenVolumePacket::screenId,
        ByteBufCodecs.FLOAT,
        VideoScreenVolumePacket::volume,
        VideoScreenVolumePacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(VideoScreenVolumePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            VLCVideoPlayer player = ClientVideoScreenManager.getInstance().getPlayer(packet.screenId());
            if (player != null) {
                player.setVolume(packet.volume());
            }
        });
    }
}
