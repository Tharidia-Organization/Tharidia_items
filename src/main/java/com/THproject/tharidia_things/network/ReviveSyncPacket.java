package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReviveSyncPacket(boolean canRevive) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ReviveSyncPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "revive_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReviveSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ReviveSyncPacket::canRevive,
            ReviveSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
