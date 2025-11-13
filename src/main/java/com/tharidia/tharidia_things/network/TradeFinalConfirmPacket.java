package com.tharidia.tharidia_things.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

import static com.tharidia.tharidia_things.TharidiaThings.MODID;

/**
 * Sent when a player makes the final confirmation after both players have confirmed their items
 */
public record TradeFinalConfirmPacket(UUID traderId, boolean confirmed) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<TradeFinalConfirmPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "trade_final_confirm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeFinalConfirmPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
        TradeFinalConfirmPacket::traderId,
        ByteBufCodecs.BOOL,
        TradeFinalConfirmPacket::confirmed,
        TradeFinalConfirmPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
