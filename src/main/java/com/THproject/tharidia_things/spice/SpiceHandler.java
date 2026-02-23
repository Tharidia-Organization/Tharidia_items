package com.THproject.tharidia_things.spice;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.network.SpiceSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Handles spice level changes: +10 per spice when eating spiced food,
 * initializes default values (50) on first login, and syncs to all clients.
 */
@EventBusSubscriber(modid = TharidiaThings.MODID)
public class SpiceHandler {

    @SubscribeEvent
    public static void onFoodConsumed(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }

        ItemStack stack = event.getItem();
        if (stack.isEmpty() || stack.getItem().getFoodProperties(stack, player) == null) {
            return;
        }

        SpiceData spiceData = stack.get(SpiceDataComponents.SPICE_DATA.get());
        if (spiceData == null || spiceData.isEmpty()) {
            return;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        PlayerSpiceData data = serverPlayer.getData(SpiceAttachments.PLAYER_SPICE_DATA);
        data.ensureInitialized();
        data.onFoodConsumed(spiceData);

        // Sync to all players so renderers update
        SpiceSyncPacket.syncToAll(serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerSpiceData data = serverPlayer.getData(SpiceAttachments.PLAYER_SPICE_DATA);
        data.ensureInitialized();

        // Send this player's spice data to all clients
        SpiceSyncPacket.syncToAll(serverPlayer);

        // Send all other online players' spice data to this player
        for (ServerPlayer other : serverPlayer.server.getPlayerList().getPlayers()) {
            if (other != serverPlayer) {
                SpiceSyncPacket.syncTo(other, serverPlayer);
            }
        }
    }
}
