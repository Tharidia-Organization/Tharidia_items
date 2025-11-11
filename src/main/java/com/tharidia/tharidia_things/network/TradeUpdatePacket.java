package com.tharidia.tharidia_things.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

import static com.tharidia.tharidia_things.TharidiaThings.MODID;

/**
 * Sent when a player updates their trade offer
 */
public record TradeUpdatePacket(UUID traderId, List<ItemStack> items, boolean confirmed) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<TradeUpdatePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "trade_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
        TradeUpdatePacket::traderId,
        ItemStack.OPTIONAL_LIST_STREAM_CODEC,
        TradeUpdatePacket::items,
        ByteBufCodecs.BOOL,
        TradeUpdatePacket::confirmed,
        TradeUpdatePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
