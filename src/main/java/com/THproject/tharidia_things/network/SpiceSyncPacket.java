package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.spice.PlayerSpiceData;
import com.THproject.tharidia_things.spice.SpiceAttachments;
import com.THproject.tharidia_things.spice.SpiceType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Syncs a player's spice levels to all clients, identified by entity ID.
 * This allows other players' renderers to read the data.
 */
public record SpiceSyncPacket(int entityId, float[] values) implements CustomPacketPayload {
    public static final Type<SpiceSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "spice_sync"));

    private static final StreamCodec<RegistryFriendlyByteBuf, float[]> FLOAT_ARRAY_CODEC =
            new StreamCodec<>() {
                @Override
                public float[] decode(RegistryFriendlyByteBuf buf) {
                    int length = buf.readVarInt();
                    float[] arr = new float[length];
                    for (int i = 0; i < length; i++) {
                        arr[i] = buf.readFloat();
                    }
                    return arr;
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, float[] arr) {
                    buf.writeVarInt(arr.length);
                    for (float v : arr) {
                        buf.writeFloat(v);
                    }
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, SpiceSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT, SpiceSyncPacket::entityId,
                    FLOAT_ARRAY_CODEC, SpiceSyncPacket::values,
                    SpiceSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Client-side handler: applies the received spice values to the target entity's attachment.
     */
    public static void handle(SpiceSyncPacket packet, Player localPlayer) {
        net.minecraft.world.entity.Entity entity = localPlayer.level().getEntity(packet.entityId());
        if (entity instanceof Player targetPlayer) {
            PlayerSpiceData data = targetPlayer.getData(SpiceAttachments.PLAYER_SPICE_DATA);
            float[] received = packet.values();
            for (int i = 0; i < received.length && i < SpiceType.VALUES.length; i++) {
                data.set(SpiceType.VALUES[i], received[i]);
            }
        }
    }

    /**
     * Sends a player's spice data to ALL connected players.
     */
    public static void syncToAll(ServerPlayer player) {
        PlayerSpiceData data = player.getData(SpiceAttachments.PLAYER_SPICE_DATA);
        float[] snapshot = new float[SpiceType.VALUES.length];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = data.get(SpiceType.VALUES[i]);
        }
        PacketDistributor.sendToAllPlayers(new SpiceSyncPacket(player.getId(), snapshot));
    }

    /**
     * Sends a player's spice data to a specific player (e.g. on login, send all existing data).
     */
    public static void syncTo(ServerPlayer dataOwner, ServerPlayer receiver) {
        PlayerSpiceData data = dataOwner.getData(SpiceAttachments.PLAYER_SPICE_DATA);
        float[] snapshot = new float[SpiceType.VALUES.length];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = data.get(SpiceType.VALUES[i]);
        }
        PacketDistributor.sendToPlayer(receiver, new SpiceSyncPacket(dataOwner.getId(), snapshot));
    }
}
