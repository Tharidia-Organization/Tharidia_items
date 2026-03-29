package com.THproject.tharidia_things.features;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class ModAnimations {
    public static AnimationAccessor<StaticAnimation> FALLEN_ANIMATION;

    @SubscribeEvent
    public static void registerAnimations(AnimationRegistryEvent event) {
        event.newBuilder(TharidiaThings.MODID, ModAnimations::build);
    }

    public static void build(AnimationBuilder builder) {
        FALLEN_ANIMATION = builder.nextAccessor("biped/fallen_animation",
                (accessor) -> new StaticAnimation(false, accessor, Armatures.BIPED));
    }

    public static void startAnimation(Player player, AnimationAccessor<? extends StaticAnimation> animation) {
        if (ModList.get().isLoaded("epicfight")) {
            PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
            if (playerPatch != null) {
                try {
                    playerPatch.playAnimationSynchronized(animation, 0.0f);
                } catch (Exception e) {
                    TharidiaThings.LOGGER.error("[EPIC FIGHT ANIMATION] failed to start player animation %s", animation.toString());
                }
            }
        }
    }

    public static void stopAnimation(Player player, AnimationAccessor<? extends StaticAnimation> animation) {
        if (ModList.get().isLoaded("epicfight")) {
            PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
            if (playerPatch != null) {
                try {
                    playerPatch.stopPlaying(animation);
                } catch (Exception e) {
                    TharidiaThings.LOGGER.error("[EPIC FIGHT ANIMATION] failed to stop player animation %s", animation.toString());
                }
            }
        }
    }
}
