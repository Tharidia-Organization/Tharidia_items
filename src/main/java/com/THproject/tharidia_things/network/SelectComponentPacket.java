package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.IHotMetalAnvilEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;

/**
 * Packet sent from client to server to select a component type for any hot metal
 */
public record SelectComponentPacket(BlockPos pos, String componentId) implements CustomPacketPayload {

    private static final Set<String> VALID_COMPONENTS = Set.of("lama_lunga", "lama_corta", "elsa");

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
                // Validate componentId
                if (!VALID_COMPONENTS.contains(packet.componentId)) {
                    return;
                }

                // Distance check
                BlockPos p = packet.pos;
                if (serverPlayer.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) > 64.0) {
                    return;
                }

                if (serverPlayer.level().getBlockEntity(packet.pos) instanceof IHotMetalAnvilEntity entity) {
                    // Material-specific restriction: no elsa for gold
                    if (entity.getMaterialType().equals("gold") && packet.componentId.equals("elsa")) {
                        return;
                    }

                    // Owner check
                    if (entity.getOwnerUUID() != null && !entity.getOwnerUUID().equals(serverPlayer.getUUID())) {
                        return;
                    }

                    entity.setSelectedComponent(packet.componentId);
                }
            }
        });
    }
}
