package com.THproject.tharidia_things.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record VideoScreenSeekPacket(UUID screenId, int seconds, boolean forward) implements CustomPacketPayload {
    
    public static final Type<VideoScreenSeekPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("tharidia_things", "video_screen_seek")
    );
    
    public static final StreamCodec<ByteBuf, VideoScreenSeekPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
        VideoScreenSeekPacket::screenId,
        ByteBufCodecs.INT,
        VideoScreenSeekPacket::seconds,
        ByteBufCodecs.BOOL,
        VideoScreenSeekPacket::forward,
        VideoScreenSeekPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
