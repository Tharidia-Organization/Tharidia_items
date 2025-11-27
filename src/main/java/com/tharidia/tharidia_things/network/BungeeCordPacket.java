package com.tharidia.tharidia_things.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Dummy packet for bungeecord:main channel registration
 * This packet exists only to satisfy server requirements - no actual functionality
 */
public record BungeeCordPacket() implements CustomPacketPayload {
    
    public static final Type<BungeeCordPacket> TYPE = new Type<>(ResourceLocation.parse("bungeecord:main"));
    
    public static final StreamCodec<FriendlyByteBuf, BungeeCordPacket> STREAM_CODEC = StreamCodec.unit(new BungeeCordPacket());
    
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
