package com.tharidia.tharidia_things.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import static com.tharidia.tharidia_things.TharidiaThings.MODID;

/**
 * Packet sent from client to request joining dungeon queue
 */
public record DungeonQueuePacket() implements CustomPacketPayload {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final CustomPacketPayload.Type<DungeonQueuePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "dungeon_queue"));

    public static final StreamCodec<ByteBuf, DungeonQueuePacket> STREAM_CODEC = StreamCodec.unit(new DungeonQueuePacket());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    /**
     * Handles the packet on the server side
     */
    public static void handle(DungeonQueuePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                try {
                    // Use reflection to access DungeonManager from Tharidia Features
                    Class<?> dungeonManagerClass = Class.forName("com.lucab.tharidia_features.dungeon.DungeonManager");
                    java.lang.reflect.Method getInstanceMethod = dungeonManagerClass.getMethod("getInstance");
                    Object dungeonManager = getInstanceMethod.invoke(null);
                    
                    if (dungeonManager != null) {
                        // Call joinQueue method
                        java.lang.reflect.Method joinQueueMethod = dungeonManagerClass.getMethod("joinQueue", ServerPlayer.class);
                        boolean success = (Boolean) joinQueueMethod.invoke(dungeonManager, serverPlayer);
                        
                        if (success) {
                            serverPlayer.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal("§aSei stato aggiunto alla coda del dungeon!")
                            );
                            
                            // Send queue position if applicable
                            java.lang.reflect.Method getQueuePositionMethod = dungeonManagerClass.getMethod("getQueuePosition", java.util.UUID.class);
                            int position = (Integer) getQueuePositionMethod.invoke(dungeonManager, serverPlayer.getUUID());
                            if (position > 0) {
                                serverPlayer.sendSystemMessage(
                                    net.minecraft.network.chat.Component.literal("§7Posizione in coda: §e" + position)
                                );
                            }
                        } else {
                            serverPlayer.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal("§cSei già in coda o nel dungeon!")
                            );
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to handle dungeon queue packet", e);
                    serverPlayer.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("§cErrore durante l'accesso al dungeon.")
                    );
                }
            }
        });
    }
}
