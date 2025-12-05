package com.tharidia.tharidia_things.servertransfer;

import com.tharidia.tharidia_things.TharidiaThings;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class ServerTransferEvents {
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Attendi un tick prima di ripristinare per assicurarsi che il giocatore sia completamente caricato
            player.server.execute(() -> {
                TharidiaThings.LOGGER.info("Player {} connesso - tentativo ripristino dati transfer", player.getName().getString());
                
                // Valida il token di trasferimento
                String currentServer = ServerTransferManager.getCurrentServerName();
                boolean tokenValid = TransferTokenManager.validateAndConsumeToken(player.getUUID(), currentServer);
                
                if (tokenValid) {
                    // Token valido, procedi con il ripristino
                    boolean restored = ServerTransferManager.restorePlayerData(player);
                    if (!restored) {
                        TharidiaThings.LOGGER.warn("Token valido ma ripristino dati fallito per {}", player.getName().getString());
                    }
                } else {
                    // Nessun token valido - login normale (non da trasferimento)
                    TharidiaThings.LOGGER.debug("Player {} login normale (no transfer token)", player.getName().getString());
                }
            });
        }
    }
}
