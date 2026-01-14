package com.THproject.tharidia_things.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

public record ClaimOwnerSyncPacket(BlockPos pos, UUID ownerUUID) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<ClaimOwnerSyncPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "claim_owner_sync"));

    public static final StreamCodec<ByteBuf, ClaimOwnerSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ClaimOwnerSyncPacket::pos,
        ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
        ClaimOwnerSyncPacket::ownerUUID,
        ClaimOwnerSyncPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
