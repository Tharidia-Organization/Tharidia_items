package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.Config;
import com.THproject.tharidia_things.config.StaminaConfig;
import com.THproject.tharidia_things.network.StaminaSyncPacket;
import com.THproject.tharidia_things.stamina.StaminaAttachments;
import com.THproject.tharidia_things.stamina.CombatState;
import com.THproject.tharidia_things.stamina.StaminaComputedStats;
import com.THproject.tharidia_things.stamina.StaminaData;
import com.THproject.tharidia_things.stamina.StaminaModifierEngine;
import com.THproject.tharidia_things.stamina.TagModifierBridge;
import com.THproject.tharidia_things.weight.WeightRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class StaminaHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Float> LAST_SENT_CURRENT = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> LAST_SENT_MAX = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> LAST_SENT_IN_COMBAT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SENT_BOW_LOCK_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_SWING_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_NO_STAMINA_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_BOW_LOCK_MSG_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_MELEE_COST_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_STAMINA_DEBUG_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_COMBAT_EXIT_TICK = new ConcurrentHashMap<>();
    private static final int BOW_DRAW_LOCK_TICKS = 40;
    private static final int MELEE_COST_GRACE_TICKS = 3;
    private static boolean EPIC_FIGHT_LISTENERS_REGISTERED = false;

    private static boolean debugStamina() {
        return Config.DEBUG_STAMINA.get();
    }

    private static boolean debugStaminaEpicFight() {
        return Config.DEBUG_STAMINA_EPICFIGHT.get();
    }

    private static float staminaEpsilon(float cost) {
        if (cost <= 0.0f) {
            return 0.0f;
        }
        return Mth.clamp(cost * 0.02f, 0.001f, 0.1f);
    }

    private static boolean isInsufficient(float current, float cost) {
        return current + staminaEpsilon(cost) < cost;
    }

    public static void registerEpicFightCompat() {
        if (EPIC_FIGHT_LISTENERS_REGISTERED) {
            return;
        }
        EPIC_FIGHT_LISTENERS_REGISTERED = true;

        if (!ModList.get().isLoaded("epicfight")) {
            return;
        }

        registerEpicFightEventListener(
                "yesman.epicfight.api.neoevent.playerpatch.ComboAttackEvent",
                StaminaHandler::onEpicFightComboAttackEvent
        );

        registerEpicFightEventListener(
                "yesman.epicfight.api.neoevent.playerpatch.DealDamageEvent$Income",
                StaminaHandler::onEpicFightDealDamageIncomeEvent
        );

        registerEpicFightEventListener(
                "yesman.epicfight.api.neoevent.playerpatch.SkillConsumeEvent",
                StaminaHandler::onEpicFightSkillConsumeEvent
        );

        registerEpicFightEventListener(
                "yesman.epicfight.api.neoevent.playerpatch.SkillCastEvent",
                StaminaHandler::onEpicFightSkillCastEvent
        );
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void registerEpicFightEventListener(String className, Consumer<Object> handler) {
        try {
            Class<?> raw = Class.forName(className);
            if (!net.neoforged.bus.api.Event.class.isAssignableFrom(raw)) {
                return;
            }
            Class<? extends net.neoforged.bus.api.Event> eventClass = (Class<? extends net.neoforged.bus.api.Event>) raw;
            Consumer consumer = (Consumer) handler;
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, eventClass, consumer);
        } catch (Throwable t) {
            LOGGER.debug("Epic Fight compat listener not registered for {}", className, t);
        }
    }

    private static void onEpicFightComboAttackEvent(Object event) {
        ServerPlayer attacker = extractEpicFightServerPlayer(event);
        if (attacker == null || attacker.level().isClientSide() || attacker.isCreative()) {
            return;
        }

        UUID attackerId = attacker.getUUID();
        Integer lastTick = LAST_MELEE_COST_TICK.get(attackerId);
        boolean recentlyConsumed = lastTick != null && attacker.tickCount - lastTick <= 1;

        StaminaData data = attacker.getData(StaminaAttachments.STAMINA_DATA);
        StaminaComputedStats stats = computeStats(data);
        float maxStamina = stats.maxStamina();
        data.setMaxStamina(maxStamina);
        if (!data.isInitialized()) {
            data.setCurrentStamina(maxStamina);
            data.setInitialized(true);
        }

        float cost = computeAttackCost(attacker, stats);
        if (cost <= 0.0f) {
            return;
        }

        if (recentlyConsumed) {
            return;
        }

        float current = data.getCurrentStamina();
        if (isInsufficient(current, cost)) {
            cancelAny(event);
            if (debugStamina() || debugStaminaEpicFight()) {
                LOGGER.info(
                        "[STAMINA_EF] combo attack canceled (insufficient): attacker={} current={} cost={}",
                        attacker.getName().getString(),
                        String.format(Locale.ROOT, "%.3f", current),
                        String.format(Locale.ROOT, "%.3f", cost)
                );
            }
            notifyNoStamina(attacker);
            return;
        }

        float next = Math.max(0.0f, current - cost);
        if (next != current) {
            data.setCurrentStamina(next);
            data.setRegenDelayTicksRemaining(Math.round(stats.regenDelayAfterConsumptionSeconds() * 20.0f));
        }
        sync(attacker);
        LAST_MELEE_COST_TICK.put(attackerId, attacker.tickCount);
        if (debugStamina() || debugStaminaEpicFight()) {
            LOGGER.info(
                    "[STAMINA_EF] combo stamina consumed: attacker={} before={} after={} cost={}",
                    attacker.getName().getString(),
                    String.format(Locale.ROOT, "%.3f", current),
                    String.format(Locale.ROOT, "%.3f", next),
                    String.format(Locale.ROOT, "%.3f", cost)
            );
        }
    }

    private static void onEpicFightDealDamageIncomeEvent(Object event) {
        ServerPlayer attacker = extractEpicFightServerPlayer(event);
        if (attacker == null || attacker.level().isClientSide() || attacker.isCreative()) {
            return;
        }

        UUID attackerId = attacker.getUUID();
        Integer lastTick = LAST_MELEE_COST_TICK.get(attackerId);
        boolean recentlyConsumed = lastTick != null && attacker.tickCount - lastTick <= 1;

        StaminaData data = attacker.getData(StaminaAttachments.STAMINA_DATA);
        StaminaComputedStats stats = computeStats(data);
        float maxStamina = stats.maxStamina();
        data.setMaxStamina(maxStamina);
        if (!data.isInitialized()) {
            data.setCurrentStamina(maxStamina);
            data.setInitialized(true);
        }

        float cost = computeAttackCost(attacker, stats);
        if (cost <= 0.0f) {
            return;
        }

        if (recentlyConsumed) {
            return;
        }

        float current = data.getCurrentStamina();
        if (isInsufficient(current, cost)) {
            cancelAny(event);
            if (debugStamina() || debugStaminaEpicFight()) {
                LOGGER.info(
                        "[STAMINA_EF] deal damage canceled (insufficient): attacker={} current={} cost={}",
                        attacker.getName().getString(),
                        String.format(Locale.ROOT, "%.3f", current),
                        String.format(Locale.ROOT, "%.3f", cost)
                );
            }
            notifyNoStamina(attacker);
            return;
        }

        float next = Math.max(0.0f, current - cost);
        if (next != current) {
            data.setCurrentStamina(next);
            data.setRegenDelayTicksRemaining(Math.round(stats.regenDelayAfterConsumptionSeconds() * 20.0f));
        }
        sync(attacker);
        LAST_MELEE_COST_TICK.put(attackerId, attacker.tickCount);
        if (debugStamina() || debugStaminaEpicFight()) {
            LOGGER.info(
                    "[STAMINA_EF] damage stamina consumed: attacker={} before={} after={} cost={}",
                    attacker.getName().getString(),
                    String.format(Locale.ROOT, "%.3f", current),
                    String.format(Locale.ROOT, "%.3f", next),
                    String.format(Locale.ROOT, "%.3f", cost)
            );
        }
    }

    private static void onEpicFightSkillConsumeEvent(Object event) {
        ServerPlayer attacker = extractEpicFightServerPlayer(event);
        if (attacker == null || attacker.level().isClientSide() || attacker.isCreative()) {
            return;
        }

        Object resourceType = invokeNoArg(event, "getResourceType");
        if (resourceType == null || !"STAMINA".equals(String.valueOf(resourceType))) {
            return;
        }

        UUID attackerId = attacker.getUUID();
        Integer lastTick = LAST_MELEE_COST_TICK.get(attackerId);
        boolean recentlyConsumed = lastTick != null && attacker.tickCount - lastTick <= 1;

        StaminaData data = attacker.getData(StaminaAttachments.STAMINA_DATA);
        StaminaComputedStats stats = computeStats(data);
        float maxStamina = stats.maxStamina();
        data.setMaxStamina(maxStamina);
        if (!data.isInitialized()) {
            data.setCurrentStamina(maxStamina);
            data.setInitialized(true);
        }

        float epicFightRequested = 0.0f;
        Object rawAmount = invokeNoArg(event, "getAmount");
        if (rawAmount instanceof Number number) {
            epicFightRequested = number.floatValue();
        }

        float cost = epicFightRequested > 0.0f ? (epicFightRequested * stats.consumptionMultiplier()) : computeAttackCost(attacker, stats);
        if (cost <= 0.0f) {
            return;
        }

        if (recentlyConsumed) {
            return;
        }

        float current = data.getCurrentStamina();
        if (isInsufficient(current, cost)) {
            cancelAny(event);
            if (debugStamina() || debugStaminaEpicFight()) {
                LOGGER.info(
                        "[STAMINA_EF] skill denied (insufficient): attacker={} current={} cost={} req={} resource={}",
                        attacker.getName().getString(),
                        String.format(Locale.ROOT, "%.3f", current),
                        String.format(Locale.ROOT, "%.3f", cost),
                        String.format(Locale.ROOT, "%.3f", epicFightRequested),
                        String.valueOf(resourceType)
                );
            }
            notifyNoStamina(attacker);
            return;
        }

        float next = Math.max(0.0f, current - cost);
        if (next != current) {
            data.setCurrentStamina(next);
            data.setRegenDelayTicksRemaining(Math.round(stats.regenDelayAfterConsumptionSeconds() * 20.0f));
        }
        sync(attacker);
        LAST_MELEE_COST_TICK.put(attackerId, attacker.tickCount);

        if (debugStamina() || debugStaminaEpicFight()) {
            LOGGER.info(
                    "[STAMINA_EF] skill stamina consumed: attacker={} before={} after={} cost={} req={} resource={}",
                    attacker.getName().getString(),
                    String.format(Locale.ROOT, "%.3f", current),
                    String.format(Locale.ROOT, "%.3f", next),
                    String.format(Locale.ROOT, "%.3f", cost),
                    String.format(Locale.ROOT, "%.3f", epicFightRequested),
                    String.valueOf(resourceType)
            );
        }
    }

    private static void onEpicFightSkillCastEvent(Object event) {
        ServerPlayer attacker = extractEpicFightServerPlayer(event);
        if (attacker == null || attacker.level().isClientSide() || attacker.isCreative()) {
            return;
        }

        UUID attackerId = attacker.getUUID();
        Integer lastTick = LAST_MELEE_COST_TICK.get(attackerId);
        boolean recentlyConsumed = lastTick != null && attacker.tickCount - lastTick <= 1;

        StaminaData data = attacker.getData(StaminaAttachments.STAMINA_DATA);
        StaminaComputedStats stats = computeStats(data);
        float maxStamina = stats.maxStamina();
        data.setMaxStamina(maxStamina);
        if (!data.isInitialized()) {
            data.setCurrentStamina(maxStamina);
            data.setInitialized(true);
        }

        float cost = computeAttackCost(attacker, stats);
        if (cost <= 0.0f) {
            return;
        }

        if (recentlyConsumed) {
            return;
        }

        float current = data.getCurrentStamina();
        if (isInsufficient(current, cost)) {
            cancelAny(event);
            if (debugStamina() || debugStaminaEpicFight()) {
                LOGGER.info(
                        "[STAMINA_EF] skill cast canceled (insufficient): attacker={} current={} cost={}",
                        attacker.getName().getString(),
                        String.format(Locale.ROOT, "%.3f", current),
                        String.format(Locale.ROOT, "%.3f", cost)
                );
            }
            notifyNoStamina(attacker);
            return;
        }

        float next = Math.max(0.0f, current - cost);
        if (next != current) {
            data.setCurrentStamina(next);
            data.setRegenDelayTicksRemaining(Math.round(stats.regenDelayAfterConsumptionSeconds() * 20.0f));
        }
        sync(attacker);
        LAST_MELEE_COST_TICK.put(attackerId, attacker.tickCount);

        if (debugStamina() || debugStaminaEpicFight()) {
            LOGGER.info(
                    "[STAMINA_EF] skill cast stamina consumed: attacker={} before={} after={} cost={}",
                    attacker.getName().getString(),
                    String.format(Locale.ROOT, "%.3f", current),
                    String.format(Locale.ROOT, "%.3f", next),
                    String.format(Locale.ROOT, "%.3f", cost)
            );
        }
    }

    private static ServerPlayer extractEpicFightServerPlayer(Object event) {
        if (event == null) {
            return null;
        }

        try {
            Object playerPatch = event.getClass().getMethod("getPlayerPatch").invoke(event);
            if (playerPatch == null) {
                return null;
            }
            Object original = playerPatch.getClass().getMethod("getOriginal").invoke(playerPatch);
            if (original instanceof ServerPlayer serverPlayer) {
                return serverPlayer;
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static void cancelAny(Object event) {
        if (event == null) {
            return;
        }
        if (event instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
            return;
        }
        try {
            event.getClass().getMethod("setCanceled", boolean.class).invoke(event, true);
            return;
        } catch (Throwable ignored) {
        }
        try {
            event.getClass().getMethod("setCancelled", boolean.class).invoke(event, true);
            return;
        } catch (Throwable ignored) {
        }
        try {
            event.getClass().getMethod("cancel").invoke(event);
        } catch (Throwable ignored) {
        }
    }

    private static ServerPlayer resolveAttacker(Entity sourceEntity, Entity directEntity) {
        if (sourceEntity instanceof ServerPlayer srcPlayer) {
            return srcPlayer;
        }
        if (directEntity instanceof ServerPlayer directPlayer) {
            return directPlayer;
        }
        ServerPlayer fromDirectOwner = resolveOwnerPlayer(directEntity);
        if (fromDirectOwner != null) {
            return fromDirectOwner;
        }
        return resolveOwnerPlayer(sourceEntity);
    }

    private static ServerPlayer resolveOwnerPlayer(Entity entity) {
        if (entity == null) {
            return null;
        }
        Object owner = invokeNoArg(entity, "getOwner");
        if (owner instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        owner = invokeNoArg(entity, "getAttacker");
        if (owner instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        owner = invokeNoArg(entity, "getCaster");
        if (owner instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        owner = invokeNoArg(entity, "getOriginal");
        if (owner instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        owner = invokeNoArg(entity, "getEntity");
        if (owner instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        owner = invokeNoArg(entity, "getLivingEntity");
        if (owner instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void notifyNoStamina(ServerPlayer player) {
        showNoStamina(player);
        sync(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID id = player.getUUID();
            LAST_SENT_CURRENT.remove(id);
            LAST_SENT_MAX.remove(id);
            LAST_SENT_IN_COMBAT.remove(id);
            LAST_SENT_BOW_LOCK_UNTIL.remove(id);
            LAST_SWING_TICK.remove(id);
            LAST_NO_STAMINA_TICK.remove(id);
            LAST_BOW_LOCK_MSG_TICK.remove(id);
            LAST_MELEE_COST_TICK.remove(id);
            LAST_STAMINA_DEBUG_TICK.remove(id);
            CombatState.clear(id);
            LAST_COMBAT_EXIT_TICK.remove(id);
            TagModifierBridge.clear(id);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.isCanceled()) {
            return;
        }

        Player attacker = event.getEntity();
        if (attacker.isCreative()) {
            return;
        }

        UUID attackerId = attacker.getUUID();
        Integer lastTick = LAST_MELEE_COST_TICK.get(attackerId);
        if (lastTick != null && attacker.tickCount - lastTick <= 1) {
            return;
        }

        StaminaData data = attacker.getData(StaminaAttachments.STAMINA_DATA);
        StaminaComputedStats stats = computeStats(data);
        float maxStamina = stats.maxStamina();
        data.setMaxStamina(maxStamina);
        if (!data.isInitialized()) {
            data.setCurrentStamina(maxStamina);
            data.setInitialized(true);
        }

        float cost = computeAttackCost(attacker, stats);
        if (cost > 0.0f) {
            float current = data.getCurrentStamina();
            if (isInsufficient(current, cost)) {
                event.setCanceled(true);
                if (attacker instanceof ServerPlayer serverPlayer) {
                    if (debugStamina()) {
                        LOGGER.info(
                                "[STAMINA] melee canceled (insufficient): player={} current={} cost={} item={}",
                                attacker.getName().getString(),
                                String.format(Locale.ROOT, "%.3f", current),
                                String.format(Locale.ROOT, "%.3f", cost),
                                attacker.getMainHandItem().getItem()
                        );
                    }
                    showNoStamina(serverPlayer);
                    sync(serverPlayer);
                }
                return;
            }

            float next = Math.max(0.0f, current - cost);
            if (next != current) {
                data.setCurrentStamina(next);
                data.setRegenDelayTicksRemaining(Math.round(stats.regenDelayAfterConsumptionSeconds() * 20.0f));
            }
            sync(attacker);
            LAST_MELEE_COST_TICK.put(attackerId, attacker.tickCount);
            if (debugStamina()) {
                logStaminaDebugOncePerTick(attacker, "melee consumed", current, next, cost);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        if (player.isCreative()) {
            return;
        }

        float cost = getMeleeAttackCost(player);
        if (cost <= 0.0f) {
            return;
        }

        StaminaData data = player.getData(StaminaAttachments.STAMINA_DATA);
        if (isInsufficient(data.getCurrentStamina(), cost)) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer) {
                LOGGER.debug("Block interaction canceled for {}: insufficient stamina (current: {}, cost: {})", player.getName().getString(), data.getCurrentStamina(), cost);
                showNoStamina(serverPlayer);
                sync(serverPlayer);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        if (player.isCreative()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BowItem)) {
            return;
        }

        StaminaData data = player.getData(StaminaAttachments.STAMINA_DATA);
        StaminaComputedStats stats = computeStats(data);
        float maxStamina = stats.maxStamina();
        data.setMaxStamina(maxStamina);
        if (!data.isInitialized()) {
            data.setCurrentStamina(maxStamina);
            data.setInitialized(true);
        }

        int lockRemaining = getBowDrawLockTicksRemaining(player, data);
        if (lockRemaining > 0) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer) {
                if (debugStamina()) {
                    LOGGER.info(
                            "[STAMINA] bow canceled (lock): player={} remainingTicks={}",
                            player.getName().getString(),
                            lockRemaining
                    );
                }
                showBowRetry(serverPlayer, lockRemaining);
                sync(serverPlayer);
            }
            return;
        }

        float min = computeBowMinDrawCost(stack, stats);
        if (min <= 0.0f) {
            return;
        }

        float current = data.getCurrentStamina();
        if (current + 0.1f < min) {
            event.setCanceled(true);
            startBowDrawLockIfNotActive(player, data);
            if (player instanceof ServerPlayer serverPlayer) {
                if (debugStamina()) {
                    LOGGER.info(
                            "[STAMINA] bow canceled (insufficient): player={} current={} minCost={} lockTicks={}",
                            player.getName().getString(),
                            String.format(Locale.ROOT, "%.3f", current),
                            String.format(Locale.ROOT, "%.3f", min),
                            getBowDrawLockTicksRemaining(player, data)
                    );
                }
                showBowRetry(serverPlayer, getBowDrawLockTicksRemaining(player, data));
                sync(serverPlayer);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickItemFinal(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!event.isCanceled()) {
            return;
        }

        Player player = event.getEntity();
        if (player.isCreative()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BowItem)) {
            return;
        }

        StaminaData data = player.getData(StaminaAttachments.STAMINA_DATA);
        startBowDrawLockIfNotActive(player, data);
        if (player instanceof ServerPlayer serverPlayer) {
            if (debugStamina()) {
                LOGGER.info(
                        "[STAMINA] bow canceled (other-cancel): player={} lockTicks={}",
                        player.getName().getString(),
                        getBowDrawLockTicksRemaining(player, data)
                );
            }
            showBowRetry(serverPlayer, getBowDrawLockTicksRemaining(player, data));
            sync(serverPlayer);
        }
    }

    public static void onMeleeSwing(ServerPlayer player) {
        UUID id = player.getUUID();
        int tick = player.tickCount;
        Integer lastTick = LAST_SWING_TICK.get(id);
        if (lastTick != null && lastTick == tick) {
            return;
        }
        LAST_SWING_TICK.put(id, tick);

        StaminaData data = player.getData(StaminaAttachments.STAMINA_DATA);
        StaminaComputedStats stats = computeStats(data);
        float maxStamina = stats.maxStamina();
        data.setMaxStamina(maxStamina);
        if (!data.isInitialized()) {
            data.setCurrentStamina(maxStamina);
            data.setInitialized(true);
        }

        float cost = computeAttackCost(player, stats);
        if (cost > 0.0f && isInsufficient(data.getCurrentStamina(), cost)) {
            showNoStamina(player);
            sync(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getAmount() <= 0.0f) {
            return;
        }

        Entity sourceEntity = event.getSource().getEntity();
        Entity directEntity = event.getSource().getDirectEntity();
        ServerPlayer attacker = resolveAttacker(sourceEntity, directEntity);

        if (debugStaminaEpicFight()) {
            LOGGER.info(
                    "[STAMINA_EF] damage hook: attacker={} sourceEntity={} directEntity={} msgId={} amount={}",
                    attacker != null ? attacker.getName().getString() : "null",
                    sourceEntity != null ? sourceEntity.getType() : "null",
                    directEntity != null ? directEntity.getType() : "null",
                    event.getSource().getMsgId(),
                    String.format(Locale.ROOT, "%.2f", event.getAmount())
            );
        }

        if (event.getEntity() instanceof Player hurtPlayer) {
            StaminaData data = hurtPlayer.getData(StaminaAttachments.STAMINA_DATA);
            enterCombat(hurtPlayer, data, "neoforge:incoming_damage");
            sync(hurtPlayer);
            if (debugStaminaEpicFight() && hurtPlayer instanceof ServerPlayer serverPlayer) {
                Entity src = event.getSource().getEntity();
                Entity direct = event.getSource().getDirectEntity();
                LOGGER.info(
                        "[STAMINA_EF] incoming damage: victim={} blocking={} usingItem={} useItem={} src={} direct={}",
                        serverPlayer.getName().getString(),
                        serverPlayer.isBlocking(),
                        serverPlayer.isUsingItem(),
                        serverPlayer.getUseItem().getItem(),
                        src != null ? src.getType() : "null",
                        direct != null ? direct.getType() : "null"
                );
            }
        }
        
        if (attacker != null && attacker != event.getEntity()) {
            StaminaData attackerData = attacker.getData(StaminaAttachments.STAMINA_DATA);
            enterCombat(attacker, attackerData, "neoforge:dealt_damage");
            sync(attacker);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        Entity sourceEntity = event.getSource().getEntity();
        Entity directEntity = event.getSource().getDirectEntity();
        ServerPlayer attacker = resolveAttacker(sourceEntity, directEntity);

        if (attacker == null || attacker.isCreative()) {
            return;
        }

        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof Projectile) {
            return;
        }

        UUID attackerId = attacker.getUUID();
        Integer lastTick = LAST_MELEE_COST_TICK.get(attackerId);
        if (lastTick != null && attacker.tickCount - lastTick <= MELEE_COST_GRACE_TICKS) {
            return;
        }

        StaminaData data = attacker.getData(StaminaAttachments.STAMINA_DATA);
        StaminaComputedStats stats = computeStats(data);
        float maxStamina = stats.maxStamina();
        data.setMaxStamina(maxStamina);
        if (!data.isInitialized()) {
            data.setCurrentStamina(maxStamina);
            data.setInitialized(true);
        }

        float cost = computeAttackCost(attacker, stats);
        if (cost <= 0.0f) {
            return;
        }

        float current = data.getCurrentStamina();
        if (isInsufficient(current, cost)) {
            event.setNewDamage(0.0f);
            if (debugStamina() || debugStaminaEpicFight()) {
                LOGGER.info(
                        "[STAMINA] damage nulled (insufficient): attacker={} current={} cost={} source={}",
                        attacker.getName().getString(),
                        String.format(Locale.ROOT, "%.3f", current),
                        String.format(Locale.ROOT, "%.3f", cost),
                        event.getSource().getMsgId()
                );
            }
            showNoStamina(attacker);
            sync(attacker);
            return;
        }

        float next = Math.max(0.0f, current - cost);
        if (next != current) {
            data.setCurrentStamina(next);
            data.setRegenDelayTicksRemaining(Math.round(stats.regenDelayAfterConsumptionSeconds() * 20.0f));
        }
        sync(attacker);
        LAST_MELEE_COST_TICK.put(attackerId, attacker.tickCount);

        if (debugStamina() || debugStaminaEpicFight()) {
            LOGGER.info(
                    "[STAMINA_EF] consumed stamina (damage stage): attacker={} before={} after={} cost={} source={}",
                    attacker.getName().getString(),
                    String.format(Locale.ROOT, "%.3f", current),
                    String.format(Locale.ROOT, "%.3f", next),
                    String.format(Locale.ROOT, "%.3f", cost),
                    event.getSource().getMsgId()
            );
        }
    }

    private static void logStaminaDebugOncePerTick(Player player, String tag, float before, float after, float cost) {
        UUID id = player.getUUID();
        int tick = player.tickCount;
        Integer last = LAST_STAMINA_DEBUG_TICK.get(id);
        if (last != null && last == tick) {
            return;
        }
        LAST_STAMINA_DEBUG_TICK.put(id, tick);
        LOGGER.info(
                "[STAMINA] {}: player={} before={} after={} cost={} item={}",
                tag,
                player.getName().getString(),
                String.format(Locale.ROOT, "%.3f", before),
                String.format(Locale.ROOT, "%.3f", after),
                String.format(Locale.ROOT, "%.3f", cost),
                player.getMainHandItem().getItem()
        );
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

        int lockRemaining = getBowDrawLockTicksRemaining(player, data);
        if (lockRemaining > 0) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer) {
                showBowRetry(serverPlayer, lockRemaining);
                sync(serverPlayer);
            }
            return;
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

        float current = data.getCurrentStamina();
        if (current + 0.1f < cost) {
            event.setCanceled(true);
            startBowDrawLockIfNotActive(player, data);
            if (player instanceof ServerPlayer serverPlayer) {
                showBowRetry(serverPlayer, getBowDrawLockTicksRemaining(player, data));
                sync(serverPlayer);
            }
            return;
        }
        float next = Math.max(0.0f, current - cost);
        if (next != current) {
            data.setCurrentStamina(next);
            data.setRegenDelayTicksRemaining(Math.round(stats.regenDelayAfterConsumptionSeconds() * 20.0f));
        }
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

        boolean wasInCombat = data.isInCombat();
        tickCombat(data);
        if (wasInCombat && !data.isInCombat() && player instanceof ServerPlayer serverPlayer) {
            Integer lastExitTick = LAST_COMBAT_EXIT_TICK.get(serverPlayer.getUUID());
            if (lastExitTick == null || serverPlayer.tickCount - lastExitTick >= 20) {
                LAST_COMBAT_EXIT_TICK.put(serverPlayer.getUUID(), serverPlayer.tickCount);
                if (debugStamina() || debugStaminaEpicFight()) {
                    LOGGER.info("[STAMINA] exit combat: player={}", serverPlayer.getName().getString());
                }
            }
        }
        tickBowTension(player, data, stats);
        tickRegen(player, data, stats);

        if (player.tickCount % 20 == 0) {
            sync(player);
        } else {
            syncIfChanged(player);
        }
    }

    private static void tickCombat(StaminaData data) {
        CombatState.tickCombat(data);
    }

    private static void tickRegen(Player player, StaminaData data, StaminaComputedStats stats) {
        int regenDelay = data.getRegenDelayTicksRemaining();
        if (regenDelay > 0) {
            data.setRegenDelayTicksRemaining(regenDelay - 1);
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
        float multiplier = StaminaConfig.getRegenMultiplier(data.isInCombat());
        if (multiplier <= 0.0f) {
            return;
        }
        perTick *= multiplier;
        data.setCurrentStamina(Math.min(maxStamina, current + perTick));
    }

    private static void enterCombat(Player player, StaminaData data, String reason) {
        int timeoutTicks = StaminaConfig.getCombatTimeoutTicks();
        if (timeoutTicks <= 0) {
            return;
        }

        CombatState.EnterCombatResult result = CombatState.enterCombat(player, data, timeoutTicks, reason);
        if (!result.applied()) {
            return;
        }

        if ((debugStamina() || debugStaminaEpicFight()) && player instanceof ServerPlayer serverPlayer) {
            String previousReason = result.previousReason();
            if (!result.wasInCombat() || previousReason == null || !previousReason.equals(reason)) {
                LOGGER.info(
                        "[STAMINA] enter combat: player={} reason={} timeoutTicks={}",
                        serverPlayer.getName().getString(),
                        reason,
                        timeoutTicks
                );
            }
        }
    }

    private static void tickBowTension(Player player, StaminaData data, StaminaComputedStats stats) {
        if (!player.isUsingItem()) {
            return;
        }

        ItemStack useStack = player.getUseItem();
        if (!(useStack.getItem() instanceof BowItem)) {
            return;
        }

        int lockRemaining = getBowDrawLockTicksRemaining(player, data);
        if (lockRemaining > 0) {
            player.stopUsingItem();
            if (player instanceof ServerPlayer serverPlayer) {
                showBowRetry(serverPlayer, lockRemaining);
                sync(serverPlayer);
            }
            return;
        }

        float min = computeBowMinDrawCost(useStack, stats);
        if (min > 0.0f && data.getCurrentStamina() + 0.1f < min) {
            player.stopUsingItem();
            startBowDrawLockIfNotActive(player, data);
            if (player instanceof ServerPlayer serverPlayer) {
                showBowRetry(serverPlayer, getBowDrawLockTicksRemaining(player, data));
                sync(serverPlayer);
            }
            return;
        }

        int usedTicks = useStack.getUseDuration(player) - player.getUseItemRemainingTicks();
        float tensionSeconds = usedTicks / 20.0f;
        float bowWeight = (float) WeightRegistry.getItemWeight(useStack.getItem());
        float cost = StaminaConfig.computeBowHoldTickCost(tensionSeconds, bowWeight, stats);
        if (cost <= 0.0f) {
            return;
        }

        float current = data.getCurrentStamina();
        float next = current - cost;
        float floor = Math.max(0.0f, min);
        if (next < floor) {
            if (current != floor) {
                data.setCurrentStamina(floor);
                data.setRegenDelayTicksRemaining(Math.round(stats.regenDelayAfterConsumptionSeconds() * 20.0f));
            }
            player.stopUsingItem();
            startBowDrawLockIfNotActive(player, data);
            if (player instanceof ServerPlayer serverPlayer) {
                showBowRetry(serverPlayer, getBowDrawLockTicksRemaining(player, data));
                sync(serverPlayer);
            }
            return;
        }

        if (next != current) {
            data.setCurrentStamina(next);
            data.setRegenDelayTicksRemaining(Math.round(stats.regenDelayAfterConsumptionSeconds() * 20.0f));
        }
    }

    private static float computeAttackCost(Player attacker, StaminaComputedStats stats) {
        float base = StaminaConfig.getAttackBaseCost();
        float curve = 1.0f;
        if (StaminaConfig.isAttackUseWeaponWeight()) {
            curve = StaminaConfig.computeAttackCurveMultiplier((float) WeightRegistry.getItemWeight(attacker.getMainHandItem().getItem()));
        }
        return base * curve * stats.consumptionMultiplier() * stats.attackCostMultiplier();
    }

    public static float getMeleeAttackCost(Player player) {
        StaminaData data = player.getData(StaminaAttachments.STAMINA_DATA);
        StaminaComputedStats stats = computeStats(data);
        float maxStamina = stats.maxStamina();
        data.setMaxStamina(maxStamina);
        if (!data.isInitialized()) {
            data.setCurrentStamina(maxStamina);
            data.setInitialized(true);
        }
        return computeAttackCost(player, stats);
    }

    public static float getBowMinDrawCost(Player player, ItemStack bowStack) {
        if (!(bowStack.getItem() instanceof BowItem)) {
            return 0.0f;
        }

        StaminaData data = player.getData(StaminaAttachments.STAMINA_DATA);
        StaminaComputedStats stats = computeStats(data);
        float maxStamina = stats.maxStamina();
        data.setMaxStamina(maxStamina);
        if (!data.isInitialized()) {
            data.setCurrentStamina(maxStamina);
            data.setInitialized(true);
        }
        return computeBowMinDrawCost(bowStack, stats);
    }

    private static void showNoStamina(ServerPlayer player) {
        UUID id = player.getUUID();
        int tick = player.tickCount;
        Integer last = LAST_NO_STAMINA_TICK.get(id);
        if (last != null && tick - last < 5) {
            return;
        }
        LAST_NO_STAMINA_TICK.put(id, tick);
        player.displayClientMessage(Component.literal("§cStamina insufficiente (Server)"), true);
        player.playSound(SoundEvents.VILLAGER_NO, 0.6f, 1.0f);
    }

    private static float computeBowMinDrawCost(ItemStack bowStack, StaminaComputedStats stats) {
        float base = Math.max(0.0f, StaminaConfig.getBowBaseCost());
        if (base <= 0.0f || stats == null) {
            return 0.0f;
        }

        float bowWeight = (float) WeightRegistry.getItemWeight(bowStack.getItem());
        float weightCurve = 1.0f;
        if (StaminaConfig.isBowUseWeaponWeight()) {
            weightCurve = StaminaConfig.computeBowCurveMultiplier(bowWeight);
        }
        return base * weightCurve * stats.consumptionMultiplier() * stats.bowTensionCostMultiplier();
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
            PacketDistributor.sendToPlayer(serverPlayer, new StaminaSyncPacket(data.getCurrentStamina(), data.getMaxStamina(), data.isInCombat(), data.getBowDrawLockUntilGameTime()));

            UUID id = serverPlayer.getUUID();
            LAST_SENT_CURRENT.put(id, data.getCurrentStamina());
            LAST_SENT_MAX.put(id, data.getMaxStamina());
            LAST_SENT_IN_COMBAT.put(id, data.isInCombat());
            LAST_SENT_BOW_LOCK_UNTIL.put(id, data.getBowDrawLockUntilGameTime());
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
        long lastBowLockUntil = LAST_SENT_BOW_LOCK_UNTIL.getOrDefault(id, 0L);

        boolean changed =
                Float.isNaN(lastCurrent)
                        || Math.abs(lastCurrent - data.getCurrentStamina()) > 0.01f
                        || Float.isNaN(lastMax)
                        || Math.abs(lastMax - data.getMaxStamina()) > 0.01f
                        || lastInCombat != data.isInCombat()
                        || lastBowLockUntil != data.getBowDrawLockUntilGameTime();

        if (changed) {
            sync(serverPlayer);
        }
    }

    private static int getBowDrawLockTicksRemaining(Player player, StaminaData data) {
        long now = player.level().getGameTime();
        long until = data.getBowDrawLockUntilGameTime();
        long remaining = until - now;
        if (remaining <= 0L) {
            return 0;
        }
        return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
    }

    private static void startBowDrawLockIfNotActive(Player player, StaminaData data) {
        long now = player.level().getGameTime();
        if (data.getBowDrawLockUntilGameTime() > now) {
            return;
        }
        data.setBowDrawLockUntilGameTime(now + BOW_DRAW_LOCK_TICKS);
    }

    private static void showBowRetry(ServerPlayer player, int remainingTicks) {
        UUID id = player.getUUID();
        int tick = player.tickCount;
        Integer last = LAST_BOW_LOCK_MSG_TICK.get(id);
        if (last != null && tick - last < 5) {
            return;
        }
        LAST_BOW_LOCK_MSG_TICK.put(id, tick);
        float seconds = remainingTicks / 20.0f;
        String text = "§cRiprova tra " + String.format(Locale.ROOT, "%.1f", seconds) + "s";
        player.displayClientMessage(Component.literal(text), true);
    }
}
