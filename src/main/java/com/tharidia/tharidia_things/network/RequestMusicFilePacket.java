package com.tharidia.tharidia_things.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Network packet sent from client to server to request a music file download
 */
public record RequestMusicFilePacket(String musicFile) implements CustomPacketPayload {
    
    public static final Type<RequestMusicFilePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "request_music_file")
    );
    
    public static final StreamCodec<FriendlyByteBuf, RequestMusicFilePacket> STREAM_CODEC = StreamCodec.of(
        RequestMusicFilePacket::write,
        RequestMusicFilePacket::read
    );
    
    public static void write(FriendlyByteBuf buf, RequestMusicFilePacket packet) {
        buf.writeUtf(packet.musicFile);
    }
    
    public static RequestMusicFilePacket read(FriendlyByteBuf buf) {
        String musicFile = buf.readUtf();
        return new RequestMusicFilePacket(musicFile);
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
