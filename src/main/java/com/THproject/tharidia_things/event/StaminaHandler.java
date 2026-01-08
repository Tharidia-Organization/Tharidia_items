package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.config.StaminaConfig;
import com.THproject.tharidia_things.network.StaminaSyncPacket;
import com.THproject.tharidia_things.stamina.StaminaAttachments;
import com.THproject.tharidia_things.stamina.StaminaComputedStats;
import com.THproject.tharidia_things.stamina.StaminaData;
import com.THproject.tharidia_things.stamina.StaminaModifierEngine;
import com.THproject.tharidia_things.stamina.TagModifierBridge;
import com.THproject.tharidia_things.weight.WeightRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StaminaHandler {
    private static final Map<UUID, Float> LAST_SENT_CURRENT = new HashMap<>();
    private static final Map<UUID, Float> LAST_SENT_MAX = new HashMap<>();
    private static final Map<UUID, Boolean> LAST_SENT_IN_COMBAT = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID id = player.getUUID();
            LAST_SENT_CURRENT.remove(id);
            LAST_SENT_MAX.remove(id);
            LAST_SENT_IN_COMBAT.remove(id);
            TagModifierBridge.clear(id);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Player attacker = event.getEntity();
        StaminaData data = attacker.getData(StaminaAttachments.STAMINA_DATA);

        StaminaComputedStats stats = computeStats(data);
        float maxStamina = stats.maxStamina();
        data.setMaxStamina(maxStamina);
        if (!data.isInitialized()) {
            data.setCurrentStamina(maxStamina);
            data.setInitialized(true);
        }

        enterCombat(data);
        sync(attacker);

        Entity target = event.getTarget();
        if (target instanceof Player targetPlayer) {
            StaminaData targetData = targetPlayer.getData(StaminaAttachments.STAMINA_DATA);
            enterCombat(targetData);
            sync(targetPlayer);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Player hurtPlayer) {
            StaminaData data = hurtPlayer.getData(StaminaAttachments.STAMINA_DATA);
            enterCombat(data);
            sync(hurtPlayer);
        }
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        ItemStack bowStack = event.getBow();
        if (!(bowStack.getItem() instanceof BowItem)) {
            return;
        }

        StaminaData data = player.getData(StaminaAttachments.STAMINA_DATA);
        StaminaComputedStats stats = computeStats(data);
        data.setMaxStamina(stats.maxStamina());
        if (!data.isInitialized()) {
            data.setCurrentStamina(stats.maxStamina());
            data.setInitialized(true);
        }

        float power = BowItem.getPowerForTime(event.getCharge());
        power = Mth.clamp(power, 0.0f, 1.0f);
        if (power <= 0.0f) {
            return;
        }

        float bowWeight = (float) WeightRegistry.getItemWeight(bowStack.getItem());
        float cost = StaminaConfig.computeBowReleaseCost(power, bowWeight, stats);
        if (cost <= 0.0f) {
            return;
        }

        enterCombat(data);
        sync(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        StaminaData data = player.getData(StaminaAttachments.STAMINA_DATA);

        if (player.tickCount % 20 == 0 && player instanceof ServerPlayer serverPlayer) {
            TagModifierBridge.refresh(serverPlayer, data);
        }

        StaminaComputedStats stats = computeStats(data);
        data.setMaxStamina(stats.maxStamina());

        if (!data.isInitialized()) {
            data.setCurrentStamina(stats.maxStamina());
            data.setInitialized(true);
        }

        if (data.getCurrentStamina() > stats.maxStamina()) {
            data.setCurrentStamina(stats.maxStamina());
        }

        tickCombat(data);
        tickRegen(player, data, stats);

        if (player.tickCount % 20 == 0) {
            sync(player);
        } else {
            syncIfChanged(player);
        }
    }

    private static void tickCombat(StaminaData data) {
        int remaining = data.getCombatTicksRemaining();
        if (remaining > 0) {
            data.setCombatTicksRemaining(remaining - 1);
            data.setInCombat(true);
            return;
        }

        if (data.isInCombat()) {
            data.setInCombat(false);
        }
    }

    private static void tickRegen(Player player, StaminaData data, StaminaComputedStats stats) {
        int regenDelay = data.getRegenDelayTicksRemaining();
        if (regenDelay > 0) {
            data.setRegenDelayTicksRemaining(regenDelay - 1);
            return;
        }

        if (data.isInCombat()) {
            return;
        }

        if (player.getFoodData().getFoodLevel() <= 4) {
            return;
        }

        float maxStamina = stats.maxStamina();
        float current = data.getCurrentStamina();
        if (current >= maxStamina) {
            return;
        }

        float perTick = stats.regenRatePerSecond() / 20.0f;
        data.setCurrentStamina(Math.min(maxStamina, current + perTick));
    }

    private static void enterCombat(StaminaData data) {
        data.setCombatTicksRemaining(StaminaConfig.getCombatTimeoutTicks());
        data.setInCombat(true);
    }

    private static StaminaComputedStats computeStats(StaminaData data) {
        return StaminaModifierEngine.compute(
                StaminaConfig.getBaseMaxStamina(),
                StaminaConfig.getBaseRegenRatePerSecond(),
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                StaminaConfig.getBaseSprintThresholdFraction(),
                StaminaConfig.getRegenDelayAfterConsumptionSeconds(),
                false,
                data.getModifiers()
        );
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            StaminaData data = serverPlayer.getData(StaminaAttachments.STAMINA_DATA);
            PacketDistributor.sendToPlayer(serverPlayer, new StaminaSyncPacket(data.getCurrentStamina(), data.getMaxStamina(), data.isInCombat()));

            UUID id = serverPlayer.getUUID();
            LAST_SENT_CURRENT.put(id, data.getCurrentStamina());
            LAST_SENT_MAX.put(id, data.getMaxStamina());
            LAST_SENT_IN_COMBAT.put(id, data.isInCombat());
        }
    }

    private static void syncIfChanged(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        StaminaData data = serverPlayer.getData(StaminaAttachments.STAMINA_DATA);
        UUID id = serverPlayer.getUUID();

        float lastCurrent = LAST_SENT_CURRENT.getOrDefault(id, Float.NaN);
        float lastMax = LAST_SENT_MAX.getOrDefault(id, Float.NaN);
        boolean lastInCombat = LAST_SENT_IN_COMBAT.getOrDefault(id, false);

        boolean changed =
                Float.isNaN(lastCurrent)
                        || Math.abs(lastCurrent - data.getCurrentStamina()) > 0.01f
                        || Float.isNaN(lastMax)
                        || Math.abs(lastMax - data.getMaxStamina()) > 0.01f
                        || lastInCombat != data.isInCombat();

        if (changed) {
            sync(serverPlayer);
        }
    }
}
