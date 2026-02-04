package com.THproject.tharidia_things.network.revive;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.ReviveAttachments;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public record ReviveSyncPayload(int entityId, CompoundTag data) implements CustomPacketPayload {
    public static final Type<ReviveSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "revive_sync"));

    public static final StreamCodec<ByteBuf, ReviveSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ReviveSyncPayload::entityId,
            ByteBufCodecs.COMPOUND_TAG, ReviveSyncPayload::data,
            ReviveSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Sync revive data of a player to all connected players
     * 
     * @param player
     */
    public static void sync(Player player) {
        if (player instanceof ServerPlayer) {
            ReviveAttachments data = player.getData(ReviveAttachments.REVIVE_DATA.get());
            CompoundTag nbt = data.serializeNBT(player.level().registryAccess());

            // Sincronizza i dati con TUTTI i giocatori connessi al server
            PacketDistributor.sendToAllPlayers(new ReviveSyncPayload(player.getId(), nbt));
        }
    }

    /**
     * Sync revive data of a player to a specific player
     * 
     * @param player         The player whose data is to be sent
     * @param receiverPlayer The player who will receive the data
     */
    public static void sync(Player dataPlayer, Player receiverPlayer) {
        if (receiverPlayer instanceof ServerPlayer serverReceiver) {
            ReviveAttachments data = dataPlayer.getData(ReviveAttachments.REVIVE_DATA.get());
            CompoundTag nbt = data.serializeNBT(dataPlayer.level().registryAccess());

            ReviveSyncPayload payload = new ReviveSyncPayload(dataPlayer.getId(), nbt);

            // Invio mirato al singolo giocatore
            PacketDistributor.sendToPlayer(serverReceiver, payload);
        }
    }

    /**
     * Sync revive data to self
     * 
     * @param player The player whose data is to be sent
     */
    public static void syncSelf(Player player) {
        if (player instanceof ServerPlayer serverReceiver) {
            ReviveAttachments data = player.getData(ReviveAttachments.REVIVE_DATA.get());
            CompoundTag nbt = data.serializeNBT(player.level().registryAccess());

            ReviveSyncPayload payload = new ReviveSyncPayload(player.getId(), nbt);

            // Invio mirato al singolo giocatore
            PacketDistributor.sendToPlayer(serverReceiver, payload);
        }
    }
}
