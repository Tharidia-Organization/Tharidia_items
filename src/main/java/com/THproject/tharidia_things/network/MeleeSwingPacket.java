package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.event.StaminaHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

public record MeleeSwingPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MeleeSwingPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "melee_swing"));

    public static final StreamCodec<ByteBuf, MeleeSwingPacket> STREAM_CODEC = StreamCodec.unit(new MeleeSwingPacket());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MeleeSwingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                StaminaHandler.onMeleeSwing(serverPlayer);
            }
        });
    }
}

