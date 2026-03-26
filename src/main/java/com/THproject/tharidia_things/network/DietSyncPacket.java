package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.diet.DietAttachments;
import com.THproject.tharidia_things.diet.DietData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Syncs diet values from server to client.
 */
public record DietSyncPacket(int entityId, float[] values) implements CustomPacketPayload {
    public static final Type<DietSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "diet_sync"));

    private static final StreamCodec<RegistryFriendlyByteBuf, float[]> FLOAT_ARRAY_CODEC =
            new StreamCodec<>() {
                @Override
                public float[] decode(RegistryFriendlyByteBuf buf) {
                    int length = buf.readVarInt();
                    float[] values = new float[length];
                    for (int i = 0; i < length; i++) {
                        values[i] = buf.readFloat();
                    }
                    return values;
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, float[] values) {
                    buf.writeVarInt(values.length);
                    for (float value : values) {
                        buf.writeFloat(value);
                    }
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, DietSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT, DietSyncPacket::entityId,
                    FLOAT_ARRAY_CODEC, DietSyncPacket::values,
                    DietSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DietSyncPacket packet, Player localPlayer) {
        net.minecraft.world.entity.Entity entity = localPlayer.level().getEntity(packet.entityId());
        if (entity instanceof Player targetPlayer) {
            DietData data = targetPlayer.getData(DietAttachments.DIET_DATA);
            data.setAll(packet.values());
        }
    }
}
