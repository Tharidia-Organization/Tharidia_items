package com.THproject.tharidia_things.poison;

import com.THproject.tharidia_things.features.Revive;
import com.THproject.tharidia_things.features.Revive.FallState;

import net.minecraft.world.entity.player.Player;

public class PoisonHelper {
    public enum PoisonType {
        NONE,
        SOFT,
        HARD
    }
    
    public static final long SOFT_POISON_DURATION = 1000L * 40; // 40 seconds
    public static final long HARD_POISON_DURATION = 1000L * 20; // 20 seconds

    public static PoisonAttachments getAttachment(Player player) {
        if (player != null)
            return player.getData(PoisonAttachments.POISON.get());
        return null;
    }

    public static void calcPoisonProgress(Player player) {
        long currentTime = System.currentTimeMillis();
        PoisonAttachments attachment = getAttachment(player);
        if (attachment == null)
            return;

        switch (attachment.getPoisonType()) {
            case HARD:
                attachment.setProgress((float) (currentTime - attachment.getPoisonTime()) / HARD_POISON_DURATION);
                break;
            case SOFT:
                attachment.setProgress((float) (currentTime - attachment.getPoisonTime()) / SOFT_POISON_DURATION);
                break;
            case NONE:
                attachment.setProgress(0.0f);
                break;
            default:
                attachment.setProgress(0.0f);
                break;
        }
    }

    /**
     * Fall the player if the duration has exceeded.
     */
    public static void fallIfTimeExceed(Player player) {
        PoisonAttachments attachment = getAttachment(player);
        if (attachment == null)
            return;

        if (attachment.getProgress() >= 1.0f && !Revive.isPlayerFallen(player))
            Revive.fallPlayer(player, FallState.POISON);
    }

    public static void cure(Player player) {
        PoisonAttachments attachment = getAttachment(player);
        if (attachment == null)
            return;

        attachment.removePoison();
    }
}
