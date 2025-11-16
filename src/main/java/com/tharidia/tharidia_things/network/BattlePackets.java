package com.tharidia.tharidia_things.network;

import com.tharidia.tharidia_things.TharidiaThings;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class BattlePackets {
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(TharidiaThings.MODID);

        // Register the packet and link it to its handler
        registrar.playToServer(
                BattleInviteResponsePacket.TYPE,
                BattleInviteResponsePacket.STREAM_CODEC,
                BattleInviteResponsePacket::handle);
    }
}
