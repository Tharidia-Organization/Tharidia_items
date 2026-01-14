package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import com.THproject.tharidia_things.realm.HierarchyRank;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

/**
 * Packet sent from server to client to sync hierarchy data for a realm
 */
public record HierarchySyncPacket(Map<UUID, Integer> hierarchyData, UUID ownerUUID, String ownerName) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<HierarchySyncPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "hierarchy_sync"));

    public static final StreamCodec<ByteBuf, HierarchySyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public HierarchySyncPacket decode(ByteBuf buf) {
            int count = ByteBufCodecs.INT.decode(buf);
            Map<UUID, Integer> hierarchyData = new HashMap<>();
            
            for (int i = 0; i < count; i++) {
                String uuidStr = ByteBufCodecs.STRING_UTF8.decode(buf);
                UUID uuid = UUID.fromString(uuidStr);
                int rankLevel = ByteBufCodecs.INT.decode(buf);
                hierarchyData.put(uuid, rankLevel);
            }
            
            String ownerUUIDStr = ByteBufCodecs.STRING_UTF8.decode(buf);
            UUID ownerUUID = UUID.fromString(ownerUUIDStr);
            String ownerName = ByteBufCodecs.STRING_UTF8.decode(buf);
            
            return new HierarchySyncPacket(hierarchyData, ownerUUID, ownerName);
        }

        @Override
        public void encode(ByteBuf buf, HierarchySyncPacket packet) {
            ByteBufCodecs.INT.encode(buf, packet.hierarchyData.size());
            
            for (Map.Entry<UUID, Integer> entry : packet.hierarchyData.entrySet()) {
                ByteBufCodecs.STRING_UTF8.encode(buf, entry.getKey().toString());
                ByteBufCodecs.INT.encode(buf, entry.getValue());
            }
            
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.ownerUUID.toString());
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.ownerName);
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    /**
     * Creates a hierarchy sync packet from a PietroBlockEntity
     */
    public static HierarchySyncPacket fromPietroBlock(PietroBlockEntity pietroBlock) {
        Map<UUID, Integer> hierarchyData = new HashMap<>();
        
        for (Map.Entry<UUID, HierarchyRank> entry : pietroBlock.getAllPlayerHierarchies().entrySet()) {
            hierarchyData.put(entry.getKey(), entry.getValue().getLevel());
        }
        
        return new HierarchySyncPacket(hierarchyData, pietroBlock.getOwnerUUID(), pietroBlock.getOwnerName());
    }
}
