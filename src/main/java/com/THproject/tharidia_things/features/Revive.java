package com.THproject.tharidia_things.features;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.ReviveAttachments;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class Revive {
    public static final List<UUID> fallenPlayers = new ArrayList<>();

    public static final ResourceLocation FREEZE_MOVEMENT_ID = ResourceLocation
            .fromNamespaceAndPath(TharidiaThings.MODID, "freeze_movement");
    public static final ResourceLocation FREEZE_JUMP_ID = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID,
            "freeze_jump");

    public static void fallPlayer(Player player, boolean can_revive) {
        if (!fallenPlayers.contains(player.getUUID()))
            fallenPlayers.add(player.getUUID());
        player.setForcedPose(Pose.SWIMMING);
        player.setSwimming(true);

        ReviveAttachments reviveAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
        reviveAttachments.resetResTime();
        reviveAttachments.setCanRevive(can_revive);
        reviveAttachments.setInvulnerabilityTick(player.tickCount);

        // Apply attributes to prevent movement and jumping
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null && movement.getModifier(FREEZE_MOVEMENT_ID) == null) {
            movement.addTransientModifier(
                    new AttributeModifier(FREEZE_MOVEMENT_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }

        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null && jump.getModifier(FREEZE_JUMP_ID) == null) {
            jump.addTransientModifier(
                    new AttributeModifier(FREEZE_JUMP_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void revivePlayer(Player player) {
        fallenPlayers.remove(player.getUUID());
        player.setForcedPose(null);
        player.setSwimming(false);

        // Remove attributes
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(FREEZE_MOVEMENT_ID);
        }

        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) {
            jump.removeModifier(FREEZE_JUMP_ID);
        }
    }

    public static boolean isPlayerFallen(Player player) {
        return fallenPlayers.contains(player.getUUID());
    }
}
