package com.tharidia.tharidia_things.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

import static com.tharidia.tharidia_things.TharidiaThings.MODID;

/**
 * Sent from target player back to requester with accept/decline
 */
public record TradeResponsePacket(UUID requesterId, boolean accepted) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<TradeResponsePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "trade_response"));

    public static final StreamCodec<ByteBuf, TradeResponsePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
        TradeResponsePacket::requesterId,
        ByteBufCodecs.BOOL,
        TradeResponsePacket::accepted,
        TradeResponsePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
