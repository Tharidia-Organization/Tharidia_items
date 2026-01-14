package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.features.Revive;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class ClientFallenHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) {
            return;
        }

        Player player = event.getEntity();

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
}
