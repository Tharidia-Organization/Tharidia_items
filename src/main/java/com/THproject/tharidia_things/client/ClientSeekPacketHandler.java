package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.client.video.ClientVideoScreenManager;
import com.THproject.tharidia_things.client.video.VLCVideoPlayer;
import com.THproject.tharidia_things.network.VideoScreenSeekPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientSeekPacketHandler {
    
    public static void handleSeekPacket(VideoScreenSeekPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientVideoScreenManager manager = ClientVideoScreenManager.getInstance();
            VLCVideoPlayer player = manager.getPlayer(packet.screenId());
            
            if (player != null) {
                if (packet.forward()) {
                    player.seekForward(packet.seconds());
                } else {
                    player.seekBackward(packet.seconds());
                }
            } else {
                TharidiaThings.LOGGER.warn("Cannot seek: No player found for screen {}", packet.screenId());
            }
        });
    }
}
