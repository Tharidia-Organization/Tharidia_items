package com.THproject.tharidia_things.diet;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.network.DietSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles diet gain from food consumption and periodic decay.
 */
@EventBusSubscriber(modid = TharidiaThings.MODID)
public class DietHandler {
    private static final float START_PERCENT = 0.8f;

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

        DietProfile profile = DietRegistry.getProfile(stack);
        if (profile.isEmpty()) {
            return;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        DietData data = serverPlayer.getData(DietAttachments.DIET_DATA);
        initializeIfNeeded(serverPlayer, data);
        data.add(profile, DietRegistry.getMaxValues());
        data.setLastDecayTimeMs(System.currentTimeMillis());
        syncIfNeeded(serverPlayer, data, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        DietData data = player.getData(DietAttachments.DIET_DATA);
        initializeIfNeeded(serverPlayer, data);
        DietSystemSettings settings = DietRegistry.getSettings();
        long decayIntervalMs = settings.decayIntervalMillis();
        long now = System.currentTimeMillis();
        long lastDecay = data.getLastDecayTimeMs();
        if (now - lastDecay < decayIntervalMs) {
            return;
        }

        float deltaSeconds = (now - lastDecay) / 1000.0f;
        boolean changed = data.applyDecay(DietRegistry.getDecayRates(), deltaSeconds);
        data.setLastDecayTimeMs(now);

        if (changed) {
            syncIfNeeded(serverPlayer, data, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        DietData data = serverPlayer.getData(DietAttachments.DIET_DATA);
        initializeIfNeeded(serverPlayer, data);
    }

    private static void syncIfNeeded(ServerPlayer player, DietData data, boolean force) {
        boolean dirty = force || data.consumeDirty();
        if (!dirty) {
            return;
        }

        float[] values = data.copyValues();
        PacketDistributor.sendToPlayer(player, new DietSyncPacket(values));
    }

    private static void initializeIfNeeded(ServerPlayer player, DietData data) {
        if (data.isInitialized()) {
            return;
        }
        data.ensureInitialized(DietRegistry.getMaxValues(), START_PERCENT);
        data.setLastDecayTimeMs(System.currentTimeMillis());
        syncIfNeeded(player, data, true);
    }
}
