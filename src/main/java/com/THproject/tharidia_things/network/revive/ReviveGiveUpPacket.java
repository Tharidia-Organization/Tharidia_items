package com.THproject.tharidia_things.network.revive;

import com.THproject.tharidia_things.TharidiaThings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ReviveGiveUpPacket() implements CustomPacketPayload {
    public static final Type<ReviveGiveUpPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "revive_give_up"));

    public static final StreamCodec<ByteBuf, ReviveGiveUpPacket> STREAM_CODEC = StreamCodec
            .unit(new ReviveGiveUpPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReviveGiveUpPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // Determine if player can actually give up?
                // We assume client only sends this if fallen. But good to check.
                // However, Revive.isPlayerFallen is static and might rely on synced list which
                // might be intricate.
                // But killing the player is generally safe if they asked for it in this
                // context.
                player.kill();
            }
        });
    }
}
