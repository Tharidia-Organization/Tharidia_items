package com.THproject.tharidia_things.features;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import com.THproject.tharidia_things.network.revive.ReviveSyncPayload;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class Revive {
    public enum FallState {
        NONE(false),
        /** Fallen from death **/
        DEATH(true, Items.STICK),
        /** Fallen from battle with battle gauntlet **/
        BATTLE(false),
        /** Fallen from poison food effect **/
        POISON(true, Items.POISONOUS_POTATO);

        public final boolean canRevive;
        public final Item itemToRevive;

        FallState(boolean canRevive) {
            this(canRevive, null);
        }

        FallState(boolean canRevive, @Nullable Item itemToRevive) {
            this.canRevive = canRevive;
            this.itemToRevive = itemToRevive;
        }
    };

    public static final List<UUID> fallenPlayers = new ArrayList<>();

    public static void fallPlayer(Player player, FallState fallState) {
        if (!fallenPlayers.contains(player.getUUID()))
            fallenPlayers.add(player.getUUID());

        ReviveAttachments reviveAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
        reviveAttachments.resetResTime();
        reviveAttachments.setTimeFallen(0);
        reviveAttachments.setFallState(fallState);
        reviveAttachments.setIsFallen(true);

        ModAnimations.startFallenAnimation(player);

        ReviveSyncPayload.sync(player);
    }

    public static void revivePlayer(Player player) {
        revivePlayer(player, null);
    }

    public static void revivePlayer(Player player, Player reviver) {
        fallenPlayers.remove(player.getUUID());

        ReviveAttachments reviveAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
        reviveAttachments.setFallState(FallState.NONE);
        reviveAttachments.setIsFallen(false);

        ModAnimations.stopFallenAnimation(player);

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
