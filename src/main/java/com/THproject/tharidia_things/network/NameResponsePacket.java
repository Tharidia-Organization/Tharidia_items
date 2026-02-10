package com.THproject.tharidia_things.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

/**
 * Packet sent from server to client after a name submission is processed.
 * Tells the client whether the name was accepted or rejected so the
 * PreLoginNameScreen can react accordingly.
 */
public record NameResponsePacket(boolean accepted, String message) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NameResponsePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "name_response"));

    public static final StreamCodec<ByteBuf, NameResponsePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        NameResponsePacket::accepted,
        ByteBufCodecs.STRING_UTF8,
        NameResponsePacket::message,
        NameResponsePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
