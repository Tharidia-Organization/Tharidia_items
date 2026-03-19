package com.THproject.tharidia_things.features;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public class PlayerReviveEvent extends Event {
    private final Player player;
    private final Player target;

    /**
     * Fired when a player is revived.
     * 
     * @param player the player that revive the target (can be null if the revive was caused by something else, like a timer or command)
     * @param target the player that is being revived
     */
    public PlayerReviveEvent(Player player, Player target) {
        this.player = player;
        this.target = target;
    }

    public Player getPlayer() {
        return this.player;
    }

    public Player getTarget() {
        return this.target;
    }
}
