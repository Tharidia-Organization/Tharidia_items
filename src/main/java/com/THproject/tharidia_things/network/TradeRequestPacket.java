package com.THproject.tharidia_things.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

/**
 * Sent from initiating player to target player to request a trade
 */
public record TradeRequestPacket(UUID requesterId, String requesterName) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<TradeRequestPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "trade_request"));

    public static final StreamCodec<ByteBuf, TradeRequestPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
        TradeRequestPacket::requesterId,
        ByteBufCodecs.STRING_UTF8,
        TradeRequestPacket::requesterName,
        TradeRequestPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
