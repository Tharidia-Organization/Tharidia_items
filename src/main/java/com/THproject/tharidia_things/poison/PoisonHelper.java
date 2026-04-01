package com.THproject.tharidia_things.poison;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class PoisonHelper {
    public static final long SOFT_POISON_DURATION = 1000L * 40; // 40 seconds
    public static final long HARD_POISON_DURATION = 1000L * 20; // 20 seconds

    public static PoisonAttachments getAttachment(Player player) {
        if (player != null)
            return player.getData(PoisonAttachments.POISON.get());
        return null;
    }

    public static void applyNauseaEffect(Player player) {
        PoisonAttachments attachments = getAttachment(player);
        if (attachments == null)
            return;

        float progress = 0.0f;
        if (attachments.isHardPoisoned()) {
            progress = attachments.getHardProgress();
        } else if (attachments.isSoftPoisoned()) {
            progress = attachments.getSoftProgress();
        }

        if (progress >= 0.66f) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false));
        }
    }

    public static void calcPoisonProgress(Player player) {
        long currentTime = System.currentTimeMillis();
        PoisonAttachments attachment = getAttachment(player);
        if (attachment == null)
            return;

        if (attachment.isSoftPoisoned()) {
            attachment.setSoftProgress(
                    Math.min(1.0f, (float) (currentTime - attachment.getSoftPoisonTime()) / SOFT_POISON_DURATION));
        } else {
            attachment.setSoftProgress(0.0f);
        }

        if (attachment.isHardPoisoned()) {
            attachment.setHardProgress(
                    Math.min(1.0f, (float) (currentTime - attachment.getHardPoisonTime()) / HARD_POISON_DURATION));
        } else {
            attachment.setHardProgress(0.0f);
        }
    }

    /**
     * Remove poison stat if the duration has exceeded. This should be called every
     * tick to ensure timely removal of poison effects.
     */
    public static void removeIfTimeExceed(Player player) {
        long currentTime = System.currentTimeMillis();
        PoisonAttachments attachment = getAttachment(player);
        if (attachment == null)
            return;

        if (attachment.isSoftPoisoned() && currentTime - attachment.getSoftPoisonTime() >= SOFT_POISON_DURATION) {
            attachment.removeSoftPoison();
        }
        if (attachment.isHardPoisoned() && currentTime - attachment.getHardPoisonTime() >= HARD_POISON_DURATION) {
            attachment.removeHardPoison();
        }

    }
}
