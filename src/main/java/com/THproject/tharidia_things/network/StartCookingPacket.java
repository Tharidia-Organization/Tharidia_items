package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.CookTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → Server: player selected a recipe from the recipe book.
 */
public record StartCookingPacket(BlockPos blockPos, String recipeId) implements CustomPacketPayload {

    public static final Type<StartCookingPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "start_cooking"));

    public static final StreamCodec<FriendlyByteBuf, StartCookingPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> { buf.writeBlockPos(p.blockPos()); buf.writeUtf(p.recipeId()); },
                    buf -> new StartCookingPacket(buf.readBlockPos(), buf.readUtf())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(StartCookingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) return;

            if (sp.level().getBlockEntity(packet.blockPos()) instanceof CookTableBlockEntity be) {
                boolean started = be.startCooking(packet.recipeId(), sp);
                if (!started) {
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cImpossibile avviare la ricetta. Verifica gli ingredienti o che non ci sia già una sessione attiva."));
                }
            }
        });
    }
}
