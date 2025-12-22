package com.THproject.tharidia_things.servertransfer;

import com.THproject.tharidia_things.Config;
import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.network.chat.Component;
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
                
                String currentServer = ServerTransferManager.getCurrentServerName();
                boolean tokenValid = TransferTokenManager.validateAndConsumeToken(player.getUUID(), currentServer);
                boolean isDevServer = "dev".equalsIgnoreCase(currentServer);
                boolean whitelistReady = DevWhitelistManager.isInitialized();
                
                if (isDevServer) {
                    if (tokenValid) {
                        boolean restored = ServerTransferManager.restorePlayerData(player);
                        if (!restored) {
                            TharidiaThings.LOGGER.warn("Token valido ma ripristino dati fallito per {}", player.getName().getString());
                        }
                    } else {
                        boolean directAllowed = Config.DEV_DIRECT_ACCESS_ENABLED.get() && whitelistReady &&
                                DevWhitelistManager.isWhitelisted(player.getUUID());
                        
                        if (directAllowed) {
                            TharidiaThings.LOGGER.info("Player {} connesso direttamente al dev tramite whitelist", player.getName().getString());
                            ServerTransferManager.restorePlayerPosition(player);
                        } else {
                            player.connection.disconnect(Component.literal(
                                "§c✗ Accesso Negato\n\n" +
                                "§7Il server dev è riservato a staff e tester autorizzati.\n" +
                                "§7Usa il comando §f/tharidia transfer dev §7dal main server."
                            ));
                        }
                    }
                } else {
                    if (tokenValid) {
                        boolean restored = ServerTransferManager.restorePlayerData(player);
                        if (!restored) {
                            TharidiaThings.LOGGER.warn("Token valido ma ripristino dati fallito per {}", player.getName().getString());
                        }
                    } else {
                        TharidiaThings.LOGGER.debug("Player {} login normale (no transfer token)", player.getName().getString());
                        ServerTransferManager.restorePlayerPosition(player);
                    }
                }
            });
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TharidiaThings.LOGGER.debug("Player {} disconnesso - salvataggio posizione server", player.getName().getString());
            ServerTransferManager.savePlayerPosition(player);
        }
    }
}
