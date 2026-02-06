package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.CustomArmorAttachments;
import com.THproject.tharidia_things.gui.ArmorMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenArmorMenuPacket() implements CustomPacketPayload {
    public static final Type<OpenArmorMenuPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "open_armor_menu"));

    public static final StreamCodec<FriendlyByteBuf, OpenArmorMenuPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
            },
            buf -> new OpenArmorMenuPacket());

    @Override
    public Type<OpenArmorMenuPacket> type() {
        return TYPE;
    }

    public static void handle(OpenArmorMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // Open the server-side container
                // We use SimpleMenuProvider because we don't have a BlockEntity
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inventory, player) -> {
                            Container container = player.getData(CustomArmorAttachments.CUSTOM_ARMOR_DATA.get());
                            return new ArmorMenu(id, inventory, container);
                        },
                        Component.literal("Armor")));
            }
        });
    }
}
