package com.tharidia.tharidia_things.network;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.block.entity.IHotMetalAnvilEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from client to server to select a component type for any hot metal
 */
public record SelectComponentPacket(BlockPos pos, String componentId) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SelectComponentPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "select_component"));
    
    public static final StreamCodec<ByteBuf, SelectComponentPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        SelectComponentPacket::pos,
        ByteBufCodecs.STRING_UTF8,
        SelectComponentPacket::componentId,
        SelectComponentPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(SelectComponentPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.level().getBlockEntity(packet.pos) instanceof IHotMetalAnvilEntity entity) {
                    entity.setSelectedComponent(packet.componentId);
                }
            }
        });
    }
}
