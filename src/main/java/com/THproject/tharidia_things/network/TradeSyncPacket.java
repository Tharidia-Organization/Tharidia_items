package com.THproject.tharidia_things.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

/**
 * Sent from server to client to sync trade state
 * Updates the other player's confirmation status and items
 */
public record TradeSyncPacket(
    List<ItemStack> otherPlayerItems,
    boolean otherPlayerConfirmed,
    boolean otherPlayerFinalConfirmed,
    double taxRate,
    int taxAmount
) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<TradeSyncPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "trade_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_LIST_STREAM_CODEC,
        TradeSyncPacket::otherPlayerItems,
        ByteBufCodecs.BOOL,
        TradeSyncPacket::otherPlayerConfirmed,
        ByteBufCodecs.BOOL,
        TradeSyncPacket::otherPlayerFinalConfirmed,
        ByteBufCodecs.DOUBLE,
        TradeSyncPacket::taxRate,
        ByteBufCodecs.INT,
        TradeSyncPacket::taxAmount,
        TradeSyncPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
