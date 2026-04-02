package com.THproject.tharidia_things.poison;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.poison.PoisonHelper.PoisonType;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PoisonSyncPacket(int entityId, PoisonAttachments attachments) implements CustomPacketPayload {
    public static final Type<PoisonSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "sync_poison"));

    @Override
    public Type<PoisonSyncPacket> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, PoisonSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.entityId);
                buf.writeFloat(packet.attachments.getProgress());
                buf.writeUtf(packet.attachments.getPoisonType().toString());
            },
            buf -> {
                int id = buf.readVarInt();
                PoisonAttachments attachments = new PoisonAttachments();
                attachments.setProgress(buf.readFloat());
                attachments.setPoisoned(PoisonType.valueOf(buf.readUtf()));
                return new PoisonSyncPacket(id, attachments);
            });

    public static void handle(PoisonSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player localPlayer = context.player();
            if (localPlayer == null || localPlayer.isRemoved()) {
                return;
            }

            Entity entity = localPlayer.level().getEntity(packet.entityId);
            if (entity instanceof Player player) {
                PoisonAttachments attachment = PoisonHelper.getAttachment(player);
                attachment.copyFrom(packet.attachments);
            }
        });
    }

    public static void syncSelf(Player player) {
        PoisonAttachments attachments = PoisonHelper.getAttachment(player);
        if (attachments != null && player instanceof ServerPlayer serverPlayer)
            PacketDistributor.sendToPlayer(serverPlayer, new PoisonSyncPacket(player.getId(), attachments));
    }
}
