package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

/**
 * Packet sent from client to server to toggle rank particle visibility for the player
 */
public record ToggleParticlePacket(BlockPos realmPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ToggleParticlePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "toggle_particle"));

    public static final StreamCodec<ByteBuf, ToggleParticlePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ToggleParticlePacket::realmPos,
        ToggleParticlePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handles the packet on the server side
     */
    public static void handle(ToggleParticlePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.level() instanceof ServerLevel serverLevel) {
                BlockEntity blockEntity = serverLevel.getBlockEntity(packet.realmPos);

                if (blockEntity instanceof PietroBlockEntity pietroBlock) {
                    // Toggle the particle setting for this player
                    pietroBlock.toggleParticleDisabled(player.getUUID());
                }
            }
        });
    }
}
