package com.tharidia.tharidia_things.lobby;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * Handles transferring players between servers via Velocity proxy
 * Uses BungeeCord plugin messaging protocol
 * 
 * NOTE: The BungeeCordPayload is NOT registered in TharidiaThings.registerPayloads()
 * because another mod (tharidiatweaks) already registers the bungeecord:main channel.
 * The payload is sent directly and intercepted by the Velocity proxy at the network level.
 * This avoids version conflicts between mods that use the same channel.
 */
public class ServerTransferManager {
    
    private final Logger logger;
    private static final String MAIN_SERVER_NAME = "main";
    private static final ResourceLocation BUNGEECORD_CHANNEL = ResourceLocation.fromNamespaceAndPath("bungeecord", "main");
    
    /**
     * Payload wrapper for BungeeCord plugin messages
     * NOT registered with NeoForge - another mod handles registration
     * Sent directly to client and intercepted by Velocity proxy
     */
    public static class BungeeCordPayload implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BungeeCordPayload> TYPE = 
            new CustomPacketPayload.Type<>(BUNGEECORD_CHANNEL);
        
        // StreamCodec for serialization
        public static final StreamCodec<FriendlyByteBuf, BungeeCordPayload> STREAM_CODEC = 
            StreamCodec.of(
                (buf, payload) -> buf.writeBytes(payload.data),
                buf -> {
                    byte[] data = new byte[buf.readableBytes()];
                    buf.readBytes(data);
                    return new BungeeCordPayload(data);
                }
            );
        
        private final byte[] data;
        
        public BungeeCordPayload(byte[] data) {
            this.data = data;
        }
        
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        
        public byte[] getData() {
            return data;
        }
    }
    
    public ServerTransferManager(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Transfer a player to the main server
     */
    public void transferToMain(ServerPlayer player) {
        if (player == null || player.hasDisconnected()) {
            return;
        }
        
        player.sendSystemMessage(Component.literal("§a§l[TRANSFER] §7Connecting you to the main server..."));
        logger.info("Transferring {} to main server", player.getName().getString());
        
        // Send plugin message to proxy
        sendPluginMessage(player, MAIN_SERVER_NAME);
    }
    
    /**
     * Transfer a player to a specific server
     */
    public void transferToServer(ServerPlayer player, String serverName) {
        if (player == null || player.hasDisconnected()) {
            return;
        }
        
        player.sendSystemMessage(Component.literal("§a§l[TRANSFER] §7Connecting you to §6" + serverName + "§7..."));
        logger.info("Transferring {} to {}", player.getName().getString(), serverName);
        
        // Send plugin message to proxy
        sendPluginMessage(player, serverName);
    }
    
    /**
     * Kick a player with a message
     */
    public void kickPlayer(ServerPlayer player, String reason) {
        if (player == null || player.hasDisconnected()) {
            return;
        }
        
        player.connection.disconnect(Component.literal(reason));
        logger.info("Kicked player {}: {}", player.getName().getString(), reason);
    }
    
    /**
     * Sends a player to another server via Velocity
     * Creates and sends the payload directly without global registration
     */
    private void sendPluginMessage(ServerPlayer player, String serverName) {
        try {
            // Create BungeeCord "Connect" message
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            byte[] data = out.toByteArray();
            
            logger.info("Sending player {} to server {} via Velocity (payload: {} bytes)", 
                player.getName().getString(), serverName, data.length);
            
            // Create the BungeeCord payload (not globally registered)
            BungeeCordPayload payload = new BungeeCordPayload(data);
            
            // Send as a clientbound custom payload packet
            // The Velocity proxy will intercept the bungeecord:main channel
            net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket packet = 
                new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(payload);
            
            player.connection.send(packet);
            
            logger.info("Transfer packet sent successfully for {}", player.getName().getString());
            
        } catch (Exception e) {
            logger.error("Failed to send transfer packet for {}: {}", 
                player.getName().getString(), e.getMessage(), e);
            player.connection.disconnect(Component.literal("§cTransfer failed: " + e.getMessage()));
        }
    }
}
