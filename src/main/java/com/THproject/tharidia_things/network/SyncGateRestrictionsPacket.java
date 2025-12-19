package com.THproject.tharidia_things.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.Set;

/**
 * Packet sent from server (tharidiatweaks) to client to sync which items/blocks are currently restricted.
 * This allows the client to block input BEFORE showing animations.
 */
public record SyncGateRestrictionsPacket(Set<Item> blockedItems) implements CustomPacketPayload {
    
    public static final Type<SyncGateRestrictionsPacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("tharidiatweaks", "sync_restrictions"));
    
    public static final StreamCodec<ByteBuf, SyncGateRestrictionsPacket> STREAM_CODEC = 
        StreamCodec.of(
            // Encoder: write the packet to the buffer
            (buf, packet) -> {
                buf.writeInt(packet.blockedItems.size());
                for (Item item : packet.blockedItems) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    String idStr = id.toString();
                    buf.writeInt(idStr.length());
                    buf.writeCharSequence(idStr, java.nio.charset.StandardCharsets.UTF_8);
                }
            },
            // Decoder: read the packet from the buffer
            buf -> {
                int count = buf.readInt();
                Set<Item> items = new HashSet<>();
                for (int i = 0; i < count; i++) {
                    int length = buf.readInt();
                    String idStr = buf.readCharSequence(length, java.nio.charset.StandardCharsets.UTF_8).toString();
                    ResourceLocation id = ResourceLocation.parse(idStr);
                    Item item = BuiltInRegistries.ITEM.get(id);
                    if (item != null) {
                        items.add(item);
                    }
                }
                return new SyncGateRestrictionsPacket(items);
            }
        );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
