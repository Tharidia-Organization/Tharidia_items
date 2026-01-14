package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.event.StaminaHandler;
import com.THproject.tharidia_things.network.MeleeSwingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-side input handler that blocks attack input BEFORE the swing animation.
 * This prevents the visual swing when attacking with restricted weapons.
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class ClientInputHandler {
    private static int LAST_NO_STAMINA_TICK = -999999;
    
    /**
     * Intercepts attack input (left click) before swing animation
     */
    @SubscribeEvent
    public static void onAttackInput(InputEvent.InteractionKeyMappingTriggered event) {
        // Only care about attack key
        if (event.isAttack()) {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null) return;
            if (!player.isCreative()) {
                float cost = StaminaHandler.getMeleeAttackCost(player);
                if (cost > 0.0f) {
                    float current = player.getData(com.THproject.tharidia_things.stamina.StaminaAttachments.STAMINA_DATA).getCurrentStamina();
                    if (current < cost + 0.1f) {
                        int tick = player.tickCount;
                        if (tick - LAST_NO_STAMINA_TICK >= 5) {
                            LAST_NO_STAMINA_TICK = tick;
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cStamina insufficiente (Client)"), true);
                            TharidiaThings.LOGGER.debug("Client blocked attack: stamina {} < cost {}", current, cost);
                        }
                        event.setCanceled(true);
                        event.setSwingHand(false);
                        return;
                    }
                }
            }
            
            var mainHand = player.getMainHandItem();
            
            // Check if the weapon is blocked in client cache
            if (ClientGateCache.isBlocked(mainHand)) {
                // Cancel the input -> NO swing animation
                event.setCanceled(true);
                event.setSwingHand(false);
                
                TharidiaThings.LOGGER.debug(
                    "Client blocked attack input with {} (cached as restricted)",
                    mainHand.getItem()
                );
                return;
            }

            HitResult hitResult = minecraft.hitResult;
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                return;
            }
            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                return;
            }

            PacketDistributor.sendToServer(new MeleeSwingPacket());
        }
    }

    /**
     * Extra safety: Intercept raw mouse input to prevent attacks even if key mapping fails
     */
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == 1 && Minecraft.getInstance().screen == null) {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null || player.isCreative()) return;

            if (event.getButton() == 0) {
                float cost = StaminaHandler.getMeleeAttackCost(player);
                if (cost > 0.0f) {
                    float current = player.getData(com.THproject.tharidia_things.stamina.StaminaAttachments.STAMINA_DATA).getCurrentStamina();
                    if (current < cost + 0.1f) {
                        event.setCanceled(true);
                        TharidiaThings.LOGGER.debug("Client blocked mouse input: stamina {} < cost {}", current, cost);
                    }
                }
                return;
            }

            if (event.getButton() == 1) {
                HitResult hitResult = minecraft.hitResult;
                if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                    return;
                }

                ItemStack stack = player.getMainHandItem();
                if (!(stack.getItem() instanceof BowItem)) {
                    stack = player.getOffhandItem();
                    if (!(stack.getItem() instanceof BowItem)) {
                        return;
                    }
                }

                long now = player.level().getGameTime();
                long until = player.getData(com.THproject.tharidia_things.stamina.StaminaAttachments.STAMINA_DATA).getBowDrawLockUntilGameTime();
                long remaining = until - now;
                if (remaining > 0L) {
                    int tick = player.tickCount;
                    if (tick - LAST_NO_STAMINA_TICK >= 5) {
                        LAST_NO_STAMINA_TICK = tick;
                        float seconds = remaining / 20.0f;
                        player.displayClientMessage(Component.literal("§cRiprova tra " + String.format(java.util.Locale.ROOT, "%.1f", seconds) + "s"), true);
                    }
                    event.setCanceled(true);
                    return;
                }

                float min = StaminaHandler.getBowMinDrawCost(player, stack);
                if (min > 0.0f) {
                    float current = player.getData(com.THproject.tharidia_things.stamina.StaminaAttachments.STAMINA_DATA).getCurrentStamina();
                    if (current < min + 0.1f) {
                        int tick = player.tickCount;
                        if (tick - LAST_NO_STAMINA_TICK >= 5) {
                            LAST_NO_STAMINA_TICK = tick;
                            player.displayClientMessage(Component.literal("§cRiprova tra 2.0s"), true);
                        }
                        long lockUntil = player.level().getGameTime() + 40L;
                        var data = player.getData(com.THproject.tharidia_things.stamina.StaminaAttachments.STAMINA_DATA);
                        if (data.getBowDrawLockUntilGameTime() <= player.level().getGameTime()) {
                            data.setBowDrawLockUntilGameTime(lockUntil);
                        }
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
}
