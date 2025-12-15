package com.tharidia.tharidia_things.network;

import com.tharidia.tharidia_things.TharidiaThings;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SecondLayerSyncPacket(CompoundTag tag) implements CustomPacketPayload {
    public static final Type<SecondLayerSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "second_layer_sync"));

    public static final StreamCodec<ByteBuf, SecondLayerSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG,
            SecondLayerSyncPacket::tag,
            SecondLayerSyncPacket::new);

    @Override
    public Type<SecondLayerSyncPacket> type() {
        return TYPE;
    }

    public static void handle(SecondLayerSyncPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof Player player) {
                player.getData(TharidiaThings.SECOND_LAYER_INVENTORY.get()).deserializeNBT(player.registryAccess(),
                        payload.tag());
            }
        });
    }
}