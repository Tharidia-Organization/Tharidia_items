package com.tharidia.tharidia_things.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.tharidia.tharidia_things.TharidiaThings.MODID;

/**
 * Packet sent from client to server when player submits their chosen name
 * Uses NameService from tharidia_tweaks (server-side only dependency)
 */
public record SubmitNamePacket(String chosenName) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SubmitNamePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "submit_name"));

    public static final StreamCodec<ByteBuf, SubmitNamePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        SubmitNamePacket::chosenName,
        SubmitNamePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    /**
     * Handles the packet on the server side
     */
    public static void handle(SubmitNamePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                try {
                    // Use reflection to call NameService (server-side only dependency)
                    Class<?> nameServiceClass = Class.forName("com.tharidia.tharidia_tweaks.name.NameService");
                    java.lang.reflect.Method submitMethod = nameServiceClass.getMethod("submitDisplayName", ServerPlayer.class, String.class);
                    
                    Object result = submitMethod.invoke(null, serverPlayer, packet.chosenName);
                    
                    // Get ValidationResult methods via reflection
                    Class<?> resultClass = result.getClass();
                    java.lang.reflect.Method okMethod = resultClass.getMethod("ok");
                    java.lang.reflect.Method sanitizedMethod = resultClass.getMethod("sanitized");
                    java.lang.reflect.Method errorMethod = resultClass.getMethod("error");
                    
                    boolean ok = (boolean) okMethod.invoke(result);
                    
                    if (ok) {
                        // Name saved successfully
                        String sanitized = (String) sanitizedMethod.invoke(result);
                        serverPlayer.sendSystemMessage(Component.literal("§aName set successfully: " + sanitized));
                        
                        // Close the GUI
                        serverPlayer.closeContainer();
                        
                        // Log success
                        com.tharidia.tharidia_things.TharidiaThings.LOGGER.info("Player {} chose name: {}", 
                            serverPlayer.getName().getString(), sanitized);
                    } else {
                        // Error - send message back to client
                        String error = (String) errorMethod.invoke(result);
                        serverPlayer.sendSystemMessage(Component.literal("§c" + error));
                        
                        // Reopen the name selection GUI
                        serverPlayer.closeContainer();
                        serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                            (id, inv, player) -> new com.tharidia.tharidia_things.gui.NameSelectionMenu(id, inv),
                            Component.translatable("gui.tharidiathings.name_selection")
                        ));
                    }
                } catch (Exception e) {
                    com.tharidia.tharidia_things.TharidiaThings.LOGGER.error("Error submitting name - tharidia_tweaks might not be loaded", e);
                    serverPlayer.sendSystemMessage(Component.literal("§cError: Name service unavailable. Contact server admin."));
                }
            }
        });
    }
}
