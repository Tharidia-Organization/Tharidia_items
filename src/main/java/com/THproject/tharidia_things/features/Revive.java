package com.THproject.tharidia_things.features;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import com.THproject.tharidia_things.network.revive.ReviveSyncPayload;

import net.minecraft.world.entity.player.Player;

public class Revive {
    public static final List<UUID> fallenPlayers = new ArrayList<>();

    public static void fallPlayer(Player player, boolean can_revive) {
        if (!fallenPlayers.contains(player.getUUID()))
            fallenPlayers.add(player.getUUID());

        ReviveAttachments reviveAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
        reviveAttachments.resetResTime();
        reviveAttachments.setTimeFallen(0);
        reviveAttachments.setCanRevive(can_revive);
        reviveAttachments.setIsFallen(true);

        ModAnimations.startAnimation(player, ModAnimations.FALLEN_ANIMATION);
        
        ReviveSyncPayload.sync(player);
    }

    public static void revivePlayer(Player player) {
        revivePlayer(player, null);
    }

    public static void revivePlayer(Player player, Player reviver) {
        fallenPlayers.remove(player.getUUID());

        ReviveAttachments reviveAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
        reviveAttachments.setCanRevive(false);
        reviveAttachments.setIsFallen(false);

        ModAnimations.stopAnimation(player, ModAnimations.FALLEN_ANIMATION);

        ReviveSyncPayload.sync(player);
    }

    public static boolean isPlayerFallen(Player player) {
        if (player.level().isClientSide)
            return player.getData(ReviveAttachments.REVIVE_DATA.get()).isFallen();
        else
            return fallenPlayers.contains(player.getUUID());
    }

    public static List<UUID> getFallenPlayers() {
        return fallenPlayers;
    }
}
