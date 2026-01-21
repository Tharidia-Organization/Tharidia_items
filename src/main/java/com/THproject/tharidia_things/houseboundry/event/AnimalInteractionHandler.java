package com.THproject.tharidia_things.houseboundry.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.houseboundry.AnimalWellnessAttachments;
import com.THproject.tharidia_things.houseboundry.AnimalWellnessData;
import com.THproject.tharidia_things.houseboundry.config.AnimalConfigRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles player interactions with animals for Houseboundry system.
 * - Honey: 60% cure chance for disease
 * - Medicine item: 90% cure chance for disease
 */
@EventBusSubscriber(modid = TharidiaThings.MODID)
public class AnimalInteractionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimalInteractionHandler.class);

    // Cure chances
    private static final float HONEY_CURE_CHANCE = 0.60f;
    private static final float MEDICINE_CURE_CHANCE = 0.90f;

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) {
            return;
        }

        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        if (target instanceof Player) {
            return;
        }

        // Check if target is an animal or has wellness config
        boolean isAnimal = target instanceof Animal;
        boolean hasConfig = AnimalConfigRegistry.hasConfig(target.getType());

        if (!isAnimal && !hasConfig) {
            return;
        }

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        // Get wellness data
        AnimalWellnessData data = target.getData(AnimalWellnessAttachments.WELLNESS_DATA);

        // Honey bottle - cure disease (60% chance)
        if (stack.is(Items.HONEY_BOTTLE) && data.isDiseased()) {
            if (tryHealDisease(target, data, HONEY_CURE_CHANCE)) {
                // Success
                stack.shrink(1);
                player.addItem(new ItemStack(Items.GLASS_BOTTLE));
                player.level().playSound(null, target.blockPosition(),
                    SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 1.0F, 1.0F);
                LOGGER.info("{} cured with honey!", EntityType.getKey(target.getType()));
            } else {
                // Failed cure
                stack.shrink(1);
                player.addItem(new ItemStack(Items.GLASS_BOTTLE));
                player.level().playSound(null, target.blockPosition(),
                    SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 1.0F, 0.8F);
                LOGGER.debug("{} honey cure failed", EntityType.getKey(target.getType()));
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        // TODO: Add medicine item cure (90% chance) when medicine item is created
        // if (stack.is(TharidiaThings.ANIMAL_MEDICINE.get()) && data.isDiseased()) { ... }
    }

    /**
     * Attempts to heal disease with given chance.
     * @return true if disease was cured
     */
    private static boolean tryHealDisease(LivingEntity entity, AnimalWellnessData data, float chance) {
        if (!data.isDiseased()) {
            return false;
        }

        if (entity.level().random.nextFloat() < chance) {
            data.cureDisease();
            // Also reduce stress from cure
            data.addStress(-20);
            return true;
        }

        return false;
    }
}
