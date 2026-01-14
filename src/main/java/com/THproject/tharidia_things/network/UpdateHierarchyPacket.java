package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import com.THproject.tharidia_things.realm.HierarchyRank;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

/**
 * Packet sent from client to server to update a player's hierarchy rank in a realm
 */
public record UpdateHierarchyPacket(BlockPos realmPos, UUID targetPlayerUUID, int rankLevel) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<UpdateHierarchyPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "update_hierarchy"));

    public static final StreamCodec<ByteBuf, UpdateHierarchyPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        UpdateHierarchyPacket::realmPos,
        ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
        UpdateHierarchyPacket::targetPlayerUUID,
        ByteBufCodecs.INT,
        UpdateHierarchyPacket::rankLevel,
        UpdateHierarchyPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    /**
     * Handles the packet on the server side
     */
    public static void handle(UpdateHierarchyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.level() instanceof ServerLevel serverLevel) {
                BlockEntity blockEntity = serverLevel.getBlockEntity(packet.realmPos);
                
                if (blockEntity instanceof PietroBlockEntity pietroBlock) {
                    // Only allow the realm owner to change hierarchies
                    if (player.getUUID().equals(pietroBlock.getOwnerUUID())) {
                        HierarchyRank rank = HierarchyRank.fromLevel(packet.rankLevel);
                        pietroBlock.setPlayerHierarchy(packet.targetPlayerUUID, rank);
                        
                        // Send update to all players viewing the GUI
                        serverLevel.sendBlockUpdated(packet.realmPos, pietroBlock.getBlockState(), pietroBlock.getBlockState(), 3);
                    }
                }
            }
        });
    }
}
