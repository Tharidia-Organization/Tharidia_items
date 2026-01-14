package com.THproject.tharidia_things.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Network packet for zone music playback commands from server
 * Sent from tharidiatweaks mod to control music playback on client
 */
public record ZoneMusicPacket(String musicFile, boolean loop, boolean stop) implements CustomPacketPayload {
    
    public static final Type<ZoneMusicPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tharidiatweaks", "zone_music"));
    
    public static final StreamCodec<FriendlyByteBuf, ZoneMusicPacket> STREAM_CODEC = StreamCodec.of(
        ZoneMusicPacket::write,
        ZoneMusicPacket::read
    );
    
    public static void write(FriendlyByteBuf buf, ZoneMusicPacket packet) {
        buf.writeUtf(packet.musicFile);
        buf.writeBoolean(packet.loop);
        buf.writeBoolean(packet.stop);
    }
    
    public static ZoneMusicPacket read(FriendlyByteBuf buf) {
        String musicFile = buf.readUtf();
        boolean loop = buf.readBoolean();
        boolean stop = buf.readBoolean();
        return new ZoneMusicPacket(musicFile, loop, stop);
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
