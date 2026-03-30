package com.THproject.tharidia_things.features;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class ModAnimations {
    public static AnimationAccessor<StaticAnimation> FALLEN_ANIMATION;
    public static AnimationAccessor<StaticAnimation> FALLEN_LOOP_ANIMATION;

    @SubscribeEvent
    public static void registerAnimations(AnimationRegistryEvent event) {
        event.newBuilder(TharidiaThings.MODID, ModAnimations::build);
    }

    public static void build(AnimationBuilder builder) {
        FALLEN_LOOP_ANIMATION = builder.nextAccessor("biped/fallen_loop_animation",
                (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));

        FALLEN_ANIMATION = builder.nextAccessor("biped/fallen_animation",
                (accessor) -> {
                    StaticAnimation animation = new StaticAnimation(false, accessor, Armatures.BIPED);
                    animation.addEvents(AnimationEvent.InTimeEvent.create(0.5F, (entitypatch, self, params) -> {
                        if (entitypatch.getOriginal() instanceof Player player) {
                            startAnimation(player, FALLEN_LOOP_ANIMATION);
                        }
                    }, AnimationEvent.Side.CLIENT));
                    return animation;
                });
    }

    public static void startAnimation(Player player, AnimationAccessor<? extends StaticAnimation> animation) {
        if (ModList.get().isLoaded("epicfight") && animation != null) {
            PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
            if (playerPatch != null) {
                try {
                    playerPatch.playAnimationSynchronized(animation, 0.0f);
                } catch (Exception e) {
                    TharidiaThings.LOGGER.error("[EPIC FIGHT ANIMATION] failed to start player animation %s",
                            animation.toString());
                }
            }
        }
    }

    public static void reserveAnimation(Player player, AnimationAccessor<? extends StaticAnimation> animation) {
        if (ModList.get().isLoaded("epicfight") && animation != null) {
            PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
            if (playerPatch != null) {
                try {
                    playerPatch.reserveAnimation(animation);
                } catch (Exception e) {
                    TharidiaThings.LOGGER.error("[EPIC FIGHT ANIMATION] failed to reserve player animation %s",
                            animation.toString());
                }
            }
        }
    }

    public static void stopAnimation(Player player, AnimationAccessor<? extends StaticAnimation> animation) {
        if (ModList.get().isLoaded("epicfight") && animation != null) {
            PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
            if (playerPatch != null) {
                try {
                    playerPatch.stopPlaying(animation);
                } catch (Exception e) {
                    TharidiaThings.LOGGER.error("[EPIC FIGHT ANIMATION] failed to stop player animation %s",
                            animation.toString());
                }
            }
        }
    }

    public static void startFallenAnimation(Player player) {
        startAnimation(player, FALLEN_ANIMATION);
        // reserveAnimation(player, FALLEN_LOOP_ANIMATION);
    }

    public static void stopFallenAnimation(Player player) {
        stopAnimation(player, FALLEN_ANIMATION);
        stopAnimation(player, FALLEN_LOOP_ANIMATION);
    }
}
