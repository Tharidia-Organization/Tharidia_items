package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.client.screen.FallenScreen;
import com.THproject.tharidia_things.features.Revive;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class ClientFallenHandler {

    private static boolean isBlurActive = false;
    private static CameraType lastCameraType = null;
    private static final ResourceLocation BLUR_SHADER = ResourceLocation.withDefaultNamespace("shaders/post/blur.json");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) {
            return;
        }

        Player player = event.getEntity();

        // Check if this is the client player
        if (player == Minecraft.getInstance().player) {
            checkCameraChange();
            handleClientPlayerBlur(player);
            handleFallenScreen(player);
        }

        // Check if player has the fallen freeze attribute
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null && movement.getModifier(Revive.FREEZE_MOVEMENT_ID) != null) {
            // Player is fallen - force pose on client side
            if (player.getForcedPose() != Pose.SWIMMING) {
                player.setForcedPose(Pose.SWIMMING);
            }
            if (!player.isSwimming()) {
                player.setSwimming(true);
            }
        } else {
            // Player is not fallen - ensure normal pose
            if (player.getForcedPose() == Pose.SWIMMING) {
                player.setForcedPose(null);
            }
            if (player.isSwimming()) {
                player.setSwimming(false);
            }
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (isBlurActive) {
            if (Minecraft.getInstance().gameRenderer != null) {
                Minecraft.getInstance().gameRenderer.shutdownEffect();
            }
            isBlurActive = false;
        }
    }

    // Helper to check camera
    private static void checkCameraChange() {
        CameraType currentType = Minecraft.getInstance().options.getCameraType();
        if (lastCameraType != currentType) {
            lastCameraType = currentType;
            if (isBlurActive) {
                // Force reload if we think it should be active but camera changed
                Minecraft.getInstance().gameRenderer.loadEffect(BLUR_SHADER);
            }
        }
    }

    private static void handleClientPlayerBlur(Player player) {
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        boolean isFallen = movement != null && movement.getModifier(Revive.FREEZE_MOVEMENT_ID) != null;

        if (isFallen) {
            if (!isBlurActive) {
                Minecraft.getInstance().gameRenderer.loadEffect(BLUR_SHADER);
                isBlurActive = true;
            }
        } else {
            if (isBlurActive) {
                Minecraft.getInstance().gameRenderer.shutdownEffect();
                isBlurActive = false;
            }
        }
    }

    private static void handleFallenScreen(Player player) {
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        boolean isFallen = movement != null && movement.getModifier(Revive.FREEZE_MOVEMENT_ID) != null;
        Minecraft mc = Minecraft.getInstance();

        if (isFallen) {
            if (!(mc.screen instanceof FallenScreen)) {
                mc.setScreen(new FallenScreen());
            }
        } else {
            if (mc.screen instanceof FallenScreen) {
                mc.setScreen(null);
            }
        }
    }
}
