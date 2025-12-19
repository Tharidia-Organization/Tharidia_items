package com.tharidia.tharidia_things.diet;

import com.tharidia.tharidia_things.network.DietSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles diet gain from food consumption and periodic decay.
 */
@EventBusSubscriber(modid = com.tharidia.tharidia_things.TharidiaThings.MODID)
public class DietHandler {
    private static final long DECAY_INTERVAL_MS = 1000L; // once per second
    private static final float SYNC_THRESHOLD = 1.0f; // percent change

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

        DietData data = player.getData(DietAttachments.DIET_DATA);
        data.add(profile, DietRegistry.getMaxValues());
        data.setLastDecayTimeMs(System.currentTimeMillis());
        syncIfNeeded((ServerPlayer) player, data, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        DietData data = player.getData(DietAttachments.DIET_DATA);
        long now = System.currentTimeMillis();
        long lastDecay = data.getLastDecayTimeMs();
        if (now - lastDecay < DECAY_INTERVAL_MS) {
            return;
        }

        float deltaSeconds = (now - lastDecay) / 1000.0f;
        boolean changed = data.applyDecay(DietRegistry.getDecayRates(), deltaSeconds);
        data.setLastDecayTimeMs(now);

        if (changed) {
            syncIfNeeded(serverPlayer, data, false);
        }
    }

    private static void syncIfNeeded(ServerPlayer player, DietData data, boolean force) {
        boolean dirty = force || data.consumeDirty();
        if (!dirty) {
            return;
        }

        float[] values = data.copyValues();
        PacketDistributor.sendToPlayer(player, new DietSyncPacket(values));
    }
}
