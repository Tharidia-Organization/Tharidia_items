package com.tharidia.tharidia_things.network;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.fatigue.FatigueAttachments;
import com.tharidia.tharidia_things.fatigue.FatigueData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Packet to sync fatigue data from server to client
 */
public record FatigueSyncPacket(int fatigueTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FatigueSyncPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "fatigue_sync"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, FatigueSyncPacket> STREAM_CODEC = 
        StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.INT,
            FatigueSyncPacket::fatigueTicks,
            FatigueSyncPacket::new
        );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    /**
     * Handles the packet on the client side
     */
    public static void handle(FatigueSyncPacket packet, Player player) {
        FatigueData data = player.getData(FatigueAttachments.FATIGUE_DATA);
        data.setFatigueTicks(packet.fatigueTicks());
    }
}
